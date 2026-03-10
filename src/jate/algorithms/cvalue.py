"""C-Value scoring algorithm."""

from __future__ import annotations

import math
from typing import Any

from jate.algorithms.base import Algorithm
from jate.features import Containment, TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class CValue(Algorithm):
    """C-Value algorithm for multi-word term extraction.

    See Frantzi et al. 2000, *Automatic recognition of multi-word terms:
    the C-value/NC-value method*.

    For each candidate *a*:
    - If *a* is not contained in any longer candidate:
      ``score = log2(|a| + 0.1) * f(a)``
    - Otherwise:
      ``score = log2(|a| + 0.1) * (f(a) - (1/P(Ta)) * sum(f(b) for b in Ta))``

    where ``|a|`` is word count, ``f(a)`` is total frequency, ``Ta`` is the set
    of longer candidates containing *a*, and ``P(Ta) = |Ta|``.
    """

    @property
    def description(self) -> str:
        return "C-Value for multi-word term extraction"

    def score(
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

            # Java uses log2(|a| + 0.1) to handle single-word terms
            log2a = math.log2(word_count + 0.1)
            freq_a = float(ttf)

            parent_terms = containment.get_parents(nf)
            p_ta = float(len(parent_terms))

            if p_ta == 0:
                s = log2a * freq_a
            else:
                sum_freq_b = sum(term_freq.get_ttf(pt) for pt in parent_terms)
                s = log2a * (freq_a - sum_freq_b / p_ta)

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
