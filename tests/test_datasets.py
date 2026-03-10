"""Tests for jate.datasets module."""

from __future__ import annotations

from pathlib import Path

from jate.datasets.acl_rdtec import AclRdtecMini, _load_documents, _load_gold, load_acl_rdtec_mini
from jate.datasets.base import Dataset
from jate.models import Document

FIXTURE_DIR = Path(__file__).resolve().parent / "fixtures" / "acl_rdtec_mini"


class TestLoadGold:
    """Gold standard loading."""

    def test_loads_terms(self):
        gold = _load_gold(FIXTURE_DIR / "gold.txt")
        assert isinstance(gold, set)
        assert len(gold) > 0
        # All terms should be lowercase
        for t in gold:
            assert t == t.lower(), f"Gold term not lowercase: {t!r}"

    def test_no_empty_terms(self):
        gold = _load_gold(FIXTURE_DIR / "gold.txt")
        assert "" not in gold


class TestLoadDocuments:
    """Document loading from text files."""

    def test_loads_three_docs(self):
        docs = _load_documents(FIXTURE_DIR)
        assert len(docs) == 3

    def test_documents_have_content(self):
        docs = _load_documents(FIXTURE_DIR)
        for doc in docs:
            assert isinstance(doc, Document)
            assert doc.doc_id
            assert len(doc.content) > 100, f"Doc {doc.doc_id} too short"

    def test_gold_txt_excluded(self):
        docs = _load_documents(FIXTURE_DIR)
        doc_ids = {d.doc_id for d in docs}
        assert "gold" not in doc_ids


class TestLoadAclRdtecMini:
    """Integration: load_acl_rdtec_mini function."""

    def test_returns_docs_and_gold(self):
        docs, gold = load_acl_rdtec_mini()
        assert len(docs) == 3
        assert len(gold) > 50  # should have a reasonable number of gold terms

    def test_gold_terms_appear_in_docs(self):
        docs, gold = load_acl_rdtec_mini()
        all_text = " ".join(d.content for d in docs).lower()
        # At least some gold terms should appear in the documents
        found = sum(1 for t in gold if t in all_text)
        assert found > 10, f"Only {found} gold terms found in documents"


class TestAclRdtecMiniClass:
    """AclRdtecMini wrapper class."""

    def test_name(self):
        ds = AclRdtecMini()
        assert ds.name == "acl_rdtec_mini"

    def test_satisfies_protocol(self):
        ds = AclRdtecMini()
        assert isinstance(ds, Dataset)

    def test_lazy_loading(self):
        ds = AclRdtecMini()
        # Internal state should be None before access
        assert ds._documents is None
        assert ds._gold is None
        # Access triggers load
        _ = ds.documents
        assert ds._documents is not None
        assert ds._gold is not None

    def test_custom_directory(self):
        ds = AclRdtecMini(FIXTURE_DIR)
        docs = ds.documents
        assert len(docs) == 3
