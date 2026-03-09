"""Chi-Square scoring algorithm."""

from __future__ import annotations

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore


class ChiSquare(Algorithm):
    """Chi-square test for term independence.

    See Matsuo & Ishizuka (2003), *Keyword Extraction from a Single Document
    Using Word Co-Occurrence Statistical Information*.

    Constructor takes *frequent_terms*: a dict mapping frequent/reference terms
    to their expected probabilities, used as the basis for chi-square
    computation.

    For each candidate *w*, the score is:

    ``sum_chi_w = n_w * sum(p_g for g in G)`` (baseline assuming zero
    co-occurrence), then adjusted for actual co-occurrences using:
    ``chi_g = (freq(w,g) - n_w * p_g)^2 / (n_w * p_g)``

    Final score = ``sum_chi_w - max_chi`` (subtract the max single
    chi-square contribution).
    """

    def __init__(
        self,
        frequent_terms: dict[str, float],
        context_term_totals: dict[str, int] | None = None,
    ) -> None:
        """
        Parameters
        ----------
        frequent_terms:
            Mapping of reference/frequent terms to their expected probabilities.
        context_term_totals:
            Mapping of candidate term to the total number of terms in contexts
            where the candidate appears (n_w).  If ``None``, falls back to
            corpus term frequency as a proxy.
        """
        self._frequent_terms = frequent_terms
        self._context_term_totals = context_term_totals or {}
        self._sum_exp_prob = sum(frequent_terms.values())
        self._max_exp_prob = max(frequent_terms.values()) if frequent_terms else 0.0

    @property
    def description(self) -> str:
        return "Chi-Square test for term independence"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
    ) -> TermExtractionResult:
        result = TermExtractionResult()

        for candidate in candidates:
            nf = candidate.normalized_form
            # n_w: total number of terms in contexts where w appears
            n_w = self._context_term_totals.get(nf, corpus_store.get_term_frequency(nf))
            if n_w == 0:
                result.add(Term(
                    string=candidate.normalized_form,
                    score=0.0,
                    frequency=0,
                    surface_forms=set(candidate.surface_forms),
                ))
                continue

            max_chi_square = self._max_exp_prob

            # Baseline: assume all co-occurrences are zero
            sum_chi_w = n_w * self._sum_exp_prob

            # Adjust for actual co-occurrences
            for g_term, p_g in self._frequent_terms.items():
                freq_wg = corpus_store.get_cooccurrences(nf, g_term)
                if freq_wg == 0:
                    continue

                nw_pg = n_w * p_g
                if nw_pg == 0:
                    continue

                # Remove the zero-assumption contribution
                sum_chi_w -= nw_pg

                # Add the real chi-square contribution
                diff = freq_wg - nw_pg
                chi_g = (diff * diff) / nw_pg
                sum_chi_w += chi_g

                if chi_g > max_chi_square:
                    max_chi_square = chi_g

            s = sum_chi_w - max_chi_square

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=corpus_store.get_term_frequency(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
