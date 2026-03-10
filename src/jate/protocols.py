"""Protocol interfaces for pluggable backends in JATE.

These use :class:`typing.Protocol` so that any object satisfying the
method signatures is accepted — no inheritance required.
"""

from __future__ import annotations

from typing import Protocol, runtime_checkable

from jate.models import Candidate, Document

# ---------------------------------------------------------------------------
# NLP backend
# ---------------------------------------------------------------------------


@runtime_checkable
class NLPBackend(Protocol):
    """Interface for NLP operations (tokenisation, POS tagging, etc.)."""

    def tokenize(self, text: str) -> list[str]:
        """Split *text* into tokens."""
        ...

    def pos_tag(self, text: str) -> list[tuple[str, str]]:
        """Return ``(token, POS-tag)`` pairs."""
        ...

    def lemmatize(self, text: str) -> list[tuple[str, str]]:
        """Return ``(token, lemma)`` pairs."""
        ...

    def sentence_split(self, text: str) -> list[str]:
        """Split *text* into sentences."""
        ...

    def noun_chunks(self, text: str) -> list[str]:
        """Extract noun-phrase chunks from *text*."""
        ...


# ---------------------------------------------------------------------------
# Candidate extraction
# ---------------------------------------------------------------------------


@runtime_checkable
class CandidateExtractor(Protocol):
    """Interface for extracting term candidates from a corpus."""

    def extract(self, documents: list[Document]) -> list[Candidate]:
        """Extract candidate terms from *documents*."""
        ...


# ---------------------------------------------------------------------------
# Corpus store
# ---------------------------------------------------------------------------


@runtime_checkable
class CorpusStore(Protocol):
    """Interface for storing and querying corpus statistics."""

    def add_document(self, document: Document) -> None:
        """Ingest a document into the store."""
        ...

    def get_term_frequency(self, term: str) -> int:
        """Total frequency of *term* across the entire corpus."""
        ...

    def get_document_frequency(self, term: str) -> int:
        """Number of documents containing *term*."""
        ...

    def get_total_documents(self) -> int:
        """Total number of documents in the corpus."""
        ...

    def get_cooccurrences(self, term_a: str, term_b: str) -> int:
        """Number of documents in which both *term_a* and *term_b* appear."""
        ...

    def get_corpus_total(self) -> int:
        """Sum of frequencies of all terms in the corpus."""
        ...
