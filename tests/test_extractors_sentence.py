"""Verify extractors tag candidates with sentence indices."""

from __future__ import annotations

import pytest

from jate.extractors.ngram import NGramExtractor
from jate.extractors.noun_phrase import NounPhraseExtractor
from jate.extractors.pos_pattern import PosPatternExtractor
from jate.models import Document
from jate.nlp.spacy_backend import SpacyBackend
from jate.store.memory_store import MemoryCorpusStore


@pytest.fixture(scope="module")
def nlp() -> SpacyBackend:
    return SpacyBackend("en_core_web_sm")


TWO_SENTENCE_DOC = [Document(doc_id="d1", content="Machine learning is important. Neural networks are powerful.")]


class TestPosPatternSentenceIdx:
    def test_candidates_have_sentence_idx(self, nlp: SpacyBackend) -> None:
        store = MemoryCorpusStore()
        ext = PosPatternExtractor()
        candidates = ext.extract(TWO_SENTENCE_DOC, nlp, store)
        assert len(candidates) > 0
        for c in candidates:
            for doc_id, positions in c.doc_positions.items():
                for start, end, sent_idx in positions:
                    assert sent_idx >= 0, f"{c.normalized_form} has sent_idx={sent_idx}"

    def test_different_sentences_get_different_idx(self, nlp: SpacyBackend) -> None:
        store = MemoryCorpusStore()
        ext = PosPatternExtractor()
        candidates = ext.extract(TWO_SENTENCE_DOC, nlp, store)
        sent_indices = set()
        for c in candidates:
            for positions in c.doc_positions.values():
                for _, _, sent_idx in positions:
                    sent_indices.add(sent_idx)
        assert len(sent_indices) >= 2, "Expected candidates from at least 2 sentences"


class TestNGramSentenceIdx:
    def test_candidates_have_sentence_idx(self, nlp: SpacyBackend) -> None:
        store = MemoryCorpusStore()
        ext = NGramExtractor(min_n=1, max_n=2)
        candidates = ext.extract(TWO_SENTENCE_DOC, nlp, store)
        assert len(candidates) > 0
        for c in candidates:
            for doc_id, positions in c.doc_positions.items():
                for start, end, sent_idx in positions:
                    assert sent_idx >= 0, f"{c.normalized_form} has sent_idx={sent_idx}"


class TestNounPhraseSentenceIdx:
    def test_candidates_have_sentence_idx(self, nlp: SpacyBackend) -> None:
        store = MemoryCorpusStore()
        ext = NounPhraseExtractor()
        candidates = ext.extract(TWO_SENTENCE_DOC, nlp, store)
        assert len(candidates) > 0
        for c in candidates:
            for doc_id, positions in c.doc_positions.items():
                for start, end, sent_idx in positions:
                    assert sent_idx >= 0, f"{c.normalized_form} has sent_idx={sent_idx}"
