"""TF-IDF scoring algorithm."""

from __future__ import annotations

import math

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


class TFIDF(Algorithm):
    """TF-IDF modified to work at corpus level (matches Java JATE).

    ``tf_norm = TTF / (corpus_total + 1)`` is the normalised term frequency and
    ``idf = log(N / df)`` where *N* is the total number of documents.
    The final score is ``tf_norm * idf``.
    """

    @property
    def description(self) -> str:
        return "TF-IDF at corpus level: score = tf_norm * log(N / df)"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        total_docs = corpus_store.get_total_documents()
        result = TermExtractionResult()

        corpus_total = corpus_store.get_corpus_total()

        for candidate in candidates:
            tf = corpus_store.get_term_frequency(candidate.normalized_form)
            df = corpus_store.get_document_frequency(candidate.normalized_form)
            tf_norm = tf / (corpus_total + 1)
            if df == 0:
                idf = 0.0
            else:
                idf = math.log(total_docs / df)
            term = Term(
                string=candidate.normalized_form,
                score=tf_norm * idf,
                frequency=tf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
