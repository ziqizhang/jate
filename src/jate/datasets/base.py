"""Base protocol for JATE benchmark datasets."""

from __future__ import annotations

from typing import Protocol, runtime_checkable

from jate.models import Document


@runtime_checkable
class Dataset(Protocol):
    """Protocol that dataset loaders should satisfy.

    A dataset provides a list of documents and a set of gold-standard
    terms for evaluation.
    """

    @property
    def name(self) -> str:
        """Short identifier for the dataset."""
        ...

    @property
    def documents(self) -> list[Document]:
        """Corpus documents."""
        ...

    @property
    def gold_terms(self) -> set[str]:
        """Gold-standard terms (lowercase normalised)."""
        ...
