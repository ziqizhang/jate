"""Tests for jate.datasets module."""

from __future__ import annotations

from pathlib import Path
from unittest.mock import MagicMock, patch

from jate.datasets.acl_rdtec import AclRdtecMini, _load_documents, _load_gold, load_acl_rdtec_mini
from jate.datasets.acl_rdtec_full import AclRdtecFull
from jate.datasets.acter import Acter
from jate.datasets.base import Dataset
from jate.datasets.coastterm import CoastTerm
from jate.datasets.downloader import _get_cache_dir, download_dataset
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


# ------------------------------------------------------------------
# Downloader tests
# ------------------------------------------------------------------


class TestGetCacheDir:
    """_get_cache_dir returns the expected path."""

    def test_returns_correct_path(self):
        result = _get_cache_dir("test_dataset")
        expected = Path.home() / ".jate" / "datasets" / "test_dataset"
        assert result == expected

    def test_different_names_different_paths(self):
        a = _get_cache_dir("alpha")
        b = _get_cache_dir("beta")
        assert a != b
        assert a.name == "alpha"
        assert b.name == "beta"


class TestDownloadDataset:
    """download_dataset skips download when cache exists."""

    def test_skips_download_when_cached(self, tmp_path):
        cache_dir = tmp_path / "cached_ds"
        cache_dir.mkdir()
        # Put a marker file to confirm it's the same dir
        (cache_dir / "marker.txt").write_text("exists")

        with patch("jate.datasets.downloader._get_cache_dir", return_value=cache_dir):
            result = download_dataset("test", "https://github.com/fake/repo")

        assert result == cache_dir
        # Marker file should still exist (no re-download)
        assert (cache_dir / "marker.txt").exists()

    def test_force_triggers_redownload(self, tmp_path):
        """When force=True and cache exists, it should attempt to download."""
        cache_dir = tmp_path / "cached_ds"
        cache_dir.mkdir()

        with (
            patch("jate.datasets.downloader._get_cache_dir", return_value=cache_dir),
            patch("jate.datasets.downloader.urllib.request.urlopen") as mock_urlopen,
        ):
            # Simulate a download failure so we don't need a real zip
            mock_urlopen.side_effect = Exception("simulated network error")
            try:
                download_dataset("test", "https://github.com/fake/repo", force=True)
            except Exception:
                pass  # Expected: we just verify it attempted download

            mock_urlopen.assert_called_once()


# ------------------------------------------------------------------
# Protocol compliance for new dataset loaders
# ------------------------------------------------------------------


class TestAclRdtecFullProtocol:
    """AclRdtecFull satisfies the Dataset protocol."""

    def test_satisfies_protocol(self):
        ds = AclRdtecFull(directory="/fake/path")
        assert isinstance(ds, Dataset)

    def test_name(self):
        ds = AclRdtecFull(directory="/fake/path")
        assert ds.name == "acl_rdtec"


class TestActerProtocol:
    """Acter satisfies the Dataset protocol."""

    def test_satisfies_protocol(self):
        ds = Acter(directory="/fake/path")
        assert isinstance(ds, Dataset)

    def test_name_all_domains(self):
        ds = Acter(directory="/fake/path")
        assert ds.name == "acter"

    def test_name_specific_domain(self):
        ds = Acter(domain="wind", directory="/fake/path")
        assert ds.name == "acter_wind"

    def test_name_each_domain(self):
        for domain in ("corp", "equi", "htfl", "wind"):
            ds = Acter(domain=domain, directory="/fake/path")
            assert ds.name == f"acter_{domain}"


class TestCoastTermProtocol:
    """CoastTerm satisfies the Dataset protocol."""

    def test_satisfies_protocol(self):
        ds = CoastTerm(directory="/fake/path")
        assert isinstance(ds, Dataset)

    def test_name(self):
        ds = CoastTerm(directory="/fake/path")
        assert ds.name == "coastterm"


# ------------------------------------------------------------------
# CLI --list-datasets
# ------------------------------------------------------------------


class TestCliListDatasets:
    """CLI --list-datasets flag outputs dataset names."""

    def test_list_datasets_output(self, capsys):
        from jate.cli import _cmd_benchmark

        args = MagicMock()
        args.list_datasets = True

        _cmd_benchmark(args)

        captured = capsys.readouterr()
        assert "acl_rdtec_mini" in captured.out
        assert "acl_rdtec" in captured.out
        assert "acter" in captured.out
        assert "coastterm" in captured.out
        assert "acter_corp" in captured.out
        assert "acter_wind" in captured.out

    def test_list_datasets_descriptions(self, capsys):
        from jate.cli import _cmd_benchmark

        args = MagicMock()
        args.list_datasets = True

        _cmd_benchmark(args)

        captured = capsys.readouterr()
        # Should include some descriptive text
        assert "ACL RD-TEC" in captured.out
        assert "ACTER" in captured.out
        assert "CoastTerm" in captured.out
