"""Tests for the spaCy NLP backend and document loader."""

from __future__ import annotations

from pathlib import Path

import pytest

from jate.models import Document
from jate.nlp import DocumentLoader, SpacyBackend
from jate.protocols import NLPBackend

# ---------------------------------------------------------------------------
# Fixtures
# ---------------------------------------------------------------------------


@pytest.fixture(scope="module")
def backend() -> SpacyBackend:
    return SpacyBackend("en_core_web_sm")


SAMPLE_TEXT = "The quick brown fox jumps over the lazy dog."
TWO_SENTENCES = "Machine learning is powerful. Natural language processing is fascinating."


# ---------------------------------------------------------------------------
# SpacyBackend — protocol conformance
# ---------------------------------------------------------------------------


class TestSpacyBackendProtocol:
    def test_satisfies_nlp_backend_protocol(self, backend: SpacyBackend) -> None:
        assert isinstance(backend, NLPBackend)


# ---------------------------------------------------------------------------
# SpacyBackend — method tests
# ---------------------------------------------------------------------------


class TestSpacyBackendMethods:
    def test_tokenize(self, backend: SpacyBackend) -> None:
        tokens = backend.tokenize(SAMPLE_TEXT)
        assert isinstance(tokens, list)
        assert len(tokens) > 0
        assert "fox" in tokens
        assert "." in tokens

    def test_pos_tag(self, backend: SpacyBackend) -> None:
        tags = backend.pos_tag(SAMPLE_TEXT)
        assert isinstance(tags, list)
        assert all(isinstance(pair, tuple) and len(pair) == 2 for pair in tags)
        # "fox" should be tagged as a NOUN
        tag_dict = dict(tags)
        assert tag_dict["fox"] == "NOUN"

    def test_lemmatize(self, backend: SpacyBackend) -> None:
        pairs = backend.lemmatize(SAMPLE_TEXT)
        assert isinstance(pairs, list)
        lemma_dict = dict(pairs)
        assert lemma_dict["jumps"] == "jump"

    def test_sentence_split(self, backend: SpacyBackend) -> None:
        sentences = backend.sentence_split(TWO_SENTENCES)
        assert len(sentences) == 2
        assert "Machine learning is powerful." in sentences[0]

    def test_noun_chunks(self, backend: SpacyBackend) -> None:
        chunks = backend.noun_chunks(SAMPLE_TEXT)
        assert isinstance(chunks, list)
        assert len(chunks) > 0
        # "The quick brown fox" should be one chunk
        assert any("fox" in chunk for chunk in chunks)


# ---------------------------------------------------------------------------
# SpacyBackend — error handling
# ---------------------------------------------------------------------------


class TestSpacyBackendErrors:
    def test_missing_model_raises(self) -> None:
        with pytest.raises(OSError, match="not installed"):
            SpacyBackend("nonexistent_model_xyz")


# ---------------------------------------------------------------------------
# DocumentLoader
# ---------------------------------------------------------------------------


class TestDocumentLoader:
    def test_load_texts(self) -> None:
        texts = ["Hello world.", "Another document."]
        docs = DocumentLoader.load_texts(texts)
        assert len(docs) == 2
        assert all(isinstance(d, Document) for d in docs)
        assert docs[0].content == "Hello world."
        assert docs[1].content == "Another document."
        # IDs should be unique
        assert docs[0].doc_id != docs[1].doc_id

    def test_load_file(self, tmp_path: Path) -> None:
        file = tmp_path / "sample.txt"
        file.write_text("Test content here.", encoding="utf-8")
        doc = DocumentLoader.load_file(file)
        assert isinstance(doc, Document)
        assert doc.doc_id == "sample"
        assert doc.content == "Test content here."
        assert "source" in doc.metadata

    def test_load_directory(self, tmp_path: Path) -> None:
        (tmp_path / "a.txt").write_text("Alpha", encoding="utf-8")
        (tmp_path / "b.txt").write_text("Beta", encoding="utf-8")
        (tmp_path / "c.csv").write_text("not matched", encoding="utf-8")
        docs = DocumentLoader.load_directory(tmp_path)
        assert len(docs) == 2
        ids = {d.doc_id for d in docs}
        assert ids == {"a", "b"}
