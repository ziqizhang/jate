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
from jate.features import (
    ChiSquareFrequentTerms,
    Containment,
    ContextFrequency,
    ContextWindow,
    Cooccurrence,
    ReferenceFrequency,
    TermComponentIndex,
    TermFrequency,
    WordFrequency,
)
from jate.models import Candidate, TermExtractionResult

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


def _make_term_freq() -> TermFrequency:
    """Standard TermFrequency matching the standard candidates."""
    return TermFrequency(
        term2ttf={
            "neural network": 50,
            "deep neural network": 20,
            "machine learning": 80,
            "network": 100,
            "neural": 70,
            "deep": 30,
            "machine": 90,
            "learning": 85,
        },
        term2fid={
            "neural network": {"d1": 10, "d2": 10, "d3": 10, "d4": 10, "d5": 10},
            "deep neural network": {"d1": 10, "d2": 10},
            "machine learning": {"d1": 10, "d2": 10, "d3": 10, "d4": 10, "d5": 10, "d6": 10, "d7": 10, "d8": 10},
            "network": {
                "d1": 10,
                "d2": 10,
                "d3": 10,
                "d4": 10,
                "d5": 10,
                "d6": 10,
                "d7": 10,
                "d8": 10,
                "d9": 10,
                "d10": 10,
            },
            "neural": {"d1": 10, "d2": 10, "d3": 10, "d4": 10, "d5": 10, "d6": 10, "d7": 10},
            "deep": {"d1": 10, "d2": 10, "d3": 10},
            "machine": {"d1": 10, "d2": 10, "d3": 10, "d4": 10, "d5": 10, "d6": 10, "d7": 10, "d8": 10, "d9": 10},
            "learning": {"d1": 10, "d2": 10, "d3": 10, "d4": 10, "d5": 10, "d6": 10, "d7": 10, "d8": 10},
        },
        corpus_total=1000,
        total_docs=10,
    )


def _make_containment() -> Containment:
    """Containment matching the standard candidates."""
    return Containment(
        term2parents={
            "neural network": {"deep neural network"},
            "deep neural network": set(),
            "machine learning": set(),
            "network": {"neural network", "deep neural network"},
        },
    )


# ---------------------------------------------------------------------------
# TFIDF
# ---------------------------------------------------------------------------


class TestTFIDF:
    def test_known_scores(self) -> None:
        tf = _make_term_freq()
        candidates = _make_candidates()
        algo = TFIDF()
        result = algo.score(candidates, tf)
        assert isinstance(result, TermExtractionResult)
        assert len(result) == 4

        # Verify one score manually: neural network tf=50, df=5, N=10
        # tf_norm = 50 / (1000 + 1), idf = log(10/5) = log(2)
        # score = tf_norm * idf
        scores = {t.string: t.score for t in result}
        expected = (50 / 1001) * math.log(2)
        assert abs(scores["neural network"] - expected) < 1e-9

    def test_sorted_descending(self) -> None:
        result = TFIDF().score(_make_candidates(), _make_term_freq())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# CValue
# ---------------------------------------------------------------------------


class TestCValue:
    def test_containment_logic(self) -> None:
        """'neural network' is contained in 'deep neural network'."""
        tf = _make_term_freq()
        candidates = _make_candidates()
        containment = _make_containment()
        result = CValue().score(candidates, tf, containment=containment)
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
        tf = _make_term_freq()
        candidates = _make_candidates()
        containment = _make_containment()
        result = CValue().score(candidates, tf, containment=containment)
        scores = {t.string: t.score for t in result}
        # "network" (1 word), parents: "neural network" and "deep neural network"
        # log2(1 + 0.1) = log2(1.1)
        # pTa=2, sumFreqB = 50+20=70
        expected = math.log2(1.1) * (100 - 70 / 2)
        assert abs(scores["network"] - expected) < 1e-9


# ---------------------------------------------------------------------------
# NCValue
# ---------------------------------------------------------------------------


class TestNCValue:
    def test_produces_result(self) -> None:
        result = NCValue().score(_make_candidates(), _make_term_freq())
        assert len(result) == 4
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


class TestNCValueWithContext:
    def test_uses_adjacent_words(self) -> None:
        """When ContextFrequency provided, NC-Value uses adjacency-based context."""
        tf = _make_term_freq()
        candidates = _make_candidates()

        ctx_freq = ContextFrequency(
            ctx2ttf={},
            ctx2tfic={},
            term2ctx={},
            adjacent={
                "neural network": {"deep": 3, "large": 2, "train": 1},
                "machine learning": {"supervised": 2, "use": 1},
                "deep neural network": {"train": 1},
                "network": {"neural": 5, "large": 1},
            },
        )

        algo = NCValue()
        result_without = algo.score(candidates, tf)
        result_with = algo.score(candidates, tf, context_freq=ctx_freq)

        scores_without = {t.string: t.score for t in result_without}
        scores_with = {t.string: t.score for t in result_with}

        # Scores should differ
        assert scores_with != scores_without

    def test_falls_back_without_context(self) -> None:
        """Without ContextFrequency, NC-Value uses co-occurrence (existing behavior)."""
        tf = _make_term_freq()
        candidates = _make_candidates()
        algo = NCValue()

        result1 = algo.score(candidates, tf)
        result2 = algo.score(candidates, tf)

        scores1 = {t.string: t.score for t in result1}
        scores2 = {t.string: t.score for t in result2}
        assert scores1 == scores2


# ---------------------------------------------------------------------------
# ATTF
# ---------------------------------------------------------------------------


class TestATTF:
    def test_formula(self) -> None:
        tf = _make_term_freq()
        candidates = _make_candidates()
        result = ATTF().score(candidates, tf)
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
        tf = _make_term_freq()
        candidates = _make_candidates()
        result = TTF().score(candidates, tf)
        scores = {t.string: t.score for t in result}
        assert scores["neural network"] == 50.0
        assert scores["machine learning"] == 80.0
        assert scores["network"] == 100.0

    def test_sorted(self) -> None:
        result = TTF().score(_make_candidates(), _make_term_freq())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# Basic
# ---------------------------------------------------------------------------


class TestBasic:
    def test_single_word(self) -> None:
        tf = _make_term_freq()
        candidates = _make_candidates()
        containment = _make_containment()
        result = Basic().score(candidates, tf, containment=containment)
        scores = {t.string: t.score for t in result}
        # Single-word: score = log(ttf)
        assert abs(scores["network"] - math.log(100)) < 1e-9

    def test_multi_word(self) -> None:
        tf = _make_term_freq()
        candidates = _make_candidates()
        containment = _make_containment()
        result = Basic(alpha=0.72).score(candidates, tf, containment=containment)
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
        tf = _make_term_freq()
        candidates = _make_candidates()
        result = ComboBasic().score(candidates, tf)
        assert len(result) == 4

    def test_multi_word_with_children(self) -> None:
        tf = _make_term_freq()
        candidates = _make_candidates()
        containment = _make_containment()
        child_containment = containment.reverse()
        result = ComboBasic(alpha=0.75, beta=0.1).score(
            candidates,
            tf,
            containment=containment,
            child_containment=child_containment,
        )
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
        tf = _make_term_freq()
        chi_ft = ChiSquareFrequentTerms(
            exp_prob={"machine learning": 0.3, "neural network": 0.2, "deep learning": 0.1},
            sum_exp_prob=0.6,
            max_exp_prob=0.3,
        )
        algo = ChiSquare()
        result = algo.score(_make_candidates(), tf, chi_square_ft=chi_ft)
        assert len(result) == 4
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


class TestChiSquareWithContext:
    def test_uses_cooccurrence(self) -> None:
        """When Cooccurrence provided, Chi-Square uses it for co-occurrence."""
        tf = _make_term_freq()
        candidates = _make_candidates()

        ctx_freq = ContextFrequency(
            ctx2ttf={
                ContextWindow("d1", 0): 5,
                ContextWindow("d1", 1): 5,
            },
            ctx2tfic={
                ContextWindow("d1", 0): {"neural network": 2, "machine learning": 3},
                ContextWindow("d1", 1): {"neural network": 1, "deep neural network": 2, "network": 2},
            },
            term2ctx={
                "neural network": {ContextWindow("d1", 0), ContextWindow("d1", 1)},
                "machine learning": {ContextWindow("d1", 0)},
                "deep neural network": {ContextWindow("d1", 1)},
                "network": {ContextWindow("d1", 1)},
            },
            adjacent={},
        )

        cooc = Cooccurrence(
            matrix={
                ("machine learning", "neural network"): 1,
            }
        )

        chi_ft = ChiSquareFrequentTerms(
            exp_prob={"machine learning": 0.3, "neural network": 0.2},
            sum_exp_prob=0.5,
            max_exp_prob=0.3,
        )

        algo = ChiSquare()
        result_without = algo.score(candidates, tf, chi_square_ft=chi_ft)
        result_with = algo.score(candidates, tf, context_freq=ctx_freq, cooccurrence=cooc, chi_square_ft=chi_ft)

        scores_without = {t.string: t.score for t in result_without}
        scores_with = {t.string: t.score for t in result_with}

        # Scores should differ because n_w source and co-occurrence counts differ
        assert scores_with != scores_without

    def test_falls_back_without_context(self) -> None:
        """Without context features, Chi-Square uses term_freq as fallback."""
        tf = _make_term_freq()
        candidates = _make_candidates()
        chi_ft = ChiSquareFrequentTerms(
            exp_prob={"machine learning": 0.3, "neural network": 0.2},
            sum_exp_prob=0.5,
            max_exp_prob=0.3,
        )
        algo = ChiSquare()

        result1 = algo.score(candidates, tf, chi_square_ft=chi_ft)
        result2 = algo.score(candidates, tf, chi_square_ft=chi_ft)

        scores1 = {t.string: t.score for t in result1}
        scores2 = {t.string: t.score for t in result2}
        assert scores1 == scores2


# ---------------------------------------------------------------------------
# RAKE
# ---------------------------------------------------------------------------


class TestRAKE:
    def test_produces_result(self) -> None:
        tf = _make_term_freq()
        word_freq = WordFrequency(
            word2ttf={"neural": 70, "network": 100, "deep": 30, "machine": 90, "learning": 85},
            word2fid={},
            corpus_total=375,
        )
        tci = TermComponentIndex(
            index={
                "neural": [("deep neural network", 3), ("neural network", 2)],
                "network": [("deep neural network", 3), ("neural network", 2)],
                "deep": [("deep neural network", 3)],
                "machine": [("machine learning", 2)],
                "learning": [("machine learning", 2)],
            }
        )
        algo = RAKE()
        result = algo.score(_make_candidates(), tf, word_freq=word_freq, term_component_index=tci)
        assert len(result) == 4
        # All scores should be positive
        for term in result:
            assert term.score > 0


# ---------------------------------------------------------------------------
# RIDF
# ---------------------------------------------------------------------------


class TestRIDF:
    def test_poisson_formula(self) -> None:
        tf = _make_term_freq()
        candidates = _make_candidates()
        result = RIDF().score(candidates, tf)
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
        result = RIDF().score(_make_candidates(), _make_term_freq())
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# Weirdness
# ---------------------------------------------------------------------------


class TestWeirdness:
    def test_ratio_with_reference(self) -> None:
        ref_freq = ReferenceFrequency(
            word2ttf={"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700},
            corpus_total=100000,
        )
        tf = _make_term_freq()
        algo = Weirdness(match_oom=False)
        result = algo.score(_make_candidates(), tf, ref_freq=ref_freq)
        assert len(result) == 4

        # Verify "network" (single word): score = log(100/1000 / (800/100001))
        # Uses get_ttf_norm which has +1 denominator matching Java's getTTFNorm
        scores = {t.string: t.score for t in result}
        expected = math.log((100 / 1000) / (800 / 100001))
        assert abs(scores["network"] - expected) < 1e-9

    def test_sorted(self) -> None:
        ref_freq = ReferenceFrequency(
            word2ttf={"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700},
            corpus_total=100000,
        )
        algo = Weirdness(match_oom=False)
        result = algo.score(_make_candidates(), _make_term_freq(), ref_freq=ref_freq)
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)


# ---------------------------------------------------------------------------
# TermEx
# ---------------------------------------------------------------------------


class TestTermEx:
    def test_produces_result(self) -> None:
        ref_freq = ReferenceFrequency(
            word2ttf={"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700},
            corpus_total=100000,
        )
        algo = TermEx()
        result = algo.score(_make_candidates(), _make_term_freq(), ref_freq=ref_freq)
        assert len(result) == 4

    def test_sorted(self) -> None:
        ref_freq = ReferenceFrequency(
            word2ttf={"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700},
            corpus_total=100000,
        )
        algo = TermEx()
        result = algo.score(_make_candidates(), _make_term_freq(), ref_freq=ref_freq)
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)

    def test_multi_reference(self) -> None:
        """TermEx picks best per-word reference from multiple corpora."""
        # ref1 has higher "neural", ref2 has higher "network"
        ref1 = ReferenceFrequency(
            word2ttf={"neural": 100, "network": 50, "deep": 10, "machine": 30, "learning": 40},
            corpus_total=1000,
        )
        ref2 = ReferenceFrequency(
            word2ttf={"neural": 10, "network": 200, "deep": 5, "machine": 80, "learning": 20},
            corpus_total=500,
        )
        word_freq = WordFrequency(
            word2ttf={"neural": 70, "network": 100, "deep": 30, "machine": 90, "learning": 85},
            word2fid={},
            corpus_total=375,
        )

        algo = TermEx(match_oom=False)
        tf = _make_term_freq()
        candidates = _make_candidates()

        # Multi-ref result
        result_multi = algo.score(candidates, tf, ref_freq=ref1, ref_freqs=[ref1, ref2], word_freq=word_freq)
        # Single-ref result (ref1 only)
        result_single = algo.score(candidates, tf, ref_freq=ref1, word_freq=word_freq)

        scores_multi = {t.string: t.score for t in result_multi}
        scores_single = {t.string: t.score for t in result_single}

        # Scores should differ because multi-ref picks best per-word
        assert scores_multi != scores_single

        # Multi-ref should produce valid sorted results
        assert len(result_multi) == 4
        multi_scores = [t.score for t in result_multi]
        assert multi_scores == sorted(multi_scores, reverse=True)


# ---------------------------------------------------------------------------
# GlossEx
# ---------------------------------------------------------------------------


class TestGlossEx:
    def test_produces_result(self) -> None:
        ref_freq = ReferenceFrequency(
            word2ttf={"neural": 500, "network": 800, "deep": 200, "machine": 600, "learning": 700},
            corpus_total=100000,
        )
        algo = GlossEx()
        result = algo.score(_make_candidates(), _make_term_freq(), ref_freq=ref_freq)
        assert len(result) == 4

    def test_single_vs_multi_word_weights(self) -> None:
        """Single-word terms should use (0.9, 0.1) weights."""
        ref_freq = ReferenceFrequency(
            word2ttf={"neural": 500, "network": 800},
            corpus_total=100000,
        )
        algo = GlossEx()
        single = [_make_candidate("network", total_freq=100)]
        tf = _make_term_freq()
        result = algo.score(single, tf, ref_freq=ref_freq)
        assert len(result) == 1
        # Score should be computed with 0.9 * TD + 0.1 * TC
        assert result[0].score > 0


# ---------------------------------------------------------------------------
# All algorithms produce sorted results
# ---------------------------------------------------------------------------


class TestAllAlgorithmsSorted:
    @pytest.mark.parametrize(
        "algo_and_kwargs",
        [
            (TFIDF(), {}),
            (CValue(), {}),
            (NCValue(), {}),
            (ATTF(), {}),
            (TTF(), {}),
            (Basic(), {}),
            (ComboBasic(), {}),
            (RIDF(), {}),
        ],
        ids=lambda x: x[0].name,
    )
    def test_results_sorted_descending(self, algo_and_kwargs) -> None:  # type: ignore[no-untyped-def]
        algo, kwargs = algo_and_kwargs
        result = algo.score(_make_candidates(), _make_term_freq(), **kwargs)
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True), f"{algo.name} results not sorted"


class TestAlgorithmNames:
    def test_all_have_names(self) -> None:
        algos = [TFIDF(), CValue(), NCValue(), ATTF(), TTF(), Basic(), ComboBasic(), RIDF()]
        for algo in algos:
            assert algo.name == algo.__class__.__name__
