"""Tests for JATEConfig and parallel_map."""
from __future__ import annotations

from jate.config import JATEConfig
from jate.parallel import parallel_map, split_range


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
