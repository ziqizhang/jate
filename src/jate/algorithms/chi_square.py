"""Chi-Square scoring algorithm."""

from __future__ import annotations

from typing import Any

from jate.algorithms.base import Algorithm
from jate.features import ChiSquareFrequentTerms, ContextFrequency, Cooccurrence, TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class ChiSquare(Algorithm):
    """Chi-square test for term independence.

    See Matsuo & Ishizuka (2003), *Keyword Extraction from a Single Document
    Using Word Co-Occurrence Statistical Information*.

    For each candidate *w*, the score is:

    ``sum_chi_w = n_w * sum(p_g for g in G)`` (baseline assuming zero
    co-occurrence), then adjusted for actual co-occurrences using:
    ``chi_g = (freq(w,g) - n_w * p_g)^2 / (n_w * p_g)``

    Final score = ``sum_chi_w - max_chi`` (subtract the max single
    chi-square contribution).

    Required kwargs
    ---------------
    context_freq : ContextFrequency
        Sentence-level context frequencies.
    cooccurrence : Cooccurrence
        Term co-occurrence data.
    chi_square_ft : ChiSquareFrequentTerms
        Expected probabilities for frequent reference terms.
    """

    @property
    def description(self) -> str:
        return "Chi-Square test for term independence"

    def score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        context_freq: ContextFrequency | None = kwargs.get("context_freq")
        cooccurrence: Cooccurrence | None = kwargs.get("cooccurrence")
        chi_square_ft: ChiSquareFrequentTerms | None = kwargs.get("chi_square_ft")

        # Extract frequent terms data
        if chi_square_ft is not None:
            frequent_terms = chi_square_ft.exp_prob
            sum_exp_prob = chi_square_ft.sum_exp_prob
            max_exp_prob = chi_square_ft.max_exp_prob
        else:
            frequent_terms = {}
            sum_exp_prob = 0.0
            max_exp_prob = 0.0

        # Pre-build per-candidate co-occurrence lookups (sparse).
        # Instead of iterating all frequent_terms for each candidate,
        # only iterate terms that actually co-occur.
        cooc_by_candidate: dict[str, dict[str, int]] = {}
        if cooccurrence is not None:
            freq_term_set = set(frequent_terms)
            for candidate in candidates:
                nf = candidate.normalized_form.lower()
                coocs = cooccurrence.get_cooccurrences_for(nf)
                # Filter to only frequent terms
                filtered = {g: c for g, c in coocs.items() if g in freq_term_set and c > 0}
                if filtered:
                    cooc_by_candidate[nf] = filtered

        result = TermExtractionResult()

        for candidate in candidates:
            nf = candidate.normalized_form
            nf_lower = nf.lower()
            # n_w: total number of terms in contexts where w appears
            if context_freq is not None:
                n_w = context_freq.get_n_w(nf)
            else:
                n_w = term_freq.get_ttf(nf)
            if n_w == 0:
                result.add(
                    Term(
                        string=candidate.normalized_form,
                        score=0.0,
                        frequency=0,
                        surface_forms=set(candidate.surface_forms),
                    )
                )
                continue

            max_chi_square = max_exp_prob

            # Baseline: assume all co-occurrences are zero
            sum_chi_w = n_w * sum_exp_prob

            # Adjust only for terms that actually co-occur (sparse iteration)
            candidate_coocs = cooc_by_candidate.get(nf_lower, {})
            for g_term, freq_wg in candidate_coocs.items():
                p_g = frequent_terms.get(g_term)
                if p_g is None:
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
                frequency=term_freq.get_ttf(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
