"""Abstract base class for term-scoring algorithms."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import Any

from jate.features import TermFrequency
from jate.models import Candidate, TermExtractionResult


class Algorithm(ABC):
    """Base class for ATE scoring algorithms.

    Subclasses must implement :meth:`score` and may override the
    :attr:`name` / :attr:`description` properties.
    """

    @property
    def name(self) -> str:
        """Human-readable algorithm name (defaults to class name)."""
        return self.__class__.__name__

    @property
    def description(self) -> str:
        """One-line description of the algorithm."""
        return ""

    @abstractmethod
    def score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        """Score every candidate and return a :class:`TermExtractionResult`.

        Parameters
        ----------
        candidates:
            Candidate terms produced by an extractor.
        term_freq:
            Term-level corpus frequency statistics.
        **kwargs:
            Additional feature objects required by specific algorithms
            (e.g. ``containment``, ``context_freq``, ``word_freq``, etc.).
        """
        ...
