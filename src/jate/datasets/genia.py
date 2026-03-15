"""GENIA corpus dataset loader.

Loads the GENIA 3.02 corpus from a local directory. The corpus XML uses
``<cons lex="..." sem="...">`` tags for annotated biomedical terms.

The corpus is not auto-downloadable due to licensing. Users must place
the files in ``.data/`` or provide a path to the directory containing
``GENIAcorpus3.02.xml`` and ``concept.txt``.
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

from jate.models import Document

# Default location relative to the project root.
_DEFAULT_DIR = Path(__file__).resolve().parents[3] / ".data"


class Genia:
    """Loader for the GENIA 3.02 corpus.

    Parameters
    ----------
    directory:
        Path to the directory containing ``GENIAcorpus3.02.xml`` and
        ``concept.txt``. Defaults to ``.data/`` at the project root.
    """

    def __init__(self, directory: Path | str | None = None) -> None:
        self._dir = Path(directory) if directory else _DEFAULT_DIR
        self._documents: list[Document] | None = None
        self._gold: set[str] | None = None

    @property
    def name(self) -> str:
        return "genia"

    @property
    def documents(self) -> list[Document]:
        if self._documents is None:
            self._load()
        assert self._documents is not None
        return self._documents

    @property
    def gold_terms(self) -> set[str]:
        if self._gold is None:
            self._load()
        assert self._gold is not None
        return self._gold

    def _load(self) -> None:
        corpus_path = self._dir / "GENIAcorpus3.02.xml"
        concept_path = self._dir / "concept.txt"

        if not corpus_path.exists():
            msg = (
                f"GENIA corpus not found at {corpus_path}. "
                "Place GENIAcorpus3.02.xml in the .data/ directory "
                "or provide the directory path."
            )
            raise FileNotFoundError(msg)

        self._documents = _load_genia_documents(corpus_path)

        if concept_path.exists():
            self._gold = _load_concept_file(concept_path)
        else:
            # Fall back to extracting terms from XML <cons> tags
            self._gold = _extract_terms_from_xml(corpus_path)


def _load_genia_documents(corpus_path: Path) -> list[Document]:
    """Parse the GENIA XML and extract one Document per article."""
    tree = ET.parse(corpus_path)  # noqa: S314
    root = tree.getroot()

    documents: list[Document] = []
    for article in root.iter("article"):
        doc_id = _get_article_id(article)
        text = _get_article_text(article)
        if text.strip():
            documents.append(Document(doc_id=doc_id, content=text.strip()))

    return documents


def _get_article_id(article: ET.Element) -> str:
    """Extract a document ID from an article element."""
    bib = article.find(".//bibliomisc")
    if bib is not None and bib.text:
        return bib.text.strip()
    # Fallback: use position
    return f"article_{id(article)}"


def _get_article_text(article: ET.Element) -> str:
    """Extract plain text from an article, stripping all XML markup."""
    parts: list[str] = []

    for sentence in article.iter("sentence"):
        sentence_text = _element_text_content(sentence)
        if sentence_text.strip():
            parts.append(sentence_text.strip())

    return " ".join(parts)


def _element_text_content(element: ET.Element) -> str:
    """Recursively extract all text content from an element."""
    parts: list[str] = []
    if element.text:
        parts.append(element.text)
    for child in element:
        parts.append(_element_text_content(child))
        if child.tail:
            parts.append(child.tail)
    return "".join(parts)


def _load_concept_file(concept_path: Path) -> set[str]:
    """Load gold terms from concept.txt (one term per line)."""
    terms: set[str] = set()
    with open(concept_path, encoding="utf-8") as fh:
        for line in fh:
            term = line.strip()
            if term:
                terms.add(term.lower())
    return terms


def _extract_terms_from_xml(corpus_path: Path) -> set[str]:
    """Extract terms from <cons> tags as a fallback when concept.txt is missing."""
    tree = ET.parse(corpus_path)  # noqa: S314
    root = tree.getroot()

    terms: set[str] = set()
    for cons in root.iter("cons"):
        lex = cons.get("lex", "")
        if lex:
            # The lex attribute uses underscores for spaces
            term = lex.replace("_", " ").strip()
            if term:
                terms.add(term.lower())

    return terms
