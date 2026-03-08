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
    Weirdness,
)
from jate.api import compare, extract, extract_corpus
from jate.corpus import Corpus
from jate.extractors import NGramExtractor, NounPhraseExtractor, PosPatternExtractor
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
    # Extractors
    "PosPatternExtractor",
    "NGramExtractor",
    "NounPhraseExtractor",
    # Corpus
    "Corpus",
    # Stores
    "SQLiteCorpusStore",
    "MemoryCorpusStore",
    # NLP
    "SpacyBackend",
    "DocumentLoader",
]
