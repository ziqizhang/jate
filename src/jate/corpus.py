"""High-level Corpus wrapper for JATE.

Provides a convenient, self-contained object that bundles document loading,
an NLP backend, and a corpus store behind a simple API.
"""

from __future__ import annotations

from pathlib import Path
from typing import Any

from jate.models import Document
from jate.nlp.document_loader import DocumentLoader
from jate.nlp.spacy_backend import SpacyBackend
from jate.store.memory_store import MemoryCorpusStore
from jate.store.sqlite_store import SQLiteCorpusStore


class Corpus:
    """A corpus of documents with an embedded NLP backend and store.

    Satisfies the :class:`~jate.protocols.CorpusStore` protocol by
    delegating to an internal :class:`MemoryCorpusStore` or
    :class:`SQLiteCorpusStore`.

    Parameters
    ----------
    db_path:
        If *None* (default), uses an in-memory store.  If a path string
        is given, uses a SQLite-backed store for persistence.
    model:
        spaCy model name for the NLP backend.
    """

    def __init__(
        self,
        db_path: str | Path | None = None,
        model: str = "en_core_web_sm",
    ) -> None:
        if db_path is not None:
            self._store: MemoryCorpusStore | SQLiteCorpusStore = SQLiteCorpusStore(str(db_path))
        else:
            self._store = MemoryCorpusStore()
        self._nlp_backend = SpacyBackend(model)
        self._documents: list[Document] = []

    # ------------------------------------------------------------------
    # Document loading
    # ------------------------------------------------------------------

    def add_documents(self, sources: list[str]) -> None:
        """Add documents from file paths or raw text strings.

        Each entry in *sources* is auto-detected: if it points to an
        existing file, the file is loaded; otherwise the string is
        treated as raw text.
        """
        for source in sources:
            p = Path(source)
            if p.is_file():
                doc = DocumentLoader.load_file(p)
            else:
                doc = DocumentLoader.load_texts([source])[0]
            self._documents.append(doc)
            self._store.add_document(doc)

    def add_directory(self, path: str, glob_pattern: str = "*.txt") -> None:
        """Load all files matching *glob_pattern* under *path*."""
        docs = DocumentLoader.load_directory(path, glob_pattern)
        for doc in docs:
            self._documents.append(doc)
            self._store.add_document(doc)

    # ------------------------------------------------------------------
    # Properties
    # ------------------------------------------------------------------

    @property
    def documents(self) -> list[Document]:
        """Return the list of loaded documents."""
        return list(self._documents)

    @property
    def nlp(self) -> SpacyBackend:
        """Return the NLP backend."""
        return self._nlp_backend

    @property
    def store(self) -> MemoryCorpusStore | SQLiteCorpusStore:
        """Return the underlying corpus store."""
        return self._store

    # ------------------------------------------------------------------
    # CorpusStore protocol delegation
    # ------------------------------------------------------------------

    def add_document(self, document: Document) -> None:
        """Ingest a document into the store."""
        self._store.add_document(document)

    def get_term_frequency(self, term: str) -> int:
        """Total frequency of *term* across the entire corpus."""
        return self._store.get_term_frequency(term)

    def get_document_frequency(self, term: str) -> int:
        """Number of documents containing *term*."""
        return self._store.get_document_frequency(term)

    def get_total_documents(self) -> int:
        """Total number of documents in the corpus."""
        return self._store.get_total_documents()

    def get_cooccurrences(self, term_a: str, term_b: str) -> int:
        """Number of documents in which both *term_a* and *term_b* appear."""
        return self._store.get_cooccurrences(term_a, term_b)

    def get_corpus_total(self) -> int:
        """Sum of frequencies of all terms in the corpus."""
        return self._store.get_corpus_total()
