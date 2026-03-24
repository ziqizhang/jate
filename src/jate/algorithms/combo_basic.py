"""ComboBasic scoring algorithm."""

from __future__ import annotations

import math
from typing import Any

from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.features import Containment, TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class ComboBasic(ATERanker):
    """Combination of Basic scores with parent and child containment.

    ``ComboBasic(t) = |t| * log(f(t)) + alpha * e_t + beta * e'_t``

    where ``e_t`` is the number of longer terms containing *t* (parents) and
    ``e'_t`` is the number of shorter terms contained in *t* (children).
    For single-word terms: ``score = log(f(t))``.
    """

    def __init__(self, alpha: float = 0.75, beta: float = 0.1) -> None:
        self._alpha = alpha
        self._beta = beta

    @property
    def description(self) -> str:
        return "ComboBasic: Basic with parent and child containment"

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_scores=True, produces_ranking=True, requires_corpus=True)

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        containment: Containment | None = kwargs.get("containment")
        child_containment: Containment | None = kwargs.get("child_containment")

        # Build containment if not provided (fallback)
        if containment is None or child_containment is None:
            from jate.features import Containment as _Cont
            from jate.features import TermComponentIndex

            tci = TermComponentIndex.build(candidates)
            if containment is None:
                containment = _Cont.build(candidates, tci)
            if child_containment is None:
                # child_containment is the reverse: for each term, which shorter terms it contains
                if containment is not None:
                    child_containment = containment.reverse()
                else:
                    built = _Cont.build(candidates, tci)
                    child_containment = built.reverse()

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
                etp = float(len(child_containment.get_parents(nf)))
                s = word_count * log_f + self._alpha * et + self._beta * etp

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
