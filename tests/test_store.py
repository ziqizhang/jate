"""Tests for SQLiteCorpusStore and MemoryCorpusStore."""

from __future__ import annotations

import tempfile
from pathlib import Path
from typing import Any

import pytest

from jate.models import Candidate, Document
from jate.protocols import CorpusStore
from jate.store import MemoryCorpusStore, SQLiteCorpusStore

# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


def _make_sqlite_store() -> SQLiteCorpusStore:
    return SQLiteCorpusStore()  # in-memory default


def _make_memory_store() -> MemoryCorpusStore:
    return MemoryCorpusStore()


@pytest.fixture(params=["sqlite", "memory"])
def store(request: pytest.FixtureRequest) -> Any:
    if request.param == "sqlite":
        return _make_sqlite_store()
    return _make_memory_store()


# Shared test data

DOC_A = Document(doc_id="doc1", content="machine learning is great")
DOC_B = Document(doc_id="doc2", content="deep learning for NLP")

CANDIDATES = [
    Candidate(
        surface_form="machine learning",
        normalized_form="machine learning",
        doc_positions={"doc1": [(0, 16, -1)], "doc2": [(5, 21, -1)]},
        total_frequency=2,
    ),
    Candidate(
        surface_form="deep learning",
        normalized_form="deep learning",
        doc_positions={"doc2": [(0, 13, -1)]},
        total_frequency=1,
    ),
    Candidate(
        surface_form="learning",
        normalized_form="learning",
        doc_positions={"doc1": [(8, 16, -1)], "doc2": [(5, 13, -1)]},
        total_frequency=2,
    ),
]


# ---------------------------------------------------------------------------
# Protocol conformance
# ---------------------------------------------------------------------------


def test_sqlite_satisfies_protocol() -> None:
    store = _make_sqlite_store()
    assert isinstance(store, CorpusStore)


def test_memory_satisfies_protocol() -> None:
    store = _make_memory_store()
    assert isinstance(store, CorpusStore)


# ---------------------------------------------------------------------------
# Parametrised tests (run against both stores)
# ---------------------------------------------------------------------------


def test_add_document_and_total(store: Any) -> None:
    store.add_document(DOC_A)
    assert store.get_total_documents() == 1
    store.add_document(DOC_B)
    assert store.get_total_documents() == 2


def test_duplicate_document(store: Any) -> None:
    store.add_document(DOC_A)
    store.add_document(DOC_A)
    assert store.get_total_documents() == 1


def test_term_frequency(store: Any) -> None:
    store.add_document(DOC_A)
    store.add_document(DOC_B)
    store.index_candidates(CANDIDATES)

    assert store.get_term_frequency("machine learning") == 2
    assert store.get_term_frequency("deep learning") == 1
    assert store.get_term_frequency("learning") == 2
    assert store.get_term_frequency("nonexistent") == 0


def test_term_frequency_case_insensitive(store: Any) -> None:
    store.index_candidates(CANDIDATES)
    assert store.get_term_frequency("Machine Learning") == 2
    assert store.get_term_frequency("LEARNING") == 2


def test_document_frequency(store: Any) -> None:
    store.add_document(DOC_A)
    store.add_document(DOC_B)
    store.index_candidates(CANDIDATES)

    assert store.get_document_frequency("machine learning") == 2
    assert store.get_document_frequency("deep learning") == 1
    assert store.get_document_frequency("learning") == 2
    assert store.get_document_frequency("nonexistent") == 0


def test_corpus_total(store: Any) -> None:
    store.index_candidates(CANDIDATES)
    # machine learning: 2 (doc1:1 + doc2:1)
    # deep learning: 1 (doc2:1)
    # learning: 2 (doc1:1 + doc2:1)
    assert store.get_corpus_total() == 5


def test_cooccurrences(store: Any) -> None:
    store.index_candidates(CANDIDATES)

    # machine learning & learning both in doc1 and doc2
    assert store.get_cooccurrences("machine learning", "learning") == 2
    # machine learning & deep learning both in doc2 only
    assert store.get_cooccurrences("machine learning", "deep learning") == 1
    # deep learning & learning both in doc2 only
    assert store.get_cooccurrences("deep learning", "learning") == 1
    # reverse order should work too
    assert store.get_cooccurrences("learning", "machine learning") == 2
    # nonexistent
    assert store.get_cooccurrences("foo", "bar") == 0


def test_cooccurrences_case_insensitive(store: Any) -> None:
    store.index_candidates(CANDIDATES)
    assert store.get_cooccurrences("Machine Learning", "LEARNING") == 2


def test_containing_terms(store: Any) -> None:
    store.index_candidates(CANDIDATES)
    containers = store.get_containing_terms("learning")
    assert set(containers) == {"machine learning", "deep learning"}
    # "machine learning" should not contain itself
    assert "learning" not in containers


def test_term_document_frequency_map(store: Any) -> None:
    store.index_candidates(CANDIDATES)
    freq_map = store.get_term_document_frequency_map("machine learning")
    assert freq_map == {"doc1": 1, "doc2": 1}


def test_empty_store(store: Any) -> None:
    assert store.get_total_documents() == 0
    assert store.get_corpus_total() == 0
    assert store.get_term_frequency("anything") == 0
    assert store.get_document_frequency("anything") == 0
    assert store.get_cooccurrences("a", "b") == 0


# ---------------------------------------------------------------------------
# SQLite-specific: persistence
# ---------------------------------------------------------------------------


def test_sqlite_persistence() -> None:
    with tempfile.TemporaryDirectory() as tmp:
        db_path = Path(tmp) / "test.db"

        store = SQLiteCorpusStore(db_path)
        store.add_document(DOC_A)
        store.add_document(DOC_B)
        store.index_candidates(CANDIDATES)
        store.close()

        # Reopen and verify
        store2 = SQLiteCorpusStore(db_path)
        assert store2.get_total_documents() == 2
        assert store2.get_term_frequency("machine learning") == 2
        assert store2.get_document_frequency("deep learning") == 1
        assert store2.get_corpus_total() == 5
        assert store2.get_cooccurrences("machine learning", "learning") == 2
        store2.close()
