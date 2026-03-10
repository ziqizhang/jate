"""Voting ensemble algorithm using reciprocal rank fusion."""

from __future__ import annotations

from collections import defaultdict

from jate.models import Term, TermExtractionResult


class Voting:
    """Reciprocal rank fusion across multiple algorithm results.

    Mirrors Java's ``Voting``.  For each ranked list the score contributed
    by a term at rank *i* (0-indexed) is ``weight / (i + 1)``.
    """

    @staticmethod
    def combine(
        results: list[tuple[TermExtractionResult, float]],
    ) -> TermExtractionResult:
        """Fuse multiple ranked term lists into a single scored result.

        Parameters
        ----------
        results:
            Pairs of ``(ranked_result, weight)``.  Each
            :class:`TermExtractionResult` should already be sorted by its
            algorithm's score (highest first).

        Returns
        -------
        TermExtractionResult
            A new result sorted by aggregated vote score.
        """
        vote_scores: dict[str, float] = defaultdict(float)
        term_data: dict[str, Term] = {}

        for ranked_result, weight in results:
            for i, term in enumerate(ranked_result):
                rank_score = weight / (i + 1)
                vote_scores[term.string] += rank_score
                if term.string not in term_data:
                    term_data[term.string] = Term(
                        string=term.string,
                        frequency=term.frequency,
                        surface_forms=set(term.surface_forms),
                    )

        result = TermExtractionResult()
        for term_str, score in vote_scores.items():
            t = term_data[term_str]
            t.score = score
            result.add(t)

        return result.sort()
