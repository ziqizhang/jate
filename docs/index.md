# JATE — Just Automatic Term Extraction

A Python library for automatic term extraction (ATE) from text corpora. JATE provides 14 ATE algorithms (13 classical + ensemble voting), corpus-level statistics, built-in evaluation, and a CLI — all pip-installable with no external services required.

## Why JATE?

- **14 algorithms** in one library — from simple frequency-based to sophisticated statistical methods, plus ensemble voting
- **No external services** — corpus statistics are computed in-memory or backed by SQLite
- **Lemmatisation-aware** — "neural networks" and "neural network" are treated as the same term, with all surface variants tracked
- **Built-in evaluation** — precision, recall, F1 against gold standard term lists
- **Multiple extraction strategies** — POS pattern matching, n-grams, or noun phrase chunking
- **Export anywhere** — pandas DataFrame, CSV, JSON
- **Sentence-level context** — Chi-Square and NC-Value use sentence co-occurrence and adjacency as described in the original papers
- **Configurable parallelism** — speed up large corpora with `JATEConfig(max_workers=N)`
- **CLI included** — extract, compare, and benchmark from the command line

## At a glance

```python
import jate

# Extract terms from a single document
result = jate.extract("Your document text here...")

# Corpus-level extraction
result = jate.extract_corpus(["Doc 1...", "Doc 2..."], algorithm="cvalue")

# Compare algorithms
results = jate.compare(docs, algorithms=["cvalue", "tfidf", "rake"])

# Evaluate against a gold standard
evaluator = jate.Evaluator(gold_terms={"machine learning", "neural network"})
print(evaluator.evaluate(result).summary())
```

## Background

JATE was originally developed at the University of Sheffield as a Java library for automatic term extraction (originally "Java Automatic Term Extraction"). It has been used by researchers and practitioners in NLP, biomedical text mining, and knowledge engineering since 2014.

Version 3.0 is a complete rewrite in Python, designed to integrate seamlessly with spaCy, pandas, and modern NLP workflows.

> Looking for the original Java version? It's preserved on the [`legacy/java`](https://github.com/ziqizhang/jate/tree/legacy/java) branch.

## Key publications

- Zhang, Z., Gao, J., Ciravegna, F. (2018). *SemRe-Rank: Improving Automatic Term Extraction By Incorporating Semantic Relatedness With Personalised PageRank.* ACM TKDD.
- Zhang, Z., Iria, J., Brewster, C., Ciravegna, F. (2008). *A Comparative Evaluation of Term Recognition Algorithms.* LREC.
