"""Weirdness scoring algorithm."""

from __future__ import annotations

import math

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore


class Weirdness(Algorithm):
    """Weirdness index for term recognition.

    See Ahmad et al. (1999), *Surrey Participation in TREC8: Weirdness
    Indexing for Logical Document Extrapolation and Retrieval*.

    For each component word *wi* of term *t*:
    ``v = (freq(wi) / total_words_corpus) / prob_ref(wi)``

    ``score = (1/T) * sum(log(v) for wi in t)``

    where *T* is the number of words in *t*.

    Constructor takes *reference_frequencies*: a dict mapping words to their
    frequencies in the reference corpus, and *reference_total*: total word
    count in the reference corpus.
    """

    def __init__(
        self,
        reference_frequencies: dict[str, int],
        reference_total: int,
        word_frequencies: dict[str, int] | None = None,
    ) -> None:
        self._ref_freq = reference_frequencies
        self._ref_total = reference_total
        self._word_freq = word_frequencies or {}

        # Compute null probability (smallest normalized freq in reference)
        if reference_frequencies and reference_total > 0:
            min_freq = min(reference_frequencies.values())
            self._null_prob = min_freq / reference_total
        else:
            self._null_prob = 0.1

    @property
    def description(self) -> str:
        return "Weirdness index comparing target vs reference corpus"

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
            sum_wi = 0.0

            for wi in elements:
                # Word frequency in target corpus
                freq = self._word_freq.get(wi, 0)
                if freq == 0:
                    freq = corpus_store.get_term_frequency(wi)
                if freq == 0:
                    continue

                # Normalised probability in reference corpus
                ref_freq = self._ref_freq.get(wi, 0)
                if ref_freq > 0 and self._ref_total > 0:
                    pc_wi = ref_freq / self._ref_total
                else:
                    pc_wi = self._null_prob

                v = (freq / total_words) / pc_wi
                sum_wi += math.log(v)

            td = sum_wi / t if t > 0 else 0.0
            term = Term(
                string=candidate.surface_form,
                score=td,
                frequency=corpus_store.get_term_frequency(nf),
            )
            result.add(term)

        return result.sort()
