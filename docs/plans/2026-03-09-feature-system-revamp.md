# Feature System Revamp — Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Replace ad-hoc Python feature building with a 1:1 mirror of the Java JATE feature system, fixing all 7 algorithm discrepancies.

**Architecture:** Create a `features.py` module with 9 feature classes mirroring Java's `uk.ac.shef.dcs.jate.feature` package. Replace `MemoryCorpusStore` with `TermFrequency` as the primary term-level feature. Update all 13 algorithms to consume proper features. Add `Voting` ensemble algorithm.

**Tech Stack:** Python 3.11+, dataclasses, `re` module for word-boundary regex, spaCy for tokenization.

**Java → Python 1:1 mapping:**

| Java Class | Python Class | Module |
|---|---|---|
| `FrequencyTermBased` (type=0) | `TermFrequency` | `features.py` |
| `FrequencyTermBased` (type=1) | `WordFrequency` | `features.py` |
| `FrequencyTermBased` (reference) | `ReferenceFrequency` | `features.py` |
| `FrequencyCtxBased` | `ContextFrequency` | `features.py` |
| `ContextWindow` | `ContextWindow` | `features.py` |
| `TermComponentIndex` | `TermComponentIndex` | `features.py` |
| `Containment` + reverse | `Containment` | `features.py` |
| `Cooccurrence` | `Cooccurrence` | `features.py` |
| `ChiSquareFrequentTerms` | `ChiSquareFrequentTerms` | `features.py` |
| `Voting` | `Voting` | `algorithms/voting.py` |

**What gets removed:**
- `src/jate/context.py` — replaced by `ContextFrequency` (adjacent words move there too)
- `src/jate/store/memory_store.py` — replaced by `TermFrequency`
- `src/jate/store/sqlite_store.py` — replaced by `TermFrequency` (SQLite backing becomes optional param)
- `src/jate/store/__init__.py` — removed
- `src/jate/protocols.py` `CorpusStore` protocol — replaced by `TermFrequency` type
- Ad-hoc builders in `api.py`: `_build_word_frequencies`, `_build_term_component_index`, `_build_frequent_terms`, `_build_reference_data`, `_build_context_index`
- Co-occurrence code in `MemoryCorpusStore.index_candidates()`

**What stays unchanged:**
- `models.py` (Term, Candidate, Document, TermExtractionResult)
- Algorithm class hierarchy (`base.py`, all 13 algorithm files)
- Extractors (`pos_pattern.py`, `ngram.py`, `noun_phrase.py`)
- `SpacyBackend`, `DocumentLoader`
- `parallel.py` (parallel_map, split_range)
- Public API signatures (extract, extract_corpus, compare)

---

### Task 1: TermFrequency (replaces MemoryCorpusStore)

**Files:**
- Modify: `src/jate/features.py`
- Create: `tests/test_features.py`

**Context:** Java's `FrequencyTermBased` stores: `term2TTF` (term → total frequency), `term2FID` (term → {docId → freq}), `corpusTotal`, `totalDocs`. It also provides `getTTFNorm(t) = TTF / (corpusTotal + 1)`. Our `MemoryCorpusStore` has the same data but a different name. We rename and reshape it as a feature.

**Step 1: Write tests for TermFrequency**

```python
# tests/test_features.py
import pytest
from jate.features import TermFrequency
from jate.models import Candidate, Document


class TestTermFrequency:
    def _make_candidates(self):
        """Two candidates across 2 docs."""
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 50, 64, sentence_idx=1)
        c1.add_position("doc2", 10, 24, sentence_idx=0)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 20, 33, sentence_idx=0)
        return [c1, c2]

    def test_build_from_candidates(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        assert tf.get_ttf("neural network") == 3
        assert tf.get_ttf("deep learning") == 1
        assert tf.get_df("neural network") == 2
        assert tf.get_df("deep learning") == 1
        assert tf.total_docs == 2
        # corpus_total = sum of all TTF = 3 + 1 = 4
        assert tf.corpus_total == 4

    def test_get_ttf_norm(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        # Java: getTTFNorm = TTF / (corpusTotal + 1) = 3 / 5
        assert tf.get_ttf_norm("neural network") == pytest.approx(3 / 5)

    def test_get_doc_freq_map(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        m = tf.get_doc_freq_map("neural network")
        assert m == {"doc1": 2, "doc2": 1}

    def test_unknown_term_returns_zero(self):
        tf = TermFrequency.build([], total_docs=0)
        assert tf.get_ttf("nonexistent") == 0
        assert tf.get_df("nonexistent") == 0
        assert tf.get_ttf_norm("nonexistent") == 0.0
```

Run: `poetry run pytest tests/test_features.py::TestTermFrequency -v`
Expected: FAIL (TermFrequency doesn't exist yet)

**Step 2: Implement TermFrequency**

```python
# In src/jate/features.py — add at the top, after existing imports
from __future__ import annotations

import re
from collections import defaultdict
from dataclasses import dataclass, field
from itertools import combinations
from typing import TYPE_CHECKING

from jate.models import Candidate, Document
from jate.parallel import parallel_map, split_range

if TYPE_CHECKING:
    from jate.protocols import NLPBackend


@dataclass
class TermFrequency:
    """Term-level corpus frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` (feature type 0).

    Attributes
    ----------
    term2ttf : dict[str, int]
        Term → total term frequency across corpus.
    term2fid : dict[str, dict[str, int]]
        Term → {doc_id → frequency in that document}.
    corpus_total : int
        Sum of all TTF values.
    total_docs : int
        Total number of documents.
    """

    term2ttf: dict[str, int] = field(default_factory=dict)
    term2fid: dict[str, dict[str, int]] = field(default_factory=dict)
    corpus_total: int = 0
    total_docs: int = 0

    @classmethod
    def build(cls, candidates: list[Candidate], total_docs: int) -> TermFrequency:
        term2ttf: dict[str, int] = defaultdict(int)
        term2fid: dict[str, dict[str, int]] = defaultdict(dict)

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                freq = len(positions)
                term2ttf[term] += freq
                term2fid[term][doc_id] = term2fid[term].get(doc_id, 0) + freq

        corpus_total = sum(term2ttf.values())
        return cls(
            term2ttf=dict(term2ttf),
            term2fid=dict(term2fid),
            corpus_total=corpus_total,
            total_docs=total_docs,
        )

    def get_ttf(self, term: str) -> int:
        return self.term2ttf.get(term.lower(), 0)

    def get_df(self, term: str) -> int:
        return len(self.term2fid.get(term.lower(), {}))

    def get_ttf_norm(self, term: str) -> float:
        """Java: getTTFNorm = TTF / (corpusTotal + 1)."""
        ttf = self.get_ttf(term)
        return ttf / (self.corpus_total + 1)

    def get_doc_freq_map(self, term: str) -> dict[str, int]:
        return dict(self.term2fid.get(term.lower(), {}))
```

Run: `poetry run pytest tests/test_features.py::TestTermFrequency -v`
Expected: PASS

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add TermFrequency feature (mirrors Java FrequencyTermBased type=0)"
```

---

### Task 2: WordFrequency (Java FrequencyTermBased type=1)

**Files:**
- Modify: `src/jate/features.py`
- Modify: `tests/test_features.py`

**Context:** Java builds word-level frequencies by tokenizing all documents and counting every unigram. The current Python `_build_word_frequencies()` in api.py is wrong — it queries `store.get_term_frequency(word)` which only returns a count if the word is a standalone candidate term. We need to count actual token occurrences across all documents.

**Step 1: Write tests for WordFrequency**

```python
class TestWordFrequency:
    def test_build_from_documents(self):
        docs = [
            Document(doc_id="d1", content="neural network for deep learning"),
            Document(doc_id="d2", content="neural network analysis"),
        ]
        # Simple whitespace tokenizer for testing
        def tokenize(text):
            return text.lower().split()

        wf = WordFrequency.build(docs, tokenize_fn=tokenize)
        assert wf.get_ttf("neural") == 2
        assert wf.get_ttf("network") == 2
        assert wf.get_ttf("for") == 1
        assert wf.get_ttf("deep") == 1
        assert wf.get_ttf("learning") == 1
        assert wf.get_ttf("analysis") == 1
        assert wf.get_ttf("nonexistent") == 0
        assert wf.corpus_total == 8  # 5 + 3

    def test_build_with_spacy_tokenizer(self):
        """WordFrequency should accept any callable tokenizer."""
        docs = [Document(doc_id="d1", content="Machine learning works.")]
        wf = WordFrequency.build(docs, tokenize_fn=lambda t: t.lower().split())
        assert wf.get_ttf("machine") == 1
        assert wf.get_ttf("learning") == 1
        assert wf.get_ttf("works.") == 1  # basic split doesn't strip punctuation
```

Run: `poetry run pytest tests/test_features.py::TestWordFrequency -v`
Expected: FAIL

**Step 2: Implement WordFrequency**

```python
@dataclass
class WordFrequency:
    """Word-level (unigram) corpus frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` (feature type 1).

    Built by tokenizing every document and counting individual word occurrences.
    Unlike TermFrequency, this counts ALL tokens, not just candidate terms.
    """

    word2ttf: dict[str, int] = field(default_factory=dict)
    word2fid: dict[str, dict[str, int]] = field(default_factory=dict)
    corpus_total: int = 0

    @classmethod
    def build(
        cls,
        documents: list[Document],
        tokenize_fn: callable,
    ) -> WordFrequency:
        word2ttf: dict[str, int] = defaultdict(int)
        word2fid: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))

        for doc in documents:
            tokens = tokenize_fn(doc.content)
            for token in tokens:
                word = token.lower()
                word2ttf[word] += 1
                word2fid[word][doc.doc_id] += 1

        corpus_total = sum(word2ttf.values())
        return cls(
            word2ttf=dict(word2ttf),
            word2fid={k: dict(v) for k, v in word2fid.items()},
            corpus_total=corpus_total,
        )

    def get_ttf(self, word: str) -> int:
        return self.word2ttf.get(word.lower(), 0)

    def get_df(self, word: str) -> int:
        return len(self.word2fid.get(word.lower(), {}))

    def get_ttf_norm(self, word: str) -> float:
        """Java: getTTFNorm = TTF / (corpusTotal + 1)."""
        ttf = self.get_ttf(word)
        return ttf / (self.corpus_total + 1)
```

Run: `poetry run pytest tests/test_features.py::TestWordFrequency -v`
Expected: PASS

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add WordFrequency feature (mirrors Java FrequencyTermBased type=1)"
```

---

### Task 3: ReferenceFrequency (Java FrequencyTermBased from file)

**Files:**
- Modify: `src/jate/features.py`
- Modify: `tests/test_features.py`

**Context:** Java loads reference corpus word frequencies from a file via `TTFReferenceFeatureFileBuilder`. Currently Python builds a self-referencing fallback from the target corpus itself. We need a proper feature class that can be built from either an external file or from the corpus as fallback. Java's `ReferenceBased` also provides `matchOrdersOfMagnitude` and `nullWordProbInReference` — these stay in `_reference_utils.py` but ReferenceFrequency holds the data.

**Step 1: Write tests**

```python
class TestReferenceFrequency:
    def test_build_from_dict(self):
        data = {"neural": 100, "network": 80, "deep": 50}
        rf = ReferenceFrequency(word2ttf=data, corpus_total=1000)
        assert rf.get_ttf("neural") == 100
        assert rf.get_ttf("unknown") == 0
        assert rf.corpus_total == 1000
        assert rf.null_prob == pytest.approx(50 / 1000)  # min_freq / total

    def test_build_as_self_reference(self):
        """When no external ref corpus, build from WordFrequency."""
        docs = [Document(doc_id="d1", content="neural network deep learning")]
        wf = WordFrequency.build(docs, tokenize_fn=lambda t: t.lower().split())
        rf = ReferenceFrequency.from_word_frequency(wf)
        assert rf.get_ttf("neural") == 1
        assert rf.corpus_total == 4

    def test_null_prob_excludes_zero(self):
        rf = ReferenceFrequency(word2ttf={"a": 10, "b": 0}, corpus_total=100)
        # null_prob = min of non-zero freqs / total = 10 / 100
        assert rf.null_prob == pytest.approx(0.1)
```

**Step 2: Implement**

```python
@dataclass
class ReferenceFrequency:
    """Reference corpus word frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` loaded from a reference file.
    """

    word2ttf: dict[str, int] = field(default_factory=dict)
    corpus_total: int = 0

    @property
    def null_prob(self) -> float:
        """Minimum non-zero probability. Java: setNullWordProbInReference."""
        nonzero = [v for v in self.word2ttf.values() if v > 0]
        if nonzero and self.corpus_total > 0:
            return min(nonzero) / self.corpus_total
        return 0.1

    def get_ttf(self, word: str) -> int:
        return self.word2ttf.get(word.lower(), 0)

    def get_ttf_norm(self, word: str) -> float:
        return self.get_ttf(word) / (self.corpus_total + 1)

    @classmethod
    def from_word_frequency(cls, wf: WordFrequency) -> ReferenceFrequency:
        """Build self-reference from target corpus word frequencies."""
        # Filter out zero-frequency words
        filtered = {k: v for k, v in wf.word2ttf.items() if v > 0}
        total = sum(filtered.values()) if filtered else 1
        return cls(word2ttf=filtered, corpus_total=total)
```

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add ReferenceFrequency feature (mirrors Java FrequencyTermBased ref)"
```

---

### Task 4: ContextWindow + ContextFrequency (Java FrequencyCtxBased)

**Files:**
- Modify: `src/jate/features.py`
- Modify: `tests/test_features.py`

**Context:** This is the most critical new feature. Java's `FrequencyCtxBased` stores:
- `ctx2TTF`: context → total term occurrence count in that context
- `ctx2TFIC`: context → {term → freq in context}
- `term2Ctx`: term → set of contexts where it appears

In Java, `ctx2TTF` is incremented by 1 for EACH candidate term occurrence in a sentence. So if a sentence has 3 occurrences of candidate terms (including duplicates), `ctx2TTF = 3`.

We build this from `Candidate.doc_positions` which gives us `(start, end, sentence_idx)` per occurrence.

Also includes adjacent word tracking (currently in `ContextIndex`) for NC-Value.

**Step 1: Write tests**

```python
from jate.features import ContextWindow, ContextFrequency


class TestContextFrequency:
    def _make_data(self):
        """3 candidates across 2 sentences in 1 doc."""
        # Sentence 0: "neural network" appears twice, "deep learning" once → ctx2TTF = 3
        # Sentence 1: "neural network" once, "machine learning" once → ctx2TTF = 2
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 30, 44, sentence_idx=0)
        c1.add_position("doc1", 100, 114, sentence_idx=1)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 15, 28, sentence_idx=0)

        c3 = Candidate(surface_form="machine learning", normalized_form="machine learning")
        c3.add_position("doc1", 115, 131, sentence_idx=1)

        return [c1, c2, c3]

    def test_ctx2ttf(self):
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        ctx0 = ContextWindow(doc_id="doc1", sentence_id=0)
        ctx1 = ContextWindow(doc_id="doc1", sentence_id=1)
        # Sentence 0: neural network x2 + deep learning x1 = 3
        assert cf.get_ctx_ttf(ctx0) == 3
        # Sentence 1: neural network x1 + machine learning x1 = 2
        assert cf.get_ctx_ttf(ctx1) == 2

    def test_ctx2tfic(self):
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        ctx0 = ContextWindow(doc_id="doc1", sentence_id=0)
        tfic = cf.get_tfic(ctx0)
        assert tfic["neural network"] == 2
        assert tfic["deep learning"] == 1

    def test_term2ctx(self):
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        ctxs = cf.get_contexts("neural network")
        assert len(ctxs) == 2  # appears in sentence 0 and 1

    def test_n_w_for_chi_square(self):
        """n_w = sum of ctx2TTF for all contexts containing term w."""
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        # neural network appears in both sentences: n_w = 3 + 2 = 5
        assert cf.get_n_w("neural network") == 5
        # deep learning appears only in sentence 0: n_w = 3
        assert cf.get_n_w("deep learning") == 3
        # machine learning appears only in sentence 1: n_w = 2
        assert cf.get_n_w("machine learning") == 2
```

**Step 2: Implement**

```python
from typing import NamedTuple


class ContextWindow(NamedTuple):
    """Identifies a context (sentence within a document).

    Mirrors Java's ``ContextWindow``. For sentence-level contexts,
    ``doc_id`` and ``sentence_id`` are set.
    """

    doc_id: str
    sentence_id: int = -1


@dataclass
class ContextFrequency:
    """Context-aware frequency statistics.

    Mirrors Java's ``FrequencyCtxBased`` at sentence granularity.

    Attributes
    ----------
    ctx2ttf : dict[ContextWindow, int]
        Context → total candidate term occurrences in that context.
    ctx2tfic : dict[ContextWindow, dict[str, int]]
        Context → {term → frequency in context}.
    term2ctx : dict[str, set[ContextWindow]]
        Term → set of contexts where it appears.
    adjacent : dict[str, dict[str, int]]
        Term → {word → count} for adjacent words (NC-Value).
    """

    ctx2ttf: dict[ContextWindow, int] = field(default_factory=dict)
    ctx2tfic: dict[ContextWindow, dict[str, int]] = field(default_factory=dict)
    term2ctx: dict[str, set[ContextWindow]] = field(default_factory=dict)
    adjacent: dict[str, dict[str, int]] = field(default_factory=dict)

    @classmethod
    def build(
        cls,
        candidates: list[Candidate],
        documents: list[Document] | None = None,
        nlp_backend: NLPBackend | None = None,
    ) -> ContextFrequency:
        ctx2ttf: dict[ContextWindow, int] = defaultdict(int)
        ctx2tfic: dict[ContextWindow, dict[str, int]] = defaultdict(lambda: defaultdict(int))
        term2ctx: dict[str, set[ContextWindow]] = defaultdict(set)

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                for _start, _end, sent_idx in positions:
                    if sent_idx < 0:
                        continue
                    ctx = ContextWindow(doc_id=doc_id, sentence_id=sent_idx)
                    ctx2ttf[ctx] += 1
                    ctx2tfic[ctx][term] += 1
                    term2ctx[term].add(ctx)

        # Adjacent words (for NC-Value) — same logic as old ContextIndex
        adjacent: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
        if nlp_backend is not None and documents is not None:
            from jate.extractors.utils import compute_token_offsets

            doc_map = {doc.doc_id: doc for doc in documents}
            for cand in candidates:
                norm = cand.normalized_form.lower()
                for doc_id, positions in cand.doc_positions.items():
                    doc = doc_map.get(doc_id)
                    if doc is None:
                        continue
                    text = doc.content
                    tokens = nlp_backend.tokenize(text)
                    token_offsets = compute_token_offsets(text, tokens)
                    for char_start, char_end, _sent_idx in positions:
                        before_token = None
                        after_token = None
                        for i, (t_start, t_end) in enumerate(token_offsets):
                            if t_end <= char_start:
                                before_token = tokens[i]
                            if t_start >= char_end:
                                after_token = tokens[i]
                                break
                        if before_token:
                            adjacent[norm][before_token.lower()] += 1
                        if after_token:
                            adjacent[norm][after_token.lower()] += 1

        return cls(
            ctx2ttf=dict(ctx2ttf),
            ctx2tfic={k: dict(v) for k, v in ctx2tfic.items()},
            term2ctx={k: set(v) for k, v in term2ctx.items()},
            adjacent={k: dict(v) for k, v in adjacent.items()},
        )

    def get_ctx_ttf(self, ctx: ContextWindow) -> int:
        return self.ctx2ttf.get(ctx, 0)

    def get_tfic(self, ctx: ContextWindow) -> dict[str, int]:
        return dict(self.ctx2tfic.get(ctx, {}))

    def get_contexts(self, term: str) -> set[ContextWindow]:
        return self.term2ctx.get(term.lower(), set())

    def get_n_w(self, term: str) -> int:
        """Chi-Square's n_w: sum of TTF across all contexts containing term."""
        total = 0
        for ctx in self.get_contexts(term):
            total += self.ctx2ttf.get(ctx, 0)
        return total

    def get_adjacent_words(self, term: str) -> dict[str, int]:
        """Adjacent words for NC-Value."""
        return self.adjacent.get(term.lower(), {})

    def copy_top_fraction(
        self, term_freq: TermFrequency, fraction: float = 0.3
    ) -> ContextFrequency:
        """Java's FrequencyCtxBasedCopier: filter to top-fraction most frequent terms.

        Returns a new ContextFrequency containing only terms whose TTF
        is at or above the given percentile threshold.
        """
        all_ttfs = sorted(term_freq.term2ttf.values())
        if not all_ttfs:
            return ContextFrequency()
        threshold_idx = int(len(all_ttfs) * (1 - fraction))
        threshold = all_ttfs[min(threshold_idx, len(all_ttfs) - 1)]

        kept_terms = {t for t, f in term_freq.term2ttf.items() if f >= threshold}

        new_ctx2ttf: dict[ContextWindow, int] = {}
        new_ctx2tfic: dict[ContextWindow, dict[str, int]] = {}
        new_term2ctx: dict[str, set[ContextWindow]] = defaultdict(set)

        for ctx, tfic in self.ctx2tfic.items():
            filtered_tfic = {t: f for t, f in tfic.items() if t in kept_terms}
            if not filtered_tfic:
                continue
            new_ctx2tfic[ctx] = filtered_tfic
            new_ctx2ttf[ctx] = sum(filtered_tfic.values())
            for t in filtered_tfic:
                new_term2ctx[t].add(ctx)

        return ContextFrequency(
            ctx2ttf=new_ctx2ttf,
            ctx2tfic=new_ctx2tfic,
            term2ctx=dict(new_term2ctx),
            adjacent={},
        )
```

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add ContextWindow + ContextFrequency (mirrors Java FrequencyCtxBased)"
```

---

### Task 5: TermComponentIndex

**Files:**
- Modify: `src/jate/features.py`
- Modify: `tests/test_features.py`

**Context:** Java's `TermComponentIndex` maps each unigram to the multi-word candidate terms that contain it, sorted by descending word count. Used by RAKE and by Containment for efficient lookup.

**Step 1: Write tests**

```python
class TestTermComponentIndex:
    def test_build(self):
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c2 = Candidate(surface_form="deep neural network", normalized_form="deep neural network")
        c3 = Candidate(surface_form="network analysis", normalized_form="network analysis")
        idx = TermComponentIndex.build([c1, c2, c3])

        # "neural" maps to two terms
        neural_terms = idx.get_sorted("neural")
        assert len(neural_terms) == 2
        # Sorted by descending word count
        assert neural_terms[0] == ("deep neural network", 3)
        assert neural_terms[1] == ("neural network", 2)

        # "network" maps to three terms
        network_terms = idx.get_sorted("network")
        assert len(network_terms) == 3

    def test_unknown_word(self):
        idx = TermComponentIndex.build([])
        assert idx.get_sorted("unknown") == []
```

**Step 2: Implement**

```python
@dataclass
class TermComponentIndex:
    """Maps unigrams to candidate terms containing them.

    Mirrors Java's ``TermComponentIndex``. Used by RAKE and
    Containment for efficient lookup.
    """

    index: dict[str, list[tuple[str, int]]] = field(default_factory=dict)

    @classmethod
    def build(cls, candidates: list[Candidate]) -> TermComponentIndex:
        raw: dict[str, list[tuple[str, int]]] = defaultdict(list)
        for cand in candidates:
            term = cand.normalized_form.lower()
            words = term.split()
            wc = len(words)
            for w in words:
                raw[w].append((term, wc))
        # Sort each entry by descending word count
        for word in raw:
            raw[word].sort(key=lambda x: -x[1])
        return cls(index=dict(raw))

    def get_sorted(self, word: str) -> list[tuple[str, int]]:
        """Return [(term, word_count)] sorted by descending word count."""
        return list(self.index.get(word.lower(), []))
```

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add TermComponentIndex (mirrors Java TermComponentIndex)"
```

---

### Task 6: Containment (regex word boundaries, uses TermComponentIndex)

**Files:**
- Modify: `src/jate/features.py`
- Modify: `tests/test_features.py`

**Context:** Current Python uses space-padded substring matching which fails on hyphens and apostrophes. Java uses `TermComponentIndex` for efficient candidate narrowing + regex `(?<!\w)term(?!\w)` for precise word-boundary matching. The reverse builder creates child→parent mapping by inverting the forward containment.

**Step 1: Write tests**

```python
class TestContainment:
    def test_build_parents(self):
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c2 = Candidate(surface_form="deep neural network", normalized_form="deep neural network")
        c3 = Candidate(surface_form="network analysis", normalized_form="network analysis")
        tci = TermComponentIndex.build([c1, c2, c3])
        cont = Containment.build([c1, c2, c3], tci)

        parents = cont.get_parents("neural network")
        assert "deep neural network" in parents
        assert "network analysis" not in parents  # not a parent

    def test_build_children(self):
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c2 = Candidate(surface_form="deep neural network", normalized_form="deep neural network")
        tci = TermComponentIndex.build([c1, c2])
        cont = Containment.build([c1, c2], tci)
        rev = cont.reverse()

        children = rev.get_parents("deep neural network")  # reversed: "parents" = children
        assert "neural network" in children

    def test_word_boundary_regex(self):
        """Hyphenated terms should not false-match."""
        c1 = Candidate(surface_form="co op", normalized_form="co op")
        c2 = Candidate(surface_form="co-operative co op", normalized_form="co-operative co op")
        tci = TermComponentIndex.build([c1, c2])
        cont = Containment.build([c1, c2], tci)
        parents = cont.get_parents("co op")
        # "co op" IS contained in "co-operative co op" as a whole-word match
        assert "co-operative co op" in parents
```

**Step 2: Implement**

```python
@dataclass
class Containment:
    """Parent/child containment relationships between terms.

    Mirrors Java's ``Containment``. Built using TermComponentIndex
    for efficient lookup and regex for precise word-boundary matching.
    """

    term2parents: dict[str, set[str]] = field(default_factory=dict)

    @classmethod
    def build(
        cls,
        candidates: list[Candidate],
        term_component_index: TermComponentIndex,
        max_workers: int = 1,
    ) -> Containment:
        term2parents: dict[str, set[str]] = {}
        all_terms = {c.normalized_form.lower() for c in candidates}

        for cand in candidates:
            term = cand.normalized_form.lower()
            words = term.split()
            term_wc = len(words)

            # Collect candidate parents via TermComponentIndex
            compare_candidates: set[str] = set()
            for w in words:
                for parent_term, parent_wc in term_component_index.get_sorted(w):
                    if parent_wc <= term_wc:
                        break  # sorted descending, no more longer terms
                    if parent_term in all_terms:
                        compare_candidates.add(parent_term)

            # Regex word-boundary check (Java: (?<!\w)term(?!\w))
            pattern = re.compile(r"(?<!\w)" + re.escape(term) + r"(?!\w)")
            parents: set[str] = set()
            for parent in compare_candidates:
                if pattern.search(parent):
                    parents.add(parent)

            term2parents[term] = parents

        return cls(term2parents=term2parents)

    def get_parents(self, term: str) -> set[str]:
        return self.term2parents.get(term.lower(), set())

    def reverse(self) -> Containment:
        """Java's ContainmentReverseBuilder: invert parent→child to child→parent."""
        reversed_map: dict[str, set[str]] = defaultdict(set)
        for child, parents in self.term2parents.items():
            for parent in parents:
                reversed_map[parent].add(child)
        return Containment(term2parents=dict(reversed_map))
```

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add Containment with regex word boundaries and TermComponentIndex"
```

---

### Task 7: Cooccurrence (sentence-level, min frequency model)

**Files:**
- Modify: `src/jate/features.py`
- Modify: `tests/test_features.py`

**Context:** Java computes co-occurrence as `min(freq_a_in_ctx, freq_b_in_ctx)` for each context where both terms appear. Current Python uses document-level binary presence. We rebuild this from `ContextFrequency`.

**Step 1: Write tests**

```python
class TestCooccurrence:
    def test_build_from_context_frequency(self):
        # Sentence 0: "neural network" x2, "deep learning" x1
        # Sentence 1: "neural network" x1, "machine learning" x1
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 30, 44, sentence_idx=0)
        c1.add_position("doc1", 100, 114, sentence_idx=1)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 15, 28, sentence_idx=0)

        c3 = Candidate(surface_form="machine learning", normalized_form="machine learning")
        c3.add_position("doc1", 115, 131, sentence_idx=1)

        cf = ContextFrequency.build([c1, c2, c3])
        cooc = Cooccurrence.build(cf, cf)

        # In sentence 0: min(nn=2, dl=1) = 1
        assert cooc.get("neural network", "deep learning") == 1
        # In sentence 1: min(nn=1, ml=1) = 1
        assert cooc.get("neural network", "machine learning") == 1
        # deep learning and machine learning never share a sentence
        assert cooc.get("deep learning", "machine learning") == 0

    def test_symmetric(self):
        c1 = Candidate(surface_form="a", normalized_form="a")
        c1.add_position("doc1", 0, 1, sentence_idx=0)
        c2 = Candidate(surface_form="b", normalized_form="b")
        c2.add_position("doc1", 2, 3, sentence_idx=0)
        cf = ContextFrequency.build([c1, c2])
        cooc = Cooccurrence.build(cf, cf)
        assert cooc.get("a", "b") == cooc.get("b", "a")
```

**Step 2: Implement**

```python
@dataclass
class Cooccurrence:
    """Term co-occurrence based on shared contexts.

    Mirrors Java's ``Cooccurrence``. For each context where both a target
    term and a reference term appear, the co-occurrence count is incremented
    by ``min(freq_target_in_ctx, freq_ref_in_ctx)``.
    """

    matrix: dict[tuple[str, str], int] = field(default_factory=dict)

    @classmethod
    def build(
        cls,
        target_ctx: ContextFrequency,
        ref_ctx: ContextFrequency,
    ) -> Cooccurrence:
        matrix: dict[tuple[str, str], int] = defaultdict(int)

        # Iterate over all contexts that have target terms
        for ctx, target_tfic in target_ctx.ctx2tfic.items():
            ref_tfic = ref_ctx.ctx2tfic.get(ctx)
            if ref_tfic is None:
                continue
            for target_term, target_freq in target_tfic.items():
                for ref_term, ref_freq in ref_tfic.items():
                    if target_term == ref_term:
                        continue
                    key = tuple(sorted([target_term, ref_term]))
                    matrix[key] += min(target_freq, ref_freq)

        return cls(matrix=dict(matrix))

    def get(self, term_a: str, term_b: str) -> int:
        key = tuple(sorted([term_a.lower(), term_b.lower()]))
        return self.matrix.get(key, 0)

    def get_cooccurrences_for(self, term: str) -> dict[str, int]:
        """All co-occurring terms and their counts for a given term."""
        t = term.lower()
        result: dict[str, int] = {}
        for (a, b), count in self.matrix.items():
            if a == t:
                result[b] = count
            elif b == t:
                result[a] = count
        return result
```

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add Cooccurrence feature (sentence-level, min-freq model)"
```

---

### Task 8: ChiSquareFrequentTerms

**Files:**
- Modify: `src/jate/features.py`
- Modify: `tests/test_features.py`

**Context:** Java selects the top 30% most frequent terms as reference terms, then computes `p_g = (sum of TTF in contexts where g appears) / ttfInCorpus`. Current Python uses all candidates with `p_g = tf / corpus_total`.

**Step 1: Write tests**

```python
class TestChiSquareFrequentTerms:
    def test_build(self):
        # Create ContextFrequency with known data
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 30, 44, sentence_idx=0)
        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 15, 28, sentence_idx=0)
        cf = ContextFrequency.build([c1, c2])
        tf = TermFrequency.build([c1, c2], total_docs=1)

        # With fraction=1.0 (all terms are "frequent")
        ft = ChiSquareFrequentTerms.build(cf, tf.corpus_total, fraction=1.0)
        assert "neural network" in ft.exp_prob
        assert "deep learning" in ft.exp_prob
        assert ft.sum_exp_prob == pytest.approx(
            ft.exp_prob["neural network"] + ft.exp_prob["deep learning"]
        )

    def test_p_g_formula(self):
        """p_g = (sum of TTF in contexts where g appears) / corpus_total."""
        c1 = Candidate(surface_form="a", normalized_form="a")
        c1.add_position("doc1", 0, 1, sentence_idx=0)  # sentence 0: a x1
        c2 = Candidate(surface_form="b", normalized_form="b")
        c2.add_position("doc1", 2, 3, sentence_idx=0)  # sentence 0: b x1
        cf = ContextFrequency.build([c1, c2])
        # ctx TTF for sentence 0 = 2 (a:1 + b:1)
        # g_w for "a" = sum of TTF in contexts containing "a" = 2
        # corpus_total = 2
        # p_g("a") = 2 / 2 = 1.0
        ft = ChiSquareFrequentTerms.build(cf, corpus_total=2, fraction=1.0)
        assert ft.exp_prob["a"] == pytest.approx(1.0)
```

**Step 2: Implement**

```python
@dataclass
class ChiSquareFrequentTerms:
    """Expected probabilities for frequent reference terms.

    Mirrors Java's ``ChiSquareFrequentTerms``.
    ``p_g = (sum of TTF in contexts where g appears) / ttfInCorpus``
    """

    exp_prob: dict[str, float] = field(default_factory=dict)
    sum_exp_prob: float = 0.0
    max_exp_prob: float = 0.0

    @classmethod
    def build(
        cls,
        ref_ctx_freq: ContextFrequency,
        corpus_total: int,
        fraction: float = 0.3,
    ) -> ChiSquareFrequentTerms:
        if corpus_total <= 0:
            corpus_total = 1

        exp_prob: dict[str, float] = {}
        for term, contexts in ref_ctx_freq.term2ctx.items():
            g_w = sum(ref_ctx_freq.ctx2ttf.get(ctx, 0) for ctx in contexts)
            p_g = g_w / corpus_total
            exp_prob[term] = p_g

        sum_exp_prob = sum(exp_prob.values())
        max_exp_prob = max(exp_prob.values()) if exp_prob else 0.0

        return cls(
            exp_prob=exp_prob,
            sum_exp_prob=sum_exp_prob,
            max_exp_prob=max_exp_prob,
        )
```

**Step 3: Commit**

```bash
git add src/jate/features.py tests/test_features.py
git commit -m "feat: add ChiSquareFrequentTerms (top-fraction, context-based p_g)"
```

---

### Task 9: Update all 13 algorithms to use new features

**Files:**
- Modify: `src/jate/algorithms/tfidf.py`
- Modify: `src/jate/algorithms/ttf.py`
- Modify: `src/jate/algorithms/attf.py`
- Modify: `src/jate/algorithms/ridf.py`
- Modify: `src/jate/algorithms/cvalue.py`
- Modify: `src/jate/algorithms/ncvalue.py`
- Modify: `src/jate/algorithms/basic.py`
- Modify: `src/jate/algorithms/combo_basic.py`
- Modify: `src/jate/algorithms/chi_square.py`
- Modify: `src/jate/algorithms/rake.py`
- Modify: `src/jate/algorithms/weirdness.py`
- Modify: `src/jate/algorithms/termex.py`
- Modify: `src/jate/algorithms/glossex.py`
- Modify: `src/jate/algorithms/base.py`
- Modify: `tests/test_algorithms.py`

**Context:** All algorithms currently take `CorpusStore` as their primary data source. They need to switch to accepting the proper feature objects. The key changes per algorithm:

**Base class change:**
```python
# base.py — change score() signature
class Algorithm:
    def score(self, candidates, term_freq, **kwargs) -> TermExtractionResult:
        ...
```

**Simple algorithms (TTF, ATTF, TFIDF, RIDF):** Replace `corpus_store: CorpusStore` with `term_freq: TermFrequency`.
- `corpus_store.get_term_frequency(t)` → `term_freq.get_ttf(t)`
- `corpus_store.get_document_frequency(t)` → `term_freq.get_df(t)`
- `corpus_store.get_total_documents()` → `term_freq.total_docs`
- `corpus_store.get_corpus_total()` → `term_freq.corpus_total`
- TFIDF: `ttf / (corpus_total + 1)` → `term_freq.get_ttf_norm(t)` (now matches Java exactly)

**Containment algorithms (CValue, NCValue, Basic, ComboBasic):** Same as simple + accept `Containment` instead of `dict[str, list[str]]`.
- `containment.get(nf, [])` → `containment.get_parents(nf)`

**Chi-Square:** Major rewrite.
- Remove `frequent_terms` and `context_term_totals` constructor args
- Accept `ChiSquareFrequentTerms`, `Cooccurrence`, `ContextFrequency` in `score()`
- `n_w` = `context_freq.get_n_w(term)` (correct: sum of ctx2TTF)
- Co-occurrence from `Cooccurrence.get()` (correct: min-freq model)

**RAKE:** Accept `WordFrequency` and `TermComponentIndex` instead of dicts.
- `self._word_frequencies.get(word, 0)` → `word_freq.get_ttf(word)`

**Weirdness, GlossEx, TermEx:** Accept `WordFrequency` and `ReferenceFrequency`.
- `self._word_freq.get(wi, 0)` → `word_freq.get_ttf(wi)`
- `self._ref_freq.get(wi, 0)` → `ref_freq.get_ttf(wi)`
- `self._ref_total` → `ref_freq.corpus_total`

**TermEx DC:** Accept `ContextFrequency` (document-level) for document distribution. Use `term_freq.get_doc_freq_map()` for per-doc frequencies and per-doc totals.

**NCValue:** Accept `ContextFrequency` for adjacent words instead of `ContextIndex`.

This is a large task. Implement algorithm by algorithm, running tests after each. Update `tests/test_algorithms.py` to construct feature objects instead of CorpusStore.

**Step 1: Update base.py signature**
**Step 2: Update TTF, ATTF, TFIDF, RIDF (simple algorithms)**
**Step 3: Update CValue, Basic, ComboBasic, NCValue (containment algorithms)**
**Step 4: Update ChiSquare**
**Step 5: Update RAKE**
**Step 6: Update Weirdness, GlossEx, TermEx (reference algorithms)**
**Step 7: Update tests, run full suite**

Commit after each sub-step.

---

### Task 10: Update api.py orchestration

**Files:**
- Modify: `src/jate/api.py`

**Context:** The public API functions (`extract`, `extract_corpus`, `compare`) need to build features in the right order and pass them to algorithms. Remove all ad-hoc builder functions. The new flow:

```
1. NLP + extract candidates → candidates, documents
2. TermFrequency.build(candidates, total_docs)
3. WordFrequency.build(documents, nlp.tokenize)
4. ContextFrequency.build(candidates, documents, nlp)
5. TermComponentIndex.build(candidates)
6. Containment.build(candidates, tci) — only if needed
7. For Chi-Square: ref_ctx = ctx_freq.copy_top_fraction(term_freq, 0.3)
                   Cooccurrence.build(ctx_freq, ref_ctx)
                   ChiSquareFrequentTerms.build(ref_ctx, term_freq.corpus_total)
8. For reference algorithms: ReferenceFrequency.from_word_frequency(word_freq)
9. Pass correct features to each algorithm's score()
```

Remove: `_build_word_frequencies`, `_build_term_component_index`, `_build_frequent_terms`, `_build_reference_data`, `_build_context_index`.

Remove: imports of `MemoryCorpusStore`, `SQLiteCorpusStore`, `CorpusStore` from api.py.

---

### Task 11: Update __init__.py, protocols.py, remove old modules

**Files:**
- Modify: `src/jate/__init__.py`
- Modify: `src/jate/protocols.py`
- Delete: `src/jate/context.py`
- Delete: `src/jate/store/memory_store.py` (or keep as thin wrapper for backward compat)
- Delete: `src/jate/store/sqlite_store.py` (or keep as thin wrapper)
- Modify: `src/jate/corpus.py`

**Context:** Remove `CorpusStore` protocol (algorithms no longer use it). Replace `ContextIndex` export with `ContextFrequency`. Keep `MemoryCorpusStore`/`SQLiteCorpusStore` as deprecated thin wrappers around `TermFrequency` if backward compatibility is needed, or remove them entirely.

Update `__init__.py` exports: add all new feature classes, remove `ContextIndex`, `MemoryCorpusStore`, `SQLiteCorpusStore`.

---

### Task 12: Update all existing tests

**Files:**
- Modify: `tests/test_algorithms.py`
- Modify: `tests/test_api.py`
- Modify: `tests/test_context.py` → rename to `tests/test_features.py` or merge
- Modify: `tests/test_store.py` → update or remove
- Modify: `tests/test_corpus.py`
- Modify: `tests/test_parallel.py`

**Context:** All tests that create `MemoryCorpusStore` or `ContextIndex` need updating. Run full suite: `poetry run pytest tests/ -v`

---

### Task 13: Add Voting algorithm

**Files:**
- Create: `src/jate/algorithms/voting.py`
- Modify: `src/jate/algorithms/__init__.py`
- Create: `tests/test_voting.py`

**Context:** Java's `Voting` uses reciprocal rank fusion:
```
for each (ranked_list, weight) in algorithm_results:
    for each term at rank i (0-indexed):
        vote_score[term] += weight / (i + 1)
```

**Step 1: Write tests**

```python
class TestVoting:
    def test_reciprocal_rank_fusion(self):
        r1 = TermExtractionResult([
            Term(string="a", score=3.0),
            Term(string="b", score=2.0),
            Term(string="c", score=1.0),
        ])
        r2 = TermExtractionResult([
            Term(string="b", score=5.0),
            Term(string="a", score=3.0),
            Term(string="c", score=1.0),
        ])
        result = Voting.combine([(r1, 1.0), (r2, 1.0)])
        scores = {t.string: t.score for t in result}
        # a: 1/1 * 1.0 + 1/2 * 1.0 = 1.5
        # b: 1/2 * 1.0 + 1/1 * 1.0 = 1.5
        # c: 1/3 * 1.0 + 1/3 * 1.0 = 0.667
        assert scores["a"] == pytest.approx(1.5)
        assert scores["b"] == pytest.approx(1.5)

    def test_weighted(self):
        r1 = TermExtractionResult([Term(string="a", score=1.0)])
        r2 = TermExtractionResult([Term(string="a", score=1.0)])
        result = Voting.combine([(r1, 2.0), (r2, 1.0)])
        # a: 1/1 * 2.0 + 1/1 * 1.0 = 3.0
        assert result[0].score == pytest.approx(3.0)
```

**Step 2: Implement**

```python
# src/jate/algorithms/voting.py
class Voting:
    """Reciprocal rank fusion across multiple algorithm results.

    Mirrors Java's ``Voting``.
    """

    @staticmethod
    def combine(
        results: list[tuple[TermExtractionResult, float]],
    ) -> TermExtractionResult:
        vote_scores: dict[str, float] = defaultdict(float)
        term_data: dict[str, Term] = {}

        for ranked_result, weight in results:
            for i, term in enumerate(ranked_result):
                rank_score = weight / (i + 1)
                vote_scores[term.string] += rank_score
                if term.string not in term_data:
                    term_data[term.string] = Term(
                        string=term.string,
                        frequency=term.frequency,
                        surface_forms=set(term.surface_forms),
                    )

        result = TermExtractionResult()
        for term_str, score in vote_scores.items():
            t = term_data[term_str]
            t.score = score
            result.add(t)

        return result.sort()
```

**Step 3: Wire into compare() API**

Add optional `voting=True` parameter to `compare()` that, when set, also returns a `"voting"` key in the results dict.

**Step 4: Commit**

```bash
git add src/jate/algorithms/voting.py src/jate/algorithms/__init__.py tests/test_voting.py src/jate/api.py
git commit -m "feat: add Voting ensemble algorithm (reciprocal rank fusion)"
```

---

### Task 14: Update docs and run full test suite

**Files:**
- Modify: `docs/reference/api.md`
- Modify: `docs/guide/architecture.md`
- Run: `poetry run pytest tests/ -v --tb=short`

**Context:** Update mkdocs API reference to document new feature classes. Update architecture guide to reflect the new feature system. Ensure all 227+ tests pass (some will need updating from earlier tasks, some new ones added).

---

### Dependency order

```
Task 1 (TermFrequency) — no deps
Task 2 (WordFrequency) — no deps
Task 3 (ReferenceFrequency) — depends on Task 2
Task 4 (ContextFrequency) — depends on Task 1 (for copy_top_fraction)
Task 5 (TermComponentIndex) — no deps
Task 6 (Containment) — depends on Task 5
Task 7 (Cooccurrence) — depends on Task 4
Task 8 (ChiSquareFrequentTerms) — depends on Task 4
Task 9 (Update algorithms) — depends on Tasks 1-8
Task 10 (Update api.py) — depends on Task 9
Task 11 (Cleanup old modules) — depends on Task 10
Task 12 (Update tests) — depends on Task 11
Task 13 (Voting) — depends on Task 9
Task 14 (Docs + full test) — depends on all
```
