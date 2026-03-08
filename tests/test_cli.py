"""Tests for the JATE CLI."""

from __future__ import annotations

import json
import sys
from io import StringIO
from unittest.mock import patch

import pytest

from jate.cli import main


def _run_cli(*args: str) -> str:
    """Run the CLI with the given arguments and capture stdout."""
    with patch.object(sys, "argv", ["jate", *args]):
        buf = StringIO()
        with patch("sys.stdout", buf):
            main()
        return buf.getvalue()


class TestCLIExtract:
    def test_extract_text_table(self) -> None:
        output = _run_cli(
            "extract",
            "Machine learning is a branch of artificial intelligence. Machine learning uses algorithms.",
            "--algorithm",
            "cvalue",
        )
        assert "Rank" in output
        assert "Term" in output
        assert "Score" in output

    def test_extract_text_csv(self) -> None:
        output = _run_cli(
            "extract",
            "Machine learning is a branch of artificial intelligence. Machine learning uses algorithms.",
            "--algorithm",
            "cvalue",
            "--output",
            "csv",
        )
        assert "term,score,frequency,rank" in output

    def test_extract_text_json(self) -> None:
        output = _run_cli(
            "extract",
            "Machine learning is a branch of artificial intelligence. Machine learning uses algorithms.",
            "--algorithm",
            "tfidf",
            "--output",
            "json",
        )
        data = json.loads(output)
        assert isinstance(data, list)
        if len(data) > 0:
            assert "term" in data[0]
            assert "score" in data[0]

    def test_extract_top_limit(self) -> None:
        output = _run_cli(
            "extract",
            "Machine learning is a branch of artificial intelligence. Deep learning uses neural networks. Machine learning algorithms improve.",
            "--algorithm",
            "cvalue",
            "--top",
            "2",
        )
        # Count data rows (skip header and separator)
        lines = [l for l in output.strip().split("\n") if l.strip()]
        # header + separator + data rows
        data_lines = lines[2:]  # skip header and dashes
        assert len(data_lines) <= 2


class TestCLICorpus:
    def test_corpus_cvalue(self, tmp_path: object) -> None:
        import pathlib

        d = pathlib.Path(str(tmp_path))
        (d / "doc1.txt").write_text(
            "Machine learning is a branch of artificial intelligence. " "Machine learning algorithms learn from data.",
            encoding="utf-8",
        )
        (d / "doc2.txt").write_text(
            "Deep learning is a subset of machine learning. " "Neural networks power deep learning systems.",
            encoding="utf-8",
        )
        (d / "doc3.txt").write_text(
            "Natural language processing uses machine learning techniques. "
            "Machine learning models process language data.",
            encoding="utf-8",
        )
        output = _run_cli(
            "corpus",
            str(d),
            "--algorithm",
            "cvalue",
            "--top",
            "5",
            "--min-frequency",
            "1",
        )
        assert "Rank" in output
        assert "Term" in output
        assert "Score" in output

    def test_corpus_min_frequency_filter(self, tmp_path: object) -> None:
        import pathlib

        d = pathlib.Path(str(tmp_path))
        (d / "a.txt").write_text("Rare term appears once.", encoding="utf-8")
        (d / "b.txt").write_text("Another document entirely.", encoding="utf-8")
        output = _run_cli(
            "corpus",
            str(d),
            "--algorithm",
            "cvalue",
            "--min-frequency",
            "1",
        )
        # Should produce some output (table or "No terms found.")
        assert len(output.strip()) > 0


class TestCLICompare:
    def test_compare_algorithms(self, tmp_path: object) -> None:
        import pathlib

        d = pathlib.Path(str(tmp_path))
        (d / "doc1.txt").write_text(
            "Machine learning is a branch of artificial intelligence. " "Machine learning algorithms learn from data.",
            encoding="utf-8",
        )
        (d / "doc2.txt").write_text(
            "Deep learning is a subset of machine learning. " "Neural networks power deep learning systems.",
            encoding="utf-8",
        )
        output = _run_cli(
            "compare",
            str(d),
            "--algorithms",
            "cvalue,tfidf",
            "--top",
            "3",
        )
        assert "CVALUE" in output
        assert "TFIDF" in output

    def test_compare_default_algorithms(self, tmp_path: object) -> None:
        import pathlib

        d = pathlib.Path(str(tmp_path))
        (d / "f1.txt").write_text(
            "Machine learning uses algorithms. Machine learning is popular.",
            encoding="utf-8",
        )
        (d / "f2.txt").write_text(
            "Deep learning networks. Deep learning is advanced.",
            encoding="utf-8",
        )
        output = _run_cli(
            "compare",
            str(d),
            "--top",
            "3",
        )
        # Default algorithms include cvalue, tfidf, rake, ridf, basic
        assert "CVALUE" in output
        assert "TFIDF" in output


class TestCLIDemo:
    def test_demo_placeholder(self) -> None:
        output = _run_cli("demo")
        assert "Demo UI not yet available" in output


class TestCLINoCommand:
    def test_no_command_shows_help(self) -> None:
        output = _run_cli()
        assert "JATE" in output or "usage" in output.lower() or "help" in output.lower()
