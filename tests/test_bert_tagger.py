"""Tests for transformer-based token classification taggers."""

from __future__ import annotations

import warnings
from unittest.mock import MagicMock, patch

import pytest

from jate.algorithms.base import ATETagger, OutputCapabilities
from jate.algorithms.bert_tagger import BertTagger, RoBERTaTagger, XLMRTagger
from jate.api import _is_tagger, _resolve_tagger
from jate.models import TermExtractionResult

# ---------------------------------------------------------------------------
# Step 1: corpus_level_warning
# ---------------------------------------------------------------------------


class TestCorpusLevelWarning:
    def test_returns_string(self):
        tagger = BertTagger("fake-model")
        warning = tagger.corpus_level_warning()
        assert isinstance(warning, str)
        assert "document-level tagger" in warning

    def test_mentions_class_name(self):
        tagger = XLMRTagger("fake-model")
        warning = tagger.corpus_level_warning()
        assert "XLMRTagger" in warning


# ---------------------------------------------------------------------------
# Step 2: BertTagger class hierarchy
# ---------------------------------------------------------------------------


class TestTaggerSubclasses:
    def test_bert_tagger_is_ate_tagger(self):
        assert issubclass(BertTagger, ATETagger)

    def test_xlmr_tagger_is_ate_tagger(self):
        assert issubclass(XLMRTagger, ATETagger)

    def test_roberta_tagger_is_ate_tagger(self):
        assert issubclass(RoBERTaTagger, ATETagger)

    def test_xlmr_tagger_is_bert_tagger(self):
        assert issubclass(XLMRTagger, BertTagger)

    def test_roberta_tagger_is_bert_tagger(self):
        assert issubclass(RoBERTaTagger, BertTagger)


class TestOutputCapabilities:
    def test_bert_tagger_capabilities(self):
        tagger = BertTagger("fake-model")
        caps = tagger.output_capabilities()
        assert isinstance(caps, OutputCapabilities)
        assert caps.produces_scores is True
        assert caps.produces_ranking is False
        assert caps.produces_offsets is True
        assert caps.produces_labels is False

    def test_xlmr_tagger_capabilities(self):
        tagger = XLMRTagger()
        caps = tagger.output_capabilities()
        assert caps.produces_scores is True
        assert caps.produces_offsets is True

    def test_roberta_tagger_capabilities(self):
        tagger = RoBERTaTagger()
        caps = tagger.output_capabilities()
        assert caps.produces_scores is True
        assert caps.produces_offsets is True


class TestTaggerProperties:
    def test_name(self):
        assert BertTagger("fake-model").name == "BertTagger"
        assert XLMRTagger().name == "XLMRTagger"
        assert RoBERTaTagger().name == "RoBERTaTagger"

    def test_description(self):
        tagger = BertTagger("my-model")
        assert "my-model" in tagger.description

    def test_default_models(self):
        xlmr = XLMRTagger()
        assert xlmr._model_name == "ziqizhang/jate-ate-xlmr"
        roberta = RoBERTaTagger()
        assert roberta._model_name == "ziqizhang/jate-ate-roberta"


# ---------------------------------------------------------------------------
# Step 2: _ensure_transformers
# ---------------------------------------------------------------------------


class TestEnsureTransformers:
    def test_raises_import_error_when_missing(self):
        from jate.algorithms.bert_tagger import _ensure_transformers

        with patch.dict("sys.modules", {"transformers": None}):
            with pytest.raises(ImportError, match="jate\\[neural\\]"):
                _ensure_transformers()


# ---------------------------------------------------------------------------
# Step 2: tag() with mocked HF pipeline
# ---------------------------------------------------------------------------


class TestTagMethod:
    def _make_tagger_with_mock(self, fake_entities):
        """Create a BertTagger with a mocked HF pipeline."""
        tagger = BertTagger("fake-model")
        mock_pipe = MagicMock(return_value=fake_entities)
        tagger._pipeline = mock_pipe
        return tagger

    def test_basic_extraction(self):
        fake_entities = [
            {"entity_group": "TERM", "start": 0, "end": 16, "score": 0.95, "word": "Machine learning"},
            {"entity_group": "TERM", "start": 21, "end": 36, "score": 0.88, "word": "neural networks"},
        ]
        tagger = self._make_tagger_with_mock(fake_entities)

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = tagger.tag("Machine learning and neural networks improve models.")

        assert isinstance(result, TermExtractionResult)
        terms = list(result)
        assert len(terms) == 2

        term_strings = {t.string for t in terms}
        assert "machine learning" in term_strings
        assert "neural networks" in term_strings

    def test_deduplication(self):
        """Same normalised term appearing twice -> one Term with two spans."""
        fake_entities = [
            {"entity_group": "TERM", "start": 0, "end": 4, "score": 0.9, "word": "Term"},
            {"entity_group": "TERM", "start": 20, "end": 24, "score": 0.85, "word": "term"},
        ]
        tagger = self._make_tagger_with_mock(fake_entities)

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = tagger.tag("Term is repeated. A term appears again.")

        terms = list(result)
        assert len(terms) == 1
        assert terms[0].string == "term"
        assert terms[0].frequency == 2
        assert len(terms[0].spans) == 2
        # Highest score should be kept
        assert terms[0].score == 0.9

    def test_surface_forms_collected(self):
        fake_entities = [
            {"entity_group": "TERM", "start": 0, "end": 3, "score": 0.9, "word": "NLP"},
            {"entity_group": "TERM", "start": 13, "end": 16, "score": 0.85, "word": "nlp"},
        ]
        tagger = self._make_tagger_with_mock(fake_entities)

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = tagger.tag("NLP is good. nlp is fun.")

        terms = list(result)
        assert len(terms) == 1
        assert "NLP" in terms[0].surface_forms
        assert "nlp" in terms[0].surface_forms

    def test_empty_text(self):
        tagger = self._make_tagger_with_mock([])

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = tagger.tag("   ")

        assert len(result) == 0

    def test_string_input(self):
        fake_entities = [
            {"entity_group": "TERM", "start": 0, "end": 4, "score": 0.9, "word": "test"},
        ]
        tagger = self._make_tagger_with_mock(fake_entities)

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = tagger.tag("test string input")

        terms = list(result)
        assert len(terms) == 1
        assert terms[0].spans[0].doc_id.startswith("doc_")

    def test_doc_with_text_attribute(self):
        fake_entities = [
            {"entity_group": "TERM", "start": 0, "end": 4, "score": 0.9, "word": "test"},
        ]
        tagger = self._make_tagger_with_mock(fake_entities)

        mock_doc = MagicMock()
        mock_doc.text = "test document object"
        mock_doc._ = MagicMock()
        mock_doc._.doc_id = "my-doc-123"

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = tagger.tag(mock_doc)

        terms = list(result)
        assert len(terms) == 1
        assert terms[0].spans[0].doc_id == "my-doc-123"

    def test_emits_warning(self):
        fake_entities = [
            {"entity_group": "TERM", "start": 0, "end": 4, "score": 0.9, "word": "test"},
        ]
        tagger = self._make_tagger_with_mock(fake_entities)

        with warnings.catch_warnings(record=True) as w:
            warnings.simplefilter("always")
            tagger.tag("test text")

        warning_messages = [str(x.message) for x in w]
        assert any("document-level tagger" in msg for msg in warning_messages)

    def test_span_offsets_correct(self):
        fake_entities = [
            {"entity_group": "TERM", "start": 5, "end": 12, "score": 0.9, "word": "machine"},
        ]
        tagger = self._make_tagger_with_mock(fake_entities)

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = tagger.tag("The  machine works.")

        terms = list(result)
        assert terms[0].spans[0].start == 5
        assert terms[0].spans[0].end == 12


# ---------------------------------------------------------------------------
# Step 3: _is_tagger and _resolve_tagger
# ---------------------------------------------------------------------------


class TestIsTagger:
    def test_tagger_names(self):
        assert _is_tagger("xlmr-tagger") is True
        assert _is_tagger("roberta-tagger") is True
        assert _is_tagger("XLMR-TAGGER") is True
        assert _is_tagger(" roberta-tagger ") is True

    def test_ranker_names(self):
        assert _is_tagger("cvalue") is False
        assert _is_tagger("tfidf") is False
        assert _is_tagger("rake") is False


class TestResolveTagger:
    def test_resolve_xlmr(self):
        tagger = _resolve_tagger("xlmr-tagger")
        assert isinstance(tagger, XLMRTagger)

    def test_resolve_roberta(self):
        tagger = _resolve_tagger("roberta-tagger")
        assert isinstance(tagger, RoBERTaTagger)

    def test_unknown_tagger_raises(self):
        with pytest.raises(ValueError, match="Unknown tagger"):
            _resolve_tagger("nonexistent-tagger")

    def test_resolve_with_kwargs(self):
        tagger = _resolve_tagger("xlmr-tagger", device=0)
        assert tagger._device == 0

    def test_resolve_with_custom_model(self):
        tagger = _resolve_tagger("xlmr-tagger", model="custom/model")
        assert tagger._model_name == "custom/model"


# ---------------------------------------------------------------------------
# Step 2: score() raises NotImplementedError
# ---------------------------------------------------------------------------


class TestScoreNotImplemented:
    def test_score_raises(self):
        tagger = BertTagger("fake-model")
        with pytest.raises(NotImplementedError, match="tag\\(\\)"):
            tagger.score([], MagicMock())
