# Sentence-Level Context for Chi-Square and NC-Value

**Date:** 2026-03-09
**Status:** Approved

## Problem

The Python JATE implementation uses document-level co-occurrence for Chi-Square and NC-Value. The original papers and the Java JATE implementation use finer-grained context:

- **Chi-Square** (Matsuo & Ishizuka 2003): sentence-level co-occurrence, where each sentence is a "basket"
- **NC-Value** (Frantzi et al. 2000): immediate-adjacency context — the single word directly before/after each term occurrence

## Approach

**Separate feature layer (Approach A).** A new `ContextIndex` object is built from candidates as a post-extraction step. It holds sentence-level co-occurrences and adjacency data. Algorithms that need it accept it as an optional parameter. The `CorpusStore` stays document-level only.

```
Extractor -> Candidates (with sentence IDs per position)
     |
ContextIndex.build(candidates, documents, nlp_backend)
     |
Algorithm.score(candidates, corpus_store, context_index=...)
```

Sentence-level only for now. The architecture supports adding sliding-window contexts later without rework.

## Data Model Changes

Candidate position tuples gain a sentence index:

```python
# Before
doc_positions: dict[str, list[tuple[int, int]]]  # doc_id -> [(char_start, char_end)]

# After
doc_positions: dict[str, list[tuple[int, int, int]]]  # doc_id -> [(char_start, char_end, sentence_idx)]
```

`sentence_idx` is the 0-based sentence index within the document. Extractors that don't do sentence splitting use `-1` as a sentinel. No changes to `Term`, `Document`, or `TermExtractionResult`.

`Candidate.add_position()` signature:

```python
def add_position(self, doc_id: str, start: int, end: int,
                 sentence_idx: int = -1, surface: str = "") -> None:
```

## Extractor Changes

All three extractors (POS-pattern, N-gram, NounPhrase) split documents into sentences before processing:

```
for doc in documents:
    sentences = nlp_backend.sentence_split(doc.text)
    for sent_idx, sentence in enumerate(sentences):
        # extract candidates from this sentence
        # add_position(doc.doc_id, char_start, char_end, sent_idx, surface=...)
```

Character offsets remain relative to the full document text (not the sentence), so downstream code that doesn't care about sentences is unaffected.

## ContextIndex

New class providing sentence-level statistics:

```python
class ContextIndex:
    @classmethod
    def build(cls, candidates: list[Candidate], documents: list[Document],
              nlp_backend: NLPBackend | None = None) -> ContextIndex: ...

    def get_sentence_cooccurrences(self, term_a: str, term_b: str) -> int:
        """Number of sentences in which both terms appear."""

    def get_adjacent_words(self, term: str) -> dict[str, int]:
        """Words immediately before/after each occurrence of term.
        Returns {word: count} for NC-Value context weighting."""

    def get_context_term_total(self, term: str) -> int:
        """Total number of distinct terms in sentences where term appears.
        Used by Chi-Square's n_w parameter."""
```

Internal data structures:
- `_sent_cooc: dict[tuple[str, str], int]` — sentence co-occurrence counts (keys sorted)
- `_adjacent: dict[str, dict[str, int]]` — term -> {adjacent_word: count}
- `_context_totals: dict[str, int]` — term -> total terms in its sentence contexts

`build()` works by:
1. Grouping candidate positions by `(doc_id, sentence_idx)` to find which terms appear in each sentence
2. Computing pairwise co-occurrence per sentence
3. For adjacency: using character offsets + document text + tokenisation to find immediately adjacent words

If `nlp_backend` is not provided, adjacency data is empty and NC-Value falls back to document-level co-occurrence.

## Algorithm Changes

**Base class** `Algorithm.score()` gains an optional parameter:

```python
def score(self, candidates: list[Candidate], corpus_store: CorpusStore,
          context_index: ContextIndex | None = None) -> TermExtractionResult:
```

All 13 algorithms get the parameter; only Chi-Square and NC-Value use it.

**Chi-Square:**
- Uses `context_index.get_sentence_cooccurrences()` instead of `corpus_store.get_cooccurrences()`
- Uses `context_index.get_context_term_total()` for `n_w`
- Constructor params `context_term_totals` and `frequent_terms` kept for backwards compatibility

**NC-Value:**
- Uses `context_index.get_adjacent_words()` to compute context word weights per Frantzi formula: `weight(w) = t(w) / n`
- Context score: `sum(weight(w) * freq(w, term))` for each context word `w`
- Falls back to document-level co-occurrence without `ContextIndex`

## Integration

The pipeline builds `ContextIndex` automatically when candidates have sentence data:

```python
candidates = extractor.extract(documents)
corpus_store.index_candidates(candidates)

has_sentences = any(
    sent_idx >= 0
    for c in candidates
    for positions in c.doc_positions.values()
    for _, _, sent_idx in positions
)
context_index = ContextIndex.build(candidates, documents, nlp_backend) if has_sentences else None

result = algorithm.score(candidates, corpus_store, context_index=context_index)
```

Users never need to build `ContextIndex` manually unless they want to.

## Testing

1. Unit tests for `ContextIndex.build()` — hand-crafted candidates with known sentence IDs
2. Unit tests for Chi-Square with `ContextIndex` — verify sentence co-occurrences used
3. Unit tests for NC-Value with `ContextIndex` — verify adjacency-based context weights match Frantzi formula
4. Backwards compatibility — all existing tests pass unchanged (no `ContextIndex` provided)
5. Integration test — end-to-end extraction with sentence splitting

## What Stays the Same

- All existing tests
- Document-level co-occurrence in CorpusStore
- The 11 algorithms that don't use context
- The `CorpusStore` protocol (no changes)
