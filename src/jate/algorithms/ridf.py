"""Residual IDF (RIDF) scoring algorithm."""

from __future__ import annotations

import math
from typing import Any

from jate.algorithms.base import Algorithm
from jate.features import TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class RIDF(Algorithm):
    """Residual IDF.

    See Church & Gale (1995), *Inverse Document Frequency (IDF): A Measure
    of Deviation from Poisson*.

    ``RIDF = IDF_observed - IDF_expected``

    where:
    - ``IDF = log(N / df)`` (natural log, matching Java ``Math.log``)
    - ``attf = ttf / N``
    - ``pi = exp(-attf)``
    - ``IDF_expected = -log(1 - pi) / log(2)`` (log base 2)
    """

    @property
    def description(self) -> str:
        return "Residual IDF: deviation from Poisson model"

    def score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        total_docs = float(term_freq.total_docs)
        result = TermExtractionResult()

        for candidate in candidates:
            nf = candidate.normalized_form
            ttf = term_freq.get_ttf(nf)
            df = term_freq.get_df(nf)

            if df == 0 or total_docs == 0:
                result.add(
                    Term(
                        string=candidate.normalized_form,
                        score=0.0,
                        frequency=ttf,
                        surface_forms=set(candidate.surface_forms),
                    )
                )
                continue

            attf = ttf / total_docs
            pi = math.exp(-attf)

            # Expected IDF (Poisson-based), log base 2
            if pi >= 1.0:
                eidf = 0.0
            else:
                eidf = -(math.log(1 - pi) / math.log(2))

            # Observed IDF (natural log, matching Java)
            idf = math.log(total_docs / df)

            ridf = idf - eidf

            term = Term(
                string=candidate.normalized_form,
                score=ridf,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
