"""Average Total Term Frequency (ATTF) scoring algorithm."""

from __future__ import annotations

import warnings
from typing import Any

from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.features import TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class ATTF(ATERanker):
    """Average Total Term Frequency.

    ``score = ttf / df`` where *ttf* is total term frequency in corpus and
    *df* is the number of documents containing the term.
    """

    @property
    def description(self) -> str:
        return "Average Total Term Frequency: ttf / df"

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_scores=True, produces_ranking=True, requires_corpus=True)

    def doc_level_compatibility(self, term_freq: TermFrequency, **kwargs: Any) -> None:
        if term_freq.total_docs <= 1:
            warnings.warn(
                "ATTF degrades to TTF on a single document (DF=1 for all terms). "
                "Results will be equivalent to the TTF algorithm.",
                stacklevel=3,
            )

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        result = TermExtractionResult()

        for candidate in candidates:
            ttf = term_freq.get_ttf(candidate.normalized_form)
            df = term_freq.get_df(candidate.normalized_form)
            if ttf == 0 or df == 0:
                s = 0.0
            else:
                s = ttf / df
            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
