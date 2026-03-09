"""C-Value scoring algorithm."""

from __future__ import annotations

import math

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


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
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
        containment: dict[str, list[str]] | None = None,
    ) -> TermExtractionResult:
        # Build containment index if not provided: for each candidate, find
        # longer candidates that contain it as a substring.
        if containment is None:
            containment = {}
            for c in candidates:
                parents: list[str] = []
                for other in candidates:
                    if other.normalized_form == c.normalized_form:
                        continue
                    # Parent must be strictly longer and contain this term
                    # as a whole-word subsequence (space-padded `in` check).
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

            # Java uses log2(|a| + 0.1) to handle single-word terms
            log2a = math.log2(word_count + 0.1)
            freq_a = float(ttf)

            parent_terms = containment.get(nf, [])
            p_ta = float(len(parent_terms))

            if p_ta == 0:
                s = log2a * freq_a
            else:
                sum_freq_b = sum(corpus_store.get_term_frequency(pt) for pt in parent_terms)
                s = log2a * (freq_a - sum_freq_b / p_ta)

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
