"""Shared utilities for candidate term extractors."""

from __future__ import annotations


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
