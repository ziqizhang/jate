"""GlossEx scoring algorithm."""

from __future__ import annotations

import math

from jate.algorithms._reference_utils import _match_orders_of_magnitude
from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore


class GlossEx(Algorithm):
    """GlossEx: domain specificity using glossary comparison.

    See Park et al. (2002), *Automatic Glossary Extraction: beyond
    terminology identification*.

    ``C(T) = alpha * TD(T) + beta * TC(T)``

    where:
    - TD = average word-level domain specificity (target vs reference)
    - TC = ``(T * log10(ttf + 1) * ttf) / (sum_word_freq + 1)``

    For single-word terms, uses weights ``(0.9, 0.1)`` instead.

    Constructor takes reference corpus data.
    """

    def __init__(
        self,
        reference_frequencies: dict[str, int],
        reference_total: int,
        word_frequencies: dict[str, int] | None = None,
        alpha: float = 0.2,
        beta: float = 0.8,
    ) -> None:
        self._ref_freq = reference_frequencies
        self._ref_total = reference_total
        self._word_freq = word_frequencies or {}
        self._alpha = alpha
        self._beta = beta

        # Null probability for missing reference words
        if reference_frequencies and reference_total > 0:
            min_freq = min(reference_frequencies.values())
            self._null_prob = min_freq / reference_total
        else:
            self._null_prob = 0.1

        # Order-of-magnitude scaling (Java ReferenceBased.matchOrdersOfMagnitude)
        self._oom_scalar = (
            _match_orders_of_magnitude(self._word_freq, reference_frequencies, reference_total)
            if self._word_freq
            else 1.0
        )

    @property
    def description(self) -> str:
        return "GlossEx: domain specificity via glossary comparison"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
    ) -> TermExtractionResult:
        total_words = corpus_store.get_corpus_total()
        if total_words == 0:
            total_words = 1

        result = TermExtractionResult()
        for candidate in candidates:
            nf = candidate.normalized_form
            elements = nf.split()
            t = float(len(elements))

            ttf = corpus_store.get_term_frequency(nf)

            sum_wi = 0.0
            sum_fwi = 0.0

            for wi in elements:
                wf = self._word_freq.get(wi, 0)
                if wf == 0:
                    wf = corpus_store.get_term_frequency(wi)

                # Reference normalised probability
                ref_f = self._ref_freq.get(wi, 0)
                if ref_f > 0 and self._ref_total > 0:
                    pc_wi = ref_f / self._ref_total
                else:
                    pc_wi = self._null_prob
                pc_wi *= self._oom_scalar

                sum_wi += (wf / total_words) / pc_wi
                sum_fwi += wf

            td = sum_wi / t if t > 0 else 0.0
            tc = (t * math.log10(ttf + 1) * ttf) / (sum_fwi + 1)

            # Single-word terms use different weights
            if t == 1:
                s = 0.9 * td + 0.1 * tc
            else:
                s = self._alpha * td + self._beta * tc

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
