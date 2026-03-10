"""In-memory corpus statistics store."""

from __future__ import annotations

from typing import Any

from jate.models import Candidate, Document
from jate.parallel import parallel_map, split_range


def _compute_cooc_chunk(args: tuple[Any, ...]) -> dict[tuple[str, str], int]:
    """Compute co-occurrences for a chunk of the outer loop (must be top-level for pickling)."""
    terms, term_docs, start, end = args
    partial: dict[tuple[str, str], int] = {}
    for i in range(start, end):
        t_a = terms[i]
        docs_a = term_docs[t_a]
        for t_b in terms[i + 1 :]:
            shared = len(docs_a & term_docs[t_b])
            if shared > 0:
                partial[(t_a, t_b)] = shared
    return partial


class MemoryCorpusStore:
    """Corpus store using plain Python dicts.

    Suitable for quick single-document extraction where persistence is
    not needed.
    """

    def __init__(self) -> None:
        # doc_id -> Document
        self._documents: dict[str, Document] = {}
        # term -> {doc_id: frequency}
        self._term_freq: dict[str, dict[str, int]] = {}
        # (term_a, term_b) -> doc_count  (keys kept in sorted order)
        self._cooccurrences: dict[tuple[str, str], int] = {}

    # ------------------------------------------------------------------
    # Document ingestion
    # ------------------------------------------------------------------

    def add_document(self, document: Document) -> None:
        """Ingest a document into the store."""
        self._documents[document.doc_id] = document

    # ------------------------------------------------------------------
    # Candidate indexing
    # ------------------------------------------------------------------

    def index_candidates(self, candidates: list[Candidate], *, max_workers: int = 1) -> None:
        """Bulk-index candidate frequencies.

        Also computes pairwise co-occurrence counts.  When *max_workers* > 1
        the co-occurrence computation is parallelized across processes.
        """
        term_docs: dict[str, set[str]] = {}

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                freq = len(positions)
                self._term_freq.setdefault(term, {})[doc_id] = freq
                term_docs.setdefault(term, set()).add(doc_id)

        # Compute co-occurrences
        terms = sorted(term_docs.keys())

        if max_workers > 1:
            chunks = split_range(0, len(terms), max_workers)
            results = parallel_map(
                _compute_cooc_chunk,
                [(terms, term_docs, s, e) for s, e in chunks],
                max_workers=max_workers,
            )
            for partial in results:
                self._cooccurrences.update(partial)
        else:
            for i, t_a in enumerate(terms):
                for t_b in terms[i + 1 :]:
                    shared = len(term_docs[t_a] & term_docs[t_b])
                    if shared > 0:
                        self._cooccurrences[(t_a, t_b)] = shared

    # ------------------------------------------------------------------
    # Queries
    # ------------------------------------------------------------------

    def get_term_frequency(self, term: str) -> int:
        """Total frequency of *term* across all documents."""
        doc_freqs = self._term_freq.get(term.lower(), {})
        return sum(doc_freqs.values())

    def get_document_frequency(self, term: str) -> int:
        """Number of distinct documents containing *term*."""
        return len(self._term_freq.get(term.lower(), {}))

    def get_total_documents(self) -> int:
        """Total number of documents in the corpus."""
        return len(self._documents)

    def get_cooccurrences(self, term_a: str, term_b: str) -> int:
        """Number of documents in which both *term_a* and *term_b* appear."""
        key = tuple(sorted([term_a.lower(), term_b.lower()]))
        return self._cooccurrences.get(key, 0)  # type: ignore[arg-type]

    def get_corpus_total(self) -> int:
        """Sum of frequencies of all terms in the corpus."""
        total = 0
        for doc_freqs in self._term_freq.values():
            total += sum(doc_freqs.values())
        return total

    # ------------------------------------------------------------------
    # Additional helpers
    # ------------------------------------------------------------------

    def get_containing_terms(self, term: str) -> list[str]:
        """Return terms that contain *term* as a whole-word subsequence (excluding exact match)."""
        padded = f" {term.lower()} "
        return [t for t in self._term_freq if t != term.lower() and padded in f" {t} "]

    def get_term_document_frequency_map(self, term: str) -> dict[str, int]:
        """Per-document frequencies for *term*."""
        return dict(self._term_freq.get(term.lower(), {}))
