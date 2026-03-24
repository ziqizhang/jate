"""spaCy pipeline component for JATE term extraction."""

import warnings
from typing import Any, Dict, List, Optional, Tuple

from spacy.language import Language
from spacy.tokens import Doc

# Register custom extension on Doc
if not Doc.has_extension("terms"):
    Doc.set_extension("terms", default=[])


@Language.factory(
    "jate",
    default_config={
        "algorithm": "cvalue",
        "pattern": "default",
        "min_frequency": 1,
        "min_words": 1,
        "max_words": None,
        "reference_frequency_file": None,
    },
)
def create_jate_component(
    nlp: Language,
    name: str,
    algorithm: str,
    pattern: str,
    min_frequency: int,
    min_words: int,
    max_words: Optional[int],
    reference_frequency_file: Optional[str],
) -> "JATEComponent":
    return JATEComponent(
        nlp=nlp,
        algorithm=algorithm,
        pattern=pattern,
        min_frequency=min_frequency,
        min_words=min_words,
        max_words=max_words,
        reference_frequency_file=reference_frequency_file,
    )


class _SpacyLanguageBackend:
    """Thin adapter wrapping a spaCy ``Language`` to satisfy the NLPBackend protocol.

    This avoids loading a second spaCy model -- we reuse the pipeline's own
    ``nlp`` object for tokenisation, POS tagging, lemmatisation, etc.
    """

    __slots__ = ("_nlp", "_cached_text", "_cached_doc")

    def __init__(self, nlp: Language) -> None:
        self._nlp = nlp
        self._cached_text: Optional[str] = None
        self._cached_doc: Optional[Doc] = None

    def process(self, text: str) -> Doc:
        if text != self._cached_text or self._cached_doc is None:
            self._cached_doc = self._nlp(text)
            self._cached_text = text
        return self._cached_doc

    def process_batch(self, texts: List[str], batch_size: int = 256) -> List[Doc]:
        if not texts:
            return []
        return list(self._nlp.pipe(texts, batch_size=batch_size))

    def tokenize(self, text: str) -> List[str]:
        doc = self.process(text)
        return [token.text for token in doc]

    def pos_tag(self, text: str) -> List[Tuple[str, str]]:
        doc = self.process(text)
        return [(token.text, token.pos_) for token in doc]

    def lemmatize(self, text: str) -> List[Tuple[str, str]]:
        doc = self.process(text)
        return [(token.text, token.lemma_) for token in doc]

    def sentence_split(self, text: str) -> List[str]:
        doc = self.process(text)
        return [sent.text for sent in doc.sents]

    def noun_chunks(self, text: str) -> List[str]:
        doc = self.process(text)
        return [chunk.text for chunk in doc.noun_chunks]


class JATEComponent:
    """spaCy pipeline component that extracts terms from each document."""

    def __init__(
        self,
        nlp: Language,
        algorithm: str = "cvalue",
        pattern: str = "default",
        min_frequency: int = 1,
        min_words: int = 1,
        max_words: Optional[int] = None,
        reference_frequency_file: Optional[str] = None,
    ) -> None:
        self._nlp = nlp
        self._backend = _SpacyLanguageBackend(nlp)
        self._algorithm = algorithm
        self._pattern = pattern
        self._min_frequency = min_frequency
        self._min_words = min_words
        self._max_words = max_words
        self._reference_frequency_file = reference_frequency_file

    def __call__(self, doc: Doc) -> Doc:
        """Process a spaCy Doc and attach extracted terms to doc._.terms."""
        from jate.algorithms.base import AlgorithmIncompatibleError
        from jate.api import _build_features, _is_tagger, _resolve_algorithm, _resolve_extractor, _resolve_tagger
        from jate.config import JATEConfig
        from jate.features import TermFrequency
        from jate.models import Document, TermSpan
        from jate.store.memory_store import MemoryCorpusStore

        # Handle empty documents
        if not doc.text.strip():
            doc._.terms = []
            return doc

        # Tagger path: bypass candidate extraction and scoring pipeline
        if _is_tagger(self._algorithm):
            tagger = _resolve_tagger(self._algorithm)
            result = tagger.tag(doc)
            result = result.filter_by_frequency(self._min_frequency)
            result = result.filter_by_length(min_words=self._min_words, max_words=self._max_words)
            for i, term in enumerate(result):
                term.rank = i + 1
            doc._.terms = list(result)
            return doc

        # Build a JATE Document from the spaCy Doc
        doc_id: str
        if doc.has_extension("doc_id") and getattr(doc._, "doc_id", None) is not None:
            doc_id = doc._.doc_id
        else:
            doc_id = f"doc_{id(doc)}"
        jate_doc = Document(doc_id=doc_id, content=doc.text)

        # Extract candidates using the pre-built spaCy Doc
        store = MemoryCorpusStore()
        store.add_document(jate_doc)
        from jate.extractors.pos_pattern import PosPatternExtractor

        ext_base = _resolve_extractor("pos_pattern", pattern=self._pattern)
        assert isinstance(ext_base, PosPatternExtractor)
        ext = ext_base

        # Use _extract_from_spacy_docs directly to reuse the NLP work
        merged: Dict[str, Any] = {}
        ext._extract_from_spacy_docs([jate_doc], [doc], merged)
        candidates = list(merged.values())

        if not candidates:
            doc._.terms = []
            return doc

        store.index_candidates(candidates, compute_cooccurrences=False)

        # Build features and score
        term_freq = TermFrequency.build(candidates, total_docs=1)

        config: Optional[JATEConfig] = None
        if self._reference_frequency_file:
            config = JATEConfig(reference_frequency_file=self._reference_frequency_file)

        algo = _resolve_algorithm(self._algorithm)

        # Build features -- use our backend adapter so algorithms that need
        # tokenisation / context (rake, weirdness, etc.) work correctly.
        try:
            score_kwargs = _build_features(
                algo, candidates, [jate_doc], self._backend, term_freq, config  # type: ignore[arg-type]
            )
        except Exception:
            score_kwargs = {}

        # Score -- doc_level_compatibility will warn automatically
        try:
            result = algo.score(candidates, term_freq, **score_kwargs)
        except AlgorithmIncompatibleError as exc:
            warnings.warn(str(exc), stacklevel=2)
            doc._.terms = []
            return doc

        result = result.filter_by_frequency(self._min_frequency)
        result = result.filter_by_length(min_words=self._min_words, max_words=self._max_words)

        # Assign ranks
        for i, term in enumerate(result):
            term.rank = i + 1

        # Populate spans from candidates
        cand_map = {c.normalized_form.lower(): c for c in candidates}
        for term in result:
            cand = cand_map.get(term.string.lower())
            if cand and doc_id in cand.doc_positions:
                term.spans = [
                    TermSpan(doc_id=doc_id, start=s, end=e, sentence_idx=si) for s, e, si in cand.doc_positions[doc_id]
                ]

        doc._.terms = list(result)
        return doc
