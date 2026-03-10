# Corpus Management

For meaningful term extraction, you typically need multiple documents so that corpus-level statistics (document frequency, co-occurrence) are informative. JATE provides a `Corpus` class for managing document collections.

## Creating a corpus

### From text strings

```python
import jate

result = jate.extract_corpus(
    ["First document...", "Second document...", "Third document..."],
    algorithm="cvalue",
)
```

### From files

```python
result = jate.extract_corpus(
    ["paper1.txt", "paper2.txt"],
    algorithm="tfidf",
)
```

File paths are auto-detected — if all strings in the list are existing file paths, they are loaded as files. Otherwise they are treated as raw text.

### From a directory

```python
result = jate.extract_corpus("path/to/corpus/", algorithm="cvalue")
```

A single string that is an existing directory loads all `.txt` files from it.

## Storage backends

### In-memory (default)

Statistics are computed in memory using Python dicts. Fast, but lost when the session ends.

```python
result = jate.extract_corpus(docs, algorithm="cvalue")
```

### SQLite

For large corpora or when you want to persist statistics across sessions, use SQLite:

```python
result = jate.extract_corpus(
    "corpus/",
    algorithm="cvalue",
    db_path="my_corpus.db",
)
```

The database file stores documents, term frequencies, and co-occurrences. Subsequent runs with the same `db_path` reuse the existing data.

## Using the Corpus class directly

For more control, use the `Corpus` class:

```python
from jate import Corpus

# In-memory
corpus = Corpus()

# SQLite-backed
corpus = Corpus(db_path="corpus.db")

# Add documents
corpus.add_documents(["Doc 1...", "Doc 2..."])
corpus.add_documents(["paper.txt"])
corpus.add_directory("more_docs/")

# Access components
corpus.documents   # list of Document objects
corpus.nlp         # the NLP backend (SpacyBackend)
corpus.store       # the CorpusStore (Memory or SQLite)
```

The `Corpus` object provides access to documents, the NLP backend, and a store for candidate extraction:

```python
from jate import CValue, PosPatternExtractor, TermFrequency, Containment, TermComponentIndex

extractor = PosPatternExtractor()
candidates = extractor.extract(corpus.documents, corpus.nlp, corpus.store)

# Build features from candidates
term_freq = TermFrequency.build(candidates, total_docs=len(corpus.documents))
tci = TermComponentIndex.build(candidates)
containment = Containment.build(candidates, tci)

algo = CValue()
result = algo.score(candidates, term_freq, containment=containment)
```

## Corpus statistics

The corpus store tracks:

| Statistic | Method | Description |
|-----------|--------|-------------|
| Term frequency | `get_term_frequency(term)` | Total occurrences across all documents |
| Document frequency | `get_document_frequency(term)` | Number of documents containing the term |
| Co-occurrence | `get_cooccurrences(a, b)` | Number of documents containing both terms |
| Corpus total | `get_corpus_total()` | Sum of all term frequencies |
| Containing terms | `get_containing_terms(term)` | Longer terms that contain this term as a substring |

All lookups are case-insensitive and operate on the normalised (lemmatised) form.
