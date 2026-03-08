"""In-memory corpus statistics store."""

from __future__ import annotations

from jate.models import Candidate, Document


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

    def index_candidates(self, candidates: list[Candidate]) -> None:
        """Bulk-index candidate frequencies.

        Also computes pairwise co-occurrence counts.
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
        """Return terms that contain *term* as a substring (excluding exact match)."""
        needle = term.lower()
        return [t for t in self._term_freq if needle in t and t != needle]

    def get_term_document_frequency_map(self, term: str) -> dict[str, int]:
        """Per-document frequencies for *term*."""
        return dict(self._term_freq.get(term.lower(), {}))
