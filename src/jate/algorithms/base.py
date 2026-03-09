"""Abstract base class for term-scoring algorithms."""

from __future__ import annotations

from abc import ABC, abstractmethod
from typing import TYPE_CHECKING

from jate.models import Candidate, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


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
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        """Score every candidate and return a :class:`TermExtractionResult`.

        Parameters
        ----------
        candidates:
            Candidate terms produced by an extractor.
        corpus_store:
            A store providing corpus-level statistics.
        context_index:
            Optional context index for context-aware scoring.
        """
        ...
