"""RAKE (Rapid Automatic Keyword Extraction) scoring algorithm."""

from __future__ import annotations

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


class RAKE(Algorithm):
    """Rapid Automatic Keyword Extraction.

    See Rose et al. (2010), *Automatic Keyword Extraction from Individual
    Documents*.

    For each component word, ``word_score = degree(word) / freq(word)``.
    The candidate score is the sum of word scores for all component words.

    Degree of a word = its own frequency + sum over all parent terms
    containing this word of ``parent_tf * (len(parent) - 1)``.

    Constructor takes *word_frequencies*: a dict of individual word
    frequencies in the corpus, and *term_component_index*: a dict mapping
    each word to a list of ``(parent_term, word_count)`` tuples.
    """

    def __init__(
        self,
        word_frequencies: dict[str, int],
        term_component_index: dict[str, list[tuple[str, int]]],
    ) -> None:
        self._word_frequencies = word_frequencies
        self._term_component_index = term_component_index

    @property
    def description(self) -> str:
        return "RAKE: Rapid Automatic Keyword Extraction"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        result = TermExtractionResult()

        for candidate in candidates:
            nf = candidate.normalized_form
            elements = nf.split()
            total_score = 0.0

            for word in elements:
                freq = self._word_frequencies.get(word, 0)
                if freq == 0:
                    continue

                # Degree starts at word frequency
                degree = freq

                # Add contributions from parent terms containing this word
                parent_terms = self._term_component_index.get(word, [])
                for parent_term_str, parent_word_count in parent_terms:
                    if parent_word_count == 1:
                        continue
                    parent_tf = corpus_store.get_term_frequency(parent_term_str)
                    parent_elements = parent_term_str.split()
                    for ep in parent_elements:
                        if ep == word:
                            continue
                        degree += parent_tf

                word_score = degree / freq
                total_score += word_score

            term = Term(
                string=candidate.normalized_form,
                score=total_score,
                frequency=corpus_store.get_term_frequency(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
