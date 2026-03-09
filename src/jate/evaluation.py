"""Evaluation metrics for term extraction against a gold standard."""

from __future__ import annotations

from dataclasses import dataclass

from jate.models import TermExtractionResult


@dataclass(slots=True)
class EvaluationResult:
    """Holds precision, recall, F1 and supporting counts."""

    precision: float
    recall: float
    f1: float
    true_positives: int
    false_positives: int
    false_negatives: int
    total_predicted: int
    total_gold: int

    def summary(self) -> str:
        """Return a human-readable summary string."""
        return (
            f"P={self.precision:.4f}  R={self.recall:.4f}  F1={self.f1:.4f}  "
            f"TP={self.true_positives}  FP={self.false_positives}  "
            f"FN={self.false_negatives}  "
            f"predicted={self.total_predicted}  gold={self.total_gold}"
        )


class Evaluator:
    """Evaluates term extraction results against a gold standard.

    All comparisons are case-insensitive (terms are lowercased).
    """

    def __init__(self, gold_terms: set[str]) -> None:
        """Initialise with a set of gold standard terms.

        Parameters
        ----------
        gold_terms:
            The reference set of valid terms. They will be normalised
            to lowercase internally.
        """
        self._gold: set[str] = {t.lower() for t in gold_terms}

    @property
    def gold_terms(self) -> set[str]:
        """Return the normalised gold term set."""
        return self._gold

    def evaluate(self, result: TermExtractionResult) -> EvaluationResult:
        """Compute precision, recall and F1 using exact (case-insensitive) match.

        Parameters
        ----------
        result:
            The extraction result to evaluate.

        Returns
        -------
        EvaluationResult
        """
        predicted = {t.string.lower() for t in result}
        return self._compute(predicted)

    def evaluate_at_k(self, result: TermExtractionResult, k: int) -> EvaluationResult:
        """Evaluate only the top-*k* terms (by their current order).

        Parameters
        ----------
        result:
            The extraction result (should already be sorted by score).
        k:
            Number of top terms to consider.

        Returns
        -------
        EvaluationResult
        """
        predicted = {t.string.lower() for t in list(result)[:k]}
        return self._compute(predicted)

    # ------------------------------------------------------------------
    # Internal
    # ------------------------------------------------------------------

    def _compute(self, predicted: set[str]) -> EvaluationResult:
        tp = predicted & self._gold
        fp = predicted - self._gold
        fn = self._gold - predicted

        n_tp = len(tp)
        n_fp = len(fp)
        n_fn = len(fn)
        total_predicted = len(predicted)
        total_gold = len(self._gold)

        precision = n_tp / total_predicted if total_predicted > 0 else 0.0
        recall = n_tp / total_gold if total_gold > 0 else 0.0
        if precision + recall > 0:
            f1 = 2 * precision * recall / (precision + recall)
        else:
            f1 = 0.0

        return EvaluationResult(
            precision=precision,
            recall=recall,
            f1=f1,
            true_positives=n_tp,
            false_positives=n_fp,
            false_negatives=n_fn,
            total_predicted=total_predicted,
            total_gold=total_gold,
        )
