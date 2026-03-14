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
        return download_dataset(_DATASET_NAME, _REPO_URL, branch="master", force=self._force_download)

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

            # Load corpus texts — structure: annotated/texts_tokenised/*.txt
            # or annotated/texts/*.txt
            texts_dir = domain_dir / "annotated" / "texts_tokenised"
            if not texts_dir.is_dir():
                texts_dir = domain_dir / "annotated" / "texts"
            if not texts_dir.is_dir():
                # Legacy fallback
                texts_dir = domain_dir / "texts" / "annotated"
            if not texts_dir.is_dir():
                texts_dir = domain_dir / "texts"

            if texts_dir.is_dir():
                for txt_path in sorted(texts_dir.glob("*.txt")):
                    content = txt_path.read_text(encoding="utf-8").strip()
                    if content:
                        doc_id = f"{domain}_{txt_path.stem}"
                        all_docs.append(Document(doc_id=doc_id, content=content))

            # Load gold terms from unique annotation lists
            # Structure: annotated/annotations/unique_annotation_lists/*_terms.tsv
            ann_dir = domain_dir / "annotated" / "annotations" / "unique_annotation_lists"
            if not ann_dir.is_dir():
                ann_dir = domain_dir / "annotations"
            if ann_dir.is_dir():
                terms = _load_acter_terms(ann_dir)
                all_terms.update(terms)

        self._documents = all_docs
        self._gold = all_terms


def _load_acter_terms(ann_dir: Path) -> set[str]:
    """Load unique gold terms from ACTER annotation list files.

    Reads ``*_terms.tsv`` files (without named entities variant).
    Format: ``term<TAB>category`` where category is Specific_Term,
    Common_Term, or OOD_Term. We include Specific_Term and Common_Term.
    """
    terms: set[str] = set()
    valid_categories = {"Specific_Term", "Common_Term"}

    # Find the terms TSV (without named entities)
    term_files = sorted(ann_dir.glob("*_terms.tsv"))
    # Prefer files without "nes" (named entities) in the name
    term_files = [f for f in term_files if "_nes" not in f.name] or term_files

    for path in term_files:
        try:
            text = path.read_text(encoding="utf-8")
        except (UnicodeDecodeError, OSError):
            continue
        for line in text.splitlines():
            line = line.strip()
            if not line or line.startswith("#"):
                continue
            parts = line.split("\t")
            term = parts[0].strip()
            category = parts[1].strip() if len(parts) > 1 else ""
            if term and (not category or category in valid_categories):
                terms.add(term.lower())

    return terms
