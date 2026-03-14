"""ACL RD-TEC 2.0 full dataset loader.

Downloads the full ACL RD-TEC 2.0 corpus from GitHub and provides
documents (plain text from XML files) and gold terms (extracted from
inline ``<term>`` tags).

GitHub repo: ``languagerecipes/acl-rd-tec-2.0``
"""

from __future__ import annotations

import re
import xml.etree.ElementTree as ET
from pathlib import Path

from jate.datasets.downloader import download_dataset
from jate.models import Document

_REPO_URL = "https://github.com/languagerecipes/acl-rd-tec-2.0"
_DATASET_NAME = "acl_rdtec"


class AclRdtecFull:
    """Loader for the full ACL RD-TEC 2.0 dataset."""

    def __init__(self, directory: Path | str | None = None, *, force_download: bool = False) -> None:
        self._dir = Path(directory) if directory else None
        self._force_download = force_download
        self._documents: list[Document] | None = None
        self._gold: set[str] | None = None

    @property
    def name(self) -> str:
        return "acl_rdtec"

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

    def _ensure_downloaded(self) -> Path:
        if self._dir is not None:
            return self._dir
        return download_dataset(_DATASET_NAME, _REPO_URL, branch="master", force=self._force_download)

    def _load(self) -> None:
        data_dir = self._ensure_downloaded()

        # Find XML files in the distribution/ directory (or any subdirectory)
        dist_dir = data_dir / "distribution"
        if dist_dir.is_dir():
            xml_files = sorted(dist_dir.rglob("*.xml"))
        else:
            # Fallback: search entire directory
            xml_files = sorted(data_dir.rglob("*.xml"))

        if not xml_files:
            msg = f"No XML files found in {data_dir}. Check that the dataset downloaded correctly."
            raise FileNotFoundError(msg)

        documents: list[Document] = []
        gold_terms: set[str] = set()

        for xml_path in xml_files:
            try:
                tree = ET.parse(xml_path)  # noqa: S314
            except ET.ParseError:
                continue

            root = tree.getroot()

            # Extract gold terms from <term> tags
            for term_el in root.iter("term"):
                term_text = _get_element_text(term_el).strip()
                if term_text:
                    gold_terms.add(term_text.lower())

            # Extract plain text (strip all XML tags) for document content
            full_text = _strip_xml_tags(ET.tostring(root, encoding="unicode", method="xml"))
            full_text = _normalize_whitespace(full_text)
            if full_text.strip():
                documents.append(Document(doc_id=xml_path.stem, content=full_text.strip()))

        self._documents = documents
        self._gold = gold_terms


def _get_element_text(element: ET.Element) -> str:
    """Get all text content from an element, including tail text of children."""
    parts: list[str] = []
    if element.text:
        parts.append(element.text)
    for child in element:
        parts.append(_get_element_text(child))
        if child.tail:
            parts.append(child.tail)
    return "".join(parts)


def _strip_xml_tags(xml_string: str) -> str:
    """Remove all XML/HTML tags from a string."""
    return re.sub(r"<[^>]+>", "", xml_string)


def _normalize_whitespace(text: str) -> str:
    """Collapse multiple whitespace characters into single spaces."""
    return re.sub(r"\s+", " ", text)
