"""GlossEx scoring algorithm."""

from __future__ import annotations

import math
import warnings
from typing import Any

from jate.algorithms._reference_utils import _match_orders_of_magnitude
from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.features import ReferenceFrequency, TermFrequency, WordFrequency
from jate.models import Candidate, Term, TermExtractionResult


class GlossEx(ATERanker):
    """GlossEx: domain specificity using glossary comparison.

    See Park et al. (2002), *Automatic Glossary Extraction: beyond
    terminology identification*.

    ``C(T) = alpha * TD(T) + beta * TC(T)``

    where:
    - TD = average word-level domain specificity (target vs reference)
    - TC = ``(T * log10(ttf + 1) * ttf) / (sum_word_freq + 1)``

    For single-word terms, uses weights ``(0.9, 0.1)`` instead.

    Required kwargs
    ---------------
    word_freq : WordFrequency
        Word-level corpus frequency statistics.
    ref_freq : ReferenceFrequency
        Reference corpus word frequency statistics.
    """

    def __init__(self, alpha: float = 0.2, beta: float = 0.8) -> None:
        self._alpha = alpha
        self._beta = beta

    @property
    def description(self) -> str:
        return "GlossEx: domain specificity via glossary comparison"

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_scores=True, produces_ranking=True, requires_corpus=True)

    def doc_level_compatibility(self, term_freq: TermFrequency, **kwargs: Any) -> None:
        if kwargs.get("ref_freq") is None:
            warnings.warn(
                f"{self.name} without a reference corpus falls back to self-reference, "
                "producing near-identical scores for all terms. "
                "Provide a reference frequency file via JATEConfig for meaningful results.",
                stacklevel=3,
            )

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        word_freq: WordFrequency | None = kwargs.get("word_freq")
        ref_freq: ReferenceFrequency | None = kwargs.get("ref_freq")

        if ref_freq is None:
            ref_freq = ReferenceFrequency()

        null_prob = ref_freq.null_prob
        ref_total = ref_freq.corpus_total

        # Compute OOM scalar
        if word_freq is not None and ref_freq.word2ttf:
            oom_scalar = _match_orders_of_magnitude(word_freq.word2ttf, ref_freq.word2ttf, ref_total)
        else:
            oom_scalar = 1.0

        total_words = word_freq.corpus_total if word_freq is not None else term_freq.corpus_total
        if total_words == 0:
            total_words = 1

        result = TermExtractionResult()
        for candidate in candidates:
            nf = candidate.normalized_form
            elements = nf.split()
            t = float(len(elements))

            ttf = term_freq.get_ttf(nf)

            sum_wi = 0.0
            sum_fwi = 0.0

            for wi in elements:
                wf = word_freq.get_ttf(wi) if word_freq is not None else term_freq.get_ttf(wi)

                # Reference normalised probability
                pc_wi = ref_freq.get_ttf_norm(wi)
                if pc_wi == 0.0:
                    pc_wi = null_prob
                pc_wi *= oom_scalar

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
