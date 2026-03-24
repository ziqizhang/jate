"""Core data models for JATE term extraction."""

from __future__ import annotations

import csv
import io
import json
from dataclasses import dataclass, field
from typing import Any, Iterator

import pandas as pd  # type: ignore[import-untyped]


@dataclass(slots=True)
class TermSpan:
    """A specific occurrence of a term in a document."""

    doc_id: str
    start: int
    end: int
    sentence_idx: int = -1


@dataclass(slots=True)
class Term:
    """A scored term extracted from a corpus.

    ``string`` is the canonical (normalised / lemmatised) form used for
    scoring and evaluation.  ``surface_forms`` maps back to every
    surface variant observed in the corpus so downstream consumers can
    display or highlight original text.
    """

    string: str
    score: float = 0.0
    frequency: int = 0
    rank: int = 0
    metadata: dict[str, Any] = field(default_factory=dict)
    surface_forms: set[str] = field(default_factory=set)
    spans: list[TermSpan] = field(default_factory=list)
    label: str | None = None

    # Natural ordering: higher score first, then alphabetical.
    def __lt__(self, other: object) -> bool:
        if not isinstance(other, Term):
            return NotImplemented
        if self.score != other.score:
            return self.score > other.score  # higher score = "less" for sorting
        return self.string < other.string

    def __repr__(self) -> str:
        return f"Term({self.string!r}, score={self.score})"


@dataclass(slots=True)
class Candidate:
    """A candidate term before scoring.

    Captures linguistic information gathered during candidate extraction:
    surface form, normalised (lemmatised) form, POS pattern, and the
    positions where the candidate was found across documents.

    ``normalized_form`` is the canonical (lemmatised, lowercased) key.
    ``surface_forms`` collects every distinct surface variant observed.
    """

    surface_form: str
    normalized_form: str = ""
    pos_pattern: str = ""
    # doc_id -> list of (start, end, sentence_idx) character offsets
    doc_positions: dict[str, list[tuple[int, int, int]]] = field(default_factory=dict)
    total_frequency: int = 0
    surface_forms: set[str] = field(default_factory=set)

    def __post_init__(self) -> None:
        if not self.normalized_form:
            self.normalized_form = self.surface_form.lower()
        # Ensure the initial surface_form is always in the set.
        self.surface_forms.add(self.surface_form)

    def add_position(self, doc_id: str, start: int, end: int, sentence_idx: int = -1, surface: str = "") -> None:
        """Record a new occurrence of this candidate in a document."""
        self.doc_positions.setdefault(doc_id, []).append((start, end, sentence_idx))
        self.total_frequency += 1
        if surface:
            self.surface_forms.add(surface)

    @property
    def document_frequency(self) -> int:
        """Number of distinct documents containing this candidate."""
        return len(self.doc_positions)


@dataclass(slots=True)
class Document:
    """A document in the corpus."""

    doc_id: str
    content: str = ""
    metadata: dict[str, str] = field(default_factory=dict)

    def __repr__(self) -> str:
        return f"Document(doc_id={self.doc_id!r})"


class TermExtractionResult:
    """Container for scored terms with sorting, filtering, and export helpers.

    Supports iteration, ``len()``, and indexing.
    """

    __slots__ = ("_terms",)

    def __init__(self, terms: list[Term] | None = None) -> None:
        self._terms: list[Term] = list(terms) if terms else []

    # --- Collection protocol ---------------------------------------------------

    def __len__(self) -> int:
        return len(self._terms)

    def __iter__(self) -> Iterator[Term]:
        return iter(self._terms)

    def __getitem__(self, index: int) -> Term:
        return self._terms[index]

    def __repr__(self) -> str:
        return f"TermExtractionResult({len(self._terms)} terms)"

    @property
    def terms(self) -> list[Term]:
        return self._terms

    # --- Mutators --------------------------------------------------------------

    def add(self, term: Term) -> None:
        """Append a term to the result set."""
        self._terms.append(term)

    def sort(self, *, by: str = "score", ascending: bool = False) -> TermExtractionResult:
        """Return a *new* result sorted by the given attribute.

        Parameters
        ----------
        by:
            One of ``"score"``, ``"frequency"``, ``"string"``, ``"rank"``.
        ascending:
            Sort direction.  Default is descending (highest first).
        """
        reverse = not ascending
        sorted_terms = sorted(self._terms, key=lambda t: getattr(t, by), reverse=reverse)
        return TermExtractionResult(sorted_terms)

    # --- Filters ---------------------------------------------------------------

    def filter_by_score(self, min_score: float) -> TermExtractionResult:
        """Return terms with score >= *min_score*."""
        return TermExtractionResult([t for t in self._terms if t.score >= min_score])

    def filter_by_frequency(self, min_frequency: int) -> TermExtractionResult:
        """Return terms with frequency >= *min_frequency*."""
        return TermExtractionResult([t for t in self._terms if t.frequency >= min_frequency])

    def filter_by_length(self, min_words: int = 1, max_words: int | None = None) -> TermExtractionResult:
        """Return terms whose word count is within [*min_words*, *max_words*]."""
        filtered: list[Term] = []
        for t in self._terms:
            n = len(t.string.split())
            if n >= min_words and (max_words is None or n <= max_words):
                filtered.append(t)
        return TermExtractionResult(filtered)

    # --- Export ----------------------------------------------------------------

    def to_dataframe(self) -> pd.DataFrame:
        """Export to a :class:`pandas.DataFrame`."""
        rows = [
            {
                "term": t.string,
                "score": t.score,
                "frequency": t.frequency,
                "rank": t.rank,
                "surface_forms": ", ".join(sorted(t.surface_forms)) if t.surface_forms else "",
            }
            for t in self._terms
        ]
        return pd.DataFrame(rows)

    def to_csv(self) -> str:
        """Export as a CSV string."""
        buf = io.StringIO()
        writer = csv.DictWriter(buf, fieldnames=["term", "score", "frequency", "rank"])
        writer.writeheader()
        for t in self._terms:
            writer.writerow({"term": t.string, "score": t.score, "frequency": t.frequency, "rank": t.rank})
        return buf.getvalue()

    def to_json(self) -> str:
        """Export as a JSON string (list of objects)."""
        data = [
            {
                "term": t.string,
                "score": t.score,
                "frequency": t.frequency,
                "rank": t.rank,
                "surface_forms": sorted(t.surface_forms) if t.surface_forms else [],
                "metadata": t.metadata,
            }
            for t in self._terms
        ]
        return json.dumps(data, ensure_ascii=False, indent=2)
