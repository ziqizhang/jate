# Architecture

JATE follows a pipeline architecture with pluggable components at each stage.

## Pipeline overview

```
Documents → Candidate Extraction → Corpus Statistics → Scoring → Ranked Terms
               ↓                        ↓                ↓
         NLP Backend              CorpusStore         Algorithm
         (spaCy)              (Memory / SQLite)     (13 options)
```

### 1. Document ingestion

Documents are loaded as `Document` objects (plain text with an ID and optional metadata). They can come from raw strings, text files, or a directory.

### 2. NLP processing

An `NLPBackend` (default: spaCy) provides tokenisation, POS tagging, lemmatisation, sentence splitting, and noun chunk detection. The backend is defined as a Protocol, so alternative backends can be swapped in.

### 3. Candidate extraction

A `CandidateExtractor` identifies potential terms from the processed text. Three strategies are provided:

- **POS pattern** — regex over Universal POS tag sequences (default: `(ADJ )*(NOUN )+`)
- **N-gram** — contiguous token windows of configurable size
- **Noun phrase** — spaCy's noun chunk detector

Candidates are **merged by normalised form**: the lemmatised, lowercased representation. This means "neural networks" and "neural network" produce a single candidate. All observed surface variants are tracked.

### 4. Corpus statistics

A `CorpusStore` collects frequency and co-occurrence counts:

- **Term frequency** — how many times a term appears across all documents
- **Document frequency** — how many documents contain the term
- **Co-occurrence** — how many documents two terms share
- **Corpus total** — sum of all term frequencies

Two implementations:

| Store | Use case |
|-------|----------|
| `MemoryCorpusStore` | Fast, in-memory, for single sessions |
| `SQLiteCorpusStore` | Persistent, for large corpora or repeated analysis |

### 5. Scoring

An `Algorithm` takes the list of candidates and the corpus store, and produces a `TermExtractionResult` — a sorted list of `Term` objects, each with a score, frequency, and the set of surface forms.

### 6. Output

`TermExtractionResult` supports iteration, indexing, filtering, sorting, and export to DataFrame / CSV / JSON.

## Key design decisions

### Lemmatisation as the canonical key

All corpus statistics and scores are computed on the **normalised (lemmatised) form**. This matches the approach of the original Java JATE and ensures that morphological variants are aggregated correctly. The `Term.string` field always contains this normalised form, while `Term.surface_forms` maps back to every variant observed.

### Protocol-based extensibility

The core interfaces (`NLPBackend`, `CandidateExtractor`, `CorpusStore`) are defined as Python `Protocol` classes. This means you can provide your own implementation without inheriting from a base class — any object with the right methods will work.

### Three API layers

JATE provides three levels of API:

1. **One-liner** — `jate.extract(text)` for quick extraction from a single document
2. **Corpus API** — `jate.extract_corpus(...)` and `jate.compare(...)` for multi-document workflows
3. **Component API** — use `Corpus`, extractors, algorithms, and stores directly for full control

```python
# Layer 3: full control
from jate import Corpus, CValue, PosPatternExtractor

corpus = Corpus(db_path="corpus.db")
corpus.add_directory("papers/")

extractor = PosPatternExtractor(pattern=r"(ADJ )*(NOUN )+")
candidates = extractor.extract(corpus.documents, corpus.nlp, corpus.store)

algo = CValue()
result = algo.score(candidates, corpus.store)
```

## Module map

```
jate/
├── __init__.py          # Public exports
├── api.py               # Layer 1 & 2: extract(), extract_corpus(), compare()
├── models.py            # Term, Candidate, Document, TermExtractionResult
├── protocols.py         # Protocol interfaces
├── corpus.py            # Corpus wrapper class
├── cli.py               # CLI entry point
├── evaluation.py        # Evaluator, EvaluationResult
├── benchmark.py         # BenchmarkRunner
├── algorithms/          # 13 scoring algorithms
│   ├── base.py          # Algorithm ABC
│   ├── tfidf.py
│   ├── cvalue.py
│   ├── ncvalue.py
│   └── ...
├── extractors/          # Candidate extraction strategies
│   ├── base.py          # CandidateExtractorBase ABC
│   ├── pos_pattern.py
│   ├── ngram.py
│   └── noun_phrase.py
├── nlp/                 # NLP backends
│   ├── spacy_backend.py
│   └── document_loader.py
├── store/               # Corpus statistics storage
│   ├── memory_store.py
│   └── sqlite_store.py
└── datasets/            # Built-in benchmark datasets
    ├── base.py
    └── acl_rdtec.py
```
