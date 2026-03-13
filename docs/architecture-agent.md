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

Each algorithm subclasses `Algorithm` (in `base.py`) and implements `score(candidates, term_freq, **kwargs)`.

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
