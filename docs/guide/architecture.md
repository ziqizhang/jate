# Architecture

JATE follows a pipeline architecture with pluggable components at each stage.

## Pipeline overview

```
Documents → NLP Batch Processing → Candidate Extraction → Feature Building → Scoring → Ranked Terms
                  ↓                       ↓                      ↓                ↓
            SpacyBackend              Extractor            Feature Classes     Algorithm
            (nlp.pipe())          (POS/ngram/NP)        (built from candidates)  (14 options)
                                                              ↓
                                                   TermFrequency, WordFrequency,
                                                   ContextFrequency, Containment,
                                                   Cooccurrence, ReferenceFrequency, ...
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

### 4. Feature building

Feature objects are built lazily from candidates — only the features required by the chosen algorithm are constructed. This mirrors the Java JATE feature system:

| Feature class | Java equivalent | Used by |
|--------------|-----------------|---------|
| `TermFrequency` | `FrequencyTermBased` (type 0) | All algorithms |
| `WordFrequency` | `FrequencyTermBased` (type 1) | RAKE, Weirdness, GlossEx, TermEx |
| `ReferenceFrequency` | `FrequencyTermBasedFBMaster` | Weirdness, GlossEx, TermEx |
| `ContextFrequency` | `FrequencyCtxBased` | Chi-Square, NCValue |
| `TermComponentIndex` | `TermComponentIndex` | RAKE, Containment |
| `Containment` | `Containment` | CValue, Basic, NCValue, ComboBasic |
| `Cooccurrence` | `Cooccurrence` | Chi-Square |
| `ChiSquareFrequentTerms` | `ChiSquareFrequentTerms` | Chi-Square |

A `CorpusStore` (`MemoryCorpusStore` or `SQLiteCorpusStore`) is still used during candidate extraction and can be used for legacy workflows, but algorithms now receive feature objects directly.

### 4b. Sentence-level context

`ContextFrequency` provides sentence-level context features:

- **Sentence co-occurrence** — term frequencies per sentence context (used by Chi-Square via Cooccurrence)
- **Context term totals** — total term frequency per sentence context (used by Chi-Square)
- **Adjacent words** — words immediately before/after term occurrences (used by NC-Value)

Context features are built automatically from candidate position data when sentence indices are available (default with all extractors).

### 5. Scoring

An `Algorithm` takes the list of candidates, a `TermFrequency` object, and additional feature objects as keyword arguments. It produces a `TermExtractionResult` — a sorted list of `Term` objects, each with a score, frequency, and the set of surface forms.

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
from jate import Corpus, CValue, PosPatternExtractor, TermFrequency, Containment, TermComponentIndex

corpus = Corpus(db_path="corpus.db")
corpus.add_directory("papers/")

extractor = PosPatternExtractor(pattern=r"(ADJ )*(NOUN )+")
candidates = extractor.extract(corpus.documents, corpus.nlp, corpus.store)

# Build features
term_freq = TermFrequency.build(candidates, total_docs=len(corpus.documents))
tci = TermComponentIndex.build(candidates)
containment = Containment.build(candidates, tci)

algo = CValue()
result = algo.score(candidates, term_freq, containment=containment)
```

### Configurable parallelism

CPU-intensive stages (co-occurrence computation, containment index building) support parallel execution via `ProcessPoolExecutor`. Users opt in with `JATEConfig(max_workers=N)`. Default is sequential (`max_workers=1`) — no overhead.

```python
config = jate.JATEConfig(max_workers=4)
result = jate.extract_corpus(docs, algorithm="cvalue", config=config)
```

NLP processing uses spaCy's `nlp.pipe()` for efficient batching, which releases the GIL during C-level parsing.

## Module map

```
jate/
├── __init__.py          # Public exports
├── api.py               # Layer 1 & 2: extract(), extract_corpus(), compare()
├── config.py            # JATEConfig dataclass
├── context.py           # ContextIndex (sentence co-occurrence, adjacent words)
├── features.py          # Feature classes (TermFrequency, WordFrequency, ContextFrequency, ...)
├── models.py            # Term, Candidate, Document, TermExtractionResult
├── parallel.py          # parallel_map, split_range utilities
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
│   ├── voting.py         # Ensemble voting (reciprocal rank fusion)
│   └── ...
├── extractors/          # Candidate extraction strategies
│   ├── base.py          # CandidateExtractorBase ABC
│   ├── pos_pattern.py
│   ├── ngram.py
│   ├── noun_phrase.py
│   └── utils.py         # Shared utilities (batch processing, token offsets)
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
