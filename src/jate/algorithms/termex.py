"""TermEx scoring algorithm."""

from __future__ import annotations

import math

from typing import Any

from jate.algorithms._reference_utils import _match_orders_of_magnitude
from jate.algorithms.base import Algorithm
from jate.features import ReferenceFrequency, TermFrequency, WordFrequency
from jate.models import Candidate, Term, TermExtractionResult


class TermEx(Algorithm):
    """TermEx: domain pertinence + context scoring.

    See Sclano et al. (2007), *TermExtractor: a Web application to learn the
    shared terminology of emergent web communities*.

    ``score = alpha * DP + beta * DC + zeta * LC``

    where:
    - DP (Domain Pertinence) = ratio of word frequencies in target vs reference
    - DC (Domain Consensus) = entropy-based measure of term distribution
    - LC (Lexical Cohesion) = ``T * log(ttf) * ttf / sum_word_freq``

    Required kwargs
    ---------------
    word_freq : WordFrequency
        Word-level corpus frequency statistics.
    ref_freq : ReferenceFrequency
        Reference corpus word frequency statistics.
    """

    def __init__(
        self,
        alpha: float = 0.33,
        beta: float = 0.33,
        zeta: float = 0.34,
    ) -> None:
        self._alpha = alpha
        self._beta = beta
        self._zeta = zeta

    @property
    def description(self) -> str:
        return "TermEx: domain pertinence + context scoring"

    def score(
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
            oom_scalar = _match_orders_of_magnitude(
                word_freq.word2ttf, ref_freq.word2ttf, ref_total
            )
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

            # DP: Domain Pertinence
            dp_upper = 0.0
            dp_lower = 0.0
            sum_fwi = 0.0
            for wi in elements:
                wf = word_freq.get_ttf(wi) if word_freq is not None else term_freq.get_ttf(wi)
                dp_upper += wf
                sum_fwi += wf

                ref_f = ref_freq.get_ttf(wi)
                if ref_f == 0:
                    ref_f_val = null_prob * ref_total if ref_total > 0 else 1.0
                else:
                    ref_f_val = float(ref_f)
                ref_f_val *= oom_scalar
                dp_lower += ref_f_val

            ref_total_safe = ref_total if ref_total > 0 else 1
            dp = (dp_upper / total_words) / (dp_lower / ref_total_safe) if dp_lower > 0 else 0.0

            # DC: Domain Consensus (entropy over documents)
            dc = 0.0
            doc_freq_map = term_freq.get_doc_freq_map(nf)
            for doc_id, tfid in doc_freq_map.items():
                # Approximate per-doc total from term_freq
                # Sum all term frequencies in this doc
                ttfid = sum(
                    doc_freqs.get(doc_id, 0)
                    for doc_freqs in term_freq.term2fid.values()
                    if doc_id in doc_freqs
                )
                if ttfid == 0:
                    ttfid = 1
                norm = tfid / ttfid if tfid > 0 else 0.0
                if norm > 0:
                    dc += norm * math.log(norm)
            dc = -dc  # negate as in Java

            # LC: Lexical Cohesion
            ttf = term_freq.get_ttf(nf)
            if sum_fwi == 0:
                lc = 0.0
            else:
                lc = (t * math.log(ttf + 0.000001) * ttf) / sum_fwi

            s = self._alpha * dp + self._beta * dc + self._zeta * lc

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
