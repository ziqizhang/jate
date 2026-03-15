"""Weirdness scoring algorithm."""

from __future__ import annotations

import math
import warnings
from typing import Any

from jate.algorithms._reference_utils import _match_orders_of_magnitude
from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.features import ReferenceFrequency, TermFrequency, WordFrequency
from jate.models import Candidate, Term, TermExtractionResult


class Weirdness(ATERanker):
    """Weirdness index for term recognition.

    See Ahmad et al. (1999), *Surrey Participation in TREC8: Weirdness
    Indexing for Logical Document Extrapolation and Retrieval*.

    For each component word *wi* of term *t*:
    ``v = (freq(wi) / total_words_corpus) / prob_ref(wi)``

    ``score = (1/T) * sum(log(v) for wi in t)``

    where *T* is the number of words in *t*.

    Required kwargs
    ---------------
    word_freq : WordFrequency
        Word-level corpus frequency statistics.
    ref_freq : ReferenceFrequency
        Reference corpus word frequency statistics.
    """

    def __init__(self, match_oom: bool = True) -> None:
        self._match_oom = match_oom

    @property
    def description(self) -> str:
        return "Weirdness index comparing target vs reference corpus"

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
        if self._match_oom and word_freq is not None and ref_freq.word2ttf:
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
            sum_wi = 0.0

            for wi in elements:
                # Word frequency in target corpus
                freq = word_freq.get_ttf(wi) if word_freq is not None else term_freq.get_ttf(wi)
                if freq == 0:
                    continue

                # Normalised probability in reference corpus
                pc_wi = ref_freq.get_ttf_norm(wi)
                if pc_wi == 0.0:
                    pc_wi = null_prob
                pc_wi *= oom_scalar

                v = (freq / total_words) / pc_wi
                sum_wi += math.log(v)

            td = sum_wi / t if t > 0 else 0.0
            term = Term(
                string=candidate.normalized_form,
                score=td,
                frequency=term_freq.get_ttf(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
