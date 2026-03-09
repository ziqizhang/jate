"""Parallel map-reduce utilities."""

from __future__ import annotations

from concurrent.futures import ProcessPoolExecutor
from typing import Any, Callable


def parallel_map(
    fn: Callable[..., Any],
    chunks: list[Any],
    max_workers: int = 1,
) -> list[Any]:
    """Map *fn* over *chunks*, optionally in parallel.

    When *max_workers* <= 1, runs sequentially with no overhead.
    Otherwise uses ProcessPoolExecutor.
    """
    if not chunks:
        return []
    if max_workers <= 1:
        return [fn(chunk) for chunk in chunks]
    with ProcessPoolExecutor(max_workers=max_workers) as pool:
        return list(pool.map(fn, chunks))


def split_range(start: int, end: int, n_chunks: int) -> list[tuple[int, int]]:
    """Split index range [start, end) into n_chunks non-overlapping sub-ranges."""
    total = end - start
    if total <= 0 or n_chunks <= 0:
        return []
    n_chunks = min(n_chunks, total)
    base, remainder = divmod(total, n_chunks)
    ranges = []
    cursor = start
    for i in range(n_chunks):
        size = base + (1 if i < remainder else 0)
        ranges.append((cursor, cursor + size))
        cursor += size
    return ranges
