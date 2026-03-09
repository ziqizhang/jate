"""Public API for JATE term extraction.

Provides high-level functions that wire together NLP backends, candidate
extractors, corpus stores, and scoring algorithms into a single call.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from jate.algorithms import (
    ATTF,
    RAKE,
    RIDF,
    TFIDF,
    TTF,
    Algorithm,
    Basic,
    ChiSquare,
    ComboBasic,
    CValue,
    GlossEx,
    NCValue,
    TermEx,
    Weirdness,
)
from jate.extractors.base import CandidateExtractorBase
from jate.extractors.ngram import NGramExtractor
from jate.extractors.noun_phrase import NounPhraseExtractor
from jate.extractors.pos_pattern import PosPatternExtractor
from jate.models import Candidate, TermExtractionResult
from jate.nlp.document_loader import DocumentLoader
from jate.nlp.spacy_backend import SpacyBackend
from jate.store.memory_store import MemoryCorpusStore
from jate.store.sqlite_store import SQLiteCorpusStore

# -----------------------------------------------------------------------
# Algorithm and extractor name registries
# -----------------------------------------------------------------------

_ALGORITHM_NAMES: dict[str, type[Algorithm]] = {
    "tfidf": TFIDF,
    "cvalue": CValue,
    "ncvalue": NCValue,
    "attf": ATTF,
    "ttf": TTF,
    "basic": Basic,
    "combobasic": ComboBasic,
    "chi_square": ChiSquare,
    "rake": RAKE,
    "ridf": RIDF,
    "weirdness": Weirdness,
    "termex": TermEx,
    "glossex": GlossEx,
}

_EXTRACTOR_NAMES: dict[str, type[CandidateExtractorBase]] = {
    "pos_pattern": PosPatternExtractor,
    "ngram": NGramExtractor,
    "noun_phrase": NounPhraseExtractor,
}


def _available_algorithm_names() -> str:
    return ", ".join(sorted(_ALGORITHM_NAMES.keys()))


def _available_extractor_names() -> str:
    return ", ".join(sorted(_EXTRACTOR_NAMES.keys()))


# -----------------------------------------------------------------------
# Helpers to build RAKE / Weirdness / etc. from candidates + store
# -----------------------------------------------------------------------


def _build_context_index(
    candidates: list[Candidate],
    documents: list[Any],
    nlp: SpacyBackend,
) -> Any:
    """Build ContextIndex if candidates have sentence-level data."""
    from jate.context import ContextIndex

    has_sentences = any(
        len(pos) >= 3 and pos[2] >= 0
        for c in candidates
        for positions in c.doc_positions.values()
        for pos in positions
    )
    if not has_sentences:
        return None
    return ContextIndex.build(candidates, documents, nlp_backend=nlp)


def _build_word_frequencies(
    candidates: list[Candidate],
    store: MemoryCorpusStore | SQLiteCorpusStore,
) -> dict[str, int]:
    """Build a word-level frequency dict from candidates."""
    word_freq: dict[str, int] = {}
    for cand in candidates:
        for word in cand.normalized_form.split():
            if word not in word_freq:
                word_freq[word] = store.get_term_frequency(word)
    return word_freq


def _build_term_component_index(
    candidates: list[Candidate],
) -> dict[str, list[tuple[str, int]]]:
    """Build RAKE's term_component_index: word -> [(parent_term, word_count)]."""
    index: dict[str, list[tuple[str, int]]] = {}
    for cand in candidates:
        words = cand.normalized_form.split()
        wc = len(words)
        for w in words:
            index.setdefault(w, []).append((cand.normalized_form, wc))
    return index


def _build_frequent_terms(
    candidates: list[Candidate],
    store: MemoryCorpusStore | SQLiteCorpusStore,
) -> dict[str, float]:
    """Build ChiSquare's frequent_terms: term -> expected probability."""
    corpus_total = store.get_corpus_total()
    if corpus_total == 0:
        corpus_total = 1
    freq_terms: dict[str, float] = {}
    for cand in candidates:
        nf = cand.normalized_form
        tf = store.get_term_frequency(nf)
        if tf > 0:
            freq_terms[nf] = tf / corpus_total
    return freq_terms


def _build_reference_data(
    candidates: list[Candidate],
    store: MemoryCorpusStore | SQLiteCorpusStore,
) -> tuple[dict[str, int], int]:
    """Build a simple reference frequency dict from the corpus itself.

    Used as a fallback when no external reference corpus is provided.
    Filters out zero-frequency entries to avoid division-by-zero in
    algorithms that compute null probabilities from min(values).
    """
    word_freq = _build_word_frequencies(candidates, store)
    # Remove zero-frequency entries so min(values) is always > 0
    word_freq = {k: v for k, v in word_freq.items() if v > 0}
    total = sum(word_freq.values()) if word_freq else 1
    return word_freq, total


# -----------------------------------------------------------------------
# Resolver helpers
# -----------------------------------------------------------------------


def _resolve_algorithm(
    name: str,
    candidates: list[Candidate] | None = None,
    store: MemoryCorpusStore | SQLiteCorpusStore | None = None,
    **kwargs: Any,
) -> Algorithm:
    """Map a string algorithm name to an Algorithm instance.

    For algorithms that require extra constructor arguments (RAKE, Weirdness,
    ChiSquare, TermEx, GlossEx), the needed data is auto-built from *candidates*
    and *store* unless explicitly provided in *kwargs*.
    """
    key = name.lower().strip()
    if key not in _ALGORITHM_NAMES:
        raise ValueError(f"Unknown algorithm {name!r}. Available: {_available_algorithm_names()}")

    cls = _ALGORITHM_NAMES[key]

    # Algorithms that need special constructor args
    if cls is RAKE:
        if "word_frequencies" not in kwargs or "term_component_index" not in kwargs:
            if candidates is None or store is None:
                raise ValueError("RAKE requires candidates and store for auto-building word_frequencies")
            kwargs.setdefault("word_frequencies", _build_word_frequencies(candidates, store))
            kwargs.setdefault("term_component_index", _build_term_component_index(candidates))
        return RAKE(**kwargs)

    if cls is Weirdness:
        if "reference_frequencies" not in kwargs or "reference_total" not in kwargs:
            if candidates is None or store is None:
                raise ValueError(
                    "Weirdness requires candidates and store (or explicit reference_frequencies/reference_total)"
                )
            ref_freq, ref_total = _build_reference_data(candidates, store)
            kwargs.setdefault("reference_frequencies", ref_freq)
            kwargs.setdefault("reference_total", ref_total)
        if candidates is not None and store is not None:
            kwargs.setdefault("word_frequencies", _build_word_frequencies(candidates, store))
        return Weirdness(**kwargs)

    if cls is ChiSquare:
        if "frequent_terms" not in kwargs:
            if candidates is None or store is None:
                raise ValueError("ChiSquare requires candidates and store (or explicit frequent_terms)")
            kwargs.setdefault("frequent_terms", _build_frequent_terms(candidates, store))
        return ChiSquare(**kwargs)

    if cls is TermEx:
        if "reference_frequencies" not in kwargs or "reference_total" not in kwargs:
            if candidates is None or store is None:
                raise ValueError("TermEx requires candidates and store (or explicit reference data)")
            ref_freq, ref_total = _build_reference_data(candidates, store)
            kwargs.setdefault("reference_frequencies", ref_freq)
            kwargs.setdefault("reference_total", ref_total)
        if candidates is not None and store is not None:
            kwargs.setdefault("word_frequencies", _build_word_frequencies(candidates, store))
        return TermEx(**kwargs)

    if cls is GlossEx:
        if "reference_frequencies" not in kwargs or "reference_total" not in kwargs:
            if candidates is None or store is None:
                raise ValueError("GlossEx requires candidates and store (or explicit reference data)")
            ref_freq, ref_total = _build_reference_data(candidates, store)
            kwargs.setdefault("reference_frequencies", ref_freq)
            kwargs.setdefault("reference_total", ref_total)
        if candidates is not None and store is not None:
            kwargs.setdefault("word_frequencies", _build_word_frequencies(candidates, store))
        return GlossEx(**kwargs)

    # Simple algorithms: filter out keys not accepted by the constructor
    # Basic accepts alpha; ComboBasic accepts alpha, beta; NCValue accepts weights
    # Others take no args. We pass kwargs and let Python raise if invalid.
    # For safety, only pass kwargs that the constructor accepts.
    import inspect

    sig = inspect.signature(cls.__init__)
    valid_params = set(sig.parameters.keys()) - {"self"}
    filtered = {k: v for k, v in kwargs.items() if k in valid_params}
    return cls(**filtered)


def _resolve_extractor(name: str, **kwargs: Any) -> CandidateExtractorBase:
    """Map a string extractor name to an extractor instance."""
    key = name.lower().strip()
    if key not in _EXTRACTOR_NAMES:
        raise ValueError(f"Unknown extractor {name!r}. Available: {_available_extractor_names()}")
    cls = _EXTRACTOR_NAMES[key]
    import inspect

    sig = inspect.signature(cls.__init__)
    valid_params = set(sig.parameters.keys()) - {"self"}
    filtered = {k: v for k, v in kwargs.items() if k in valid_params}
    return cls(**filtered)


# -----------------------------------------------------------------------
# Public API
# -----------------------------------------------------------------------


def extract(
    text: str,
    *,
    algorithm: str = "cvalue",
    model: str = "en_core_web_sm",
    extractor: str = "pos_pattern",
    min_frequency: int = 1,
    min_words: int = 1,
    max_words: int | None = None,
    **algo_kwargs: Any,
) -> TermExtractionResult:
    """Extract terms from a single text string.

    Parameters
    ----------
    text:
        Raw text to extract terms from.
    algorithm:
        Scoring algorithm name (e.g. ``"cvalue"``, ``"tfidf"``).
    model:
        spaCy model name.
    extractor:
        Candidate extractor name (``"pos_pattern"``, ``"ngram"``, ``"noun_phrase"``).
    min_frequency:
        Minimum term frequency to include.
    min_words:
        Minimum number of words in a term.
    max_words:
        Maximum number of words in a term (None = no limit).
    **algo_kwargs:
        Extra keyword arguments forwarded to the algorithm constructor.

    Returns
    -------
    TermExtractionResult
        Scored and sorted terms.
    """
    nlp = SpacyBackend(model)
    documents = DocumentLoader.load_texts([text])
    store = MemoryCorpusStore()
    ext = _resolve_extractor(extractor)
    candidates = ext.extract(documents, nlp, store)
    store.index_candidates(candidates)

    context_index = _build_context_index(candidates, documents, nlp)

    algo = _resolve_algorithm(algorithm, candidates=candidates, store=store, **algo_kwargs)
    result = algo.score(candidates, store, context_index=context_index)

    # Apply filters
    result = result.filter_by_frequency(min_frequency)
    result = result.filter_by_length(min_words=min_words, max_words=max_words)

    # Assign ranks
    for i, term in enumerate(result):
        term.rank = i + 1

    return result


def extract_corpus(
    sources: list[str] | str,
    *,
    algorithm: str = "cvalue",
    model: str = "en_core_web_sm",
    extractor: str = "pos_pattern",
    db_path: str | None = None,
    min_frequency: int = 2,
    min_words: int = 1,
    max_words: int | None = None,
    **algo_kwargs: Any,
) -> TermExtractionResult:
    """Extract terms from a corpus of documents.

    Parameters
    ----------
    sources:
        A list of file paths, a list of raw text strings, or a single
        directory path string.
    algorithm:
        Scoring algorithm name.
    model:
        spaCy model name.
    extractor:
        Candidate extractor name.
    db_path:
        If given, uses SQLiteCorpusStore for persistence.
    min_frequency:
        Minimum term frequency to include.
    min_words:
        Minimum number of words in a term.
    max_words:
        Maximum number of words in a term (None = no limit).
    **algo_kwargs:
        Extra keyword arguments forwarded to the algorithm constructor.

    Returns
    -------
    TermExtractionResult
        Scored and sorted terms.
    """
    nlp = SpacyBackend(model)

    # Detect input type and load documents
    documents = _load_sources(sources)

    # Create store
    if db_path is not None:
        store: MemoryCorpusStore | SQLiteCorpusStore = SQLiteCorpusStore(db_path)
    else:
        store = MemoryCorpusStore()

    ext = _resolve_extractor(extractor)
    candidates = ext.extract(documents, nlp, store)
    store.index_candidates(candidates)

    context_index = _build_context_index(candidates, documents, nlp)

    algo = _resolve_algorithm(algorithm, candidates=candidates, store=store, **algo_kwargs)
    result = algo.score(candidates, store, context_index=context_index)

    # Apply filters
    result = result.filter_by_frequency(min_frequency)
    result = result.filter_by_length(min_words=min_words, max_words=max_words)

    # Assign ranks
    for i, term in enumerate(result):
        term.rank = i + 1

    return result


def compare(
    sources: list[str] | str,
    *,
    algorithms: list[str] | None = None,
    model: str = "en_core_web_sm",
    extractor: str = "pos_pattern",
    min_frequency: int = 1,
    min_words: int = 1,
    max_words: int | None = None,
    **kwargs: Any,
) -> dict[str, TermExtractionResult]:
    """Run multiple algorithms on the same corpus and compare results.

    Parameters
    ----------
    sources:
        Input sources (same as :func:`extract_corpus`).
    algorithms:
        List of algorithm names. Defaults to
        ``["tfidf", "cvalue", "rake", "ridf", "basic"]``.
    model:
        spaCy model name.
    extractor:
        Candidate extractor name.
    **kwargs:
        Extra keyword arguments forwarded to each algorithm constructor.

    Returns
    -------
    dict[str, TermExtractionResult]
        Mapping of algorithm name to its scored results.
    """
    if algorithms is None:
        algorithms = ["tfidf", "cvalue", "rake", "ridf", "basic"]

    nlp = SpacyBackend(model)
    documents = _load_sources(sources)
    store = MemoryCorpusStore()

    ext = _resolve_extractor(extractor)
    candidates = ext.extract(documents, nlp, store)
    store.index_candidates(candidates)

    context_index = _build_context_index(candidates, documents, nlp)

    results: dict[str, TermExtractionResult] = {}
    for algo_name in algorithms:
        algo = _resolve_algorithm(algo_name, candidates=candidates, store=store, **kwargs)
        result = algo.score(candidates, store, context_index=context_index)
        result = result.filter_by_frequency(min_frequency)
        result = result.filter_by_length(min_words=min_words, max_words=max_words)
        for i, term in enumerate(result):
            term.rank = i + 1
        results[algo_name] = result

    return results


# -----------------------------------------------------------------------
# Internal helpers
# -----------------------------------------------------------------------


def _load_sources(sources: list[str] | str) -> list[Any]:
    """Detect input type and load documents accordingly."""
    from jate.models import Document

    if isinstance(sources, str):
        # Single string: check if it's a directory
        p = Path(sources)
        if p.is_dir():
            return DocumentLoader.load_directory(p)
        elif p.is_file():
            return [DocumentLoader.load_file(p)]
        else:
            # Treat as raw text
            return DocumentLoader.load_texts([sources])

    # List of strings
    if not sources:
        return []

    # Check if all look like existing file paths
    paths = [Path(s) for s in sources]
    if all(p.is_file() for p in paths):
        return [DocumentLoader.load_file(p) for p in paths]

    # Otherwise treat as raw texts
    return DocumentLoader.load_texts(sources)
