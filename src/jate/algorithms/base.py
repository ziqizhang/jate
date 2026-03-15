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
    """Base class for tagging-based ATE algorithms (document-level).

    Subclasses implement :meth:`tag` for per-document extraction.
    The ``tag()`` method receives a spaCy ``Doc`` (or similar) and returns
    a :class:`TermExtractionResult` with ``spans`` populated on each
    :class:`Term`.

    Contract for implementors
    -------------------------
    - ``tag(doc)`` must return a ``TermExtractionResult`` where each ``Term``
      has ``spans`` populated with ``TermSpan`` entries (doc_id, start, end).
    - ``label`` on each ``Term`` should be set if the tagger classifies terms
      (e.g., ``"Specific_Term"``, ``"Common_Term"``).
    - ``score`` on each ``Term`` may be set to model confidence if available.
    - ``output_capabilities()`` should accurately reflect what the tagger
      produces. Override the default if the tagger also produces scores.

    Example skeleton::

        class MyBertTagger(ATETagger):
            def __init__(self, model_path: str):
                self._model = load_model(model_path)

            def tag(self, doc) -> TermExtractionResult:
                # doc is a spaCy Doc with tokens already processed
                spans = self._model.predict(doc)
                result = TermExtractionResult()
                for span in spans:
                    term = Term(
                        string=span.text.lower(),
                        spans=[TermSpan(doc_id=doc._.doc_id, start=span.start_char, end=span.end_char)],
                        label=span.label_,
                        score=span.score,
                    )
                    result.add(term)
                return result

            def output_capabilities(self) -> OutputCapabilities:
                return OutputCapabilities(
                    produces_scores=True,
                    produces_offsets=True,
                    produces_labels=True,
                )
    """

    @abstractmethod
    def tag(self, doc: Any) -> TermExtractionResult:
        """Extract terms from a single document.

        Parameters
        ----------
        doc:
            A spaCy ``Doc`` object (or compatible). The NLP processing
            (tokenisation, POS tagging, etc.) is already done.

        Returns
        -------
        TermExtractionResult
            Extracted terms with ``spans`` populated. Each ``Term`` should
            have at least ``string``, ``spans``, and optionally ``label``
            and ``score``.
        """
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
