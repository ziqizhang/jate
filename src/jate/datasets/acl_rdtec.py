"""ACL RD-TEC dataset loader.

The full ACL RD-TEC dataset contains 10,922 annotated terms from
computational linguistics papers. This module provides a loader for
the mini test fixture (3 documents) shipped with JATE's test suite.
"""

from __future__ import annotations

import xml.etree.ElementTree as ET
from pathlib import Path

from jate.models import Document

# Default location of the mini test fixture relative to the project root.
_FIXTURE_DIR = Path(__file__).resolve().parents[3] / "tests" / "fixtures" / "acl_rdtec_mini"


class AclRdtecMini:
    """Thin wrapper around the ACL RD-TEC mini fixture."""

    def __init__(self, directory: Path | str | None = None) -> None:
        self._dir = Path(directory) if directory else _FIXTURE_DIR
        self._documents: list[Document] | None = None
        self._gold: set[str] | None = None

    @property
    def name(self) -> str:
        return "acl_rdtec_mini"

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
        self._documents = _load_documents(self._dir)
        self._gold = _load_gold(self._dir / "gold.txt")


def load_acl_rdtec_mini(
    directory: Path | str | None = None,
) -> tuple[list[Document], set[str]]:
    """Load the mini ACL RD-TEC fixture.

    Parameters
    ----------
    directory:
        Path to the fixture directory. Defaults to
        ``tests/fixtures/acl_rdtec_mini`` relative to the project root.

    Returns
    -------
    tuple[list[Document], set[str]]
        A pair of (documents, gold_terms).
    """
    ds = AclRdtecMini(directory)
    return ds.documents, ds.gold_terms


# ------------------------------------------------------------------
# Internal helpers
# ------------------------------------------------------------------


def _load_documents(directory: Path) -> list[Document]:
    """Load ``.txt`` files from *directory* as Document objects."""
    docs: list[Document] = []
    for path in sorted(directory.glob("*.txt")):
        if path.name == "gold.txt":
            continue
        content = path.read_text(encoding="utf-8")
        docs.append(Document(doc_id=path.stem, content=content))
    return docs


def _load_gold(gold_path: Path) -> set[str]:
    """Load a gold-standard file (one term per line) and return lowercase terms."""
    terms: set[str] = set()
    with open(gold_path, encoding="utf-8") as fh:
        for line in fh:
            term = line.strip()
            if term:
                terms.add(term.lower())
    return terms


def load_gold_tsv(
    tsv_path: Path | str,
    *,
    valid_annotations: set[int] | None = None,
) -> set[str]:
    """Load gold terms from the original ACL RD-TEC TSV file.

    Parameters
    ----------
    tsv_path:
        Path to the TSV file with columns TERM_ID, TERM_STRING,
        TERM_ANNOTATION.
    valid_annotations:
        Set of annotation codes to accept.  Defaults to ``{1, 2}``
        (valid term or technology term).

    Returns
    -------
    set[str]
        Lowercase gold terms.
    """
    if valid_annotations is None:
        valid_annotations = {1, 2}

    terms: set[str] = set()
    path = Path(tsv_path)
    with open(path, encoding="utf-8") as fh:
        for line in fh:
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            if len(parts) >= 3:
                try:
                    annotation = int(parts[2])
                except ValueError:
                    continue
                if annotation in valid_annotations:
                    terms.add(parts[1].lower())
    return terms


def load_xml_documents(xml_dir: Path | str) -> list[Document]:
    """Load ACL RD-TEC XML documents, extracting paragraph text.

    Parameters
    ----------
    xml_dir:
        Directory containing ``*_cln.xml`` files.

    Returns
    -------
    list[Document]
    """
    xml_dir = Path(xml_dir)
    docs: list[Document] = []
    for path in sorted(xml_dir.glob("*.xml")):
        tree = ET.parse(path)  # noqa: S314
        root = tree.getroot()
        paragraphs: list[str] = []
        for title_el in root.iter("Title"):
            if title_el.text and title_el.text.strip():
                paragraphs.append(title_el.text.strip())
        for para_el in root.iter("Paragraph"):
            if para_el.text and para_el.text.strip():
                paragraphs.append(para_el.text.strip())
        content = "\n\n".join(paragraphs)
        docs.append(Document(doc_id=path.stem, content=content))
    return docs
