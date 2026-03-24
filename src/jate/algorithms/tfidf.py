"""TF-IDF scoring algorithm."""

from __future__ import annotations

import math
from typing import Any

from jate.algorithms.base import AlgorithmIncompatibleError, ATERanker, OutputCapabilities
from jate.features import TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class TFIDF(ATERanker):
    """TF-IDF modified to work at corpus level (matches Java JATE).

    ``tf_norm = TTF / (corpus_total + 1)`` is the normalised term frequency and
    ``idf = log(N / df)`` where *N* is the total number of documents.
    The final score is ``tf_norm * idf``.
    """

    @property
    def description(self) -> str:
        return "TF-IDF at corpus level: score = tf_norm * log(N / df)"

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_scores=True, produces_ranking=True, requires_corpus=True)

    def doc_level_compatibility(self, term_freq: TermFrequency, **kwargs: Any) -> None:
        if term_freq.total_docs <= 1:
            raise AlgorithmIncompatibleError(
                "TF-IDF produces all-zero scores on a single document "
                "(IDF = log(1/1) = 0 for all terms). "
                "Use extract_corpus() with multiple documents, "
                "or choose a different algorithm (e.g., cvalue, rake)."
            )

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        total_docs = term_freq.total_docs
        result = TermExtractionResult()

        for candidate in candidates:
            tf = term_freq.get_ttf(candidate.normalized_form)
            df = term_freq.get_df(candidate.normalized_form)
            tf_norm = term_freq.get_ttf_norm(candidate.normalized_form)
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
