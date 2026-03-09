# Changelog

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

### Changed

- Renamed from "Java Automatic Term Extraction" to "Just Automatic Term Extraction"
- No longer requires Apache Solr — corpus statistics are self-contained
- Python 3.11+ required

### Previous versions

For the Java version history, see the [`legacy/java`](https://github.com/ziqizhang/jate/tree/legacy/java) branch.
