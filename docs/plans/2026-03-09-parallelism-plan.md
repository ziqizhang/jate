# Parallelism Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add configurable parallelism to JATE using ProcessPoolExecutor with map-reduce, targeting co-occurrence, containment, algorithm comparison, and NLP batch processing.

**Architecture:** A `JATEConfig` dataclass controls `max_workers`. A `parallel_map()` helper wraps the map-reduce pattern. Four pipeline stages are parallelized: NLP batching (spaCy `nlp.pipe()`), co-occurrence computation, containment index building, and multi-algorithm comparison. Default is sequential (`max_workers=1`).

**Tech Stack:** Python 3.11+, `concurrent.futures.ProcessPoolExecutor`, spaCy `nlp.pipe()`, Poetry, pytest

---

### Task 1: Create JATEConfig and parallel_map helper

**Files:**
- Create: `src/jate/config.py`
- Create: `src/jate/parallel.py`
- Create: `tests/test_parallel.py`
- Modify: `src/jate/__init__.py`

**Step 1: Write failing tests**

```python
# tests/test_parallel.py
"""Tests for JATEConfig and parallel_map."""
from __future__ import annotations

from jate.config import JATEConfig
from jate.parallel import parallel_map


class TestJATEConfig:
    def test_defaults(self) -> None:
        config = JATEConfig()
        assert config.max_workers == 1

    def test_custom_workers(self) -> None:
        config = JATEConfig(max_workers=4)
        assert config.max_workers == 4


def _double(x: int) -> int:
    return x * 2


class TestParallelMap:
    def test_sequential(self) -> None:
        result = parallel_map(_double, [1, 2, 3], max_workers=1)
        assert result == [2, 4, 6]

    def test_parallel(self) -> None:
        result = parallel_map(_double, [1, 2, 3], max_workers=2)
        assert result == [2, 4, 6]

    def test_empty_input(self) -> None:
        result = parallel_map(_double, [], max_workers=2)
        assert result == []

    def test_single_item(self) -> None:
        result = parallel_map(_double, [5], max_workers=2)
        assert result == [10]
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_parallel.py -v`
Expected: FAIL — modules don't exist

**Step 3: Implement config.py**

```python
# src/jate/config.py
"""Configuration for JATE pipeline."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class JATEConfig:
    """Pipeline configuration.

    Parameters
    ----------
    max_workers:
        Maximum number of worker processes for parallel computation.
        1 = sequential (no overhead). N > 1 = use ProcessPoolExecutor.
    """

    max_workers: int = 1
```

**Step 4: Implement parallel.py**

```python
# src/jate/parallel.py
"""Parallel map-reduce utilities."""

from __future__ import annotations

from concurrent.futures import ProcessPoolExecutor
from typing import Any, Callable


def parallel_map(
    fn: Callable[..., Any],
    chunks: list[Any],
    max_workers: int = 1,
) -> list[Any]:
    """Map *fn* over *chunks*, optionally in parallel.

    When *max_workers* <= 1, runs sequentially with no overhead.
    Otherwise uses ProcessPoolExecutor.
    """
    if not chunks:
        return []
    if max_workers <= 1:
        return [fn(chunk) for chunk in chunks]
    with ProcessPoolExecutor(max_workers=max_workers) as pool:
        return list(pool.map(fn, chunks))


def split_range(start: int, end: int, n_chunks: int) -> list[tuple[int, int]]:
    """Split index range [start, end) into n_chunks non-overlapping sub-ranges."""
    total = end - start
    if total <= 0 or n_chunks <= 0:
        return []
    chunk_size = max(1, total // n_chunks)
    ranges = []
    for i in range(0, total, chunk_size):
        chunk_start = start + i
        chunk_end = min(start + i + chunk_size, end)
        ranges.append((chunk_start, chunk_end))
    return ranges
```

**Step 5: Add to __init__.py**

Add `from jate.config import JATEConfig` and `"JATEConfig"` to `__all__`.

**Step 6: Run tests**

Run: `poetry run pytest tests/test_parallel.py -v`
Expected: ALL PASS

**Step 7: Commit**

```bash
git add src/jate/config.py src/jate/parallel.py tests/test_parallel.py src/jate/__init__.py
git commit -m "feat: add JATEConfig and parallel_map helper"
```

---

### Task 2: Parallel co-occurrence in MemoryCorpusStore

**Files:**
- Modify: `src/jate/store/memory_store.py:35-55`
- Modify: `tests/test_parallel.py`

**Step 1: Write failing test**

Add to `tests/test_parallel.py`:

```python
from jate.models import Candidate
from jate.store.memory_store import MemoryCorpusStore


def _make_indexed_store(max_workers: int = 1) -> MemoryCorpusStore:
    """Build a store with known candidates and index them."""
    candidates = []
    for term, doc_ids in [
        ("neural network", ["d1", "d2", "d3"]),
        ("machine learning", ["d1", "d2"]),
        ("deep learning", ["d2", "d3"]),
        ("network", ["d1", "d2", "d3", "d4"]),
    ]:
        c = Candidate(surface_form=term, normalized_form=term)
        for doc_id in doc_ids:
            c.add_position(doc_id, 0, len(term))
        candidates.append(c)
    store = MemoryCorpusStore()
    store.index_candidates(candidates, max_workers=max_workers)
    return store


class TestParallelCooccurrence:
    def test_sequential_and_parallel_match(self) -> None:
        store_seq = _make_indexed_store(max_workers=1)
        store_par = _make_indexed_store(max_workers=2)
        # Compare all co-occurrence values
        terms = ["neural network", "machine learning", "deep learning", "network"]
        for i, a in enumerate(terms):
            for b in terms[i + 1:]:
                assert store_seq.get_cooccurrences(a, b) == store_par.get_cooccurrences(a, b), \
                    f"Mismatch for ({a}, {b})"

    def test_known_cooccurrence_values(self) -> None:
        store = _make_indexed_store(max_workers=2)
        # "neural network" in d1,d2,d3 and "machine learning" in d1,d2 -> shared=2
        assert store.get_cooccurrences("neural network", "machine learning") == 2
        # "neural network" in d1,d2,d3 and "deep learning" in d2,d3 -> shared=2
        assert store.get_cooccurrences("neural network", "deep learning") == 2
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_parallel.py::TestParallelCooccurrence -v`
Expected: FAIL — `index_candidates()` doesn't accept `max_workers`

**Step 3: Update MemoryCorpusStore.index_candidates()**

In `src/jate/store/memory_store.py`:

```python
"""In-memory corpus statistics store."""

from __future__ import annotations

from jate.models import Candidate, Document
from jate.parallel import parallel_map, split_range


def _compute_cooc_chunk(args: tuple) -> dict[tuple[str, str], int]:
    """Compute co-occurrences for a slice of term pair indices."""
    terms, term_docs, start, end = args
    partial: dict[tuple[str, str], int] = {}
    for i in range(start, end):
        t_a = terms[i]
        docs_a = term_docs[t_a]
        for t_b in terms[i + 1 :]:
            shared = len(docs_a & term_docs[t_b])
            if shared > 0:
                partial[(t_a, t_b)] = shared
    return partial


class MemoryCorpusStore:
    # ... existing __init__, add_document unchanged ...

    def index_candidates(self, candidates: list[Candidate], max_workers: int = 1) -> None:
        """Bulk-index candidate frequencies.

        Also computes pairwise co-occurrence counts.
        """
        term_docs: dict[str, set[str]] = {}

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                freq = len(positions)
                self._term_freq.setdefault(term, {})[doc_id] = freq
                term_docs.setdefault(term, set()).add(doc_id)

        # Compute co-occurrences (parallel if max_workers > 1)
        terms = sorted(term_docs.keys())
        if len(terms) < 2:
            return

        if max_workers <= 1:
            for i, t_a in enumerate(terms):
                for t_b in terms[i + 1 :]:
                    shared = len(term_docs[t_a] & term_docs[t_b])
                    if shared > 0:
                        self._cooccurrences[(t_a, t_b)] = shared
        else:
            chunks = [
                (terms, term_docs, start, end)
                for start, end in split_range(0, len(terms), max_workers)
            ]
            partial_dicts = parallel_map(_compute_cooc_chunk, chunks, max_workers)
            for partial in partial_dicts:
                self._cooccurrences.update(partial)

    # ... rest unchanged ...
```

**Step 4: Run tests**

Run: `poetry run pytest tests/test_parallel.py tests/test_store.py -v`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/jate/store/memory_store.py tests/test_parallel.py
git commit -m "feat: parallel co-occurrence computation in MemoryCorpusStore"
```

---

### Task 3: Shared containment index builder

**Files:**
- Create: `src/jate/features.py`
- Modify: `tests/test_parallel.py`

**Step 1: Write failing tests**

Add to `tests/test_parallel.py`:

```python
from jate.features import build_containment_index


def _containment_candidates() -> list[Candidate]:
    return [
        Candidate(surface_form="neural network", normalized_form="neural network"),
        Candidate(surface_form="deep neural network", normalized_form="deep neural network"),
        Candidate(surface_form="machine learning", normalized_form="machine learning"),
        Candidate(surface_form="network", normalized_form="network"),
    ]


class TestParallelContainment:
    def test_sequential_and_parallel_match(self) -> None:
        cands = _containment_candidates()
        parents_seq = build_containment_index(cands, max_workers=1)
        parents_par = build_containment_index(cands, max_workers=2)
        assert parents_seq == parents_par

    def test_known_containment(self) -> None:
        cands = _containment_candidates()
        parents = build_containment_index(cands, max_workers=1)
        # "neural network" is contained in "deep neural network"
        assert "deep neural network" in parents["neural network"]
        # "network" is contained in "neural network" and "deep neural network"
        assert "neural network" in parents["network"]
        assert "deep neural network" in parents["network"]
        # "deep neural network" has no parents
        assert parents["deep neural network"] == []

    def test_children_index(self) -> None:
        from jate.features import build_child_containment_index
        cands = _containment_candidates()
        children = build_child_containment_index(cands, max_workers=1)
        # "deep neural network" contains "neural network" and "network"
        assert "neural network" in children["deep neural network"]
        assert "network" in children["deep neural network"]
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_parallel.py::TestParallelContainment -v`
Expected: FAIL — module doesn't exist

**Step 3: Implement features.py**

```python
# src/jate/features.py
"""Shared feature builders for term extraction algorithms."""

from __future__ import annotations

from jate.models import Candidate
from jate.parallel import parallel_map, split_range


def _containment_chunk(args: tuple) -> dict[str, list[str]]:
    """Find parent terms for a slice of candidates."""
    all_candidates, start, end = args
    partial: dict[str, list[str]] = {}
    for i in range(start, end):
        c = all_candidates[i]
        c_nf = c.normalized_form
        c_words = len(c_nf.split())
        parents: list[str] = []
        for other in all_candidates:
            o_nf = other.normalized_form
            if o_nf == c_nf:
                continue
            if len(o_nf.split()) > c_words and f" {c_nf} " in f" {o_nf} ":
                parents.append(o_nf)
        partial[c_nf] = parents
    return partial


def build_containment_index(
    candidates: list[Candidate],
    max_workers: int = 1,
) -> dict[str, list[str]]:
    """Build parent containment index: term -> [longer terms containing it].

    Each candidate is checked against all others for whole-word containment.
    """
    if not candidates:
        return {}

    if max_workers <= 1:
        return _containment_chunk((candidates, 0, len(candidates)))

    chunks = [
        (candidates, start, end)
        for start, end in split_range(0, len(candidates), max_workers)
    ]
    partial_dicts = parallel_map(_containment_chunk, chunks, max_workers)
    merged: dict[str, list[str]] = {}
    for partial in partial_dicts:
        merged.update(partial)
    return merged


def _child_containment_chunk(args: tuple) -> dict[str, list[str]]:
    """Find child terms for a slice of candidates."""
    all_candidates, start, end = args
    partial: dict[str, list[str]] = {}
    for i in range(start, end):
        c = all_candidates[i]
        c_nf = c.normalized_form
        c_words = len(c_nf.split())
        children: list[str] = []
        for other in all_candidates:
            o_nf = other.normalized_form
            if o_nf == c_nf:
                continue
            if len(o_nf.split()) < c_words and f" {o_nf} " in f" {c_nf} ":
                children.append(o_nf)
        partial[c_nf] = children
    return partial


def build_child_containment_index(
    candidates: list[Candidate],
    max_workers: int = 1,
) -> dict[str, list[str]]:
    """Build child containment index: term -> [shorter terms contained in it]."""
    if not candidates:
        return {}

    if max_workers <= 1:
        return _child_containment_chunk((candidates, 0, len(candidates)))

    chunks = [
        (candidates, start, end)
        for start, end in split_range(0, len(candidates), max_workers)
    ]
    partial_dicts = parallel_map(_child_containment_chunk, chunks, max_workers)
    merged: dict[str, list[str]] = {}
    for partial in partial_dicts:
        merged.update(partial)
    return merged
```

**Step 4: Run tests**

Run: `poetry run pytest tests/test_parallel.py::TestParallelContainment -v`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/jate/features.py tests/test_parallel.py
git commit -m "feat: shared containment index builder with parallel support"
```

---

### Task 4: Refactor algorithms to accept pre-built containment

**Files:**
- Modify: `src/jate/algorithms/basic.py`
- Modify: `src/jate/algorithms/combo_basic.py`
- Modify: `src/jate/algorithms/cvalue.py`
- Test: `tests/test_algorithms.py`

**Step 1: Update Basic to accept containment parameter**

In `src/jate/algorithms/basic.py`, change `score()` to accept optional pre-built containment:

```python
def score(
    self,
    candidates: list[Candidate],
    corpus_store: CorpusStore,
    context_index: ContextIndex | None = None,
    containment: dict[str, list[str]] | None = None,
) -> TermExtractionResult:
    # Use provided containment or build locally
    if containment is None:
        containment = {}
        for c in candidates:
            parents: list[str] = []
            for other in candidates:
                if other.normalized_form == c.normalized_form:
                    continue
                if (
                    len(other.normalized_form.split()) > len(c.normalized_form.split())
                    and f" {c.normalized_form} " in f" {other.normalized_form} "
                ):
                    parents.append(other.normalized_form)
            containment[c.normalized_form] = parents

    # ... rest of scoring unchanged, uses containment dict ...
```

**Step 2: Same for CValue**

Same pattern: add `containment: dict[str, list[str]] | None = None` parameter. Use it if provided, else build locally.

**Step 3: Same for ComboBasic**

ComboBasic needs both parent and child containment:

```python
def score(
    self,
    candidates: list[Candidate],
    corpus_store: CorpusStore,
    context_index: ContextIndex | None = None,
    containment: dict[str, list[str]] | None = None,
    child_containment: dict[str, list[str]] | None = None,
) -> TermExtractionResult:
```

**Step 4: Also update base class to allow **kwargs pass-through**

In `src/jate/algorithms/base.py`, the abstract `score()` method signature stays as-is. The containment parameters are algorithm-specific, not on the base class. Callers that pass containment use `**kwargs` or call algorithms directly.

**Step 5: Run existing tests**

Run: `poetry run pytest tests/test_algorithms.py -v`
Expected: ALL PASS — containment defaults to None, existing behavior preserved

**Step 6: Commit**

```bash
git add src/jate/algorithms/basic.py src/jate/algorithms/combo_basic.py src/jate/algorithms/cvalue.py
git commit -m "feat: algorithms accept pre-built containment index"
```

---

### Task 5: Parallel algorithm comparison in compare()

**Files:**
- Modify: `src/jate/api.py:394-449`
- Modify: `tests/test_parallel.py`

**Step 1: Write failing test**

Add to `tests/test_parallel.py`:

```python
import math
from jate.api import compare
from jate.config import JATEConfig


class TestParallelCompare:
    def test_sequential_and_parallel_match(self) -> None:
        text = "Machine learning and neural networks are used in deep learning research. Neural network architectures improve machine learning models."
        results_seq = compare(
            [text],
            algorithms=["tfidf", "cvalue", "basic", "ttf"],
            config=JATEConfig(max_workers=1),
            min_frequency=1,
        )
        results_par = compare(
            [text],
            algorithms=["tfidf", "cvalue", "basic", "ttf"],
            config=JATEConfig(max_workers=2),
            min_frequency=1,
        )
        assert set(results_seq.keys()) == set(results_par.keys())
        for algo_name in results_seq:
            scores_seq = {t.string: t.score for t in results_seq[algo_name]}
            scores_par = {t.string: t.score for t in results_par[algo_name]}
            assert scores_seq.keys() == scores_par.keys(), f"{algo_name} term sets differ"
            for term in scores_seq:
                assert abs(scores_seq[term] - scores_par[term]) < 1e-9, \
                    f"{algo_name}/{term}: {scores_seq[term]} != {scores_par[term]}"
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_parallel.py::TestParallelCompare -v`
Expected: FAIL — `compare()` doesn't accept `config`

**Step 3: Update API functions**

In `src/jate/api.py`, add `config` parameter to `extract()`, `extract_corpus()`, and `compare()`:

```python
from jate.config import JATEConfig
from jate.features import build_containment_index, build_child_containment_index
from jate.parallel import parallel_map


def extract(
    text: str,
    *,
    algorithm: str = "cvalue",
    model: str = "en_core_web_sm",
    extractor: str = "pos_pattern",
    config: JATEConfig | None = None,
    min_frequency: int = 1,
    min_words: int = 1,
    max_words: int | None = None,
    **algo_kwargs: Any,
) -> TermExtractionResult:
    if config is None:
        config = JATEConfig()
    # ... rest similar, pass config.max_workers to store.index_candidates() ...
```

For `compare()`, the key change — parallelize the algorithm scoring loop:

```python
def _score_and_filter(args):
    """Score with one algorithm and apply filters. Top-level for pickling."""
    algo, candidates, store, context_index, containment, child_containment, min_freq, min_words, max_words = args
    # Pass containment if algorithm supports it
    kwargs = {}
    if hasattr(algo, 'score') and 'containment' in algo.score.__code__.co_varnames:
        kwargs['containment'] = containment
        if 'child_containment' in algo.score.__code__.co_varnames:
            kwargs['child_containment'] = child_containment
    result = algo.score(candidates, store, context_index=context_index, **kwargs)
    result = result.filter_by_frequency(min_freq)
    result = result.filter_by_length(min_words=min_words, max_words=max_words)
    for i, term in enumerate(result):
        term.rank = i + 1
    return algo.name.lower(), result


def compare(
    sources: list[str] | str,
    *,
    algorithms: list[str] | None = None,
    model: str = "en_core_web_sm",
    extractor: str = "pos_pattern",
    config: JATEConfig | None = None,
    min_frequency: int = 1,
    min_words: int = 1,
    max_words: int | None = None,
    **kwargs: Any,
) -> dict[str, TermExtractionResult]:
    if config is None:
        config = JATEConfig()
    if algorithms is None:
        algorithms = ["tfidf", "cvalue", "rake", "ridf", "basic"]

    nlp = SpacyBackend(model)
    documents = _load_sources(sources)
    store = MemoryCorpusStore()

    ext = _resolve_extractor(extractor)
    candidates = ext.extract(documents, nlp, store)
    store.index_candidates(candidates, max_workers=config.max_workers)

    context_index = _build_context_index(candidates, documents, nlp)

    # Pre-build containment once for all algorithms
    containment = build_containment_index(candidates, max_workers=config.max_workers)
    child_containment = build_child_containment_index(candidates, max_workers=config.max_workers)

    # Build algorithm instances
    algos = [
        _resolve_algorithm(algo_name, candidates=candidates, store=store, **kwargs)
        for algo_name in algorithms
    ]

    # Score (parallel if max_workers > 1)
    chunks = [
        (algo, candidates, store, context_index, containment, child_containment,
         min_frequency, min_words, max_words)
        for algo in algos
    ]
    partial_results = parallel_map(_score_and_filter, chunks, config.max_workers)

    return {name: result for name, result in partial_results}
```

Also update `extract()` and `extract_corpus()` to pass `config.max_workers` to `store.index_candidates()` and pre-build containment for containment-based algorithms.

**Step 4: Run tests**

Run: `poetry run pytest tests/test_parallel.py tests/test_algorithms.py -v`
Expected: ALL PASS

**Step 5: Run full suite**

Run: `poetry run pytest tests/ -v`
Expected: ALL PASS

**Step 6: Commit**

```bash
git add src/jate/api.py tests/test_parallel.py
git commit -m "feat: parallel algorithm comparison and containment in API"
```

---

### Task 6: Batch NLP processing with nlp.pipe()

**Files:**
- Modify: `src/jate/nlp/spacy_backend.py`
- Modify: `tests/test_parallel.py`

**Step 1: Write failing test**

Add to `tests/test_parallel.py`:

```python
from jate.nlp.spacy_backend import SpacyBackend


class TestBatchNLP:
    def test_process_batch(self) -> None:
        nlp = SpacyBackend("en_core_web_sm")
        texts = [
            "Machine learning is great.",
            "Neural networks are powerful.",
        ]
        docs = nlp.process_batch(texts)
        assert len(docs) == 2
        # Each doc should have tokens
        assert len(docs[0]) > 0
        assert len(docs[1]) > 0

    def test_process_batch_empty(self) -> None:
        nlp = SpacyBackend("en_core_web_sm")
        docs = nlp.process_batch([])
        assert docs == []
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_parallel.py::TestBatchNLP -v`
Expected: FAIL — `process_batch` doesn't exist

**Step 3: Add process_batch to SpacyBackend**

In `src/jate/nlp/spacy_backend.py`:

```python
def process_batch(self, texts: list[str], batch_size: int = 256) -> list[Doc]:
    """Process multiple texts efficiently using spaCy's nlp.pipe().

    Uses spaCy's built-in batching which releases the GIL during
    C-level parsing, enabling multi-threaded speedup.
    """
    if not texts:
        return []
    return list(self._nlp.pipe(texts, batch_size=batch_size))
```

**Step 4: Run tests**

Run: `poetry run pytest tests/test_parallel.py::TestBatchNLP -v`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/jate/nlp/spacy_backend.py tests/test_parallel.py
git commit -m "feat: add SpacyBackend.process_batch() using nlp.pipe()"
```

---

### Task 7: Refactor extractors to use pre-processed spaCy Docs

**Files:**
- Modify: `src/jate/extractors/pos_pattern.py`
- Modify: `src/jate/extractors/ngram.py`
- Modify: `src/jate/extractors/noun_phrase.py`
- Modify: `src/jate/extractors/base.py`
- Test: existing extractor tests

**Step 1: Add extract_from_docs() method to extractors**

Each extractor gets a new method that accepts pre-processed spaCy `Doc` objects instead of calling the NLP backend per-sentence. The existing `extract()` method calls `extract_from_docs()` internally after batch-processing.

In `src/jate/extractors/base.py`, add optional method:

```python
from spacy.tokens import Doc

class CandidateExtractorBase(ABC):
    @abstractmethod
    def extract(self, documents, nlp_backend, corpus_store):
        ...

    def extract_from_docs(
        self,
        documents: list[Document],
        spacy_docs: list[Doc],
        corpus_store: CorpusStore,
    ) -> list[Candidate]:
        """Extract from pre-processed spaCy Docs. Override in subclasses."""
        raise NotImplementedError
```

**Step 2: Update PosPatternExtractor**

Refactor so the core logic works from a spaCy `Doc` rather than calling `nlp_backend.pos_tag()` etc. The `extract()` method batch-processes all documents, then calls the shared extraction logic.

```python
def extract(self, documents, nlp_backend, corpus_store):
    # Batch process all documents
    texts = [doc.content for doc in documents if doc.content.strip()]
    valid_docs = [doc for doc in documents if doc.content.strip()]
    spacy_docs = nlp_backend.process_batch(texts) if texts else []
    return self.extract_from_docs(valid_docs, spacy_docs, corpus_store)

def extract_from_docs(self, documents, spacy_docs, corpus_store):
    merged = {}
    for doc, spacy_doc in zip(documents, spacy_docs):
        corpus_store.add_document(doc)
        # Use spacy_doc.sents for sentence splitting
        # Use token.pos_, token.text, token.lemma_ from spaCy tokens
        # Same extraction logic, but from spaCy Doc objects instead of NLP backend calls
        ...
    return list(merged.values())
```

The key change: instead of calling `nlp_backend.sentence_split(text)` then `nlp_backend.pos_tag(sent_text)` for each sentence (which processes text through spaCy multiple times), we use the already-processed `spacy_doc.sents` iterator and access token attributes directly.

**Step 3: Same for NGramExtractor and NounPhraseExtractor**

Same pattern — batch process upfront, extract from Doc objects.

**Step 4: Run all extractor tests**

Run: `poetry run pytest tests/test_extractors.py tests/test_extractors_sentence.py -v`
Expected: ALL PASS — same behavior, faster path

**Step 5: Run full test suite**

Run: `poetry run pytest tests/ -v`
Expected: ALL PASS

**Step 6: Commit**

```bash
git add src/jate/extractors/
git commit -m "feat: extractors use batch NLP processing via spaCy Docs"
```

---

### Task 8: Wire config through full pipeline and final verification

**Files:**
- Modify: `src/jate/api.py` (if not already done in Task 5)
- Test: full test suite

**Step 1: Ensure config flows through entire pipeline**

Verify that `extract()`, `extract_corpus()`, and `compare()` all:
- Accept `config: JATEConfig | None = None`
- Pass `config.max_workers` to `store.index_candidates()`
- Pass `config.max_workers` to `build_containment_index()`
- Pass `config.max_workers` to `parallel_map()` for algorithm comparison

**Step 2: Run full test suite**

Run: `poetry run pytest tests/ -v --tb=long`
Expected: ALL PASS

**Step 3: Run linting**

Run: `poetry run black --check src/ tests/ && poetry run isort --check src/ tests/`
Expected: No issues (or fix them)

**Step 4: Verify backwards compatibility**

Confirm:
- All API functions work without `config` parameter (defaults to sequential)
- `MemoryCorpusStore.index_candidates()` works without `max_workers` parameter
- All algorithms work without `containment` parameter

**Step 5: Commit**

```bash
git add -A
git commit -m "feat: wire JATEConfig through full pipeline"
```
