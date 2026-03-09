"""ComboBasic scoring algorithm."""

from __future__ import annotations

import math

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore


class ComboBasic(Algorithm):
    """Combination of Basic scores with parent and child containment.

    ``ComboBasic(t) = |t| * log(f(t)) + alpha * e_t + beta * e'_t``

    where ``e_t`` is the number of longer terms containing *t* (parents) and
    ``e'_t`` is the number of shorter terms contained in *t* (children).
    For single-word terms: ``score = log(f(t))``.
    """

    def __init__(self, alpha: float = 0.75, beta: float = 0.1) -> None:
        self._alpha = alpha
        self._beta = beta

    @property
    def description(self) -> str:
        return "ComboBasic: Basic with parent and child containment"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
    ) -> TermExtractionResult:
        # Build parent containment (longer terms containing this one)
        parent_map: dict[str, list[str]] = {}
        # Build child containment (shorter terms contained in this one)
        child_map: dict[str, list[str]] = {}

        for c in candidates:
            parents: list[str] = []
            children: list[str] = []
            for other in candidates:
                if other.normalized_form == c.normalized_form:
                    continue
                c_words = len(c.normalized_form.split())
                o_words = len(other.normalized_form.split())
                if o_words > c_words and f" {c.normalized_form} " in f" {other.normalized_form} ":
                    parents.append(other.normalized_form)
                elif o_words < c_words and f" {other.normalized_form} " in f" {c.normalized_form} ":
                    children.append(other.normalized_form)
            parent_map[c.normalized_form] = parents
            child_map[c.normalized_form] = children

        result = TermExtractionResult()
        for candidate in candidates:
            nf = candidate.normalized_form
            ttf = corpus_store.get_term_frequency(nf)
            word_count = len(nf.split())

            if ttf <= 0:
                s = 0.0
            elif word_count == 1:
                s = math.log(ttf)
            else:
                log_f = math.log(ttf)
                et = float(len(parent_map.get(nf, [])))
                etp = float(len(child_map.get(nf, [])))
                s = word_count * log_f + self._alpha * et + self._beta * etp

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=ttf,
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
