"""Abstract base class for candidate-term extractors."""

from __future__ import annotations

from abc import ABC, abstractmethod

from jate.models import Candidate, Document
from jate.protocols import CorpusStore, NLPBackend


class CandidateExtractorBase(ABC):
    """Base class for candidate extraction strategies.

    Subclasses implement :meth:`extract` to turn raw documents into a
    list of :class:`Candidate` objects, using the supplied NLP backend
    and (optionally) a corpus store for frequency bookkeeping.
    """

    @abstractmethod
    def extract(
        self,
        documents: list[Document],
        nlp_backend: NLPBackend,
        corpus_store: CorpusStore,
    ) -> list[Candidate]:
        """Extract candidate terms from *documents*.

        Parameters
        ----------
        documents:
            Source documents to process.
        nlp_backend:
            NLP operations provider (tokenisation, POS tagging, etc.).
        corpus_store:
            Corpus statistics store — extractors should update it as
            they process documents.
        """
        ...
