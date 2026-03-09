from __future__ import annotations

import pytest
from jate.features import TermFrequency
from jate.models import Candidate


class TestTermFrequency:
    def _make_candidates(self):
        """Two candidates across 2 docs."""
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 50, 64, sentence_idx=1)
        c1.add_position("doc2", 10, 24, sentence_idx=0)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 20, 33, sentence_idx=0)
        return [c1, c2]

    def test_build_from_candidates(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        assert tf.get_ttf("neural network") == 3
        assert tf.get_ttf("deep learning") == 1
        assert tf.get_df("neural network") == 2
        assert tf.get_df("deep learning") == 1
        assert tf.total_docs == 2
        # corpus_total = sum of all TTF = 3 + 1 = 4
        assert tf.corpus_total == 4

    def test_get_ttf_norm(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        # Java: getTTFNorm = TTF / (corpusTotal + 1) = 3 / 5
        assert tf.get_ttf_norm("neural network") == pytest.approx(3 / 5)

    def test_get_doc_freq_map(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        m = tf.get_doc_freq_map("neural network")
        assert m == {"doc1": 2, "doc2": 1}

    def test_unknown_term_returns_zero(self):
        tf = TermFrequency.build([], total_docs=0)
        assert tf.get_ttf("nonexistent") == 0
        assert tf.get_df("nonexistent") == 0
        assert tf.get_ttf_norm("nonexistent") == 0.0
