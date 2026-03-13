"""CoastTerm dataset loader.

Downloads the CoastTerm corpus from GitHub and provides documents
and gold terms for coastal area terminology extraction.

GitHub repo: ``jdelaunay/coastal_area_term_extraction``
"""

from __future__ import annotations

from pathlib import Path

from jate.datasets.downloader import download_dataset
from jate.models import Document

_REPO_URL = "https://github.com/jdelaunay/coastal_area_term_extraction"
_DATASET_NAME = "coastterm"


class CoastTerm:
    """Loader for the CoastTerm dataset.

    Parameters
    ----------
    directory:
        Override the dataset directory (skip download).
    force_download:
        Force re-download even if cached.
    """

    def __init__(
        self,
        directory: Path | str | None = None,
        *,
        force_download: bool = False,
    ) -> None:
        self._dir = Path(directory) if directory else None
        self._force_download = force_download
        self._documents: list[Document] | None = None
        self._gold: set[str] | None = None

    @property
    def name(self) -> str:
        return "coastterm"

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

        # Load corpus documents from data/ directory
        data_subdir = data_dir / "data"
        if not data_subdir.is_dir():
            # Fallback: look for txt files in root
            data_subdir = data_dir

        txt_files = sorted(data_subdir.glob("*.txt"))
        if not txt_files:
            msg = f"No .txt corpus files found in {data_subdir}"
            raise FileNotFoundError(msg)

        documents: list[Document] = []
        for txt_path in txt_files:
            content = txt_path.read_text(encoding="utf-8").strip()
            if content:
                documents.append(Document(doc_id=txt_path.stem, content=content))

        # Load gold terms from en_terms.tsv
        gold_terms: set[str] = set()
        terms_file = data_dir / "en_terms.tsv"
        if not terms_file.exists():
            # Search for it
            candidates = list(data_dir.rglob("en_terms*"))
            if candidates:
                terms_file = candidates[0]
            else:
                # Try any tsv file with "term" in the name
                candidates = list(data_dir.rglob("*term*.*"))
                if candidates:
                    terms_file = candidates[0]

        if terms_file.exists():
            gold_terms = _load_terms_file(terms_file)
        else:
            msg = f"No gold terms file found in {data_dir}. Expected en_terms.tsv"
            raise FileNotFoundError(msg)

        self._documents = documents
        self._gold = gold_terms


def _load_terms_file(path: Path) -> set[str]:
    """Load terms from a TSV or plain text file (one term per line)."""
    terms: set[str] = set()
    text = path.read_text(encoding="utf-8")
    for line in text.splitlines():
        line = line.strip()
        if not line or line.startswith("#"):
            continue
        # If TSV, take the first column
        if "\t" in line:
            term = line.split("\t")[0].strip()
        else:
            term = line
        if term:
            terms.add(term.lower())
    return terms
