"""Shared utilities for candidate term extractors."""

from __future__ import annotations

from typing import TYPE_CHECKING, Any

if TYPE_CHECKING:
    from jate.models import Document


def compute_token_offsets(text: str, tokens: list[str]) -> list[tuple[int, int]]:
    """Return (start, end) character offsets for each token in *text*.

    Uses a greedy left-to-right scan to locate each token.
    """
    offsets: list[tuple[int, int]] = []
    search_from = 0
    for tok in tokens:
        idx = text.find(tok, search_from)
        if idx == -1:
            # Fallback: try case-insensitive or just use current position.
            idx = text.lower().find(tok.lower(), search_from)
            if idx == -1:
                idx = search_from
        offsets.append((idx, idx + len(tok)))
        search_from = idx + len(tok)
    return offsets


def batch_process_docs(
    documents: list[Document],
    nlp_backend: Any,
) -> tuple[list[Document], list[Any] | None]:
    """Batch-process documents through the NLP backend if possible.

    Returns ``(valid_docs, spacy_docs)`` where *valid_docs* are documents
    with non-empty content.  If the backend supports ``process_batch()`` or
    ``process()``, *spacy_docs* is the corresponding list of spaCy ``Doc``
    objects; otherwise *spacy_docs* is ``None``, signalling the caller to
    fall back to per-sentence NLP calls.
    """
    valid_docs = [doc for doc in documents if doc.content.strip()]
    texts = [doc.content for doc in valid_docs]

    if hasattr(nlp_backend, "process_batch"):
        return valid_docs, nlp_backend.process_batch(texts)
    if hasattr(nlp_backend, "process"):
        return valid_docs, [nlp_backend.process(text) for text in texts]
    return valid_docs, None
