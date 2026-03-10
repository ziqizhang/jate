"""SQLite-backed corpus statistics store."""

from __future__ import annotations

import json
import sqlite3
from pathlib import Path

from jate.models import Candidate, Document


class SQLiteCorpusStore:
    """Corpus store backed by SQLite.

    Uses an in-memory database by default, or a file path for persistence.
    """

    def __init__(self, db_path: str | Path = ":memory:") -> None:
        self._db_path = str(db_path)
        self._connection = sqlite3.connect(self._db_path, check_same_thread=False)
        self._create_tables()

    # ------------------------------------------------------------------
    # Schema
    # ------------------------------------------------------------------

    def _create_tables(self) -> None:
        with self._connection:
            self._connection.executescript(
                """
                CREATE TABLE IF NOT EXISTS documents (
                    doc_id   TEXT PRIMARY KEY,
                    content  TEXT,
                    metadata TEXT
                );
                CREATE TABLE IF NOT EXISTS term_frequencies (
                    term      TEXT,
                    doc_id    TEXT,
                    frequency INTEGER,
                    PRIMARY KEY (term, doc_id)
                );
                CREATE TABLE IF NOT EXISTS cooccurrences (
                    term_a    TEXT,
                    term_b    TEXT,
                    doc_count INTEGER,
                    PRIMARY KEY (term_a, term_b)
                );
                """
            )

    # ------------------------------------------------------------------
    # Document ingestion
    # ------------------------------------------------------------------

    def add_document(self, document: Document) -> None:
        """Ingest a document into the store."""
        with self._connection:
            self._connection.execute(
                "INSERT OR REPLACE INTO documents (doc_id, content, metadata) " "VALUES (?, ?, ?)",
                (
                    document.doc_id,
                    document.content,
                    json.dumps(document.metadata),
                ),
            )

    # ------------------------------------------------------------------
    # Candidate indexing
    # ------------------------------------------------------------------

    def index_candidates(self, candidates: list[Candidate]) -> None:
        """Bulk-index candidate frequencies into term_frequencies.

        Also computes pairwise co-occurrence counts (number of documents
        in which both terms appear).
        """
        # Build per-term, per-doc frequency rows
        rows: list[tuple[str, str, int]] = []
        # Track which docs each term appears in (for co-occurrence)
        term_docs: dict[str, set[str]] = {}

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                freq = len(positions)
                rows.append((term, doc_id, freq))
                term_docs.setdefault(term, set()).add(doc_id)

        with self._connection:
            self._connection.executemany(
                "INSERT OR REPLACE INTO term_frequencies (term, doc_id, frequency) " "VALUES (?, ?, ?)",
                rows,
            )

        # Compute co-occurrences
        terms = sorted(term_docs.keys())
        cooc_rows: list[tuple[str, str, int]] = []
        for i, t_a in enumerate(terms):
            for t_b in terms[i + 1 :]:
                shared = len(term_docs[t_a] & term_docs[t_b])
                if shared > 0:
                    cooc_rows.append((t_a, t_b, shared))

        if cooc_rows:
            with self._connection:
                self._connection.executemany(
                    "INSERT OR REPLACE INTO cooccurrences (term_a, term_b, doc_count) " "VALUES (?, ?, ?)",
                    cooc_rows,
                )

    # ------------------------------------------------------------------
    # Queries
    # ------------------------------------------------------------------

    def get_term_frequency(self, term: str) -> int:
        """Total frequency of *term* across all documents."""
        row = self._connection.execute(
            "SELECT COALESCE(SUM(frequency), 0) FROM term_frequencies WHERE term = ?",
            (term.lower(),),
        ).fetchone()
        return int(row[0])

    def get_document_frequency(self, term: str) -> int:
        """Number of distinct documents containing *term*."""
        row = self._connection.execute(
            "SELECT COUNT(DISTINCT doc_id) FROM term_frequencies WHERE term = ?",
            (term.lower(),),
        ).fetchone()
        return int(row[0])

    def get_total_documents(self) -> int:
        """Total number of documents in the corpus."""
        row = self._connection.execute("SELECT COUNT(*) FROM documents").fetchone()
        return int(row[0])

    def get_cooccurrences(self, term_a: str, term_b: str) -> int:
        """Number of documents in which both *term_a* and *term_b* appear."""
        a, b = sorted([term_a.lower(), term_b.lower()])
        row = self._connection.execute(
            "SELECT COALESCE(doc_count, 0) FROM cooccurrences " "WHERE term_a = ? AND term_b = ?",
            (a, b),
        ).fetchone()
        return int(row[0]) if row else 0

    def get_corpus_total(self) -> int:
        """Sum of frequencies of all terms in the corpus."""
        row = self._connection.execute("SELECT COALESCE(SUM(frequency), 0) FROM term_frequencies").fetchone()
        return int(row[0])

    # ------------------------------------------------------------------
    # Additional helpers
    # ------------------------------------------------------------------

    def get_containing_terms(self, term: str) -> list[str]:
        """Return terms that contain *term* as a whole-word subsequence (excluding exact match)."""
        pattern = f"% {term.lower()} %"
        rows = self._connection.execute(
            "SELECT DISTINCT term FROM term_frequencies " "WHERE (' ' || term || ' ') LIKE ? AND term != ?",
            (pattern, term.lower()),
        ).fetchall()
        return [r[0] for r in rows]

    def get_term_document_frequency_map(self, term: str) -> dict[str, int]:
        """Per-document frequencies for *term*."""
        rows = self._connection.execute(
            "SELECT doc_id, frequency FROM term_frequencies WHERE term = ?",
            (term.lower(),),
        ).fetchall()
        return {doc_id: freq for doc_id, freq in rows}

    # ------------------------------------------------------------------
    # Lifecycle
    # ------------------------------------------------------------------

    def close(self) -> None:
        """Close the underlying database connection."""
        self._connection.close()
