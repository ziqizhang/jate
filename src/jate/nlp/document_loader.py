"""Utilities for loading documents into JATE :class:`~jate.models.Document` objects."""

from __future__ import annotations

import uuid
from pathlib import Path

from jate.models import Document


class DocumentLoader:
    """Load plain-text files (or raw strings) as :class:`Document` instances."""

    # -- Single file ----------------------------------------------------------

    @staticmethod
    def load_file(path: str | Path) -> Document:
        """Read a single UTF-8 text file and return a :class:`Document`.

        The ``doc_id`` is derived from the file name (stem).
        """
        p = Path(path)
        content = p.read_text(encoding="utf-8")
        return Document(doc_id=p.stem, content=content, metadata={"source": str(p)})

    # -- Directory ------------------------------------------------------------

    @staticmethod
    def load_directory(path: str | Path, glob_pattern: str = "*.txt") -> list[Document]:
        """Load all files matching *glob_pattern* under *path*."""
        p = Path(path)
        documents: list[Document] = []
        for file_path in sorted(p.glob(glob_pattern)):
            if file_path.is_file():
                documents.append(DocumentLoader.load_file(file_path))
        return documents

    # -- Raw strings ----------------------------------------------------------

    @staticmethod
    def load_texts(texts: list[str]) -> list[Document]:
        """Wrap raw strings as :class:`Document` objects with auto-generated IDs."""
        return [Document(doc_id=uuid.uuid4().hex[:12], content=text) for text in texts]
