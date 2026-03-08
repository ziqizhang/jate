"""TF-IDF scoring algorithm."""

from __future__ import annotations

import math

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore


class TFIDF(Algorithm):
    """TF-IDF modified to work at corpus level.

    ``tf`` is the total term frequency in the corpus (TTF) and
    ``idf = log(N / df)`` where *N* is the total number of documents.
    """

    @property
    def description(self) -> str:
        return "TF-IDF at corpus level: score = tf * log(N / df)"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
    ) -> TermExtractionResult:
        total_docs = corpus_store.get_total_documents()
        result = TermExtractionResult()

        for candidate in candidates:
            tf = corpus_store.get_term_frequency(candidate.normalized_form)
            df = corpus_store.get_document_frequency(candidate.normalized_form)
            if df == 0:
                idf = 0.0
            else:
                idf = math.log(total_docs / df)
            term = Term(
                string=candidate.surface_form,
                score=tf * idf,
                frequency=tf,
            )
            result.add(term)

        return result.sort()
