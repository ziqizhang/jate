"""TermEx scoring algorithm."""

from __future__ import annotations

import math

from jate.algorithms._reference_utils import _match_orders_of_magnitude
from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore


class TermEx(Algorithm):
    """TermEx: domain pertinence + context scoring.

    See Sclano et al. (2007), *TermExtractor: a Web application to learn the
    shared terminology of emergent web communities*.

    ``score = alpha * DP + beta * DC + zeta * LC``

    where:
    - DP (Domain Pertinence) = ratio of word frequencies in target vs reference
    - DC (Domain Consensus) = entropy-based measure of term distribution
    - LC (Lexical Cohesion) = ``T * log(ttf) * ttf / sum_word_freq``

    Constructor takes reference corpus data and weight parameters.
    """

    def __init__(
        self,
        reference_frequencies: dict[str, int],
        reference_total: int,
        word_frequencies: dict[str, int] | None = None,
        doc_term_frequencies: dict[str, dict[str, int]] | None = None,
        doc_totals: dict[str, int] | None = None,
        alpha: float = 0.33,
        beta: float = 0.33,
        zeta: float = 0.34,
    ) -> None:
        self._ref_freq = reference_frequencies
        self._ref_total = reference_total
        self._word_freq = word_frequencies or {}
        self._doc_term_freq = doc_term_frequencies or {}
        self._doc_totals = doc_totals or {}
        self._alpha = alpha
        self._beta = beta
        self._zeta = zeta

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
        return "TermEx: domain pertinence + context scoring"

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

            # DP: Domain Pertinence
            dp_upper = 0.0
            dp_lower = 0.0
            sum_fwi = 0.0
            for wi in elements:
                wf = self._word_freq.get(wi, 0)
                if wf == 0:
                    wf = corpus_store.get_term_frequency(wi)
                dp_upper += wf
                sum_fwi += wf

                ref_f = self._ref_freq.get(wi, 0)
                if ref_f == 0:
                    ref_f_val = self._null_prob * self._ref_total if self._ref_total > 0 else 1.0
                else:
                    ref_f_val = float(ref_f)
                ref_f_val *= self._oom_scalar
                dp_lower += ref_f_val

            ref_total = self._ref_total if self._ref_total > 0 else 1
            dp = (dp_upper / total_words) / (dp_lower / ref_total) if dp_lower > 0 else 0.0

            # DC: Domain Consensus (entropy over documents)
            dc = 0.0
            for doc_id, doc_tf_map in self._doc_term_freq.items():
                tfid = doc_tf_map.get(nf, 0)
                ttfid = self._doc_totals.get(doc_id, 1)
                if ttfid == 0:
                    ttfid = 1
                norm = tfid / ttfid if tfid > 0 else 0.0
                if norm > 0:
                    dc += norm * math.log(norm)
            dc = -dc  # negate as in Java

            # LC: Lexical Cohesion
            ttf = corpus_store.get_term_frequency(nf)
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
