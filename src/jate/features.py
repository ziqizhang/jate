# src/jate/features.py
"""Shared feature builders for term extraction algorithms."""

from __future__ import annotations

from jate.models import Candidate
from jate.parallel import parallel_map, split_range


def _containment_chunk(args: tuple) -> dict[str, list[str]]:
    """Find parent terms for a slice of candidates."""
    all_candidates, start, end = args
    partial: dict[str, list[str]] = {}
    for i in range(start, end):
        c = all_candidates[i]
        c_nf = c.normalized_form
        c_words = len(c_nf.split())
        parents: list[str] = []
        for other in all_candidates:
            o_nf = other.normalized_form
            if o_nf == c_nf:
                continue
            if len(o_nf.split()) > c_words and f" {c_nf} " in f" {o_nf} ":
                parents.append(o_nf)
        partial[c_nf] = parents
    return partial


def build_containment_index(
    candidates: list[Candidate],
    max_workers: int = 1,
) -> dict[str, list[str]]:
    """Build parent containment index: term -> [longer terms containing it]."""
    if not candidates:
        return {}
    if max_workers <= 1:
        return _containment_chunk((candidates, 0, len(candidates)))
    chunks = [
        (candidates, start, end)
        for start, end in split_range(0, len(candidates), max_workers)
    ]
    partial_dicts = parallel_map(_containment_chunk, chunks, max_workers)
    merged: dict[str, list[str]] = {}
    for partial in partial_dicts:
        merged.update(partial)
    return merged


def _child_containment_chunk(args: tuple) -> dict[str, list[str]]:
    """Find child terms for a slice of candidates."""
    all_candidates, start, end = args
    partial: dict[str, list[str]] = {}
    for i in range(start, end):
        c = all_candidates[i]
        c_nf = c.normalized_form
        c_words = len(c_nf.split())
        children: list[str] = []
        for other in all_candidates:
            o_nf = other.normalized_form
            if o_nf == c_nf:
                continue
            if len(o_nf.split()) < c_words and f" {o_nf} " in f" {c_nf} ":
                children.append(o_nf)
        partial[c_nf] = children
    return partial


def build_child_containment_index(
    candidates: list[Candidate],
    max_workers: int = 1,
) -> dict[str, list[str]]:
    """Build child containment index: term -> [shorter terms contained in it]."""
    if not candidates:
        return {}
    if max_workers <= 1:
        return _child_containment_chunk((candidates, 0, len(candidates)))
    chunks = [
        (candidates, start, end)
        for start, end in split_range(0, len(candidates), max_workers)
    ]
    partial_dicts = parallel_map(_child_containment_chunk, chunks, max_workers)
    merged: dict[str, list[str]] = {}
    for partial in partial_dicts:
        merged.update(partial)
    return merged
