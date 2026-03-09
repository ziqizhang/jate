"""ComboBasic scoring algorithm."""

from __future__ import annotations

import math

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


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
        context_index: ContextIndex | None = None,
        containment: dict[str, list[str]] | None = None,
        child_containment: dict[str, list[str]] | None = None,
    ) -> TermExtractionResult:
        # Use pre-built containment maps if provided, otherwise build locally
        if containment is not None:
            parent_map = containment
        else:
            parent_map = {}

        if child_containment is not None:
            child_map = child_containment
        else:
            child_map = {}

        if containment is None or child_containment is None:
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
                if containment is None:
                    parent_map[c.normalized_form] = parents
                if child_containment is None:
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
