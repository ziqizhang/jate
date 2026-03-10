"""JATE -- Just Automatic Term Extraction."""

from __future__ import annotations

__version__ = "3.0.0"

from jate.algorithms import (
    ATTF,
    RAKE,
    RIDF,
    TFIDF,
    TTF,
    Algorithm,
    Basic,
    ChiSquare,
    ComboBasic,
    CValue,
    GlossEx,
    NCValue,
    TermEx,
    Voting,
    Weirdness,
)
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
from jate.models import Candidate, Document, Term, TermExtractionResult
from jate.nlp import DocumentLoader, SpacyBackend
from jate.store import MemoryCorpusStore, SQLiteCorpusStore

__all__ = [
    # Public API functions
    "extract",
    "extract_corpus",
    "compare",
    # Models
    "Term",
    "Candidate",
    "Document",
    "TermExtractionResult",
    # Algorithms
    "Algorithm",
    "TFIDF",
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
