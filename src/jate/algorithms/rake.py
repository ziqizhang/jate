"""RAKE (Rapid Automatic Keyword Extraction) scoring algorithm."""

from __future__ import annotations

from typing import Any

from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.features import TermComponentIndex, TermFrequency, WordFrequency
from jate.models import Candidate, Term, TermExtractionResult


class RAKE(ATERanker):
    """Rapid Automatic Keyword Extraction.

    See Rose et al. (2010), *Automatic Keyword Extraction from Individual
    Documents*.

    For each component word, ``word_score = degree(word) / freq(word)``.
    The candidate score is the sum of word scores for all component words.

    Degree of a word = its own frequency + sum over all parent terms
    containing this word of ``parent_tf * (len(parent) - 1)``.

    Required kwargs
    ---------------
    word_freq : WordFrequency
        Word-level corpus frequency statistics.
    term_component_index : TermComponentIndex
        Maps each word to candidate terms containing it.
    """

    @property
    def description(self) -> str:
        return "RAKE: Rapid Automatic Keyword Extraction"

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_scores=True, produces_ranking=True, requires_corpus=True)

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        word_freq: WordFrequency | None = kwargs.get("word_freq")
        tci: TermComponentIndex | None = kwargs.get("term_component_index")

        # Build TermComponentIndex from candidates if not provided
        if tci is None:
            tci = TermComponentIndex.build(candidates)

        result = TermExtractionResult()

        for candidate in candidates:
            nf = candidate.normalized_form
            elements = nf.split()
            total_score = 0.0

            for word in elements:
                freq = word_freq.get_ttf(word) if word_freq is not None else 0
                if freq == 0:
                    continue

                # Degree starts at word frequency
                degree = freq

                # Add contributions from parent terms containing this word
                parent_terms = tci.get_sorted(word)
                for parent_term_str, parent_word_count in parent_terms:
                    if parent_word_count == 1:
                        continue
                    parent_tf = term_freq.get_ttf(parent_term_str)
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
                frequency=term_freq.get_ttf(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
