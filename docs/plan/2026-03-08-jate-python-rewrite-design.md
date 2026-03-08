# JATE — "Just Automatic Term Extraction"

## Python Rewrite Design Document

**Date:** 2026-03-08
**Author:** Ziqi Zhang
**Repo:** github.com/ziqizhang/jate
**Package:** `jate` on PyPI
**Python:** 3.10+
**License:** LGPL 3.0

---

## Overview

JATE is being rewritten from Java/Solr to Python to become the definitive open-source library for Automatic Term Extraction. The Python ecosystem currently has no actively maintained ATE library — PyATE (archived 2023) was the closest, with only 4 algorithms. JATE will ship 12+ classical algorithms, modern neural methods, LLM integration, and an agentic orchestration system.

## Migration Strategy

1. Tag current Java codebase as `v2.0-beta.11-java`
2. Create `legacy/java` branch from current `master`
3. Rewrite `master` with the new Python project
4. README links to `legacy/java` for users of the old version

## Architecture

### 5-Stage Pipeline

```
Documents/Text → Preprocessing → Candidate Extraction → Scoring/Ranking → Output
                                                              ↑
                                                    Corpus Statistics
                                                      (SQLite)
```

**Stage 1 — Preprocessing:**
- Pluggable NLP backend (spaCy default, interface for stanza/NLTK/custom)
- Tokenization, sentence splitting, POS tagging, lemmatization
- Document format handling (PDF, DOCX, HTML, plain text)

**Stage 2 — Candidate Extraction:**
- POS pattern matching (configurable regex over POS tags, e.g., `(ADJ)*(NOUN)+`)
- N-gram extraction (configurable n)
- Noun phrase chunking (from spaCy)
- Pluggable: users can register custom candidate extractors

**Stage 3 — Corpus Statistics Store:**
- SQLite-backed by default, in-memory option for small jobs
- Stores: term frequencies, document frequencies, co-occurrence counts, reference corpus stats
- Incremental — add documents to an existing corpus without reprocessing

**Stage 4 — Scoring/Ranking:**
- Each algorithm is a standalone class implementing a `score()` interface
- Multiple algorithms can run on the same corpus and be compared
- Results are composable (ensemble/voting across algorithms)

**Stage 5 — Output:**
- Returns ranked list of `Term` objects (term string, score, frequency, metadata)
- Export to CSV, JSON, pandas DataFrame
- Filtering by score threshold, frequency threshold, term length

## API Design

### Layer 1 — One-liner (practitioners)

```python
import jate

# Single document
terms = jate.extract("Your long document text here...")

# Corpus
terms = jate.extract_corpus(["doc1.txt", "doc2.txt"], algorithm="cvalue")

# Compare algorithms
results = jate.compare(corpus, algorithms=["cvalue", "tfidf", "weirdness"])
```

### Layer 2 — Pipeline (power users)

```python
from jate import Corpus, CValue, PosPatternExtractor

corpus = Corpus("my_corpus.db")  # SQLite-backed
corpus.add_documents(["doc1.txt", "doc2.txt"])
corpus.add_directory("/path/to/pdfs/")

extractor = PosPatternExtractor(pattern="(ADJ)*(NOUN)+")
candidates = extractor.extract(corpus)

scorer = CValue()
terms = scorer.score(candidates, corpus)
terms.to_dataframe()
terms.to_json("output.json")
```

### Layer 3 — spaCy integration

```python
import spacy
nlp = spacy.load("en_core_web_sm")
nlp.add_pipe("jate", config={"algorithm": "cvalue"})

doc = nlp("Your text here")
print(doc._.terms)
```

### Layer 4 — CLI

```bash
jate extract myfile.txt --algorithm cvalue
jate corpus create mycorpus /path/to/docs/
jate corpus run mycorpus --algorithm all --output results.csv
jate demo
jate benchmark --dataset genia --algorithms cvalue,tfidf,weirdness
```

## Algorithms

### Classical (ported from JATE)

| Algorithm | Type | Description |
|-----------|------|-------------|
| TFIDF | Statistical | Term frequency-inverse document frequency |
| C-Value | Hybrid | Nested multi-word term extraction |
| NC-Value | Hybrid | C-Value extended with context words |
| RIDF | Statistical | Residual IDF — deviation from expected frequency |
| ATTF | Statistical | Average total term frequency |
| TTF | Statistical | Total term frequency |
| Basic | Statistical | Simple frequency-based baseline |
| ComboBasic | Statistical | Combined frequency measures |
| ChiSquare | Statistical | Chi-square independence test |
| RAKE | Statistical | Rapid automatic keyword extraction |
| Weirdness | Contrastive | Domain vs. reference corpus comparison |
| TermEx | Hybrid | Domain pertinence + context |
| GlossEx | Hybrid | Domain specificity using glossary comparison |

### Neural / Modern (new)

| Algorithm | Type | Source |
|-----------|------|--------|
| BERT sequence labeling | Neural | ATE as token classification (NER-style) |
| KeyBERT-style embedding | Neural | Cosine similarity with document embeddings |
| LLM-augmented filtering | Neural/LLM | LLM validates/re-ranks candidate terms |
| Syntactic retrieval ATE | Neural/LLM | ACL 2025 — few-shot LLM with syntactic demos |

### Ensemble

Combine any subset of algorithms via weighted average, rank fusion, or majority voting.

## UI & Demo

Streamlit web app launched via `jate demo`:

- **Tab 1 — Try It:** Paste text or upload, pick algorithm(s), adjust params, results table + word cloud
- **Tab 2 — Corpus Mode:** Upload multiple files, build corpus, side-by-side algorithm comparison
- **Tab 3 — Benchmarks:** Pre-computed results, interactive charts, run-your-own benchmarks
- **Tab 4 — LLM Mode:** Classical extraction + LLM re-ranking, before/after comparison
- **Tab 5 — Agent Mode:** Live agent execution graph (Tier 3.5)

Also deployed to **Hugging Face Spaces** as a permanent live demo.

## Benchmarking

### Datasets (downloaded on-demand)

| Dataset | Domain | Language | Year | Key Feature |
|---------|--------|----------|------|-------------|
| GENIA | Biomedical | EN | Classic | Standard ATE benchmark |
| ACL RD-TEC | Comp. linguistics | EN | Classic | ACL terminology |
| ACTER v1.5 | Multi-domain (4) | EN, FR, NL | 2020 | Dominant multilingual benchmark |
| CoastTerm | Coastal science | EN | 2024 | IOB + semantic labels |
| CL-RuTerm3 | 6 domains | RU | 2024 | Nested term annotations |
| EUROPA | Legal (EU court) | 24 EU langs | 2024 | Massive multilingual |
| Few-TK | AI/ML papers | EN | 2024 | Typed keyphrases |
| AnnoCTR | Cybersecurity | EN | 2024 | MITRE ATT&CK linked |
| Hindi ATE | Education | HI | 2022/2025 | Low-resource language |
| RSDO5 | Multi-domain | SL | 2021 | Cross-lingual research |

### Evaluation

- Precision, Recall, F1 at various top-K cutoffs
- Average Precision (AP)
- Auto-generated comparison tables (markdown/HTML)
- Reproducibility: every run saves config + results, replayable via `jate benchmark --reproduce`

## Implementation Tiers

### Tier 1 — Core

1. 13 classical algorithms (12 from JATE + NC-Value)
2. 3 candidate extraction methods (POS patterns, n-grams, noun phrase chunking)
3. Pluggable NLP backend (spaCy default)
4. SQLite-backed corpus stats + in-memory fallback
5. One-liner + pipeline + CLI APIs
6. Output to DataFrame, CSV, JSON
7. PyPI package

### Tier 2 — Differentiation

8. Streamlit demo UI + Hugging Face Spaces deployment
9. spaCy pipeline integration
10. Benchmarking with 10 datasets
11. Published baseline results in README
12. Ensemble/voting across algorithms
13. Multilingual support (any spaCy language model)

### Tier 3 — Cutting Edge

14. BERT sequence labeling for ATE
15. KeyBERT-style embedding scoring
16. LLM-augmented filtering/re-ranking
17. Syntactic retrieval ATE (ACL 2025 method)
18. Nested term extraction
19. Reproducibility system (save/replay benchmark configs)

### Tier 3.5 — Agentic System (LangGraph)

20. **Agentic ATE Pipeline** — orchestrator coordinating specialist agents:
    - Corpus Analyzer Agent — profiles input corpus (domain, size, language)
    - Strategy Agent — selects extraction method and scoring algorithms
    - Extraction Agent — runs candidate extraction
    - Scoring Agent — runs selected algorithms (parallel execution)
    - LLM Validation Agent — filters/re-ranks using LLM
    - Quality Agent — evaluates results, decides if re-run needed
    - Report Agent — generates structured methodology + results report

21. **Agentic AutoATE** — automatic algorithm selection, parameter tuning, cross-validation, leaderboard generation

22. Agent Mode tab in Streamlit UI with live execution graph

### Tier 4 — Production Ready

23. Test suite >90% coverage, mypy strict
24. Structured logging + OpenTelemetry tracing
25. Async support for large corpora
26. Rate limiting / batching for LLM calls
27. Docker image
28. CI/CD (GitHub Actions: lint, test, build, publish to PyPI)
29. Versioned API with deprecation policy
30. Documentation site (MkDocs, GitHub Pages)
31. Domain-specific pre-tuned configs (biomedical, legal, cyber)
32. Reference corpus management (build/share contrastive corpora)
33. Plugin system for community-contributed algorithms
