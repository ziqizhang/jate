"""NC-Value scoring algorithm."""

from __future__ import annotations

from typing import Any

from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.algorithms.cvalue import CValue
from jate.features import Containment, ContextFrequency, TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class NCValue(ATERanker):
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

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_scores=True, produces_ranking=True, requires_corpus=True)

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        containment: Containment | None = kwargs.get("containment")
        context_freq: ContextFrequency | None = kwargs.get("context_freq")

        # First compute C-Value scores, forwarding containment if provided
        cvalue_algo = CValue()
        cvalue_kwargs: dict[str, Any] = {}
        if containment is not None:
            cvalue_kwargs["containment"] = containment
        cvalue_result = cvalue_algo.score(candidates, term_freq, **cvalue_kwargs)
        cvalue_scores: dict[str, float] = {t.string: t.score for t in cvalue_result}

        # Compute context scores
        if context_freq is not None:
            context_scores = self._context_from_adjacency(candidates, context_freq)
        else:
            context_scores = self._context_from_cooccurrence(candidates, term_freq)

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
                frequency=term_freq.get_ttf(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()

    def _context_from_adjacency(
        self,
        candidates: list[Candidate],
        context_freq: ContextFrequency,
    ) -> dict[str, float]:
        """Frantzi et al. context scoring from adjacent words.

        weight(w) = t(w) / n
        where t(w) = number of distinct candidate terms that word w appears adjacent to
        and n = total number of candidate terms

        context_score(term) = sum(weight(w) * freq(w, term)) for each adjacent word w
        """
        n = len(candidates) if candidates else 1

        # Count t(w): how many distinct terms each word is adjacent to
        word_term_count: dict[str, int] = {}
        for cand in candidates:
            adj = context_freq.get_adjacent_words(cand.normalized_form)
            for word in adj:
                word_term_count[word] = word_term_count.get(word, 0) + 1

        context_scores: dict[str, float] = {}
        for cand in candidates:
            nf = cand.normalized_form
            adj = context_freq.get_adjacent_words(nf)
            score = 0.0
            for word, freq in adj.items():
                t_w = word_term_count.get(word, 0)
                weight = t_w / n
                score += weight * freq
            context_scores[nf] = score
        return context_scores

    def _context_from_cooccurrence(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
    ) -> dict[str, float]:
        """Fallback: document-level co-occurrence context scoring.

        Uses shared documents between candidates as a simple co-occurrence proxy.
        """
        context_scores: dict[str, float] = {}
        for candidate in candidates:
            ctx_score = 0.0
            nf = candidate.normalized_form
            nf_docs = set(term_freq.term2fid.get(nf.lower(), {}).keys())
            for other in candidates:
                onf = other.normalized_form
                if onf == nf:
                    continue
                other_docs = set(term_freq.term2fid.get(onf.lower(), {}).keys())
                cooc = len(nf_docs & other_docs)
                if cooc > 0:
                    ctx_score += cooc
            context_scores[nf] = ctx_score
        return context_scores
