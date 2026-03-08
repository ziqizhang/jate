# JATE — Just Automatic Term Extraction

> **JATE is being completely rewritten.** The original Java/Solr library (84+ stars, 10+ algorithms) is being rebuilt from the ground up in Python. The goal: make automatic term extraction as easy as `pip install jate`.

## What's happening?

JATE has been a trusted open-source tool for automatic term extraction since 2014, used by researchers and practitioners across NLP, biomedical text mining, and knowledge engineering. But the Java/Solr dependency made it hard to set up and integrate into modern Python-based NLP workflows.

**We're fixing that.** Over the coming weeks, JATE is being revamped into a modern Python library — lightweight, pip-installable, and designed to work seamlessly with spaCy, pandas, and the broader Python ecosystem.

This rebuild is being developed with the help of **agentic coding tools**, combining human expertise in NLP research with AI-assisted development to accelerate the process.

> Looking for the original Java version? It's preserved on the [`legacy/java`](https://github.com/ziqizhang/jate/tree/legacy/java) branch.

## Sneak peek

**Dead-simple API:**

```python
import jate

# One line — that's it
terms = jate.extract("Your document text here...")

# Corpus-level extraction with any algorithm
terms = jate.extract_corpus(docs, algorithm="cvalue")

# Compare algorithms side by side
results = jate.compare(corpus, algorithms=["cvalue", "tfidf", "weirdness"])
```

**spaCy integration:**

```python
import spacy
nlp = spacy.load("en_core_web_sm")
nlp.add_pipe("jate", config={"algorithm": "cvalue"})

doc = nlp("Your text here")
print(doc._.terms)
```

**CLI:**

```bash
jate extract paper.pdf --algorithm cvalue
jate benchmark --dataset genia --algorithms all
jate demo  # launches interactive web UI
```

## Planned features

- **13+ ATE algorithms** in one library — C-Value, NC-Value, TFIDF, RIDF, RAKE, ChiSquare, Weirdness, TermEx, GlossEx, and more
- **Neural methods** — BERT-based sequence labeling, embedding-based scoring
- **LLM-augmented extraction** — optional LLM re-ranking and validation of extracted terms
- **Corpus-level analysis** — SQLite-backed statistics that scale without external services
- **Built-in benchmarking** — 10 standard datasets (GENIA, ACTER, ACL RD-TEC, CoastTerm, and more) with one-command evaluation
- **Interactive web demo** — Streamlit UI for trying algorithms, comparing results, and visualizing terms
- **Multilingual** — works with any spaCy language model
- **Agentic pipeline** — LangGraph-powered orchestration that automatically selects the best algorithm and parameters for your corpus
- **Production-ready** — typed, tested, CI/CD, Docker, and published on PyPI

## Get involved

JATE is in active development. We'd love your input:

- **Feature requests:** [Open an issue](https://github.com/ziqizhang/jate/issues/new?template=feature_request.yml) — tell us what you need
- **Bug reports:** [Report here](https://github.com/ziqizhang/jate/issues/new?template=bug_report.yml)
- **Star the repo** to follow progress

## Background

JATE was originally developed as part of research at the University of Sheffield, with publications in venues including ACM TKDD and the Semantic Web Journal. The library has been used in academic and industry settings for terminology extraction, ontology learning, and knowledge graph construction.

**Key publications:**
- Zhang, Z., Gao, J., Ciravegna, F. (2018). *SemRe-Rank: Improving Automatic Term Extraction By Incorporating Semantic Relatedness With Personalised PageRank.* ACM TKDD.
- Zhang, Z., Iria, J., Brewster, C., Ciravegna, F. (2008). *A Comparative Evaluation of Term Recognition Algorithms.* LREC.

## License

Apache 2.0 — see [LICENSE](LICENSE) for details.

---

*Built with research expertise and agentic coding tools.*
