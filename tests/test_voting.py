from __future__ import annotations

import pytest

from jate.algorithms.voting import Voting
from jate.models import Term, TermExtractionResult


class TestVoting:
    def test_reciprocal_rank_fusion(self):
        r1 = TermExtractionResult(
            [
                Term(string="a", score=3.0),
                Term(string="b", score=2.0),
                Term(string="c", score=1.0),
            ]
        )
        r2 = TermExtractionResult(
            [
                Term(string="b", score=5.0),
                Term(string="a", score=3.0),
                Term(string="c", score=1.0),
            ]
        )
        result = Voting.combine([(r1, 1.0), (r2, 1.0)])
        scores = {t.string: t.score for t in result}
        # a: 1/1 * 1.0 + 1/2 * 1.0 = 1.5
        # b: 1/2 * 1.0 + 1/1 * 1.0 = 1.5
        # c: 1/3 * 1.0 + 1/3 * 1.0 = 0.667
        assert scores["a"] == pytest.approx(1.5)
        assert scores["b"] == pytest.approx(1.5)
        assert scores["c"] == pytest.approx(2 / 3)

    def test_weighted(self):
        r1 = TermExtractionResult([Term(string="a", score=1.0)])
        r2 = TermExtractionResult([Term(string="a", score=1.0)])
        result = Voting.combine([(r1, 2.0), (r2, 1.0)])
        # a: 1/1 * 2.0 + 1/1 * 1.0 = 3.0
        assert result[0].score == pytest.approx(3.0)

    def test_empty_input(self):
        result = Voting.combine([])
        assert len(result) == 0

    def test_single_result(self):
        r1 = TermExtractionResult(
            [
                Term(string="x", score=10.0),
                Term(string="y", score=5.0),
            ]
        )
        result = Voting.combine([(r1, 1.0)])
        scores = {t.string: t.score for t in result}
        assert scores["x"] == pytest.approx(1.0)
        assert scores["y"] == pytest.approx(0.5)

    def test_sorted_descending(self):
        r1 = TermExtractionResult(
            [
                Term(string="a", score=3.0),
                Term(string="b", score=2.0),
            ]
        )
        r2 = TermExtractionResult(
            [
                Term(string="b", score=5.0),
                Term(string="a", score=3.0),
            ]
        )
        result = Voting.combine([(r1, 1.0), (r2, 1.0)])
        # Both a and b get 1.5, so sorted by score desc then alphabetically
        assert result[0].score >= result[1].score
