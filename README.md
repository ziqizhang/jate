# JATE — Just Automatic Term Extraction

A Python library for automatic term extraction (ATE) from text corpora. JATE provides 13 classical ATE algorithms, corpus-level statistics, built-in evaluation, and a CLI — all pip-installable with no external services required.

**JATE v3.0.0** is a complete rewrite of the original [Java JATE](https://github.com/ziqizhang/jate/tree/legacy/java) library (84+ GitHub stars), which was built on Apache Solr and used in academic and industry settings for over a decade. The Python version preserves all 13 classical algorithms from the Java codebase — with every formula verified line-by-line against the original source — while removing the Solr dependency in favour of a self-contained, pip-installable package. It also adds ensemble voting via reciprocal rank fusion when comparing multiple algorithms. The original Java library is preserved on the [`legacy/java`](https://github.com/ziqizhang/jate/tree/legacy/java) branch.

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

For large corpora, NLP processing (spaCy) uses multi-threaded C-level batching, and feature building (adjacent word computation) uses multi-process parallelism automatically.

### Evaluation against a gold standard

```python
import jate

result = jate.extract_corpus(docs, algorithm="cvalue")

evaluator = jate.Evaluator({"machine learning", "neural network", ...})
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

# Run benchmark on built-in dataset (use --list-datasets to see all options)
jate benchmark --dataset acl_rdtec_mini --top 100
```

### REST API (thin server)

JATE now ships a thin JSON API server on top of the core extraction API.

Install server dependencies:

```bash
pip install "jate[server]"
```

Start the server:

```bash
jate-api
```

Or with Python module execution:

```bash
python -m uvicorn jate.server:app --host 0.0.0.0 --port 8000
```

Extract terms over HTTP:

```bash
curl --header "Content-Type: application/json" \
    --request POST \
    --data '{"text":"text to process","algorithm":"cvalue"}' \
    http://localhost:8000/jate/api/v1/extract
```

Health checks:

```bash
curl http://localhost:8000/health/live
curl http://localhost:8000/health/ready
```

### Docker / Containerization

Build the image from repo root:

```bash
docker build -t jate:latest .
```

Run modes:

```bash
# 1) CLI mode (default)
docker run --rm jate:latest jate extract "local post office" --algorithm cvalue --top 20

# Corpus mode with local volume mount (recommended for local files)
docker run --rm -v "/path/to/local/folder:/data" jate:latest \
    jate corpus /data --algorithm cvalue --top 20

# 2) API mode (explicit)
docker run --rm -d -p 8000:8000 --name jate-api-test jate:latest jate-api

# 3) Interactive mode with local corpus volume
docker run -it --rm -v "$(pwd)/path/to/docs:/data" jate:latest sh
# inside container:
# jate corpus /data --algorithm tfidf --output csv
```

Test API endpoints (when running API mode):

```bash
# Liveness
curl -s http://localhost:8000/health/live

# Readiness (validates spaCy model availability)
curl -s http://localhost:8000/health/ready

# Capabilities
curl -s http://localhost:8000/jate/api/v1/capabilities

# Extract terms
curl -s -X POST http://localhost:8000/jate/api/v1/extract \
    -H "Content-Type: application/json" \
    -d '{"text":"Russia says its consulate in Isfahan, Iran was damaged over the weekend as a result of strikes on the local governor'\''s office.","algorithm":"cvalue","top":6}'
```

Stop API mode container:

```bash
docker stop jate-api-test
```

Run dual-mode Docker smoke checks (CLI + API) with one build:

```bash
bash scripts/docker_smoke_test.sh
```

Expected extract response shape:

```json
{
    "algorithm": "cvalue",
    "extractor": "pos_pattern",
    "model": "en_core_web_sm",
    "top": 6,
    "terms": [
        {
            "rank": 1,
            "term": "local governors office",
            "score": 1.6323,
            "frequency": 1,
            "surface_forms": ["local governors office"],
            "metadata": {}
        }
    ]
}
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

Multi-algorithm comparison is available via `jate.compare()`, which also supports ensemble voting via reciprocal rank fusion (`voting=True`).

## Candidate extractors

| Extractor | Description |
|-----------|-------------|
| `pos_pattern` (default) | Regex over Universal POS tags (default: `(ADJ\|NOUN\|PROPN)*(NOUN\|PROPN)`, configurable via pattern presets) |
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

## spaCy Integration

JATE can be used as a native spaCy pipeline component, reusing the NLP processing already done by spaCy (no double computation):

```python
import spacy
import jate

nlp = spacy.load("en_core_web_sm")
nlp.add_pipe("jate", config={"algorithm": "cvalue"})

doc = nlp("Machine learning and neural networks improve deep learning models.")

for term in doc._.terms:
    surface = doc.text[term.spans[0].start:term.spans[0].end] if term.spans else ""
    print(f"{term.string:30s}  score={term.score:.4f}  at {surface!r}")
```

Configuration options:

| Option | Default | Description |
|--------|---------|-------------|
| `algorithm` | `"cvalue"` | Any of the 13 algorithms |
| `pattern` | `"default"` | POS pattern preset (`default`, `genia`, `acl_rdtec`) |
| `min_frequency` | `1` | Minimum term frequency |
| `min_words` | `1` | Minimum words per term |
| `max_words` | `None` | Maximum words per term |
| `reference_frequency_file` | `None` | Path to reference corpus (for weirdness, glossex, termex) |

**Important notes:**
- One algorithm per pipeline (for multi-algorithm comparison, use `jate.compare()`)
- All algorithms will warn about single-document mode — they are corpus-level methods designed for multi-document extraction. Results on single documents are functional but weaker.
- TF-IDF will return empty results on single documents (IDF = 0).

Try the demo: `python examples/spacy_demo.py`

## Benchmarks

JATE is evaluated on 5 standard ATE datasets using P@K (precision at top-K ranked terms). Best algorithm per dataset at P@100:

| Dataset | Domain | Docs | Gold terms | Best P@100 | Algorithm |
|---------|--------|------|-----------|------------|-----------|
| GENIA | Biomedical | 2,000 | 35,298 | 0.79 | attf |
| ACL RD-TEC 2.0 | Comp. linguistics | 1,758 | 5,031 | 0.73 | ttf |
| ACTER v1.5 | Multi-domain | 241 | 5,329 | 0.61 | basic |
| CoastTerm | Coastal science | 2,004 | 4,316 | 0.59 | combobasic |

Full results with P@100 through P@10,000 for all 13 algorithms, methodology notes, and comparison with published baselines: **[benchmark results](docs/benchmark-results.md)**.

## Contributing

Please read the [contributing guide](docs/contributing.md) first for development setup, branch workflow, and agentic coding harness details. JATE is in active development and we welcome contributions. Here's how you can get involved:

- **Browse open issues** — check the [feature roadmap](https://github.com/ziqizhang/jate/issues?q=is%3Aissue+is%3Aopen+label%3Aenhancement) for planned enhancements
- **Good first issues** — look for issues labelled [`good first issue`](https://github.com/ziqizhang/jate/issues?q=is%3Aissue+is%3Aopen+label%3A%22good+first+issue%22) if you're new to the project
- **Feature requests** — [open an issue](https://github.com/ziqizhang/jate/issues/new?template=feature_request.yml) to suggest new features
- **Bug reports** — [report here](https://github.com/ziqizhang/jate/issues/new?template=bug_report.yml)
- **Star the repo** to follow progress

## Background

JATE was originally developed as part of research at the University of Sheffield, with publications in venues including ACM TKDD and the Semantic Web Journal. The library has been used in academic and industry settings for terminology extraction, ontology learning, and knowledge graph construction.

**Key publications:**
- Zhang, Z., Gao, J., Ciravegna, F. (2018). *SemRe-Rank: Improving Automatic Term Extraction By Incorporating Semantic Relatedness With Personalised PageRank.* ACM TKDD.
- Zhang, Z., Iria, J., Brewster, C., Ciravegna, F. (2008). *A Comparative Evaluation of Term Recognition Algorithms.* LREC.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.

---
