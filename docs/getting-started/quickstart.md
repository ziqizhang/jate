# Quick Start

## Extract terms from a single document

The simplest way to use JATE — pass text, get terms:

```python
import jate

result = jate.extract("Natural language processing uses neural networks for text classification and machine learning.")

for term in result:
    print(f"{term.string:30s}  score={term.score:.4f}")
```

By default this uses C-Value with POS pattern extraction.

## Choose an algorithm

```python
result = jate.extract(
    "Your text here...",
    algorithm="tfidf",    # any of the 13 algorithms
    extractor="pos_pattern",  # or "ngram", "noun_phrase"
)
```

## Corpus-level extraction

For better results, extract from multiple documents so corpus statistics (document frequency, co-occurrence) are meaningful:

```python
result = jate.extract_corpus(
    [
        "First document about machine learning...",
        "Second document about deep learning...",
        "Third document about neural networks...",
    ],
    algorithm="cvalue",
)

# Top 10 terms
for term in list(result)[:10]:
    print(f"{term.string:30s}  score={term.score:.4f}  freq={term.frequency}")
```

### From files

```python
result = jate.extract_corpus(
    ["paper1.txt", "paper2.txt", "paper3.txt"],
    algorithm="tfidf",
)
```

File paths are auto-detected — if all strings in the list are existing file paths, they are loaded as files. Otherwise they are treated as raw text.

### From a directory

```python
result = jate.extract_corpus("path/to/corpus/", algorithm="cvalue")
```

A single string that is an existing directory loads all `.txt` files from it.

### With SQLite persistence

For large corpora, use SQLite-backed storage so statistics persist across runs:

```python
result = jate.extract_corpus(
    "path/to/corpus/",
    algorithm="cvalue",
    db_path="my_corpus.db",
)
```

## Compare algorithms

Run multiple algorithms on the same corpus and compare results:

```python
results = jate.compare(
    ["Doc one...", "Doc two..."],
    algorithms=["cvalue", "tfidf", "rake", "ridf", "basic"],
)

for name, result in results.items():
    top5 = [t.string for t in list(result)[:5]]
    print(f"{name:12s}  top 5: {top5}")
```

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

## Evaluate against a gold standard

```python
gold = {"machine learning", "neural network", "text classification", "deep learning"}

evaluator = jate.Evaluator(gold_terms=gold)
eval_result = evaluator.evaluate(result)
print(eval_result.summary())
# P=0.XXXX  R=0.XXXX  F1=0.XXXX  TP=X  FP=X  FN=X  predicted=X  gold=X

# Evaluate only the top-k terms
eval_at_50 = evaluator.evaluate_at_k(result, k=50)
```

## Filtering results

```python
result = jate.extract_corpus(docs, algorithm="cvalue")

# Only terms with score >= 2.0
filtered = result.filter_by_score(min_score=2.0)

# Only terms appearing at least 3 times
filtered = result.filter_by_frequency(min_frequency=3)

# Only multi-word terms (2-5 words)
filtered = result.filter_by_length(min_words=2, max_words=5)
```

## Export

```python
# pandas DataFrame
df = result.to_dataframe()

# CSV string
csv_str = result.to_csv()

# JSON string
json_str = result.to_json()
```

## Surface forms

Each term carries the normalised (lemmatised) form as `term.string` and all surface variants observed in the corpus:

```python
for term in result:
    print(f"Term: {term.string}")
    print(f"  Surface forms: {term.surface_forms}")
    # e.g. Term: neural network
    #   Surface forms: {'neural network', 'neural networks', 'Neural Networks'}
```

## Next steps

- [Algorithms](../guide/algorithms.md) — learn about all 13 algorithms and when to use each
- [Candidate Extractors](../guide/extractors.md) — POS patterns, n-grams, noun phrases
- [Evaluation & Benchmarking](../guide/evaluation.md) — evaluate with gold standards and run benchmarks
- [CLI Reference](../guide/cli.md) — use JATE from the command line
