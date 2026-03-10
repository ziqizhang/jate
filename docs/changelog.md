# Changelog

## 3.0.0 (unreleased)

Complete rewrite from Java to Python.

### Added

- 14 ATE algorithms: TFIDF, C-Value, NC-Value, Basic, ComboBasic, ATTF, TTF, RIDF, RAKE, Chi-Square, Weirdness, TermEx, GlossEx, plus ensemble Voting (reciprocal rank fusion)
- 3 candidate extractors: POS pattern, n-gram, noun phrase
- Lemmatisation-aware pipeline — normalised forms as canonical keys with surface form tracking
- In-memory and SQLite-backed corpus statistics stores
- Built-in evaluation (P/R/F1) against gold standard term lists
- Benchmark runner for comparing algorithms on datasets
- CLI with extract, corpus, compare, benchmark commands
- Export to pandas DataFrame, CSV, JSON
- spaCy-based NLP backend with Protocol interface for extensibility
- ACL RD-TEC Mini built-in dataset
- Feature system mirroring Java JATE: TermFrequency, WordFrequency, ReferenceFrequency, ContextFrequency, ContextWindow, TermComponentIndex, Containment, Cooccurrence, ChiSquareFrequentTerms — all formulas verified line-by-line against the Java source
- Sentence-level context via ContextFrequency — Chi-Square uses sentence co-occurrence and NC-Value uses adjacency-based context, aligning with original papers
- Configurable parallelism via `JATEConfig(max_workers=N)` — parallel co-occurrence computation, containment index building, and batch NLP processing with `nlp.pipe()`
- External reference corpus support via `JATEConfig(reference_frequency_file=...)` for Weirdness, GlossEx, and TermEx
- Multiple reference corpora support for TermEx (per-word best selection)
- Cooccurrence prefiltering via `JATEConfig(prefilter_min_ttf=..., prefilter_min_tcf=...)` to reduce problem space for Chi-Square

### Changed

- Renamed from "Java Automatic Term Extraction" to "Just Automatic Term Extraction"
- No longer requires Apache Solr — corpus statistics are self-contained
- Python 3.11+ required

### Previous versions

For the Java version history, see the [`legacy/java`](https://github.com/ziqizhang/jate/tree/legacy/java) branch.
