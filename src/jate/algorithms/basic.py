"""Basic scoring algorithm."""

from __future__ import annotations

import math

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


class Basic(Algorithm):
    """Baseline method from Bordea, Buitelaar & Polajnar (2013).

    ``Basic(t) = |t| * log(f(t)) + alpha * e_t``

    where ``|t|`` is the number of words, ``f(t)`` is total frequency,
    and ``e_t`` is the number of longer terms containing *t*.
    For single-word terms: ``score = log(f(t))``.
    """

    def __init__(self, alpha: float = 0.72) -> None:
        self._alpha = alpha

    @property
    def description(self) -> str:
        return "Basic term scoring (Bordea et al. 2013)"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
        containment: dict[str, list[str]] | None = None,
    ) -> TermExtractionResult:
        # Build containment index if not provided
        if containment is None:
            containment = {}
            for c in candidates:
                parents: list[str] = []
                for other in candidates:
                    if other.normalized_form == c.normalized_form:
                        continue
                    if (
                        len(other.normalized_form.split()) > len(c.normalized_form.split())
                        and f" {c.normalized_form} " in f" {other.normalized_form} "
                    ):
                        parents.append(other.normalized_form)
                containment[c.normalized_form] = parents

        result = TermExtractionResult()
        for candidate in candidates:
            nf = candidate.normalized_form
            ttf = corpus_store.get_term_frequency(nf)
            word_count = len(nf.split())

            if ttf <= 0:
                s = 0.0
            elif word_count == 1:
                s = math.log(ttf)
            else:
                log_f = math.log(ttf)
                et = float(len(containment.get(nf, [])))
                s = word_count * log_f + self._alpha * et

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
