"""Tests for JATEConfig and parallel_map."""
from __future__ import annotations

from jate.config import JATEConfig
from jate.parallel import parallel_map


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
