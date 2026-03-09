"""Total Term Frequency (TTF) scoring algorithm."""

from __future__ import annotations

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


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
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        result = TermExtractionResult()

        for candidate in candidates:
            ttf = corpus_store.get_term_frequency(candidate.normalized_form)
            term = Term(
                string=candidate.normalized_form,
                score=float(ttf),
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
