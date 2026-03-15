"""Tests for the spaCy pipeline component."""

import warnings

import pytest
import spacy

import jate  # noqa: F401 — triggers factory registration
from jate.models import Term


class TestSpacyComponent:
    """Test the jate spaCy pipeline component."""

    def setup_method(self):
        self.nlp = spacy.load("en_core_web_sm")

    def test_add_pipe(self):
        """Component can be added to pipeline."""
        self.nlp.add_pipe("jate")
        assert "jate" in self.nlp.pipe_names

    def test_extract_terms(self):
        """Component extracts terms from a document."""
        self.nlp.add_pipe("jate", config={"algorithm": "cvalue"})
        doc = self.nlp("Machine learning and neural networks improve deep learning models.")
        assert hasattr(doc._, "terms")
        assert len(doc._.terms) > 0
        assert all(isinstance(t, Term) for t in doc._.terms)

    def test_terms_have_scores(self):
        """Extracted terms have non-zero scores."""
        self.nlp.add_pipe("jate", config={"algorithm": "cvalue"})
        doc = self.nlp("Machine learning and neural networks improve deep learning models.")
        scored = [t for t in doc._.terms if t.score > 0]
        assert len(scored) > 0

    def test_terms_have_spans(self):
        """Extracted terms have span offsets."""
        self.nlp.add_pipe("jate", config={"algorithm": "cvalue"})
        doc = self.nlp("Machine learning and neural networks improve deep learning models.")
        terms_with_spans = [t for t in doc._.terms if t.spans]
        assert len(terms_with_spans) > 0
        # Verify spans map back to text
        for term in terms_with_spans:
            for span in term.spans:
                surface = doc.text[span.start : span.end]
                assert len(surface) > 0

    def test_tfidf_returns_empty_on_single_doc(self):
        """TF-IDF should raise or warn on single document."""
        self.nlp.add_pipe("jate", config={"algorithm": "tfidf"})
        # Should not crash -- component catches AlgorithmIncompatibleError
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            doc = self.nlp("Some text about algorithms.")
        assert doc._.terms == []

    def test_warns_on_single_doc(self):
        """All rankers should warn about single-document mode."""
        self.nlp.add_pipe("jate", config={"algorithm": "basic"})
        with pytest.warns(UserWarning, match="corpus-level"):
            self.nlp("Machine learning and neural networks.")

    def test_custom_algorithm(self):
        """Different algorithms produce results."""
        self.nlp.add_pipe("jate", config={"algorithm": "rake"})
        doc = self.nlp("Machine learning and neural networks improve deep learning models.")
        assert len(doc._.terms) > 0

    def test_nlp_pipe(self):
        """Component works with nlp.pipe() for multiple documents."""
        self.nlp.add_pipe("jate", config={"algorithm": "cvalue"})
        texts = [
            "Machine learning is important.",
            "Neural networks are powerful.",
        ]
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            docs = list(self.nlp.pipe(texts))
        assert len(docs) == 2
        for doc in docs:
            assert hasattr(doc._, "terms")

    def test_empty_doc(self):
        """Component handles empty or very short documents."""
        self.nlp.add_pipe("jate", config={"algorithm": "cvalue"})
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            doc = self.nlp("")
        assert doc._.terms == []
