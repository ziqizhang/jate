"""Tests for jate.evaluation module."""

from __future__ import annotations

import pytest

from jate.evaluation import EvaluationResult, Evaluator
from jate.models import Term, TermExtractionResult

# ---------------------------------------------------------------------------
# Helpers
# ---------------------------------------------------------------------------


def _make_result(terms: list[str], scores: list[float] | None = None) -> TermExtractionResult:
    """Build a TermExtractionResult from a list of term strings."""
    if scores is None:
        scores = [1.0 - i * 0.01 for i in range(len(terms))]
    return TermExtractionResult([Term(string=t, score=s) for t, s in zip(terms, scores)])


# ---------------------------------------------------------------------------
# Tests for Evaluator
# ---------------------------------------------------------------------------


class TestEvaluatorBasic:
    """Core precision / recall / F1 calculations."""

    def test_perfect_match(self):
        gold = {"alpha", "beta", "gamma"}
        predicted = _make_result(["alpha", "beta", "gamma"])
        ev = Evaluator(gold).evaluate(predicted)

        assert ev.precision == 1.0
        assert ev.recall == 1.0
        assert ev.f1 == 1.0
        assert ev.true_positives == 3
        assert ev.false_positives == 0
        assert ev.false_negatives == 0

    def test_no_overlap(self):
        gold = {"alpha", "beta"}
        predicted = _make_result(["gamma", "delta"])
        ev = Evaluator(gold).evaluate(predicted)

        assert ev.precision == 0.0
        assert ev.recall == 0.0
        assert ev.f1 == 0.0
        assert ev.true_positives == 0
        assert ev.false_positives == 2
        assert ev.false_negatives == 2

    def test_partial_overlap(self):
        gold = {"alpha", "beta", "gamma", "delta"}
        predicted = _make_result(["alpha", "beta", "epsilon"])
        ev = Evaluator(gold).evaluate(predicted)

        assert ev.true_positives == 2
        assert ev.false_positives == 1
        assert ev.false_negatives == 2
        assert ev.total_predicted == 3
        assert ev.total_gold == 4

        assert ev.precision == pytest.approx(2 / 3)
        assert ev.recall == pytest.approx(2 / 4)
        expected_f1 = 2 * (2 / 3) * (2 / 4) / ((2 / 3) + (2 / 4))
        assert ev.f1 == pytest.approx(expected_f1)

    def test_empty_predicted(self):
        gold = {"alpha", "beta"}
        predicted = _make_result([])
        ev = Evaluator(gold).evaluate(predicted)

        assert ev.precision == 0.0
        assert ev.recall == 0.0
        assert ev.f1 == 0.0
        assert ev.true_positives == 0
        assert ev.false_positives == 0
        assert ev.false_negatives == 2

    def test_empty_gold(self):
        gold: set[str] = set()
        predicted = _make_result(["alpha", "beta"])
        ev = Evaluator(gold).evaluate(predicted)

        assert ev.precision == 0.0
        assert ev.recall == 0.0
        assert ev.f1 == 0.0
        assert ev.total_gold == 0

    def test_both_empty(self):
        ev = Evaluator(set()).evaluate(_make_result([]))
        assert ev.precision == 0.0
        assert ev.recall == 0.0
        assert ev.f1 == 0.0
        assert ev.true_positives == 0


class TestEvaluatorCaseInsensitive:
    """Verify case-insensitive matching."""

    def test_mixed_case_gold(self):
        gold = {"Natural Language Processing", "machine learning"}
        predicted = _make_result(["natural language processing", "MACHINE LEARNING"])
        ev = Evaluator(gold).evaluate(predicted)

        assert ev.true_positives == 2
        assert ev.precision == 1.0
        assert ev.recall == 1.0

    def test_mixed_case_predicted(self):
        gold = {"nlp", "ml"}
        predicted = _make_result(["NLP", "ML", "AI"])
        ev = Evaluator(gold).evaluate(predicted)

        assert ev.true_positives == 2
        assert ev.false_positives == 1


class TestEvaluateAtK:
    """Top-K evaluation."""

    def test_at_k_basic(self):
        gold = {"alpha", "gamma"}
        # Ordered by score: alpha (best), beta, gamma, delta
        predicted = _make_result(["alpha", "beta", "gamma", "delta"])
        ev = Evaluator(gold).evaluate_at_k(predicted, k=2)

        # Top 2 = alpha, beta -> TP=1 (alpha), FP=1 (beta)
        assert ev.true_positives == 1
        assert ev.false_positives == 1
        assert ev.total_predicted == 2
        assert ev.precision == pytest.approx(0.5)

    def test_at_k_larger_than_results(self):
        gold = {"alpha"}
        predicted = _make_result(["alpha", "beta"])
        ev = Evaluator(gold).evaluate_at_k(predicted, k=100)

        assert ev.total_predicted == 2
        assert ev.true_positives == 1

    def test_at_k_zero(self):
        gold = {"alpha"}
        predicted = _make_result(["alpha", "beta"])
        ev = Evaluator(gold).evaluate_at_k(predicted, k=0)

        assert ev.total_predicted == 0
        assert ev.precision == 0.0


class TestEvaluationResultSummary:
    """EvaluationResult.summary() formatting."""

    def test_summary_contains_metrics(self):
        ev = EvaluationResult(
            precision=0.75,
            recall=0.5,
            f1=0.6,
            true_positives=3,
            false_positives=1,
            false_negatives=3,
            total_predicted=4,
            total_gold=6,
        )
        s = ev.summary()
        assert "P=0.7500" in s
        assert "R=0.5000" in s
        assert "F1=0.6000" in s
        assert "TP=3" in s
        assert "FP=1" in s
        assert "FN=3" in s
        assert "predicted=4" in s
        assert "gold=6" in s


class TestEvaluatorDuplicates:
    """Edge case: duplicate terms in predicted results."""

    def test_duplicates_collapsed(self):
        gold = {"alpha", "beta"}
        # Duplicate "alpha" in predicted — should only count once
        predicted = _make_result(["alpha", "alpha", "beta"])
        ev = Evaluator(gold).evaluate(predicted)

        # set conversion collapses duplicates
        assert ev.true_positives == 2
        assert ev.total_predicted == 2
        assert ev.precision == 1.0
