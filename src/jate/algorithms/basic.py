"""Basic scoring algorithm."""

from __future__ import annotations

import math
from typing import Any

from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.features import Containment, TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class Basic(ATERanker):
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

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_scores=True, produces_ranking=True, requires_corpus=True)

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        containment: Containment | None = kwargs.get("containment")

        # Build containment if not provided (fallback)
        if containment is None:
            from jate.features import Containment as _Cont
            from jate.features import TermComponentIndex

            tci = TermComponentIndex.build(candidates)
            containment = _Cont.build(candidates, tci)

        result = TermExtractionResult()
        for candidate in candidates:
            nf = candidate.normalized_form
            ttf = term_freq.get_ttf(nf)
            word_count = len(nf.split())

            if ttf <= 0:
                s = 0.0
            elif word_count == 1:
                s = math.log(ttf)
            else:
                log_f = math.log(ttf)
                et = float(len(containment.get_parents(nf)))
                s = word_count * log_f + self._alpha * et

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
