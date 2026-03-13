"""ACTER v1.5 dataset loader.

Downloads the ACTER corpus from GitHub and provides English-language
documents and gold terms across one or more domains.

GitHub repo: ``AylaRT/ACTER``

Domains:
- ``corp`` — corruption
- ``equi`` — dressage / equitation
- ``htfl`` — heart failure
- ``wind`` — wind energy
"""

from __future__ import annotations

from pathlib import Path

from jate.datasets.downloader import download_dataset
from jate.models import Document

_REPO_URL = "https://github.com/AylaRT/ACTER"
_DATASET_NAME = "acter"
_LANG = "en"
_ALL_DOMAINS = ("corp", "equi", "htfl", "wind")


class Acter:
    """Loader for the ACTER v1.5 dataset (English).

    Parameters
    ----------
    domain:
        Domain code to load. One of ``corp``, ``equi``, ``htfl``, ``wind``,
        or ``None`` for all domains combined.
    directory:
        Override the dataset directory (skip download).
    force_download:
        Force re-download even if cached.
    """

    def __init__(
        self,
        domain: str | None = None,
        directory: Path | str | None = None,
        *,
        force_download: bool = False,
    ) -> None:
        self._domain = domain
        self._dir = Path(directory) if directory else None
        self._force_download = force_download
        self._documents: list[Document] | None = None
        self._gold: set[str] | None = None

    @property
    def name(self) -> str:
        if self._domain:
            return f"acter_{self._domain}"
        return "acter"

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
        return download_dataset(_DATASET_NAME, _REPO_URL, force=self._force_download)

    def _load(self) -> None:
        data_dir = self._ensure_downloaded()

        domains = (self._domain,) if self._domain else _ALL_DOMAINS
        all_docs: list[Document] = []
        all_terms: set[str] = set()

        for domain in domains:
            domain_dir = data_dir / _LANG / domain
            if not domain_dir.is_dir():
                msg = f"Domain directory not found: {domain_dir}"
                raise FileNotFoundError(msg)

            # Load corpus texts
            texts_dir = domain_dir / "texts" / "annotated"
            if not texts_dir.is_dir():
                # Fallback: try texts/ directly
                texts_dir = domain_dir / "texts"

            if texts_dir.is_dir():
                for txt_path in sorted(texts_dir.glob("*.txt")):
                    content = txt_path.read_text(encoding="utf-8").strip()
                    if content:
                        doc_id = f"{domain}_{txt_path.stem}"
                        all_docs.append(Document(doc_id=doc_id, content=content))

            # Load gold terms from annotations/
            ann_dir = domain_dir / "annotations"
            if ann_dir.is_dir():
                terms = _load_acter_terms(ann_dir)
                all_terms.update(terms)

        self._documents = all_docs
        self._gold = all_terms


def _load_acter_terms(ann_dir: Path) -> set[str]:
    """Load unique gold terms from ACTER annotation files.

    Looks for files containing sequential annotations with unique terms.
    Typically files with names like ``*_seq.ann`` or term list files.
    """
    terms: set[str] = set()

    # Look for unique term list files first (common patterns in ACTER)
    # Priority: files with "unique" or "sequential" in the name
    candidates: list[Path] = []

    for pattern in ["*unique*", "*seq*", "*.ann", "*.txt", "*.tsv"]:
        candidates.extend(ann_dir.glob(pattern))

    if not candidates:
        # Fallback: any file in the annotations directory
        candidates = list(ann_dir.iterdir())

    # Deduplicate
    seen_paths: set[Path] = set()
    for path in candidates:
        if path.is_file() and path not in seen_paths:
            seen_paths.add(path)
            try:
                text = path.read_text(encoding="utf-8")
            except (UnicodeDecodeError, OSError):
                continue
            for line in text.splitlines():
                term = line.strip()
                # Skip empty lines, comments, and header-like lines
                if not term or term.startswith("#"):
                    continue
                # If TSV, take the first column
                if "\t" in term:
                    term = term.split("\t")[0].strip()
                if term:
                    terms.add(term.lower())

    return terms
