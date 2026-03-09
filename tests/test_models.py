"""Tests for core data models."""

from __future__ import annotations

import json

import pandas as pd
import pytest

from jate.models import Candidate, Document, Term, TermExtractionResult

# ---------------------------------------------------------------------------
# Term
# ---------------------------------------------------------------------------


class TestTerm:
    def test_defaults(self) -> None:
        t = Term(string="neural network")
        assert t.string == "neural network"
        assert t.score == 0.0
        assert t.frequency == 0
        assert t.rank == 0
        assert t.metadata == {}

    def test_repr(self) -> None:
        t = Term("foo", score=1.5)
        assert "foo" in repr(t)
        assert "1.5" in repr(t)

    def test_ordering_by_score(self) -> None:
        a = Term("a", score=3.0)
        b = Term("b", score=1.0)
        c = Term("c", score=5.0)
        assert sorted([a, b, c]) == [c, a, b]

    def test_ordering_tiebreak_by_string(self) -> None:
        a = Term("apple", score=2.0)
        b = Term("banana", score=2.0)
        assert sorted([b, a]) == [a, b]


# ---------------------------------------------------------------------------
# Candidate
# ---------------------------------------------------------------------------


class TestCandidate:
    def test_defaults(self) -> None:
        c = Candidate(surface_form="machine learning")
        assert c.normalized_form == "machine learning"
        assert c.pos_pattern == ""
        assert c.doc_positions == {}
        assert c.total_frequency == 0

    def test_normalized_form_auto(self) -> None:
        c = Candidate(surface_form="Machine Learning")
        assert c.normalized_form == "machine learning"

    def test_explicit_normalized_form(self) -> None:
        c = Candidate(surface_form="running", normalized_form="run")
        assert c.normalized_form == "run"

    def test_add_position(self) -> None:
        c = Candidate(surface_form="test")
        c.add_position("doc1", 0, 4)
        c.add_position("doc1", 10, 14)
        c.add_position("doc2", 5, 9)
        assert c.total_frequency == 3
        assert c.document_frequency == 2
        assert len(c.doc_positions["doc1"]) == 2
        assert c.doc_positions["doc2"] == [(5, 9)]


# ---------------------------------------------------------------------------
# Document
# ---------------------------------------------------------------------------


class TestDocument:
    def test_creation(self) -> None:
        d = Document(doc_id="d1", content="Hello world", metadata={"source": "web"})
        assert d.doc_id == "d1"
        assert d.content == "Hello world"
        assert d.metadata["source"] == "web"

    def test_repr(self) -> None:
        d = Document(doc_id="abc")
        assert "abc" in repr(d)


# ---------------------------------------------------------------------------
# TermExtractionResult
# ---------------------------------------------------------------------------


class TestTermExtractionResult:
    @pytest.fixture()
    def sample_result(self) -> TermExtractionResult:
        terms = [
            Term("neural network", score=5.0, frequency=20, rank=1),
            Term("deep learning", score=3.0, frequency=15, rank=2),
            Term("machine learning", score=8.0, frequency=50, rank=3),
            Term("ai", score=1.0, frequency=100, rank=4),
        ]
        return TermExtractionResult(terms)

    def test_len(self, sample_result: TermExtractionResult) -> None:
        assert len(sample_result) == 4

    def test_iter(self, sample_result: TermExtractionResult) -> None:
        strings = [t.string for t in sample_result]
        assert "neural network" in strings

    def test_getitem(self, sample_result: TermExtractionResult) -> None:
        assert sample_result[0].string == "neural network"

    def test_repr(self, sample_result: TermExtractionResult) -> None:
        assert "4 terms" in repr(sample_result)

    def test_add(self) -> None:
        r = TermExtractionResult()
        r.add(Term("foo", score=1.0))
        assert len(r) == 1

    def test_sort_by_score(self, sample_result: TermExtractionResult) -> None:
        sorted_r = sample_result.sort(by="score", ascending=False)
        scores = [t.score for t in sorted_r]
        assert scores == sorted(scores, reverse=True)

    def test_sort_by_frequency_ascending(self, sample_result: TermExtractionResult) -> None:
        sorted_r = sample_result.sort(by="frequency", ascending=True)
        freqs = [t.frequency for t in sorted_r]
        assert freqs == sorted(freqs)

    def test_filter_by_score(self, sample_result: TermExtractionResult) -> None:
        filtered = sample_result.filter_by_score(4.0)
        assert all(t.score >= 4.0 for t in filtered)
        assert len(filtered) == 2

    def test_filter_by_frequency(self, sample_result: TermExtractionResult) -> None:
        filtered = sample_result.filter_by_frequency(20)
        assert all(t.frequency >= 20 for t in filtered)

    def test_filter_by_length_min(self, sample_result: TermExtractionResult) -> None:
        filtered = sample_result.filter_by_length(min_words=2)
        for t in filtered:
            assert len(t.string.split()) >= 2

    def test_filter_by_length_max(self, sample_result: TermExtractionResult) -> None:
        filtered = sample_result.filter_by_length(min_words=1, max_words=1)
        assert len(filtered) == 1
        assert filtered[0].string == "ai"

    def test_to_dataframe(self, sample_result: TermExtractionResult) -> None:
        df = sample_result.to_dataframe()
        assert isinstance(df, pd.DataFrame)
        assert list(df.columns) == ["term", "score", "frequency", "rank", "surface_forms"]
        assert len(df) == 4

    def test_to_csv(self, sample_result: TermExtractionResult) -> None:
        csv_str = sample_result.to_csv()
        lines = [line.strip() for line in csv_str.strip().splitlines()]
        assert lines[0] == "term,score,frequency,rank"
        assert len(lines) == 5  # header + 4 rows

    def test_to_json(self, sample_result: TermExtractionResult) -> None:
        j = sample_result.to_json()
        data = json.loads(j)
        assert isinstance(data, list)
        assert len(data) == 4
        assert "term" in data[0]
        assert "metadata" in data[0]

    def test_empty_result(self) -> None:
        r = TermExtractionResult()
        assert len(r) == 0
        assert list(r) == []
        assert r.to_dataframe().empty
