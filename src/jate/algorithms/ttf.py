"""Total Term Frequency (TTF) scoring algorithm."""

from __future__ import annotations

from typing import Any

from jate.algorithms.base import Algorithm
from jate.features import TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class TTF(Algorithm):
    """Total Term Frequency in corpus.

    ``score = ttf`` (raw total frequency).
    """

    @property
    def description(self) -> str:
        return "Total Term Frequency in corpus"

    def score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        result = TermExtractionResult()

        for candidate in candidates:
            ttf = term_freq.get_ttf(candidate.normalized_form)
            term = Term(
                string=candidate.normalized_form,
                score=float(ttf),
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
