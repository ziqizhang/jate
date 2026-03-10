"""Corpus statistics stores."""

from jate.store.memory_store import MemoryCorpusStore
from jate.store.sqlite_store import SQLiteCorpusStore

__all__ = ["MemoryCorpusStore", "SQLiteCorpusStore"]
