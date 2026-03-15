"""Integration tests for the JATE public API."""

from __future__ import annotations

from pathlib import Path

import pytest

from jate.api import _resolve_extractor, compare, extract, extract_corpus
from jate.models import TermExtractionResult

# -----------------------------------------------------------------------
# Sample texts
# -----------------------------------------------------------------------

TEXT_ML = (
    "Machine learning is a branch of artificial intelligence. "
    "Deep learning uses neural networks. "
    "Machine learning algorithms improve through experience. "
    "Neural networks are fundamental to deep learning."
)

TEXT_NLP = (
    "Natural language processing enables computers to understand human language. "
    "Text mining and information extraction are key tasks in natural language processing. "
    "Named entity recognition is a subtask of information extraction."
)

TEXT_IR = (
    "Information retrieval systems help users find relevant documents. "
    "Search engines use information retrieval techniques. "
    "Document ranking is central to information retrieval."
)

TEXT_BIO = (
    "Protein folding is essential for biological function. "
    "Molecular biology studies protein structure and gene expression. "
    "Gene expression regulation involves transcription factors."
)


# -----------------------------------------------------------------------
# extract() tests
# -----------------------------------------------------------------------


class TestExtract:
    def test_extract_simple(self) -> None:
        result = extract(TEXT_ML)
        assert isinstance(result, TermExtractionResult)
        assert len(result) > 0
        term_strings = {t.string.lower() for t in result}
        # Should find at least some relevant terms
        assert any(
            kw in s for s in term_strings for kw in ("learning", "network", "intelligence")
        ), f"Expected relevant terms, got: {term_strings}"

    def test_extract_with_algorithm_tfidf_single_doc_raises(self) -> None:
        from jate.algorithms.base import AlgorithmIncompatibleError

        with pytest.raises(AlgorithmIncompatibleError):
            extract(TEXT_ML, algorithm="tfidf")

    def test_extract_returns_sorted(self) -> None:
        result = extract(TEXT_ML)
        scores = [t.score for t in result]
        assert scores == sorted(scores, reverse=True)

    def test_extract_ranks_assigned(self) -> None:
        result = extract(TEXT_ML)
        if len(result) > 0:
            assert result[0].rank == 1
            ranks = [t.rank for t in result]
            assert ranks == list(range(1, len(result) + 1))

    def test_extract_unknown_algorithm_raises(self) -> None:
        with pytest.raises(ValueError, match="Unknown algorithm"):
            extract(TEXT_ML, algorithm="nonexistent")

    def test_extract_unknown_extractor_raises(self) -> None:
        with pytest.raises(ValueError, match="Unknown extractor"):
            extract(TEXT_ML, extractor="nonexistent")


# -----------------------------------------------------------------------
# extract_corpus() tests
# -----------------------------------------------------------------------


class TestExtractCorpus:
    def test_extract_corpus_from_texts(self) -> None:
        texts = [TEXT_ML, TEXT_NLP, TEXT_IR]
        result = extract_corpus(texts, min_frequency=1)
        assert isinstance(result, TermExtractionResult)
        assert len(result) > 0

    def test_extract_corpus_from_files(self, tmp_path: Path) -> None:
        for i, text in enumerate([TEXT_ML, TEXT_NLP]):
            (tmp_path / f"doc{i}.txt").write_text(text, encoding="utf-8")
        file_paths = [str(tmp_path / f"doc{i}.txt") for i in range(2)]
        result = extract_corpus(file_paths, min_frequency=1)
        assert isinstance(result, TermExtractionResult)
        assert len(result) > 0

    def test_extract_corpus_from_directory(self, tmp_path: Path) -> None:
        for i, text in enumerate([TEXT_ML, TEXT_NLP, TEXT_IR]):
            (tmp_path / f"doc{i}.txt").write_text(text, encoding="utf-8")
        result = extract_corpus(str(tmp_path), min_frequency=1)
        assert isinstance(result, TermExtractionResult)
        assert len(result) > 0

    def test_extract_corpus_with_db(self, tmp_path: Path) -> None:
        db_file = str(tmp_path / "test.db")
        texts = [TEXT_ML, TEXT_NLP]
        result = extract_corpus(texts, db_path=db_file, min_frequency=1)
        assert isinstance(result, TermExtractionResult)
        assert len(result) > 0
        assert Path(db_file).exists()


# -----------------------------------------------------------------------
# compare() tests
# -----------------------------------------------------------------------


class TestCompare:
    def test_compare_default_algorithms(self) -> None:
        results = compare(TEXT_ML, algorithms=["cvalue", "rake", "ridf", "basic"])
        assert isinstance(results, dict)
        assert "cvalue" in results
        assert "rake" in results
        assert "ridf" in results
        assert "basic" in results
        for name, res in results.items():
            assert isinstance(res, TermExtractionResult), f"{name} not a TermExtractionResult"

    def test_compare_specific_algorithms(self) -> None:
        results = compare(TEXT_ML, algorithms=["basic", "ttf", "cvalue"])
        assert set(results.keys()) == {"basic", "ttf", "cvalue"}
        for res in results.values():
            assert len(res) > 0


# -----------------------------------------------------------------------
# Filter tests
# -----------------------------------------------------------------------


class TestFilters:
    def test_extract_min_frequency_filter(self) -> None:
        # With min_frequency=1, we get more terms
        result_low = extract(TEXT_ML, min_frequency=1)
        # With very high min_frequency, we should get fewer or no terms
        result_high = extract(TEXT_ML, min_frequency=100)
        assert len(result_high) <= len(result_low)

    def test_extract_min_words_filter(self) -> None:
        result = extract(TEXT_ML, min_words=2, min_frequency=1)
        for term in result:
            word_count = len(term.string.split())
            assert word_count >= 2, f"Term {term.string!r} has {word_count} words, expected >= 2"

    def test_extract_max_words_filter(self) -> None:
        result = extract(TEXT_ML, max_words=1, min_frequency=1)
        for term in result:
            word_count = len(term.string.split())
            assert word_count <= 1, f"Term {term.string!r} has {word_count} words, expected <= 1"


# -----------------------------------------------------------------------
# All algorithms runnable
# -----------------------------------------------------------------------


_ALL_ALGORITHM_NAMES = [
    "tfidf",
    "cvalue",
    "ncvalue",
    "attf",
    "ttf",
    "basic",
    "combobasic",
    "chi_square",
    "rake",
    "ridf",
    "weirdness",
    "termex",
    "glossex",
]


class TestAllAlgorithms:
    # TF-IDF raises AlgorithmIncompatibleError on single-doc input
    _SINGLE_DOC_INCOMPATIBLE = {"tfidf"}

    @pytest.mark.parametrize("algo_name", _ALL_ALGORITHM_NAMES)
    def test_algorithm_runs_without_crash(self, algo_name: str) -> None:
        if algo_name in self._SINGLE_DOC_INCOMPATIBLE:
            from jate.algorithms.base import AlgorithmIncompatibleError

            with pytest.raises(AlgorithmIncompatibleError):
                extract(TEXT_ML, algorithm=algo_name, min_frequency=1)
        else:
            result = extract(TEXT_ML, algorithm=algo_name, min_frequency=1)
            assert isinstance(result, TermExtractionResult)
            # Just check it doesn't crash and returns something
            assert len(result) >= 0


# -----------------------------------------------------------------------
# Resolver tests
# -----------------------------------------------------------------------


class TestResolvers:
    def test_resolve_extractor_pos_pattern(self) -> None:
        ext = _resolve_extractor("pos_pattern")
        assert ext is not None

    def test_resolve_extractor_ngram(self) -> None:
        ext = _resolve_extractor("ngram")
        assert ext is not None

    def test_resolve_extractor_noun_phrase(self) -> None:
        ext = _resolve_extractor("noun_phrase")
        assert ext is not None
