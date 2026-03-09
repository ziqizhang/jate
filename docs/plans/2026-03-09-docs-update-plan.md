# Documentation Update Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Update all documentation to reflect sentence-context and parallelism features, and verify all quickstart examples work.

**Architecture:** Pure documentation task — edit markdown files, verify Python examples, commit. No code changes to src/.

**Tech Stack:** Markdown, mkdocs-material, mkdocstrings, Poetry (for running verification commands)

---

### Task 1: Verify quickstart examples work

**Files:**
- Read: `docs/getting-started/quickstart.md`

Run each key code snippet and confirm output is reasonable. Use `poetry run python -c "..."` for each.

**Step 1: Test single document extraction**

Run:
```bash
poetry run python -c "
import jate
result = jate.extract('Natural language processing uses neural networks for text classification and machine learning.')
for term in result:
    print(f'{term.string:30s}  score={term.score:.4f}')
print(f'Total: {len(result)} terms')
"
```
Expected: Some terms with scores, no errors.

**Step 2: Test algorithm selection**

Run:
```bash
poetry run python -c "
import jate
result = jate.extract('Natural language processing uses neural networks.', algorithm='tfidf', extractor='pos_pattern')
print(f'{len(result)} terms extracted with tfidf')
"
```

**Step 3: Test corpus-level extraction**

Run:
```bash
poetry run python -c "
import jate
result = jate.extract_corpus(
    ['Machine learning and deep learning are subfields of artificial intelligence.',
     'Neural networks form the basis of deep learning research.',
     'Natural language processing uses machine learning techniques.'],
    algorithm='cvalue',
)
for term in list(result)[:5]:
    print(f'{term.string:30s}  score={term.score:.4f}  freq={term.frequency}')
"
```

**Step 4: Test compare**

Run:
```bash
poetry run python -c "
import jate
results = jate.compare(
    ['Machine learning and deep learning are subfields of AI.',
     'Neural networks are powerful tools for research.'],
    algorithms=['cvalue', 'tfidf', 'rake', 'ridf', 'basic'],
    min_frequency=1,
)
for name, result in results.items():
    top3 = [t.string for t in list(result)[:3]]
    print(f'{name:12s}  top 3: {top3}')
"
```

**Step 5: Test evaluation**

Run:
```bash
poetry run python -c "
import jate
result = jate.extract('Machine learning and neural networks for text classification.', algorithm='cvalue')
gold = {'machine learning', 'neural network', 'text classification'}
evaluator = jate.Evaluator(gold_terms=gold)
eval_result = evaluator.evaluate(result)
print(eval_result.summary())
"
```

**Step 6: Test filtering and export**

Run:
```bash
poetry run python -c "
import jate
result = jate.extract('Machine learning and neural networks for text classification and deep learning.', algorithm='cvalue')
filtered = result.filter_by_frequency(min_frequency=1)
print(f'Filtered: {len(filtered)} terms')
df = result.to_dataframe()
print(df.head())
csv = result.to_csv()
print(csv[:200])
"
```

**Step 7: Test parallelism config**

Run:
```bash
poetry run python -c "
import jate
config = jate.JATEConfig(max_workers=2)
results = jate.compare(
    ['Machine learning is great.', 'Neural networks are powerful.'],
    algorithms=['cvalue', 'tfidf', 'basic'],
    config=config,
    min_frequency=1,
)
print(f'Parallel compare: {list(results.keys())}')
"
```

**Step 8: Record any failures**

If any snippet fails, note the exact error. These will be fixed in code before doc updates.

---

### Task 2: Update changelog

**Files:**
- Modify: `docs/changelog.md`

**Step 1: Add new entries**

After the existing "Added" section (line 18), insert new entries for sentence-context and parallelism:

```markdown
## 3.0.0 (unreleased)

Complete rewrite from Java to Python.

### Added

- 13 ATE algorithms: TFIDF, C-Value, NC-Value, Basic, ComboBasic, ATTF, TTF, RIDF, RAKE, Chi-Square, Weirdness, TermEx, GlossEx
- 3 candidate extractors: POS pattern, n-gram, noun phrase
- Lemmatisation-aware pipeline — normalised forms as canonical keys with surface form tracking
- In-memory and SQLite-backed corpus statistics stores
- Built-in evaluation (P/R/F1) against gold standard term lists
- Benchmark runner for comparing algorithms on datasets
- CLI with extract, corpus, compare, benchmark commands
- Export to pandas DataFrame, CSV, JSON
- spaCy-based NLP backend with Protocol interface for extensibility
- ACL RD-TEC Mini built-in dataset
- Sentence-level context support — `ContextIndex` provides sentence co-occurrence for Chi-Square and adjacency-based context for NC-Value, aligning with original papers
- Configurable parallelism via `JATEConfig(max_workers=N)` — parallel co-occurrence computation, containment index building, and batch NLP processing with `nlp.pipe()`
- Shared containment index builder — computed once and reused across Basic, ComboBasic, CValue, and NCValue algorithms

### Changed

- Renamed from "Java Automatic Term Extraction" to "Just Automatic Term Extraction"
- No longer requires Apache Solr — corpus statistics are self-contained
- Python 3.11+ required
```

**Step 2: Commit**

```bash
git add docs/changelog.md
git commit -m "docs: update changelog with sentence-context and parallelism features"
```

---

### Task 3: Update architecture module map and pipeline

**Files:**
- Modify: `docs/guide/architecture.md`

**Step 1: Update the pipeline overview**

Replace the pipeline diagram (lines 7-12) with:

```
Documents → NLP Batch Processing → Candidate Extraction → Corpus Statistics → Scoring → Ranked Terms
               ↓                        ↓                      ↓                ↓
         SpacyBackend              Extractor            CorpusStore         Algorithm
         (nlp.pipe())         (POS/ngram/NP)      (Memory / SQLite)     (13 options)
                                                        ↓
                                                  ContextIndex
                                              (sentence co-occurrence)
```

**Step 2: Add section about ContextIndex after "Corpus statistics" (after line 39)**

Add:

```markdown
### 4b. Sentence-level context

A `ContextIndex` provides sub-document context features used by some algorithms:

- **Sentence co-occurrence** — how often two terms appear in the same sentence (used by Chi-Square)
- **Context term totals** — number of distinct context terms per sentence (used by Chi-Square)
- **Adjacent words** — words immediately adjacent to term occurrences (used by NC-Value)

The ContextIndex is built automatically when extractors provide sentence-level position data. It is passed to algorithms as an optional parameter.
```

**Step 3: Add section about parallelism after "Key design decisions" (after line 72)**

Add:

```markdown
### Configurable parallelism

CPU-intensive stages (co-occurrence computation, containment index building) support parallel execution via `ProcessPoolExecutor`. Users opt in with `JATEConfig(max_workers=N)`. Default is sequential (`max_workers=1`) — no overhead.

```python
config = jate.JATEConfig(max_workers=4)
result = jate.extract_corpus(docs, algorithm="cvalue", config=config)
```

NLP processing uses spaCy's `nlp.pipe()` for efficient batching, which releases the GIL during C-level parsing.
```

**Step 4: Update module map (lines 90-120)**

Add the new files to the module map:

```
jate/
├── __init__.py          # Public exports
├── api.py               # Layer 1 & 2: extract(), extract_corpus(), compare()
├── config.py            # JATEConfig dataclass
├── context.py           # ContextIndex (sentence co-occurrence, adjacent words)
├── features.py          # Shared containment index builders
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
│   └── ...
├── extractors/          # Candidate extraction strategies
│   ├── base.py          # CandidateExtractorBase ABC
│   ├── pos_pattern.py
│   ├── ngram.py
│   ├── noun_phrase.py
│   └── utils.py         # Shared utilities (batch_process_docs, compute_token_offsets)
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

**Step 5: Commit**

```bash
git add docs/guide/architecture.md
git commit -m "docs: update architecture with ContextIndex, parallelism, and module map"
```

---

### Task 4: Update algorithms guide

**Files:**
- Modify: `docs/guide/algorithms.md`

**Step 1: Update Chi-Square section**

After the existing Chi-Square description, add a note about sentence-level context:

```markdown
When sentence-level position data is available (default with all extractors), Chi-Square uses **sentence co-occurrence** rather than document co-occurrence, aligning with the original paper (Matsuo & Ishizuka 2003).
```

**Step 2: Update NC-Value section**

After the existing NC-Value description, add:

```markdown
When sentence-level position data is available, NC-Value uses **adjacency-based context** (words immediately before/after term occurrences) as described in the original paper (Frantzi et al. 2000). The context score uses the formula `weight(w) = t(w) / n` where `t(w)` is how many unique terms word `w` appears adjacent to and `n` is the total number of unique terms with context.
```

**Step 3: Commit**

```bash
git add docs/guide/algorithms.md
git commit -m "docs: add sentence-level context notes for Chi-Square and NC-Value"
```

---

### Task 5: Update quickstart with parallelism example

**Files:**
- Modify: `docs/getting-started/quickstart.md`

**Step 1: Add parallelism example after the "Compare algorithms" section (after line 91)**

Add:

```markdown
### Speed up with parallelism

For large corpora, enable parallel processing:

```python
config = jate.JATEConfig(max_workers=4)
results = jate.compare(
    docs,
    algorithms=["cvalue", "tfidf", "rake", "ridf", "basic"],
    config=config,
)
```

This parallelises co-occurrence computation and containment index building across multiple processes. Default is `max_workers=1` (sequential, no overhead).
```

**Step 2: Commit**

```bash
git add docs/getting-started/quickstart.md
git commit -m "docs: add parallelism example to quickstart"
```

---

### Task 6: Update API reference

**Files:**
- Modify: `docs/reference/api.md`

**Step 1: Add JATEConfig and ContextIndex sections**

After the "Public API functions" section (after line 11), add:

```markdown
## Configuration

::: jate.config.JATEConfig
```

After the "Corpus" section (after line 35), add:

```markdown
## Context

::: jate.context.ContextIndex
```

After the "Stores" section (after line 85), add:

```markdown
## Parallel Utilities

::: jate.parallel.parallel_map

::: jate.parallel.split_range

## Feature Builders

::: jate.features.build_containment_index

::: jate.features.build_child_containment_index
```

**Step 2: Commit**

```bash
git add docs/reference/api.md
git commit -m "docs: add JATEConfig, ContextIndex, and parallel utilities to API reference"
```

---

### Task 7: Update index.md and README.md

**Files:**
- Modify: `docs/index.md`
- Modify: `README.md`

**Step 1: Update docs/index.md "Why JATE?" list**

After the "CLI included" bullet (line 13), add:

```markdown
- **Sentence-level context** — Chi-Square and NC-Value use sentence co-occurrence and adjacency as described in the original papers
- **Configurable parallelism** — speed up large corpora with `JATEConfig(max_workers=N)`
```

**Step 2: Update README.md**

In the "How it works" section (after line 141), add step 2b:

```markdown
2. **Lemmatisation** — normalises candidates to their lemmatised form
2b. **Sentence context** *(automatic)* — builds sentence co-occurrence and adjacency features for algorithms that use them
```

In the "Quick start > Compare algorithms" section (around line 62), add a note after the code block:

```markdown
For large corpora, speed up with parallel processing:

```python
config = jate.JATEConfig(max_workers=4)
results = jate.compare(docs, algorithms=["cvalue", "tfidf", "rake"], config=config)
```
```

**Step 3: Commit**

```bash
git add docs/index.md README.md
git commit -m "docs: update index and README with sentence-context and parallelism features"
```

---

### Task 8: Final verification

**Step 1: Build mkdocs locally to check for errors**

Run:
```bash
poetry run mkdocs build --strict 2>&1
```
Expected: No errors. If there are warnings about missing mkdocstrings references, fix the api.md references.

**Step 2: Verify new examples work**

Run the parallelism quickstart example:
```bash
poetry run python -c "
import jate
config = jate.JATEConfig(max_workers=2)
result = jate.extract_corpus(
    ['Machine learning is a field of AI.', 'Deep learning uses neural networks.', 'NLP applies machine learning.'],
    algorithm='cvalue',
    config=config,
)
print(f'{len(result)} terms extracted with parallelism')
for t in list(result)[:3]:
    print(f'  {t.string}: {t.score:.4f}')
"
```

**Step 3: Run full test suite**

Run:
```bash
poetry run pytest tests/ -v --tb=short 2>&1 | tail -5
```
Expected: ALL PASS (docs changes should not affect tests)

**Step 4: Commit any fixes**

If mkdocs build failed or examples needed adjustment, commit fixes.
