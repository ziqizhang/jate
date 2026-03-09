"""Tests for JATEConfig and parallel_map."""
from __future__ import annotations

from jate.config import JATEConfig
from jate.models import Candidate
from jate.parallel import parallel_map, split_range
from jate.store.memory_store import MemoryCorpusStore


class TestJATEConfig:
    def test_defaults(self) -> None:
        config = JATEConfig()
        assert config.max_workers == 1

    def test_custom_workers(self) -> None:
        config = JATEConfig(max_workers=4)
        assert config.max_workers == 4


def _double(x: int) -> int:
    return x * 2


class TestParallelMap:
    def test_sequential(self) -> None:
        result = parallel_map(_double, [1, 2, 3], max_workers=1)
        assert result == [2, 4, 6]

    def test_parallel(self) -> None:
        result = parallel_map(_double, [1, 2, 3], max_workers=2)
        assert result == [2, 4, 6]

    def test_empty_input(self) -> None:
        result = parallel_map(_double, [], max_workers=2)
        assert result == []

    def test_single_item(self) -> None:
        result = parallel_map(_double, [5], max_workers=2)
        assert result == [10]


class TestSplitRange:
    def test_even_division(self) -> None:
        result = split_range(0, 9, 3)
        assert result == [(0, 3), (3, 6), (6, 9)]

    def test_uneven_division(self) -> None:
        result = split_range(0, 10, 3)
        assert len(result) == 3
        # First chunk gets the extra element
        assert result == [(0, 4), (4, 7), (7, 10)]
        # Verify full coverage with no gaps
        assert result[0][0] == 0
        assert result[-1][1] == 10

    def test_total_less_than_n_chunks(self) -> None:
        result = split_range(0, 2, 5)
        assert len(result) == 2
        assert result == [(0, 1), (1, 2)]

    def test_zero_total(self) -> None:
        result = split_range(5, 5, 3)
        assert result == []

    def test_negative_total(self) -> None:
        result = split_range(10, 5, 3)
        assert result == []

    def test_zero_chunks(self) -> None:
        result = split_range(0, 10, 0)
        assert result == []

    def test_negative_chunks(self) -> None:
        result = split_range(0, 10, -1)
        assert result == []

    def test_nonzero_start(self) -> None:
        result = split_range(5, 15, 3)
        assert len(result) == 3
        assert result[0][0] == 5
        assert result[-1][1] == 15


def _make_indexed_store(max_workers: int = 1) -> MemoryCorpusStore:
    """Build a store with known candidates and index them."""
    candidates = []
    for term, doc_ids in [
        ("neural network", ["d1", "d2", "d3"]),
        ("machine learning", ["d1", "d2"]),
        ("deep learning", ["d2", "d3"]),
        ("network", ["d1", "d2", "d3", "d4"]),
    ]:
        c = Candidate(surface_form=term, normalized_form=term)
        for doc_id in doc_ids:
            c.add_position(doc_id, 0, len(term))
        candidates.append(c)
    store = MemoryCorpusStore()
    store.index_candidates(candidates, max_workers=max_workers)
    return store


class TestParallelCooccurrence:
    def test_sequential_and_parallel_match(self) -> None:
        store_seq = _make_indexed_store(max_workers=1)
        store_par = _make_indexed_store(max_workers=2)
        terms = ["neural network", "machine learning", "deep learning", "network"]
        for i, a in enumerate(terms):
            for b in terms[i + 1:]:
                assert store_seq.get_cooccurrences(a, b) == store_par.get_cooccurrences(a, b), \
                    f"Mismatch for ({a}, {b})"

    def test_known_cooccurrence_values(self) -> None:
        store = _make_indexed_store(max_workers=2)
        assert store.get_cooccurrences("neural network", "machine learning") == 2
        assert store.get_cooccurrences("neural network", "deep learning") == 2
