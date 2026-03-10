"""Public API for JATE term extraction.

Provides high-level functions that wire together NLP backends, candidate
extractors, and scoring algorithms into a single call.

Feature objects (TermFrequency, WordFrequency, etc.) are built lazily —
only the features required by the chosen algorithm are constructed.
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
    Voting,
    Weirdness,
)
from jate.config import JATEConfig
from jate.extractors.base import CandidateExtractorBase
from jate.extractors.ngram import NGramExtractor
from jate.extractors.noun_phrase import NounPhraseExtractor
from jate.extractors.pos_pattern import PosPatternExtractor
from jate.features import (
    ChiSquareFrequentTerms,
    Containment,
    ContextFrequency,
    Cooccurrence,
    ReferenceFrequency,
    TermComponentIndex,
    TermFrequency,
    WordFrequency,
)
from jate.models import Candidate, TermExtractionResult
from jate.nlp.document_loader import DocumentLoader
from jate.nlp.spacy_backend import SpacyBackend

# Store imports — only needed as a CorpusStore argument for extractors,
# which still require add_document().  Will be removed once extractors
# are updated to work without a store.
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

# Which algorithms need which features (beyond term_freq, which all get).
_NEEDS_CONTAINMENT: tuple[type, ...] = (CValue, Basic, NCValue)
_NEEDS_CHILD_CONTAINMENT: tuple[type, ...] = (ComboBasic,)
_NEEDS_CONTEXT_FREQ: tuple[type, ...] = (ChiSquare, NCValue)
_NEEDS_WORD_FREQ: tuple[type, ...] = (RAKE, Weirdness, GlossEx, TermEx)
_NEEDS_REF_FREQ: tuple[type, ...] = (Weirdness, GlossEx, TermEx)
_NEEDS_CHI_SQUARE_FEATURES: tuple[type, ...] = (ChiSquare,)
_NEEDS_TCI: tuple[type, ...] = (RAKE,)


def _available_algorithm_names() -> str:
    return ", ".join(sorted(_ALGORITHM_NAMES.keys()))


def _available_extractor_names() -> str:
    return ", ".join(sorted(_EXTRACTOR_NAMES.keys()))


# -----------------------------------------------------------------------
# Resolver helpers
# -----------------------------------------------------------------------


def _resolve_algorithm(name: str, **kwargs: Any) -> Algorithm:
    """Map a string algorithm name to an Algorithm instance.

    Only constructor-level parameters (e.g. alpha, beta, weights) are
    forwarded.  Feature objects are passed to ``score()`` instead.
    """
    key = name.lower().strip()
    if key not in _ALGORITHM_NAMES:
        raise ValueError(f"Unknown algorithm {name!r}. Available: {_available_algorithm_names()}")

    cls = _ALGORITHM_NAMES[key]

    # Filter kwargs to only those accepted by the constructor.
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
# Feature building
# -----------------------------------------------------------------------


def _build_features(
    algo: Algorithm,
    candidates: list[Candidate],
    documents: list[Any],
    nlp: SpacyBackend,
    term_freq: TermFrequency,
    config: JATEConfig | None = None,
) -> dict[str, Any]:
    """Lazily build only the feature objects required by *algo*.

    Returns a dict of keyword arguments to pass to ``algo.score()``.
    """
    kwargs: dict[str, Any] = {}

    # --- Containment (CValue, Basic, NCValue, ComboBasic) ---
    containment: Containment | None = None
    if isinstance(algo, _NEEDS_CONTAINMENT + _NEEDS_CHILD_CONTAINMENT):
        tci = TermComponentIndex.build(candidates)
        containment = Containment.build(candidates, tci)
        kwargs["containment"] = containment

    if isinstance(algo, _NEEDS_CHILD_CONTAINMENT):
        if containment is None:
            tci = TermComponentIndex.build(candidates)
            containment = Containment.build(candidates, tci)
            kwargs["containment"] = containment
        kwargs["child_containment"] = containment.reverse()

    # --- ContextFrequency (ChiSquare, NCValue, TermEx) ---
    context_freq: ContextFrequency | None = None
    if isinstance(algo, _NEEDS_CONTEXT_FREQ) or isinstance(algo, _NEEDS_CHI_SQUARE_FEATURES):
        context_freq = ContextFrequency.build(candidates, documents, nlp)
        kwargs["context_freq"] = context_freq

    # --- WordFrequency (RAKE, Weirdness, GlossEx, TermEx) ---
    word_freq: WordFrequency | None = None
    if isinstance(algo, _NEEDS_WORD_FREQ):
        word_freq = WordFrequency.build(documents, nlp.tokenize)
        kwargs["word_freq"] = word_freq

    # --- TermComponentIndex (RAKE) ---
    if isinstance(algo, _NEEDS_TCI):
        tci_for_rake = TermComponentIndex.build(candidates)
        kwargs["term_component_index"] = tci_for_rake

    # --- ReferenceFrequency (Weirdness, GlossEx, TermEx) ---
    if isinstance(algo, _NEEDS_REF_FREQ):
        if word_freq is None:
            word_freq = WordFrequency.build(documents, nlp.tokenize)
            kwargs["word_freq"] = word_freq

        # Build list of reference frequencies
        ref_freqs: list[ReferenceFrequency] = []
        if config and config.reference_frequency_file:
            files = config.reference_frequency_file
            if isinstance(files, str):
                files = [files]
            ref_freqs = [ReferenceFrequency.from_file(f) for f in files]

        if not ref_freqs:
            # Self-reference fallback
            ref_freqs = [ReferenceFrequency.from_word_frequency(word_freq)]

        # TermEx gets full list; others get single
        if isinstance(algo, TermEx):
            kwargs["ref_freqs"] = ref_freqs
        kwargs["ref_freq"] = ref_freqs[0]

    # --- Chi-Square specific: Cooccurrence + ChiSquareFrequentTerms ---
    if isinstance(algo, _NEEDS_CHI_SQUARE_FEATURES):
        if context_freq is None:
            context_freq = ContextFrequency.build(candidates, documents, nlp)
            kwargs["context_freq"] = context_freq
        ref_ctx = context_freq.copy_top_fraction(term_freq, 0.3)
        min_ttf = config.prefilter_min_ttf if config else 0
        min_tcf = config.prefilter_min_tcf if config else 0
        cooccurrence = Cooccurrence.build(
            context_freq,
            ref_ctx,
            term_freq=term_freq,
            min_ttf=min_ttf,
            min_tcf=min_tcf,
        )
        chi_square_ft = ChiSquareFrequentTerms.build(ref_ctx, term_freq.corpus_total)
        kwargs["cooccurrence"] = cooccurrence
        kwargs["chi_square_ft"] = chi_square_ft

    return kwargs


# -----------------------------------------------------------------------
# Public API
# -----------------------------------------------------------------------


def extract(
    text: str,
    *,
    algorithm: str = "cvalue",
    model: str = "en_core_web_sm",
    extractor: str = "pos_pattern",
    config: JATEConfig | None = None,
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
    config:
        Pipeline configuration (controls parallelism). Defaults to sequential.
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
    if config is None:
        config = JATEConfig()

    nlp = SpacyBackend(model)
    documents = DocumentLoader.load_texts([text])

    # Extractors still need a CorpusStore for add_document()
    store = MemoryCorpusStore()
    ext = _resolve_extractor(extractor)
    candidates = ext.extract(documents, nlp, store)

    # Build features from candidates and documents
    total_docs = len(documents)
    term_freq = TermFrequency.build(candidates, total_docs)

    algo = _resolve_algorithm(algorithm, **algo_kwargs)
    score_kwargs = _build_features(algo, candidates, documents, nlp, term_freq, config)
    result = algo.score(candidates, term_freq, **score_kwargs)

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
    config: JATEConfig | None = None,
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
    config:
        Pipeline configuration (controls parallelism). Defaults to sequential.
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
    if config is None:
        config = JATEConfig()

    nlp = SpacyBackend(model)

    # Detect input type and load documents
    documents = _load_sources(sources)

    # Extractors still need a CorpusStore for add_document()
    if db_path is not None:
        store: MemoryCorpusStore | SQLiteCorpusStore = SQLiteCorpusStore(db_path)
    else:
        store = MemoryCorpusStore()

    ext = _resolve_extractor(extractor)
    candidates = ext.extract(documents, nlp, store)

    # Build features from candidates and documents
    total_docs = len(documents)
    term_freq = TermFrequency.build(candidates, total_docs)

    algo = _resolve_algorithm(algorithm, **algo_kwargs)
    score_kwargs = _build_features(algo, candidates, documents, nlp, term_freq, config)
    result = algo.score(candidates, term_freq, **score_kwargs)

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
    config: JATEConfig | None = None,
    min_frequency: int = 1,
    min_words: int = 1,
    max_words: int | None = None,
    voting: bool = False,
    voting_weights: dict[str, float] | None = None,
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
    config:
        Pipeline configuration (controls parallelism). Defaults to sequential.
    voting:
        If ``True``, also include a ``"voting"`` key in the results with
        a reciprocal-rank-fusion ensemble of all algorithm results.
    voting_weights:
        Optional per-algorithm weights for voting (algorithm name -> weight).
        Defaults to equal weight of 1.0 for all algorithms.
    **kwargs:
        Extra keyword arguments forwarded to each algorithm constructor.

    Returns
    -------
    dict[str, TermExtractionResult]
        Mapping of algorithm name to its scored results.  If *voting* is
        ``True``, an additional ``"voting"`` key is included.
    """
    if config is None:
        config = JATEConfig()

    if algorithms is None:
        algorithms = ["tfidf", "cvalue", "rake", "ridf", "basic"]

    nlp = SpacyBackend(model)
    documents = _load_sources(sources)

    # Extractors still need a CorpusStore for add_document()
    store = MemoryCorpusStore()
    ext = _resolve_extractor(extractor)
    candidates = ext.extract(documents, nlp, store)

    # Build features from candidates and documents
    total_docs = len(documents)
    term_freq = TermFrequency.build(candidates, total_docs)

    results: dict[str, TermExtractionResult] = {}
    for algo_name in algorithms:
        algo = _resolve_algorithm(algo_name, **kwargs)
        score_kwargs = _build_features(algo, candidates, documents, nlp, term_freq, config)
        result = algo.score(candidates, term_freq, **score_kwargs)
        result = result.filter_by_frequency(min_frequency)
        result = result.filter_by_length(min_words=min_words, max_words=max_words)
        for i, term in enumerate(result):
            term.rank = i + 1
        results[algo_name] = result

    if voting:
        weights = voting_weights or {}
        pairs: list[tuple[TermExtractionResult, float]] = [
            (results[name], weights.get(name, 1.0)) for name in algorithms
        ]
        results["voting"] = Voting.combine(pairs)

    return results


# -----------------------------------------------------------------------
# Internal helpers
# -----------------------------------------------------------------------


def _load_sources(sources: list[str] | str) -> list[Any]:
    """Detect input type and load documents accordingly."""
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
