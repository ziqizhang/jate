# JATE — Just Automatic Term Extraction

A Python library for automatic term extraction (ATE) from text corpora. JATE provides 14 ATE algorithms (13 classical + ensemble voting), corpus-level statistics, built-in evaluation, and a CLI — all pip-installable with no external services required.

**JATE v3.0.0** is a complete rewrite of the original [Java JATE](https://github.com/ziqizhang/jate/tree/legacy/java) library (84+ GitHub stars), which was built on Apache Solr and used in academic and industry settings for over a decade. The Python version preserves all 13 classical algorithms from the Java codebase — with every formula verified line-by-line against the original source — while removing the Solr dependency in favour of a self-contained, pip-installable package. The original Java library is preserved on the [`legacy/java`](https://github.com/ziqizhang/jate/tree/legacy/java) branch.

## Installation

```bash
pip install jate
```

Or from source:

```bash
git clone https://github.com/ziqizhang/jate.git
cd jate
pip install .
```

Requires Python 3.11+ and a spaCy model:

```bash
python -m spacy download en_core_web_sm
```

## Quick start

### Single document

```python
import jate

# Extract terms from text (default: C-Value + POS pattern extraction)
result = jate.extract("Your document text here...")

for term in result:
    print(f"{term.string:30s}  score={term.score:.4f}  surfaces={term.surface_forms}")
```

### Corpus-level extraction

```python
import jate

# From a list of texts
result = jate.extract_corpus(
    ["First document...", "Second document..."],
    algorithm="tfidf",
)

# From a directory of text files
result = jate.extract_corpus("path/to/corpus/", algorithm="cvalue")

# Export results
df = result.to_dataframe()
print(result.to_csv())
```

### Compare algorithms

```python
import jate

results = jate.compare(
    ["Doc one...", "Doc two..."],
    algorithms=["cvalue", "tfidf", "rake", "weirdness"],
)

for algo_name, result in results.items():
    print(f"\n{algo_name}: {len(result)} terms")
    for term in list(result)[:5]:
        print(f"  {term.string:30s}  {term.score:.4f}")
```

For large corpora, speed up with parallel processing:

```python
config = jate.JATEConfig(max_workers=4)
results = jate.compare(docs, algorithms=["cvalue", "tfidf", "rake"], config=config)
```

### Evaluation against a gold standard

```python
import jate

result = jate.extract_corpus(docs, algorithm="cvalue")

evaluator = jate.Evaluator(gold_terms={"machine learning", "neural network", ...})
eval_result = evaluator.evaluate(result)
print(eval_result.summary())
# P=0.2800  R=0.0644  F1=0.1047  TP=28  FP=72  FN=407  predicted=100  gold=435

# Evaluate top-k
eval_at_50 = evaluator.evaluate_at_k(result, k=50)
```

### CLI

```bash
# Extract terms from text
jate extract "Your text here" --algorithm cvalue --top 20

# Extract from a corpus directory
jate corpus path/to/docs/ --algorithm tfidf --output csv

# Compare algorithms on a corpus
jate compare path/to/docs/ --algorithms cvalue tfidf rake

# Run benchmark on built-in dataset
jate benchmark --top 100
```

## Algorithms

| Algorithm | Description | Reference |
|-----------|-------------|-----------|
| `tfidf` | TF-IDF at corpus level | — |
| `cvalue` | Multi-word term extraction via nested term frequency | Frantzi et al. 2000 |
| `ncvalue` | C-Value extended with context word information | Frantzi et al. 2000 |
| `basic` | Frequency + containment scoring | Bordea et al. 2013 |
| `combobasic` | Basic with parent and child containment | Bordea et al. 2013 |
| `attf` | Average total term frequency (TTF / DF) | — |
| `ttf` | Raw total term frequency | — |
| `ridf` | Residual IDF (deviation from Poisson) | Church & Gale 1995 |
| `rake` | Rapid Automatic Keyword Extraction | Rose et al. 2010 |
| `chi_square` | Chi-square test for term independence | Matsuo & Ishizuka 2003 |
| `weirdness` | Target vs reference corpus frequency ratio | Ahmad et al. 1999 |
| `termex` | Domain pertinence + context + lexical cohesion | Sclano et al. 2007 |
| `glossex` | Domain specificity via glossary comparison | Park et al. 2002 |
| `voting` | Ensemble via reciprocal rank fusion | — |

## Candidate extractors

| Extractor | Description |
|-----------|-------------|
| `pos_pattern` (default) | Regex over Universal POS tags (e.g. `(ADJ )*(NOUN )+`) |
| `ngram` | Contiguous token n-grams (configurable min/max n) |
| `noun_phrase` | spaCy noun chunk detection |

## How it works

1. **Candidate extraction** — identifies potential terms using POS patterns, n-grams, or noun phrases
2. **Lemmatisation** — normalises candidates to their lemmatised form (e.g. "neural networks" and "neural network" become one entry)
3. **Sentence context** *(automatic)* — builds sentence co-occurrence and adjacency features for algorithms that use them (Chi-Square, NC-Value)
4. **Corpus statistics** — builds frequency and co-occurrence counts (in-memory or SQLite-backed)
5. **Scoring** — applies the chosen algorithm to rank candidates
6. **Output** — returns `TermExtractionResult` with the normalised term, score, and all observed surface forms

Each `Term` in the result contains:
- `string` — the canonical (lemmatised) form, used for scoring and evaluation
- `score` — algorithm-assigned score
- `frequency` — total corpus frequency
- `surface_forms` — all surface variants observed (e.g. `{"neural network", "neural networks", "Neural Networks"}`)

## Contributing

JATE is in active development and we welcome contributions. Here's how you can get involved:

- **Browse open issues** — check the [feature roadmap](https://github.com/ziqizhang/jate/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement) for planned enhancements
- **Good first issues** — look for issues labelled [`good first issue`](https://github.com/ziqizhang/jate/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) if you're new to the project
- **Feature requests** — [open an issue](https://github.com/ziqizhang/jate/issues/new?template=feature_request.yml) to suggest new features
- **Bug reports** — [report here](https://github.com/ziqizhang/jate/issues/new?template=bug_report.yml)
- **Star the repo** to follow progress

See [CONTRIBUTING.md](docs/contributing.md) for development setup and guidelines.

## Background

JATE was originally developed as part of research at the University of Sheffield, with publications in venues including ACM TKDD and the Semantic Web Journal. The library has been used in academic and industry settings for terminology extraction, ontology learning, and knowledge graph construction.

**Key publications:**
- Zhang, Z., Gao, J., Ciravegna, F. (2018). *SemRe-Rank: Improving Automatic Term Extraction By Incorporating Semantic Relatedness With Personalised PageRank.* ACM TKDD.
- Zhang, Z., Iria, J., Brewster, C., Ciravegna, F. (2008). *A Comparative Evaluation of Term Recognition Algorithms.* LREC.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.

---
