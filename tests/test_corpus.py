"""Tests for the Corpus wrapper class."""

from __future__ import annotations

import tempfile
from pathlib import Path

import pytest

from jate.corpus import Corpus
from jate.protocols import CorpusStore


class TestCorpusInit:
    def test_memory_store_by_default(self) -> None:
        corpus = Corpus()
        from jate.store.memory_store import MemoryCorpusStore

        assert isinstance(corpus.store, MemoryCorpusStore)

    def test_sqlite_store_with_db_path(self, tmp_path: Path) -> None:
        db = tmp_path / "test.db"
        corpus = Corpus(db_path=str(db))
        from jate.store.sqlite_store import SQLiteCorpusStore

        assert isinstance(corpus.store, SQLiteCorpusStore)

    def test_nlp_property(self) -> None:
        corpus = Corpus()
        from jate.nlp.spacy_backend import SpacyBackend

        assert isinstance(corpus.nlp, SpacyBackend)


class TestCorpusAddDocuments:
    def test_add_raw_text(self) -> None:
        corpus = Corpus()
        corpus.add_documents(["Machine learning is great.", "Deep learning too."])
        assert len(corpus.documents) == 2
        assert corpus.get_total_documents() == 2

    def test_add_files(self, tmp_path: Path) -> None:
        f1 = tmp_path / "a.txt"
        f1.write_text("Hello world.", encoding="utf-8")
        f2 = tmp_path / "b.txt"
        f2.write_text("Goodbye world.", encoding="utf-8")

        corpus = Corpus()
        corpus.add_documents([str(f1), str(f2)])
        assert len(corpus.documents) == 2
        assert corpus.documents[0].content == "Hello world."

    def test_add_directory(self, tmp_path: Path) -> None:
        for name in ["x.txt", "y.txt", "z.txt"]:
            (tmp_path / name).write_text(f"Content of {name}", encoding="utf-8")

        corpus = Corpus()
        corpus.add_directory(str(tmp_path))
        assert len(corpus.documents) == 3


class TestCorpusStoreProtocol:
    def test_satisfies_corpus_store_protocol(self) -> None:
        corpus = Corpus()
        assert isinstance(corpus, CorpusStore)

    def test_delegation_methods(self) -> None:
        corpus = Corpus()
        corpus.add_documents(["Some text here."])
        # These should not raise
        assert corpus.get_total_documents() == 1
        assert corpus.get_term_frequency("nonexistent") == 0
        assert corpus.get_document_frequency("nonexistent") == 0
        assert corpus.get_cooccurrences("a", "b") == 0
        assert corpus.get_corpus_total() == 0


class TestCorpusExport:
    def test_exported_from_init(self) -> None:
        from jate import Corpus as ExportedCorpus

        assert ExportedCorpus is Corpus
