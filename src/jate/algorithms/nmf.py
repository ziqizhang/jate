"""NMF-based term extraction algorithm."""

from __future__ import annotations

from typing import Any

from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.features import TermFrequency
from jate.models import Candidate, Term, TermExtractionResult


class NMFRanker(ATERanker):
    """NMF topic modelling for unsupervised term extraction.

    Builds a document-candidate frequency matrix, decomposes it with NMF
    into topics, and extracts the top-T candidates per topic. Score =
    candidate's max coefficient across all topics.

    Based on Section 3.1 of Nugumanova et al. (2024), "Semantic Non-Negative
    Matrix Factorization for Term Extraction".

    Parameters
    ----------
    n_topics:
        Number of latent topics (k). Default 20.
    top_n_per_topic:
        Number of top candidates to extract per topic (T). Default 50.
        If None, all candidates are scored (no top-T filtering).
    """

    def __init__(self, n_topics: int = 20, top_n_per_topic: int | None = 50) -> None:
        self._n_topics = n_topics
        self._top_n = top_n_per_topic

    @property
    def description(self) -> str:
        return f"NMF topic modelling (k={self._n_topics}, T={self._top_n})"

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(
            produces_scores=True,
            produces_ranking=True,
            requires_corpus=True,
        )

    def _score(
        self,
        candidates: list[Candidate],
        term_freq: TermFrequency,
        **kwargs: Any,
    ) -> TermExtractionResult:
        try:
            from scipy.sparse import csr_matrix  # type: ignore[import-untyped]
            from sklearn.decomposition import NMF  # type: ignore[import-untyped]
        except ImportError:
            raise ImportError("NMF ranker requires scikit-learn. Install with: pip install scikit-learn") from None

        if not candidates:
            return TermExtractionResult()

        import numpy as np

        # Build document-candidate frequency matrix (raw counts, not TF-IDF)
        # Rows = documents, Columns = candidates
        term_list = [c.normalized_form.lower() for c in candidates]
        term_to_idx: dict[str, int] = {t: i for i, t in enumerate(term_list)}

        # Collect all document IDs
        all_doc_ids = sorted({doc_id for term_docs in term_freq.term2fid.values() for doc_id in term_docs})
        doc_to_idx: dict[str, int] = {d: i for i, d in enumerate(all_doc_ids)}

        n_docs = len(all_doc_ids)
        n_terms = len(term_list)

        if n_docs == 0 or n_terms == 0:
            return TermExtractionResult()

        # Build sparse matrix
        rows: list[int] = []
        cols: list[int] = []
        data: list[int] = []
        for term, doc_freqs in term_freq.term2fid.items():
            term_lower = term.lower()
            if term_lower not in term_to_idx:
                continue
            col = term_to_idx[term_lower]
            for doc_id, freq in doc_freqs.items():
                if doc_id in doc_to_idx:
                    rows.append(doc_to_idx[doc_id])
                    cols.append(col)
                    data.append(freq)

        A = csr_matrix((data, (rows, cols)), shape=(n_docs, n_terms))

        # Ensure n_topics doesn't exceed matrix dimensions
        k = min(self._n_topics, n_docs, n_terms)
        if k <= 0:
            return TermExtractionResult()

        # Run NMF decomposition: A ≈ WH
        nmf = NMF(
            n_components=k,
            init="nndsvda",  # good default for sparse data
            max_iter=300,
            random_state=42,
        )
        nmf.fit_transform(A)  # document-topic matrix (m × k)
        H = nmf.components_  # topic-candidate matrix (k × n)

        # Extract top-T candidates per topic, union across topics
        selected_terms: dict[str, float] = {}  # term -> max score

        if self._top_n is not None:
            # Top-T per topic extraction (paper's approach)
            for topic_idx in range(k):
                topic_weights = H[topic_idx]
                # Get indices of top-T candidates for this topic
                top_indices = np.argsort(topic_weights)[::-1][: self._top_n]
                for idx in top_indices:
                    weight = float(topic_weights[idx])
                    if weight <= 0:
                        continue
                    term = term_list[idx]
                    if term not in selected_terms or weight > selected_terms[term]:
                        selected_terms[term] = weight
        else:
            # Score all candidates by max weight across topics
            max_weights: Any = np.max(H, axis=0)  # max across topics for each term
            for idx, weight in enumerate(max_weights):
                if weight > 0:
                    selected_terms[term_list[idx]] = float(weight)

        # Build result
        cand_map: dict[str, Candidate] = {c.normalized_form.lower(): c for c in candidates}
        result = TermExtractionResult()
        for term_str, score in selected_terms.items():
            cand = cand_map.get(term_str)
            if cand is None:
                continue
            term_obj = Term(
                string=term_str,
                score=float(score),
                frequency=term_freq.get_ttf(term_str),
                surface_forms=set(cand.surface_forms),
            )
            result.add(term_obj)

        return result.sort()
