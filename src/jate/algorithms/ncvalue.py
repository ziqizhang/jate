"""NC-Value scoring algorithm."""

from __future__ import annotations

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.algorithms.cvalue import CValue
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


class NCValue(Algorithm):
    """NC-Value: extends C-Value with context word information.

    ``NC-Value(t) = cvalue_weight * CValue(t) + context_weight * context_score(t)``

    Context score is derived from co-occurring terms.  The weight factors
    default to 0.8 (C-Value) and 0.2 (context) but are configurable.
    """

    def __init__(
        self,
        cvalue_weight: float = 0.8,
        context_weight: float = 0.2,
    ) -> None:
        self._cvalue_weight = cvalue_weight
        self._context_weight = context_weight

    @property
    def description(self) -> str:
        return "NC-Value: C-Value extended with context words"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        # First compute C-Value scores
        cvalue_algo = CValue()
        cvalue_result = cvalue_algo.score(candidates, corpus_store)
        cvalue_scores: dict[str, float] = {t.string: t.score for t in cvalue_result}

        # Compute context weights: for each candidate, sum co-occurrence
        # weights with all other candidates
        context_scores: dict[str, float] = {}
        for candidate in candidates:
            ctx_score = 0.0
            nf = candidate.normalized_form
            for other in candidates:
                if other.normalized_form == nf:
                    continue
                cooc = corpus_store.get_cooccurrences(nf, other.normalized_form)
                if cooc > 0:
                    ctx_score += cooc
            context_scores[nf] = ctx_score

        # Normalise context scores to [0, 1] range
        max_ctx = max(context_scores.values()) if context_scores else 1.0
        if max_ctx == 0:
            max_ctx = 1.0

        result = TermExtractionResult()
        for candidate in candidates:
            nf = candidate.normalized_form
            cv = cvalue_scores.get(nf, 0.0)
            ctx = context_scores.get(nf, 0.0) / max_ctx
            s = self._cvalue_weight * cv + self._context_weight * ctx
            term = Term(
                string=nf,
                score=s,
                frequency=corpus_store.get_term_frequency(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
