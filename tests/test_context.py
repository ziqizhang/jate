"""Unit tests for ContextIndex."""
from __future__ import annotations

import pytest
from jate.context import ContextIndex
from jate.models import Candidate, Document
from jate.nlp.spacy_backend import SpacyBackend


def _make_candidate(normalized: str, positions: dict[str, list[tuple[int, int, int]]]) -> Candidate:
    """Build a candidate with explicit positions (bypasses add_position)."""
    c = Candidate(surface_form=normalized, normalized_form=normalized)
    c.doc_positions = positions
    c.total_frequency = sum(len(v) for v in positions.values())
    return c


class TestSentenceCooccurrence:
    def test_same_sentence(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 1

    def test_different_sentences(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(50, 66, 1)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 0

    def test_multiple_sentences(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0), (100, 114, 2)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0), (120, 136, 2)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 2

    def test_cross_document(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)], "d2": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)], "d2": [(20, 36, 0)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 2

    def test_symmetric(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("machine learning", "neural network") == 1

    def test_unknown_term(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        idx = ContextIndex.build([c1], [])
        assert idx.get_sentence_cooccurrences("neural network", "unknown") == 0


class TestContextTermTotal:
    def test_counts_distinct_terms_in_sentence(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)]})
        c3 = _make_candidate("deep learning", {"d1": [(50, 63, 1)]})
        idx = ContextIndex.build([c1, c2, c3], [])
        # sentence 0 has 2 terms, sentence 1 has 1 term
        assert idx.get_context_term_total("neural network") == 2
        assert idx.get_context_term_total("machine learning") == 2
        assert idx.get_context_term_total("deep learning") == 1

    def test_multiple_sentences(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0), (100, 114, 1)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)]})
        c3 = _make_candidate("deep learning", {"d1": [(120, 133, 1)]})
        idx = ContextIndex.build([c1, c2, c3], [])
        # "neural network" in sentence 0 (2 terms) + sentence 1 (2 terms) = 4
        assert idx.get_context_term_total("neural network") == 4

    def test_unknown_term(self) -> None:
        idx = ContextIndex.build([], [])
        assert idx.get_context_term_total("unknown") == 0


class TestAdjacentWords:
    def test_with_nlp_backend(self) -> None:
        text = "The neural network is powerful."
        doc = Document(doc_id="d1", content=text)
        c = _make_candidate("neural network", {"d1": [(4, 18, 0)]})
        nlp = SpacyBackend("en_core_web_sm")
        idx = ContextIndex.build([c], [doc], nlp_backend=nlp)
        adj = idx.get_adjacent_words("neural network")
        assert isinstance(adj, dict)
        assert len(adj) > 0

    def test_without_nlp_backend(self) -> None:
        c = _make_candidate("neural network", {"d1": [(4, 18, 0)]})
        doc = Document(doc_id="d1", content="The neural network is powerful.")
        idx = ContextIndex.build([c], [doc])
        adj = idx.get_adjacent_words("neural network")
        assert adj == {}

    def test_unknown_term(self) -> None:
        idx = ContextIndex.build([], [])
        assert idx.get_adjacent_words("unknown") == {}

    def test_direct_construction(self) -> None:
        """Constructor can be called directly with precomputed data."""
        ctx = ContextIndex(
            sent_cooc={("a", "b"): 3},
            context_totals={"a": 5, "b": 5},
            adjacent={"a": {"the": 2}},
        )
        assert ctx.get_sentence_cooccurrences("a", "b") == 3
        assert ctx.get_context_term_total("a") == 5
        assert ctx.get_adjacent_words("a") == {"the": 2}
