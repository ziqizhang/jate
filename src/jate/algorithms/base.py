"""Abstract base class for term-scoring algorithms."""

from __future__ import annotations

from abc import ABC, abstractmethod
from dataclasses import dataclass
from typing import Any

from jate.features import TermFrequency
from jate.models import Candidate, TermExtractionResult


@dataclass(frozen=True)
class OutputCapabilities:
    """Declares what kind of output an algorithm produces."""

    produces_scores: bool = False
    produces_ranking: bool = False
    produces_offsets: bool = False
    produces_labels: bool = False
    requires_corpus: bool = False


class AlgorithmIncompatibleError(Exception):
    """Raised when an algorithm cannot work in the given context."""


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
    def output_capabilities(self) -> OutputCapabilities:
        """Return the output capabilities of this algorithm."""
        ...

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


class ATERanker(Algorithm):
    """Base class for ranking-based ATE algorithms.

    Subclasses implement :meth:`_score` instead of :meth:`score`.
    The public :meth:`score` method calls :meth:`doc_level_compatibility`
    before delegating to :meth:`_score`.
    """

    def doc_level_compatibility(self, term_freq: TermFrequency, **kwargs: Any) -> None:
        """Check whether the algorithm is compatible with the input data.

        Subclasses may override to warn or raise
        :class:`AlgorithmIncompatibleError`.
        """

    def score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        self.doc_level_compatibility(term_freq, **kwargs)
        return self._score(candidates, term_freq, **kwargs)

    @abstractmethod
    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        """Implement the actual scoring logic."""
        ...


class ATETagger(Algorithm):
    """Base class for tagging-based ATE algorithms.

    Subclasses implement :meth:`tag` for document-level extraction.
    """

    @abstractmethod
    def tag(self, doc: Any) -> TermExtractionResult:
        """Tag a document and return extraction results."""
        ...

    def score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        raise NotImplementedError("ATETagger uses tag(), not score(). " "Use tag(doc) for document-level extraction.")

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(produces_offsets=True, produces_labels=True)
