# Architecture Reference (Tier 2)

> For agents needing deeper context than AGENTS.md provides.
> Auto-loaded: No. Pull in when working on cross-module changes, new algorithms, or architectural decisions.

## System Overview

JATE is a self-contained Python library for automatic term extraction (ATE). It takes raw text, extracts candidate terms using linguistic patterns, scores them with statistical algorithms, and returns ranked results. No external services required — only spaCy for NLP.

## Pipeline Flow

Every extraction follows this pipeline, orchestrated by `api.py`:

```
Text → DocumentLoader → Documents
                          ↓
                     Extractor (POS pattern / n-gram / noun phrase)
                          ↓
                     Candidates + CorpusStore (frequency bookkeeping)
                          ↓
                     FeatureBuilder (TF, DF, co-occurrence, context, etc.)
                          ↓
                     Algorithm.score(candidates, features)
                          ↓
                     TermExtractionResult (scored, ranked, filterable)
```

Entry points:
- **Single text**: `api.py::extract()` — loads one document, runs full pipeline
- **Corpus**: `api.py::extract_corpus()` — loads directory/file list, shared store across docs
- **CLI**: `cli.py::main()` — wraps `extract()` / `extract_corpus()` with argparse
- **REST**: `server.py::extract_terms()` — wraps `extract()` via FastAPI

## Module Responsibilities

### `algorithms/` — Pure Scorers

Algorithms follow a two-tier abstraction:

- **`ATERanker`** (corpus-level): subclass and implement `_score(candidates, term_freq, **kwargs)`. All 13 current algorithms are rankers. Each declares `output_capabilities()` and optionally overrides `doc_level_compatibility()` to warn or raise when used on single documents (e.g., TF-IDF raises because IDF=0 on one document).
- **`ATETagger`** (document-level, future): subclass and implement `tag(doc)`. For transformer-based / NER-style methods that process one document at a time.

Both inherit from `Algorithm` (in `base.py`). Structural tests enforce that every algorithm in `algorithms/` subclasses one of these two.

Algorithms receive pre-computed feature objects via kwargs. They must NOT:
- Perform I/O (no file access, no network)
- Call NLP backends (no spaCy)
- Import from `extractors/`, `nlp/`, or `store/`

| Algorithm | Key Features Used | Reference |
|-----------|-------------------|-----------|
| `CValue` | containment, term_component_index | Frantzi et al. (2000) |
| `NCValue` | context_freq + CValue scores | Frantzi et al. (2000) |
| `TFIDF` | term_freq | Salton & Buckley (1988) |
| `RAKE` | word_freq, cooccurrence | Rose et al. (2010) |
| `ChiSquare` | chi_square_terms | Matsuo & Ishizuka (2004) |
| `TermEx` | term_freq, reference_freq | Sclano & Velardi (2007) |
| `GlossEx` | term_freq, reference_freq | Park et al. (2002) |
| `Weirdness` | term_freq, reference_freq | Ahmad et al. (1999) |
| `RIDF` | term_freq | Church & Gale (1995) |
| `Basic` | term_freq | Frequency-based baseline |
| `ComboBasic` | term_freq, containment | Basic + sub-term boosting |
| `TTF` | term_freq | Raw total frequency |
| `ATTF` | term_freq | Average TTF per document |
| `Voting` | runs multiple algorithms | Ensemble method |

### `extractors/` — Candidate Extraction

Each extractor subclasses `CandidateExtractorBase` and implements `extract(documents, nlp_backend, corpus_store)`.

| Extractor | Strategy |
|-----------|----------|
| `PosPatternExtractor` | Regex over POS tag sequences (e.g., ADJ* NOUN+) |
| `NGramExtractor` | Sliding window n-grams with stopword filtering |
| `NounPhraseExtractor` | spaCy noun chunk extraction |

Extractors depend on `nlp/` (for POS tagging) and `models.py` (for `Candidate`, `Document`). They must NOT import from `algorithms/` or `store/` implementations.

### `nlp/` — NLP Backends

- `spacy_backend.py`: The production backend. Implements the `NLPBackend` protocol.
- `document_loader.py`: Converts raw text/files into `Document` objects.

`nlp/` must not import any domain modules (algorithms, extractors, features, store).

### `store/` — Corpus Storage

- `memory_store.py`: In-memory dict-based store. Default for single-document extraction.
- `sqlite_store.py`: SQLite-backed store for persistent corpus statistics.

Both implement the `CorpusStore` protocol. `store/` must not import domain modules.

### `features.py` — Feature Computation

Builds statistical features from candidates and documents. Each feature is a dataclass with a `build()` classmethod:

| Feature | What it computes |
|---------|-----------------|
| `TermFrequency` | Per-term and per-document frequency counts |
| `WordFrequency` | Individual word frequencies (for RAKE) |
| `Cooccurrence` | Word co-occurrence matrix (for RAKE) |
| `Containment` | Which terms contain/are contained by other terms (for CValue) |
| `TermComponentIndex` | Sub-term decomposition index (for CValue) |
| `ContextFrequency` | Context word frequencies around terms (for NCValue) |
| `ReferenceFrequency` | Reference corpus frequencies (for Weirdness, TermEx, etc.) |
| `ChiSquareFrequentTerms` | Frequent term set for chi-square test |

Features are built lazily in `api.py::_build_features()` — only those needed by the chosen algorithm.

### `protocols.py` — Structural Typing

Defines `Protocol` interfaces: `NLPBackend`, `CandidateExtractor`, `CorpusStore`. Any object matching the method signatures is accepted — no inheritance required. This enables testing with mocks and future backend swaps.

### `models.py` — Core Data Models

All dataclasses use `slots=True` for memory efficiency:
- `Document`: Input document (doc_id, content, metadata)
- `Candidate`: Pre-scoring candidate (surface form, normalized form, POS pattern, positions)
- `Term`: Scored result (string, score, frequency, rank, surface_forms, metadata)
- `TermExtractionResult`: Collection with sort/filter/export helpers (CSV, JSON, DataFrame)

### Other Modules

- `config.py`: `JATEConfig` dataclass — controls parallelism settings
- `context.py`: Context window extraction for NCValue algorithm
- `corpus.py`: High-level `Corpus` class wrapping a store with convenience methods
- `evaluation.py`: Precision/recall/F1 against gold standard term lists
- `benchmark.py`: Harness for running algorithms against built-in datasets
- `parallel.py`: `parallel_map()` utility using `ProcessPoolExecutor`
- `datasets/`: Built-in datasets (ACL RD-TEC) with download/parse helpers
- `server.py`: FastAPI REST API with model caching and health endpoints

## Architectural Rules

1. **algorithms/ are pure scorers**: No I/O, no NLP, no extraction. They receive features and return scores.
   - Enforced by: `tests/test_architecture.py::TestAlgorithmPurity`

2. **extractors/ don't know about scoring**: Extractors produce candidates. They must not import `algorithms/`.
   - Enforced by: `tests/test_architecture.py::TestExtractorIndependence`

3. **store/ and nlp/ are infrastructure**: They must not import domain modules (algorithms, extractors, features).
   - Enforced by: `tests/test_architecture.py::TestInfraIndependence`

4. **api.py is the sole orchestrator**: All pipeline wiring (choosing extractor, building features, running algorithm) happens here. Algorithms and extractors don't know about each other.

5. **Protocols over inheritance**: Backends are defined as `Protocol` classes. New backends need only satisfy the method signatures.

6. **Features are built lazily**: `api.py::_build_features()` inspects the algorithm and only computes required features. Adding a new algorithm that needs a new feature type requires updating `_NEEDS_*` tuples in `api.py`.

## Adding a New Algorithm

1. Create `src/jate/algorithms/my_algo.py`, subclass `Algorithm`
2. Implement `score(candidates, term_freq, **kwargs)` — receive features via kwargs
3. Export from `src/jate/algorithms/__init__.py`
4. Register in `api.py::_ALGORITHM_NAMES` dict
5. If new features needed: add to `_NEEDS_*` tuples in `api.py`, implement feature class in `features.py`
6. Add tests in `tests/test_algorithms.py`
7. Document in `docs/guide/algorithms.md`

## Adding a New Extractor

1. Create `src/jate/extractors/my_extractor.py`, subclass `CandidateExtractorBase`
2. Implement `extract(documents, nlp_backend, corpus_store)`
3. Register in `api.py::_EXTRACTOR_NAMES` dict
4. Add tests in `tests/test_extractors.py`

## Adding a New Store Backend

1. Create `src/jate/store/my_store.py`, implement the `CorpusStore` protocol
2. Export from `src/jate/store/__init__.py`
3. Add tests in `tests/test_store.py`

## Logging

### Principle

Any pipeline step that processes documents or candidates in bulk must emit timestamped progress to stderr so that performance degradation is visible — not just syntactic failures, but operations that take unexpectedly long.

Without logging, a function that takes 5 minutes looks identical to one that takes 5 seconds. We discovered this the hard way: `ContextFrequency.build()` silently re-tokenised every document per candidate (O(candidates × documents)), turning a 2-minute pipeline into a 60-minute one with no visible signal.

### Pattern

Use timestamped log lines to stderr with immediate flush:

```python
import sys
from datetime import datetime

def _log(msg: str) -> None:
    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] {msg}", file=sys.stderr, flush=True)
```

### What to log

- **NLP processing**: batch number, documents processed, elapsed time
- **Candidate extraction**: progress every ~10% of documents
- **Feature building**: which feature, elapsed time
- **Algorithm scoring**: per-algorithm elapsed time, P/R/F1 if evaluating
- **Downloads**: file size, progress, cache hits

### What NOT to log

- Per-document or per-sentence detail (too noisy)
- Anything to stdout (reserved for results)
- Debug-level internals (use Python `logging` module if needed)

### Example output

```
[14:32:05] Loading spaCy model 'en_core_web_sm' ...
[14:32:06] Extracting candidates from 2000 documents (extractor=pos_pattern) ...
[14:32:06]   NLP batch 1/8: processing documents 1-256 of 2000 ...
[14:32:45]   NLP batch 8/8: processing documents 1793-2000 of 2000 ...
[14:32:53]   Extracted 36355 candidates in 47.2s
[14:32:53] Building features for 13 algorithm(s) ...
[14:35:10]   Features built in 137.5s
[14:35:10] Scoring 13 algorithm(s) ...
[14:35:11]   [1/13] tfidf — P=0.6560 (0.6s)
```

## Efficiency

### Principle

JATE processes corpora with thousands of documents and tens of thousands of candidate terms. Any addition to the pipeline must consider:

1. **Build once, share across algorithms** — use `FeatureCache`, not per-algorithm feature building
2. **Avoid redundant computation** — cache tokenisation, pre-compute shared data structures
3. **Parallelise CPU-bound work** — use `parallel.py::parallel_map` or `ProcessPoolExecutor`
4. **Prefer sparse over dense iteration** — iterate only over data that exists, not all possible pairs

### FeatureCache (build once, share)

When running multiple algorithms, `FeatureCache` (in `api.py`) builds all features needed by the union of selected algorithms exactly once:

```python
from jate.api import FeatureCache, _resolve_algorithm

algos = [_resolve_algorithm(name) for name in algorithm_names]
cache = FeatureCache.build_for_algorithms(algos, candidates, documents, nlp, term_freq, config)

for algo in algos:
    kwargs = cache.get_features(algo)  # zero-cost lookup
    result = algo.score(candidates, term_freq, **kwargs)
```

The `_FEATURE_NEEDS` registry in `api.py` maps feature names to the algorithm types that require them. When adding a new algorithm that needs existing features, register it there. When adding a new feature type, add it to both `FeatureCache.build_for_algorithms()` and `get_features()`.

### Avoiding redundant computation

**Bad** — tokenises the same document thousands of times:
```python
for candidate in candidates:              # 30,000 iterations
    for doc_id in candidate.doc_positions: # multiple docs each
        tokens = nlp.tokenize(doc.content) # REDUNDANT
```

**Good** — tokenise once, reuse:
```python
token_cache = {doc.doc_id: nlp.tokenize(doc.content) for doc in documents}
for candidate in candidates:
    for doc_id in candidate.doc_positions:
        tokens = token_cache[doc_id]       # O(1) lookup
```

### Parallelism pattern

For CPU-bound work that can be split by document or by chunk:

```python
import os
from concurrent.futures import ProcessPoolExecutor

max_workers = os.cpu_count() or 1

# Split work into per-document chunks
chunks = [(doc_id, data) for doc_id, data in work_items.items()]

with ProcessPoolExecutor(max_workers=max_workers) as pool:
    partial_results = list(pool.map(process_one_chunk, chunks))

# Merge partial results
for partial in partial_results:
    merged.update(partial)
```

The worker function must be a **top-level function** (not a lambda or nested function) for pickling. See `features.py::_adjacent_for_doc` for the pattern.

### Sparse vs dense iteration

**Bad** — O(candidates × frequent_terms) even when most pairs have zero co-occurrence:
```python
for candidate in candidates:
    for g_term in frequent_terms:
        freq = cooccurrence.get(candidate, g_term)  # usually 0
```

**Good** — O(actual co-occurrences per candidate):
```python
for candidate in candidates:
    coocs = cooccurrence.get_cooccurrences_for(candidate)  # sparse
    for g_term, freq in coocs.items():
        ...  # only non-zero entries
```

### Performance lessons learned

| Issue | Root cause | Fix | Speedup |
|-------|-----------|-----|---------|
| Co-occurrence indexing | O(n²) pairwise set intersections computed even when unused | `compute_cooccurrences=False` flag | 800x |
| Context frequency | Re-tokenised every document per candidate | Pre-tokenise once + parallel per-document | 21x |
| Chi-square scoring | Dense iteration over all frequent terms per candidate | Sparse co-occurrence lookup with inverted index | 1,420x |
| Feature rebuilding | Features rebuilt per algorithm instead of shared | `FeatureCache` builds once for all algorithms | 2.2x |
