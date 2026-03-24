"""JATE -- Just Automatic Term Extraction."""

from __future__ import annotations

__version__ = "3.2.0"

from jate.algorithms import (
    ATTF,
    RAKE,
    RIDF,
    TFIDF,
    TTF,
    Algorithm,
    AlgorithmIncompatibleError,
    ATERanker,
    ATETagger,
    Basic,
    ChiSquare,
    ComboBasic,
    CValue,
    GlossEx,
    NCValue,
    OutputCapabilities,
    TermEx,
    Voting,
    Weirdness,
)

# Neural taggers (optional — requires jate[neural])
try:
    from jate.algorithms.bert_tagger import BertTagger, RoBERTaTagger, XLMRTagger
except ImportError:
    pass
from jate.api import compare, extract, extract_corpus
from jate.benchmark import BenchmarkRunner
from jate.config import JATEConfig
from jate.context import ContextIndex
from jate.corpus import Corpus
from jate.evaluation import EvaluationResult, Evaluator
from jate.extractors import NGramExtractor, NounPhraseExtractor, PosPatternExtractor
from jate.features import (
    ChiSquareFrequentTerms,
    Containment,
    ContextFrequency,
    ContextWindow,
    Cooccurrence,
    ReferenceFrequency,
    TermComponentIndex,
    TermFrequency,
    WordFrequency,
)
from jate.models import Candidate, Document, Term, TermExtractionResult, TermSpan
from jate.nlp import DocumentLoader, SpacyBackend
from jate.store import MemoryCorpusStore, SQLiteCorpusStore

# Register spaCy component factory (if spaCy is available and compatible).
# Catches broad Exception because spaCy + pydantic v2 can conflict on some
# Python versions (e.g., ConfigSchemaNlp validation errors on 3.11).
try:
    import jate.spacy_component  # noqa: F401
except Exception:
    pass

__all__ = [
    # Public API functions
    "extract",
    "extract_corpus",
    "compare",
    # Models
    "Term",
    "TermSpan",
    "Candidate",
    "Document",
    "TermExtractionResult",
    # Algorithms
    "Algorithm",
    "AlgorithmIncompatibleError",
    "ATERanker",
    "ATETagger",
    "BertTagger",
    "OutputCapabilities",
    "RoBERTaTagger",
    "TFIDF",
    "XLMRTagger",
    "CValue",
    "NCValue",
    "ATTF",
    "TTF",
    "Basic",
    "ComboBasic",
    "ChiSquare",
    "RAKE",
    "RIDF",
    "Weirdness",
    "TermEx",
    "GlossEx",
    "Voting",
    # Extractors
    "PosPatternExtractor",
    "NGramExtractor",
    "NounPhraseExtractor",
    # Evaluation & Benchmark
    "Evaluator",
    "EvaluationResult",
    "BenchmarkRunner",
    # Context
    "ContextIndex",
    # Features
    "TermFrequency",
    "WordFrequency",
    "ReferenceFrequency",
    "ContextWindow",
    "ContextFrequency",
    "TermComponentIndex",
    "Containment",
    "Cooccurrence",
    "ChiSquareFrequentTerms",
    # Corpus
    "Corpus",
    # Stores
    "SQLiteCorpusStore",
    "MemoryCorpusStore",
    # NLP
    "SpacyBackend",
    "DocumentLoader",
    # Config
    "JATEConfig",
]
