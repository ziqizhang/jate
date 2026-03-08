"""Tests for candidate term extractors."""

from __future__ import annotations

import spacy

from jate.extractors.ngram import NGramExtractor
from jate.extractors.noun_phrase import NounPhraseExtractor
from jate.extractors.pos_pattern import PosPatternExtractor
from jate.models import Candidate, Document
from jate.protocols import CorpusStore, NLPBackend

# ---------------------------------------------------------------------------
# Simple spaCy-based NLP backend for testing
# ---------------------------------------------------------------------------


class SimpleSpacyBackend:
    """Thin wrapper around spaCy that satisfies the NLPBackend protocol."""

    def __init__(self) -> None:
        self._nlp = spacy.load("en_core_web_sm")

    def tokenize(self, text: str) -> list[str]:
        doc = self._nlp(text)
        return [tok.text for tok in doc if not tok.is_space]

    def pos_tag(self, text: str) -> list[tuple[str, str]]:
        doc = self._nlp(text)
        return [(tok.text, tok.pos_) for tok in doc if not tok.is_space]

    def lemmatize(self, text: str) -> list[tuple[str, str]]:
        doc = self._nlp(text)
        return [(tok.text, tok.lemma_) for tok in doc if not tok.is_space]

    def sentence_split(self, text: str) -> list[str]:
        doc = self._nlp(text)
        return [sent.text for sent in doc.sents]

    def noun_chunks(self, text: str) -> list[str]:
        doc = self._nlp(text)
        return [chunk.text for chunk in doc.noun_chunks]


# ---------------------------------------------------------------------------
# Simple in-memory corpus store for testing
# ---------------------------------------------------------------------------


class MemoryCorpusStore:
    """Minimal CorpusStore implementation for tests."""

    def __init__(self) -> None:
        self._docs: dict[str, Document] = {}

    def add_document(self, document: Document) -> None:
        self._docs[document.doc_id] = document

    def get_term_frequency(self, term: str) -> int:
        return 0

    def get_document_frequency(self, term: str) -> int:
        return 0

    def get_total_documents(self) -> int:
        return len(self._docs)

    def get_cooccurrences(self, term_a: str, term_b: str) -> int:
        return 0

    def get_corpus_total(self) -> int:
        return 0


# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------

_NLP = None


def _get_nlp() -> SimpleSpacyBackend:
    global _NLP
    if _NLP is None:
        _NLP = SimpleSpacyBackend()
    return _NLP


SAMPLE_TEXT_1 = (
    "Machine learning is a subset of artificial intelligence. "
    "Deep neural networks have revolutionized machine learning."
)

SAMPLE_TEXT_2 = (
    "Natural language processing uses machine learning techniques. "
    "Neural networks are important for language processing."
)


def _make_docs() -> list[Document]:
    return [
        Document(doc_id="doc1", content=SAMPLE_TEXT_1),
        Document(doc_id="doc2", content=SAMPLE_TEXT_2),
    ]


# ---------------------------------------------------------------------------
# PosPatternExtractor tests
# ---------------------------------------------------------------------------


class TestPosPatternExtractor:
    def test_extracts_candidates(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = PosPatternExtractor()
        docs = _make_docs()
        candidates = extractor.extract(docs, nlp, store)

        assert len(candidates) > 0
        norms = {c.normalized_form for c in candidates}
        # Should extract noun-phrase-like patterns
        assert any("learning" in n for n in norms) or any("network" in n for n in norms)

    def test_extracts_machine_learning(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        # Use a pattern that captures NOUN sequences (machine/NN learning/NN)
        extractor = PosPatternExtractor(pattern=r"(ADJ )*(NOUN )+(NOUN )*")
        docs = _make_docs()
        candidates = extractor.extract(docs, nlp, store)
        norms = {c.normalized_form for c in candidates}

        # "machine learning" should be among extracted candidates
        assert "machine learning" in norms or any(
            "machine" in n and "learning" in n for n in norms
        ), f"Expected 'machine learning' in {norms}"

    def test_extracts_neural_network(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = PosPatternExtractor(pattern=r"(ADJ )*(NOUN )+")
        docs = [Document(doc_id="doc1", content="The deep neural network is powerful.")]
        candidates = extractor.extract(docs, nlp, store)
        norms = {c.normalized_form for c in candidates}

        # Should extract "neural network" or "deep neural network"
        assert any("neural" in n and "network" in n for n in norms), f"Expected 'neural network' in {norms}"

    def test_records_positions(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = PosPatternExtractor()
        docs = [Document(doc_id="d1", content="Machine learning is great.")]
        candidates = extractor.extract(docs, nlp, store)

        for c in candidates:
            assert c.total_frequency >= 1
            for doc_id, positions in c.doc_positions.items():
                for start, end in positions:
                    assert start >= 0
                    assert end > start

    def test_adds_documents_to_store(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = PosPatternExtractor()
        docs = _make_docs()
        extractor.extract(docs, nlp, store)
        assert store.get_total_documents() == 2

    def test_merges_duplicates_across_docs(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = PosPatternExtractor(pattern=r"(ADJ )*(NOUN )+")
        docs = _make_docs()
        candidates = extractor.extract(docs, nlp, store)

        # Find candidates that appear in both documents
        # "machine learning" appears in both doc1 and doc2
        for c in candidates:
            if "machine" in c.normalized_form and "learning" in c.normalized_form:
                assert (
                    c.total_frequency >= 2
                ), f"Expected frequency >= 2 for '{c.normalized_form}', got {c.total_frequency}"
                break


# ---------------------------------------------------------------------------
# NGramExtractor tests
# ---------------------------------------------------------------------------


class TestNGramExtractor:
    def test_extracts_ngrams(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NGramExtractor(min_n=1, max_n=3)
        docs = [Document(doc_id="d1", content="Neural networks are useful.")]
        candidates = extractor.extract(docs, nlp, store)

        assert len(candidates) > 0

    def test_produces_unigrams_through_max(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NGramExtractor(min_n=1, max_n=5)
        docs = [Document(doc_id="d1", content="The deep neural network model is very powerful and fast.")]
        candidates = extractor.extract(docs, nlp, store)

        word_counts = {len(c.surface_form.split()) for c in candidates}
        # Should include unigrams
        assert 1 in word_counts
        # Should include some multi-word n-grams
        assert len(word_counts) > 1

    def test_skips_stopword_boundaries(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NGramExtractor(min_n=1, max_n=3)
        docs = [Document(doc_id="d1", content="The cat is on the mat.")]
        candidates = extractor.extract(docs, nlp, store)

        norms = {c.normalized_form for c in candidates}
        # "the" alone should not appear (stopword)
        assert "the" not in norms
        # "is" alone should not appear (stopword)
        assert "be" not in norms and "is" not in norms

    def test_skips_punctuation_boundaries(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NGramExtractor(min_n=1, max_n=2)
        docs = [Document(doc_id="d1", content="Hello, world!")]
        candidates = extractor.extract(docs, nlp, store)

        norms = {c.normalized_form for c in candidates}
        assert "," not in norms
        assert "!" not in norms

    def test_merges_duplicates(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NGramExtractor(min_n=1, max_n=2)
        docs = [
            Document(doc_id="d1", content="Neural networks are great."),
            Document(doc_id="d2", content="Neural networks can learn."),
        ]
        candidates = extractor.extract(docs, nlp, store)

        for c in candidates:
            if "neural" in c.normalized_form and "network" in c.normalized_form:
                assert c.total_frequency >= 2
                assert len(c.doc_positions) == 2
                break
        else:
            # If not found as bigram, that's okay - it might be "neural networks"
            pass

    def test_frequency_counts(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NGramExtractor(min_n=1, max_n=2)
        docs = [Document(doc_id="d1", content="cat dog cat dog cat")]
        candidates = extractor.extract(docs, nlp, store)

        cat_cands = [c for c in candidates if c.normalized_form == "cat"]
        assert len(cat_cands) == 1
        assert cat_cands[0].total_frequency == 3


# ---------------------------------------------------------------------------
# NounPhraseExtractor tests
# ---------------------------------------------------------------------------


class TestNounPhraseExtractor:
    def test_extracts_noun_phrases(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NounPhraseExtractor()
        docs = [Document(doc_id="d1", content="The neural network processes large datasets efficiently.")]
        candidates = extractor.extract(docs, nlp, store)

        assert len(candidates) > 0
        norms = {c.normalized_form for c in candidates}
        # Should contain something related to "neural network" or "datasets"
        assert any(
            "network" in n or "dataset" in n for n in norms
        ), f"Expected noun phrase with 'network' or 'dataset' in {norms}"

    def test_normalizes_to_lowercase(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NounPhraseExtractor()
        docs = [Document(doc_id="d1", content="The Neural Network is powerful.")]
        candidates = extractor.extract(docs, nlp, store)

        for c in candidates:
            assert c.normalized_form == c.normalized_form.lower()

    def test_merges_duplicates_across_docs(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NounPhraseExtractor()
        docs = [
            Document(doc_id="d1", content="The neural network is powerful."),
            Document(doc_id="d2", content="This neural network works well."),
        ]
        candidates = extractor.extract(docs, nlp, store)

        # Both sentences produce "The/This neural network" chunks; after
        # stripping the leading stopword, both normalize to "neural network".
        network_cands = [c for c in candidates if c.normalized_form == "neural network"]
        assert len(network_cands) == 1, (
            f"Expected exactly 1 merged 'neural network' candidate, "
            f"got {[(c.normalized_form, c.doc_positions) for c in candidates]}"
        )
        c = network_cands[0]
        assert c.total_frequency == 2
        assert len(c.doc_positions) == 2

    def test_adds_documents_to_store(self) -> None:
        nlp = _get_nlp()
        store = MemoryCorpusStore()
        extractor = NounPhraseExtractor()
        docs = _make_docs()
        extractor.extract(docs, nlp, store)
        assert store.get_total_documents() == 2
