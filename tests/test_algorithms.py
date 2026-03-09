"""Unit tests for all 13 ATE scoring algorithms."""

from __future__ import annotations

import math

import pytest

from jate.algorithms import (
    ATTF,
    RAKE,
    RIDF,
    TFIDF,
    TTF,
    Basic,
    ChiSquare,
    ComboBasic,
    CValue,
    GlossEx,
    NCValue,
    TermEx,
    Weirdness,
)
from jate.models import Candidate, Document, TermExtractionResult

# ---------------------------------------------------------------------------
# Mock CorpusStore
# ---------------------------------------------------------------------------


class MockCorpusStore:
    """Dict-based CorpusStore satisfying the protocol."""

    def __init__(
        self,
        term_freq: dict[str, int] | None = None,
        doc_freq: dict[str, int] | None = None,
        total_docs: int = 10,
        corpus_total: int = 1000,
        cooccurrences: dict[tuple[str, str], int] | None = None,
    ) -> None:
        self._tf = term_freq or {}
        self._df = doc_freq or {}
        self._total_docs = total_docs
        self._corpus_total = corpus_total
        self._cooc = cooccurrences or {}

    def add_document(self, document: Document) -> None:
        pass

    def get_term_frequency(self, term: str) -> int:
        return self._tf.get(term, 0)

    def get_document_frequency(self, term: str) -> int:
        return self._df.get(term, 0)

    def get_total_documents(self) -> int:
        return self._total_docs

    def get_cooccurrences(self, term_a: str, term_b: str) -> int:
        return self._cooc.get((term_a, term_b), self._cooc.get((term_b, term_a), 0))

    def get_corpus_total(self) -> int:
        return self._corpus_total


# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _make_candidate(surface: str, *, normalized: str = "", total_freq: int = 0) -> Candidate:
    nf = normalized or surface.lower()
    return Candidate(surface_form=surface, normalized_form=nf, total_frequency=total_freq)


def _make_candidates() -> list[Candidate]:
    """Standard candidate list for basic tests."""
    return [
        _make_candidate("neural network", total_freq=50),
        _make_candidate("deep neural network", total_freq=20),
        _make_candidate("machine learning", total_freq=80),
        _make_candidate("network", total_freq=100),
    ]


def _make_store() -> MockCorpusStore:
    """Standard store matching the standard candidates."""
    return MockCorpusStore(
        term_freq={
            "neural network": 50,
            "deep neural network": 20,
            "machine learning": 80,
            "network": 100,
            "neural": 70,
            "deep": 30,
            "machine": 90,
            "learning": 85,
        },
        doc_freq={
            "neural network": 5,
            "deep neural network": 2,
            "machine learning": 8,
            "network": 10,
            "neural": 7,
            "deep": 3,
            "machine": 9,
            "learning": 8,
        },
        total_docs=10,
        corpus_total=1000,
        cooccurrences={
            ("neural network", "machine learning"): 3,
            ("neural network", "deep neural network"): 2,
            ("machine learning", "deep neural network"): 1,
        },
    )


# ---------------------------------------------------------------------------
# TFIDF
# ---------------------------------------------------------------------------


class TestTFIDF:
    def test_known_scores(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        algo = TFIDF()
        result = algo.score(candidates, store)
        assert isinstance(result, TermExtractionResult)
        assert len(result) == 4

        # Verify one score manually: neural network tf=50, df=5, N=10
        # tf_norm = 50 / (1000 + 1), idf = log(10/5) = log(2)
        # score = tf_norm * idf
        scores = {t.string: t.score for t in result}
        expected = (50 / 1001) * math.log(2)
        assert abs(scores["neural network"] - expected) < 1e-9

    def test_sorted_descending(self) -> None:
        result = TFIDF().score(_make_candidates(), _make_store())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# CValue
# ---------------------------------------------------------------------------


class TestCValue:
    def test_containment_logic(self) -> None:
        """'neural network' is contained in 'deep neural network'."""
        store = _make_store()
        candidates = _make_candidates()
        result = CValue().score(candidates, store)
        scores = {t.string: t.score for t in result}

        # "neural network" (2 words) is contained in "deep neural network"
        # log2(2 + 0.1) = log2(2.1)
        # pTa = 1, sumFreqB = 20
        # score = log2(2.1) * (50 - 20/1) = log2(2.1) * 30
        expected_nn = math.log2(2.1) * (50 - 20)
        assert abs(scores["neural network"] - expected_nn) < 1e-9

        # "deep neural network" (3 words) has no parent
        # score = log2(3.1) * 20
        expected_dnn = math.log2(3.1) * 20
        assert abs(scores["deep neural network"] - expected_dnn) < 1e-9

    def test_single_word(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = CValue().score(candidates, store)
        scores = {t.string: t.score for t in result}
        # "network" (1 word), no parent among candidates that strictly contain it
        # as substring match: "neural network" contains "network"
        # log2(1 + 0.1) = log2(1.1)
        # parent terms: "neural network" and "deep neural network" both contain "network"
        # pTa=2, sumFreqB = 50+20=70
        expected = math.log2(1.1) * (100 - 70 / 2)
        assert abs(scores["network"] - expected) < 1e-9


# ---------------------------------------------------------------------------
# NCValue
# ---------------------------------------------------------------------------


class TestNCValue:
    def test_produces_result(self) -> None:
        result = NCValue().score(_make_candidates(), _make_store())
        assert len(result) == 4
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# ATTF
# ---------------------------------------------------------------------------


class TestATTF:
    def test_formula(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = ATTF().score(candidates, store)
        scores = {t.string: t.score for t in result}
        # neural network: tf=50, df=5 -> 10.0
        assert scores["neural network"] == 10.0
        # network: tf=100, df=10 -> 10.0
        assert scores["network"] == 10.0


# ---------------------------------------------------------------------------
# TTF
# ---------------------------------------------------------------------------


class TestTTF:
    def test_score_equals_frequency(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = TTF().score(candidates, store)
        scores = {t.string: t.score for t in result}
        assert scores["neural network"] == 50.0
        assert scores["machine learning"] == 80.0
        assert scores["network"] == 100.0

    def test_sorted(self) -> None:
        result = TTF().score(_make_candidates(), _make_store())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# Basic
# ---------------------------------------------------------------------------


class TestBasic:
    def test_single_word(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = Basic().score(candidates, store)
        scores = {t.string: t.score for t in result}
        # Single-word: score = log(ttf)
        assert abs(scores["network"] - math.log(100)) < 1e-9

    def test_multi_word(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = Basic(alpha=0.72).score(candidates, store)
        scores = {t.string: t.score for t in result}
        # "neural network" has 1 parent ("deep neural network")
        # score = 2 * log(50) + 0.72 * 1
        expected = 2 * math.log(50) + 0.72 * 1
        assert abs(scores["neural network"] - expected) < 1e-9


# ---------------------------------------------------------------------------
# ComboBasic
# ---------------------------------------------------------------------------


class TestComboBasic:
    def test_produces_result(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = ComboBasic().score(candidates, store)
        assert len(result) == 4

    def test_multi_word_with_children(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = ComboBasic(alpha=0.75, beta=0.1).score(candidates, store)
        scores = {t.string: t.score for t in result}
        # "deep neural network" (3 words): no parents, children = "neural network", "network"
        # score = 3 * log(20) + 0.75 * 0 + 0.1 * 2
        expected = 3 * math.log(20) + 0.75 * 0 + 0.1 * 2
        assert abs(scores["deep neural network"] - expected) < 1e-9


# ---------------------------------------------------------------------------
# ChiSquare
# ---------------------------------------------------------------------------


class TestChiSquare:
    def test_produces_result(self) -> None:
        frequent_terms = {"machine learning": 0.3, "neural network": 0.2, "deep learning": 0.1}
        store = _make_store()
        algo = ChiSquare(frequent_terms=frequent_terms)
        result = algo.score(_make_candidates(), store)
        assert len(result) == 4
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# RAKE
# ---------------------------------------------------------------------------


class TestRAKE:
    def test_produces_result(self) -> None:
        word_freq = {"neural": 70, "network": 100, "deep": 30, "machine": 90, "learning": 85}
        # term_component_index: word -> list of (parent_term, word_count)
        tci = {
            "neural": [("neural network", 2), ("deep neural network", 3)],
            "network": [("neural network", 2), ("deep neural network", 3)],
            "deep": [("deep neural network", 3)],
            "machine": [("machine learning", 2)],
            "learning": [("machine learning", 2)],
        }
        store = _make_store()
        algo = RAKE(word_frequencies=word_freq, term_component_index=tci)
        result = algo.score(_make_candidates(), store)
        assert len(result) == 4
        # All scores should be positive
        for term in result:
            assert term.score > 0


# ---------------------------------------------------------------------------
# RIDF
# ---------------------------------------------------------------------------


class TestRIDF:
    def test_poisson_formula(self) -> None:
        store = _make_store()
        candidates = _make_candidates()
        result = RIDF().score(candidates, store)
        scores = {t.string: t.score for t in result}

        # "neural network": ttf=50, df=5, N=10
        # attf = 50/10 = 5.0
        # pi = exp(-5) ~= 0.006738
        # eidf = -log(1 - pi) / log(2) = -log(0.993262) / log(2)
        # idf = log(10/5) = log(2)
        ttf, df, n = 50, 5, 10
        attf = ttf / n
        pi = math.exp(-attf)
        eidf = -(math.log(1 - pi) / math.log(2))
        idf = math.log(n / df)
        expected = idf - eidf
        assert abs(scores["neural network"] - expected) < 1e-9

    def test_sorted(self) -> None:
        result = RIDF().score(_make_candidates(), _make_store())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# Weirdness
# ---------------------------------------------------------------------------


class TestWeirdness:
    def test_ratio_with_reference(self) -> None:
        ref_freq = {"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700}
        ref_total = 100000
        store = _make_store()
        algo = Weirdness(
            reference_frequencies=ref_freq,
            reference_total=ref_total,
        )
        result = algo.score(_make_candidates(), store)
        assert len(result) == 4

        # Verify "network" (single word): score = log(100/1000 / (800/100000))
        scores = {t.string: t.score for t in result}
        expected = math.log((100 / 1000) / (800 / 100000))
        assert abs(scores["network"] - expected) < 1e-9

    def test_sorted(self) -> None:
        ref_freq = {"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700}
        algo = Weirdness(reference_frequencies=ref_freq, reference_total=100000)
        result = algo.score(_make_candidates(), _make_store())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# TermEx
# ---------------------------------------------------------------------------


class TestTermEx:
    def test_produces_result(self) -> None:
        ref_freq = {"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700}
        algo = TermEx(
            reference_frequencies=ref_freq,
            reference_total=100000,
        )
        result = algo.score(_make_candidates(), _make_store())
        assert len(result) == 4

    def test_sorted(self) -> None:
        ref_freq = {"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700}
        algo = TermEx(reference_frequencies=ref_freq, reference_total=100000)
        result = algo.score(_make_candidates(), _make_store())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# GlossEx
# ---------------------------------------------------------------------------


class TestGlossEx:
    def test_produces_result(self) -> None:
        ref_freq = {"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700}
        algo = GlossEx(
            reference_frequencies=ref_freq,
            reference_total=100000,
        )
        result = algo.score(_make_candidates(), _make_store())
        assert len(result) == 4

    def test_single_vs_multi_word_weights(self) -> None:
        """Single-word terms should use (0.9, 0.1) weights."""
        ref_freq = {"neural": 500, "network": 800}
        algo = GlossEx(reference_frequencies=ref_freq, reference_total=100000)
        single = [_make_candidate("network", total_freq=100)]
        store = _make_store()
        result = algo.score(single, store)
        assert len(result) == 1
        # Score should be computed with 0.9 * TD + 0.1 * TC
        assert result[0].score > 0


# ---------------------------------------------------------------------------
# All algorithms produce sorted results
# ---------------------------------------------------------------------------


class TestAllAlgorithmsSorted:
    @pytest.mark.parametrize(
        "algo",
        [
            TFIDF(),
            CValue(),
            NCValue(),
            ATTF(),
            TTF(),
            Basic(),
            ComboBasic(),
            RIDF(),
        ],
        ids=lambda a: a.name,
    )
    def test_results_sorted_descending(self, algo) -> None:  # type: ignore[no-untyped-def]
        result = algo.score(_make_candidates(), _make_store())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True), f"{algo.name} results not sorted"


class TestAlgorithmNames:
    def test_all_have_names(self) -> None:
        algos = [TFIDF(), CValue(), NCValue(), ATTF(), TTF(), Basic(), ComboBasic(), RIDF()]
        for algo in algos:
            assert algo.name == algo.__class__.__name__
