# src/jate/features.py
"""Shared feature builders for term extraction algorithms."""

from __future__ import annotations

from collections import defaultdict
from dataclasses import dataclass, field

from jate.models import Candidate, Document
from jate.parallel import parallel_map, split_range


@dataclass(slots=True)
class TermFrequency:
    """Term-level corpus frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` (feature type 0).

    Attributes
    ----------
    term2ttf : dict[str, int]
        Term → total term frequency across corpus.
    term2fid : dict[str, dict[str, int]]
        Term → {doc_id → frequency in that document}.
    corpus_total : int
        Sum of all TTF values.
    total_docs : int
        Total number of documents.
    """

    term2ttf: dict[str, int] = field(default_factory=dict)
    term2fid: dict[str, dict[str, int]] = field(default_factory=dict)
    corpus_total: int = 0
    total_docs: int = 0

    @classmethod
    def build(cls, candidates: list[Candidate], total_docs: int) -> TermFrequency:
        term2ttf: dict[str, int] = defaultdict(int)
        term2fid: dict[str, dict[str, int]] = defaultdict(dict)

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                freq = len(positions)
                term2ttf[term] += freq
                term2fid[term][doc_id] = term2fid[term].get(doc_id, 0) + freq

        corpus_total = sum(term2ttf.values())
        return cls(
            term2ttf=dict(term2ttf),
            term2fid=dict(term2fid),
            corpus_total=corpus_total,
            total_docs=total_docs,
        )

    def get_ttf(self, term: str) -> int:
        return self.term2ttf.get(term.lower(), 0)

    def get_df(self, term: str) -> int:
        return len(self.term2fid.get(term.lower(), {}))

    def get_ttf_norm(self, term: str) -> float:
        """Java: getTTFNorm = TTF / (corpusTotal + 1)."""
        ttf = self.get_ttf(term)
        return ttf / (self.corpus_total + 1)

    def get_doc_freq_map(self, term: str) -> dict[str, int]:
        return dict(self.term2fid.get(term.lower(), {}))


@dataclass(slots=True)
class WordFrequency:
    """Word-level (unigram) corpus frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` (feature type 1).

    Built by tokenizing every document and counting individual word occurrences.
    Unlike TermFrequency, this counts ALL tokens, not just candidate terms.
    """

    word2ttf: dict[str, int] = field(default_factory=dict)
    word2fid: dict[str, dict[str, int]] = field(default_factory=dict)
    corpus_total: int = 0

    @classmethod
    def build(
        cls,
        documents: list[Document],
        tokenize_fn: callable,
    ) -> WordFrequency:
        word2ttf: dict[str, int] = defaultdict(int)
        word2fid: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))

        for doc in documents:
            tokens = tokenize_fn(doc.content)
            for token in tokens:
                word = token.lower()
                word2ttf[word] += 1
                word2fid[word][doc.doc_id] += 1

        corpus_total = sum(word2ttf.values())
        return cls(
            word2ttf=dict(word2ttf),
            word2fid={k: dict(v) for k, v in word2fid.items()},
            corpus_total=corpus_total,
        )

    def get_ttf(self, word: str) -> int:
        return self.word2ttf.get(word.lower(), 0)

    def get_df(self, word: str) -> int:
        return len(self.word2fid.get(word.lower(), {}))

    def get_ttf_norm(self, word: str) -> float:
        """Java: getTTFNorm = TTF / (corpusTotal + 1)."""
        ttf = self.get_ttf(word)
        return ttf / (self.corpus_total + 1)


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
