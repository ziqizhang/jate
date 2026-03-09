# Parallelism via Map-Reduce

**Date:** 2026-03-09
**Status:** Approved

## Problem

JATE processes everything sequentially. Key bottlenecks:
- Co-occurrence computation: O(m²) nested loop over term pairs
- Containment index: O(C²) nested loop, duplicated across Basic/ComboBasic/CValue
- Algorithm comparison: 5-13 algorithms scored sequentially on read-only data
- NLP processing: documents processed one at a time, no batching

The Java JATE parallelized all of these using ForkJoinPool with Master/Worker pattern.

## Approach

**Map-reduce with `ProcessPoolExecutor`** for CPU-bound work. Each target:
1. **Split** work into independent chunks (no overlapping writes)
2. **Map** — each process computes on its chunk, producing a local result
3. **Reduce** — main process merges all local results

**`nlp.pipe()` batching** for NLP processing (spaCy releases GIL internally).

Default is sequential (`max_workers=1`) — no overhead. Users opt in to parallelism.

## Configuration

New `JATEConfig` dataclass in `src/jate/config.py`:

```python
@dataclass
class JATEConfig:
    max_workers: int = 1  # 1 = sequential, N = use N processes
```

Passed through API functions:

```python
config = JATEConfig(max_workers=8)
result = jate.extract_corpus(sources, config=config)
```

## Parallel Helper

Thin utility in `src/jate/parallel.py`:

```python
def parallel_map(fn, chunks, max_workers=1):
    if max_workers <= 1:
        return [fn(chunk) for chunk in chunks]
    with ProcessPoolExecutor(max_workers=max_workers) as pool:
        return list(pool.map(fn, chunks))
```

All targets use this helper. Caller splits work, calls `parallel_map`, merges results.

## Target 1: Parallel Algorithm Comparison

In `compare()`, each algorithm scores independently on read-only data:

```python
def _score_one(args):
    algo, candidates, store, context_index = args
    return algo.name, algo.score(candidates, store, context_index=context_index)

chunks = [(algo, candidates, store, context_index) for algo in algos]
partial_results = parallel_map(_score_one, chunks, config.max_workers)
results = {name: result for name, result in partial_results}
```

Each process gets a copy of candidates + store. No shared state. SQLiteCorpusStore is not picklable — falls back to sequential when `db_path` is used.

## Target 2: Parallel Co-occurrence

Split the outer loop index range of the O(m²) nested loop. Each chunk computes co-occurrence for its assigned term pairs into a local dict. Merge via `dict.update()` — no key conflicts since each `(i, j)` pair assigned to exactly one chunk.

```python
def _compute_cooc_chunk(args):
    terms, term_docs, start, end = args
    partial = {}
    for i in range(start, end):
        t_a = terms[i]
        docs_a = term_docs[t_a]
        for t_b in terms[i + 1:]:
            shared = len(docs_a & term_docs[t_b])
            if shared > 0:
                partial[(t_a, t_b)] = shared
    return partial
```

Java split by context windows with synchronized shared matrix. Python splits by term pairs with local dicts — better fit for `ProcessPoolExecutor` (no shared memory between processes).

## Target 3: Parallel Containment Index

Currently Basic, ComboBasic, and CValue each independently compute the same O(C²) containment. Changes:

1. **Shared builder** — compute containment once, pass to algorithms
2. **Parallelize** — split outer loop across processes, each produces local dict, merge

```python
def _containment_chunk(args):
    candidates, start, end = args
    partial = {}
    for i in range(start, end):
        c = candidates[i]
        parents = [other.nf for other in candidates
                   if len(other.words) > len(c.words)
                   and f" {c.nf} " in f" {other.nf} "]
        partial[c.nf] = parents
    return partial
```

Algorithms accept pre-built containment index instead of computing their own.

## Target 4: Batch NLP Processing

SpacyBackend gains `process_batch()` using `nlp.pipe()` which releases GIL during C-level parsing:

```python
def process_batch(self, texts, batch_size=256):
    return list(self._nlp.pipe(texts, batch_size=batch_size))
```

Extractors change from per-document NLP calls to two-phase: batch NLP first, then extract from pre-processed results.

## Pipeline Flow

```
1. Load documents                          (sequential)
2. Batch NLP processing                    (PARALLEL — spaCy nlp.pipe)
3. Candidate extraction from NLP results   (sequential — fast pattern matching)
4. Store indexing + co-occurrence           (co-occurrence PARALLEL)
5. Build containment index                 (PARALLEL)
6. Build ContextIndex                      (sequential)
7. Score algorithms                        (PARALLEL in compare())
8. Filter + rank                           (sequential)
```

## API Changes

```python
def extract(text, *, algorithm="cvalue", config=None, ...):
def extract_corpus(sources, *, algorithm="cvalue", config=None, ...):
def compare(sources, *, algorithms=None, config=None, ...):
```

`config=None` defaults to `JATEConfig(max_workers=1)` — fully sequential, backwards compatible.

## Testing

- `parallel_map` with `max_workers=1` and `max_workers=2` — verify identical results
- Co-occurrence parity: sequential vs parallel, assert equal dicts
- Containment parity: sequential vs parallel, assert equal dicts
- `compare()` parity: `max_workers=1` vs `max_workers=2`, assert same scores
- Existing tests unaffected — parallelism produces identical results
- No performance assertions in CI (environment-dependent)

## What Stays the Same

- All existing tests and default behavior (max_workers=1)
- CorpusStore protocol (no changes)
- Algorithm interfaces (containment passed as optional pre-built data)
- Single-algorithm extract/extract_corpus (nothing to parallelize)
