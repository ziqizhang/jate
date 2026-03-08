"""Average Total Term Frequency (ATTF) scoring algorithm."""

from __future__ import annotations

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore


class ATTF(Algorithm):
    """Average Total Term Frequency.

    ``score = ttf / df`` where *ttf* is total term frequency in corpus and
    *df* is the number of documents containing the term.
    """

    @property
    def description(self) -> str:
        return "Average Total Term Frequency: ttf / df"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
    ) -> TermExtractionResult:
        result = TermExtractionResult()

        for candidate in candidates:
            ttf = corpus_store.get_term_frequency(candidate.normalized_form)
            df = corpus_store.get_document_frequency(candidate.normalized_form)
            if ttf == 0 or df == 0:
                s = 0.0
            else:
                s = ttf / df
            term = Term(string=candidate.surface_form, score=s, frequency=ttf)
            result.add(term)

        return result.sort()
