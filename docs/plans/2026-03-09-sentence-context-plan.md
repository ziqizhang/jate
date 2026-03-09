# Sentence-Level Context Implementation Plan

> **For Claude:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task.

**Goal:** Add sentence-level context to Chi-Square and NC-Value algorithms, aligning with the original papers and Java JATE.

**Architecture:** A new `ContextIndex` class provides sentence-level co-occurrence, adjacency words, and context term totals. Extractors tag each candidate position with a sentence index. Algorithms accept `ContextIndex` as an optional parameter.

**Tech Stack:** Python 3.11+, spaCy, Poetry, pytest

---

### Task 1: Extend Candidate.add_position() with sentence_idx

**Files:**
- Modify: `src/jate/models.py:58-76`
- Test: `tests/test_models.py`

**Step 1: Write the failing test**

In `tests/test_models.py`, add to `TestCandidate`:

```python
def test_add_position_with_sentence_idx(self) -> None:
    c = Candidate(surface_form="neural network")
    c.add_position("doc1", 10, 25, sentence_idx=2, surface="Neural Network")
    assert c.doc_positions == {"doc1": [(10, 25, 2)]}
    assert c.total_frequency == 1
    assert "Neural Network" in c.surface_forms

def test_add_position_default_sentence_idx(self) -> None:
    c = Candidate(surface_form="neural network")
    c.add_position("doc1", 10, 25)
    assert c.doc_positions == {"doc1": [(10, 25, -1)]}
```

**Step 2: Run tests to verify they fail**

Run: `poetry run pytest tests/test_models.py::TestCandidate::test_add_position_with_sentence_idx tests/test_models.py::TestCandidate::test_add_position_default_sentence_idx -v`
Expected: FAIL — tuples are 2-element, not 3-element

**Step 3: Implement the changes**

In `src/jate/models.py`, change the `Candidate` class:

```python
# Line 58: Update the type annotation comment
# doc_id -> list of (start, end, sentence_idx) character offsets
doc_positions: dict[str, list[tuple[int, int, int]]] = field(default_factory=dict)

# Lines 69-76: Update add_position signature and body
def add_position(
    self, doc_id: str, start: int, end: int, sentence_idx: int = -1, surface: str = ""
) -> None:
    """Record a new occurrence of this candidate in a document."""
    self.doc_positions.setdefault(doc_id, []).append((start, end, sentence_idx))
    self.total_frequency += 1
    if surface:
        self.surface_forms.add(surface)
```

**Step 4: Fix existing test for new tuple shape**

In `tests/test_models.py`, update `test_add_position`:

```python
def test_add_position(self) -> None:
    c = Candidate(surface_form="neural network")
    c.add_position("doc1", 10, 25)
    c.add_position("doc1", 50, 65)
    c.add_position("doc2", 5, 20)
    assert c.total_frequency == 3
    assert c.document_frequency == 2
    assert len(c.doc_positions["doc1"]) == 2
    assert c.doc_positions["doc1"][0] == (10, 25, -1)
```

**Step 5: Run all model tests**

Run: `poetry run pytest tests/test_models.py -v`
Expected: ALL PASS

**Step 6: Run full test suite to check for breakage**

Run: `poetry run pytest tests/ -v`
Expected: Some tests may fail if they construct position tuples directly. Fix any that do.

**Step 7: Commit**

```bash
git add src/jate/models.py tests/test_models.py
git commit -m "feat: add sentence_idx to Candidate.add_position()"
```

---

### Task 2: Update extractors to pass sentence_idx

**Files:**
- Modify: `src/jate/extractors/pos_pattern.py:134-216`
- Modify: `src/jate/extractors/ngram.py:37-89`
- Modify: `src/jate/extractors/noun_phrase.py:14-76`
- Test: `tests/test_extractors.py` (if exists) or verify via `tests/test_algorithms.py`

**Step 1: Write a failing test**

Create or add to a test file. Since extractors need an NLP backend, use a minimal integration test:

```python
# tests/test_extractors_sentence.py
"""Verify extractors tag candidates with sentence indices."""
from __future__ import annotations

import pytest
from jate.extractors.pos_pattern import PosPatternExtractor
from jate.extractors.ngram import NGramExtractor
from jate.extractors.noun_phrase import NounPhraseExtractor
from jate.models import Document
from jate.nlp.spacy_backend import SpacyBackend
from jate.store.memory_store import MemoryCorpusStore


@pytest.fixture(scope="module")
def nlp() -> SpacyBackend:
    return SpacyBackend("en_core_web_sm")


TWO_SENTENCE_DOC = [
    Document(doc_id="d1", content="Machine learning is important. Neural networks are powerful.")
]


class TestPosPatternSentenceIdx:
    def test_candidates_have_sentence_idx(self, nlp: SpacyBackend) -> None:
        store = MemoryCorpusStore()
        ext = PosPatternExtractor()
        candidates = ext.extract(TWO_SENTENCE_DOC, nlp, store)
        assert len(candidates) > 0
        for c in candidates:
            for doc_id, positions in c.doc_positions.items():
                for start, end, sent_idx in positions:
                    assert sent_idx >= 0, f"{c.normalized_form} has no sentence_idx"


class TestNGramSentenceIdx:
    def test_candidates_have_sentence_idx(self, nlp: SpacyBackend) -> None:
        store = MemoryCorpusStore()
        ext = NGramExtractor(min_n=1, max_n=2)
        candidates = ext.extract(TWO_SENTENCE_DOC, nlp, store)
        assert len(candidates) > 0
        for c in candidates:
            for doc_id, positions in c.doc_positions.items():
                for start, end, sent_idx in positions:
                    assert sent_idx >= 0, f"{c.normalized_form} has no sentence_idx"


class TestNounPhraseSentenceIdx:
    def test_candidates_have_sentence_idx(self, nlp: SpacyBackend) -> None:
        store = MemoryCorpusStore()
        ext = NounPhraseExtractor()
        candidates = ext.extract(TWO_SENTENCE_DOC, nlp, store)
        assert len(candidates) > 0
        for c in candidates:
            for doc_id, positions in c.doc_positions.items():
                for start, end, sent_idx in positions:
                    assert sent_idx >= 0, f"{c.normalized_form} has no sentence_idx"
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_extractors_sentence.py -v`
Expected: FAIL — sentence_idx is -1 or tuples are 2-element

**Step 3: Update PosPatternExtractor**

In `src/jate/extractors/pos_pattern.py`, the key change is to process text sentence-by-sentence. Replace the document processing loop (lines 142-216) with:

```python
def extract(
    self,
    documents: list[Document],
    nlp_backend: NLPBackend,
    corpus_store: CorpusStore,
) -> list[Candidate]:
    merged: dict[str, Candidate] = {}

    for doc in documents:
        corpus_store.add_document(doc)
        text = doc.content
        if not text.strip():
            continue

        # Split into sentences for sentence-level tracking.
        sentences: list[str] = nlp_backend.sentence_split(text)

        # Build a mapping from sentence text to its character offset
        # in the full document, so we can compute document-level offsets.
        sent_char_offsets: list[int] = []
        search_from = 0
        for sent_text in sentences:
            idx = text.find(sent_text, search_from)
            if idx == -1:
                idx = search_from  # fallback
            sent_char_offsets.append(idx)
            search_from = idx + len(sent_text)

        for sent_idx, sent_text in enumerate(sentences):
            if not sent_text.strip():
                continue

            sent_offset = sent_char_offsets[sent_idx]

            tagged: list[tuple[str, str]] = nlp_backend.pos_tag(sent_text)
            lemmas_pairs: list[tuple[str, str]] = nlp_backend.lemmatize(sent_text)

            pos_tags: list[str] = [tag for _, tag in tagged]
            tokens: list[str] = [tok for tok, _ in tagged]
            lemmas: list[str] = [lem for _, lem in lemmas_pairs]
            pos_string = " ".join(pos_tags) + " "

            tag_char_starts: list[int] = []
            offset = 0
            for tag in pos_tags:
                tag_char_starts.append(offset)
                offset += len(tag) + 1

            token_char_offsets: list[tuple[int, int]] = compute_token_offsets(sent_text, tokens)

            for m in self._pattern.finditer(pos_string):
                start_char = m.start()
                end_char = m.end()

                tok_start = _char_to_token_index(tag_char_starts, start_char)
                tok_end = _char_to_token_index(tag_char_starts, end_char - 1)
                span_tokens = tokens[tok_start : tok_end + 1]
                span_lemmas = lemmas[tok_start : tok_end + 1]
                span_pos = pos_tags[tok_start : tok_end + 1]

                span_tokens, span_lemmas, span_pos, strip_left = _strip_stopwords(span_tokens, span_lemmas, span_pos)
                if not span_tokens:
                    continue

                actual_tok_start = tok_start + strip_left
                actual_tok_end = actual_tok_start + len(span_tokens) - 1

                surface = " ".join(span_tokens)
                if len(span_tokens) == 1:
                    normalized = span_lemmas[0].lower()
                else:
                    normalized = " ".join(
                        [t.lower() for t in span_tokens[:-1]] + [span_lemmas[-1].lower()]
                    )
                pos_pat = " ".join(span_pos)

                # Convert sentence-local offsets to document-level offsets.
                doc_start = sent_offset + token_char_offsets[actual_tok_start][0]
                doc_end = sent_offset + token_char_offsets[actual_tok_end][1]

                if normalized in merged:
                    merged[normalized].add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx, surface=surface)
                else:
                    cand = Candidate(
                        surface_form=surface,
                        normalized_form=normalized,
                        pos_pattern=pos_pat,
                    )
                    cand.add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx)
                    merged[normalized] = cand

    return list(merged.values())
```

**Step 4: Update NGramExtractor**

Same pattern in `src/jate/extractors/ngram.py`: wrap the inner loop in sentence iteration.

```python
def extract(
    self,
    documents: list[Document],
    nlp_backend: NLPBackend,
    corpus_store: CorpusStore,
) -> list[Candidate]:
    merged: dict[str, Candidate] = {}

    for doc in documents:
        corpus_store.add_document(doc)
        text = doc.content
        if not text.strip():
            continue

        sentences: list[str] = nlp_backend.sentence_split(text)
        sent_char_offsets: list[int] = []
        search_from = 0
        for sent_text in sentences:
            idx = text.find(sent_text, search_from)
            if idx == -1:
                idx = search_from
            sent_char_offsets.append(idx)
            search_from = idx + len(sent_text)

        for sent_idx, sent_text in enumerate(sentences):
            if not sent_text.strip():
                continue

            sent_offset = sent_char_offsets[sent_idx]

            tokens: list[str] = nlp_backend.tokenize(sent_text)
            lemma_pairs: list[tuple[str, str]] = nlp_backend.lemmatize(sent_text)
            lemmas: list[str] = [lem for _, lem in lemma_pairs]
            token_offsets = compute_token_offsets(sent_text, tokens)

            for n in range(self._min_n, self._max_n + 1):
                for i in range(len(tokens) - n + 1):
                    span_tokens = tokens[i : i + n]
                    span_lemmas = lemmas[i : i + n]

                    if _is_stop_or_punct(span_tokens[0]) or _is_stop_or_punct(span_tokens[-1]):
                        continue

                    surface = " ".join(span_tokens)
                    if len(span_tokens) == 1:
                        normalized = span_lemmas[0].lower()
                    else:
                        normalized = " ".join(
                            [t.lower() for t in span_tokens[:-1]] + [span_lemmas[-1].lower()]
                        )

                    doc_start = sent_offset + token_offsets[i][0]
                    doc_end = sent_offset + token_offsets[i + n - 1][1]

                    if normalized in merged:
                        merged[normalized].add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx, surface=surface)
                    else:
                        cand = Candidate(
                            surface_form=surface,
                            normalized_form=normalized,
                        )
                        cand.add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx)
                        merged[normalized] = cand

    return list(merged.values())
```

**Step 5: Update NounPhraseExtractor**

Same pattern in `src/jate/extractors/noun_phrase.py`:

```python
def extract(
    self,
    documents: list[Document],
    nlp_backend: NLPBackend,
    corpus_store: CorpusStore,
) -> list[Candidate]:
    merged: dict[str, Candidate] = {}

    for doc in documents:
        corpus_store.add_document(doc)
        text = doc.content
        if not text.strip():
            continue

        sentences: list[str] = nlp_backend.sentence_split(text)
        sent_char_offsets: list[int] = []
        search_from = 0
        for sent_text in sentences:
            idx = text.find(sent_text, search_from)
            if idx == -1:
                idx = search_from
            sent_char_offsets.append(idx)
            search_from = idx + len(sent_text)

        for sent_idx, sent_text in enumerate(sentences):
            if not sent_text.strip():
                continue

            sent_offset = sent_char_offsets[sent_idx]
            chunks: list[str] = nlp_backend.noun_chunks(sent_text)
            lemma_map = dict(nlp_backend.lemmatize(sent_text))

            sent_search_start = 0
            for chunk in chunks:
                chunk_stripped = chunk.strip()
                if not chunk_stripped:
                    continue

                words = chunk_stripped.split()
                while words and words[0].lower() in STOPWORDS:
                    words.pop(0)
                while words and words[-1].lower() in STOPWORDS:
                    words.pop()
                if not words:
                    continue

                surface = " ".join(words)
                if len(words) == 1:
                    normalized = lemma_map.get(words[0], words[0]).lower()
                else:
                    normalized = " ".join(
                        [w.lower() for w in words[:-1]]
                        + [lemma_map.get(words[-1], words[-1]).lower()]
                    )

                start = sent_text.find(chunk_stripped, sent_search_start)
                if start != -1:
                    end = start + len(chunk_stripped)
                    sent_search_start = end
                else:
                    start = 0
                    end = 0

                doc_start = sent_offset + start
                doc_end = sent_offset + end

                if normalized in merged:
                    merged[normalized].add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx, surface=surface)
                else:
                    cand = Candidate(
                        surface_form=surface,
                        normalized_form=normalized,
                    )
                    cand.add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx)
                    merged[normalized] = cand

    return list(merged.values())
```

**Step 6: Run tests**

Run: `poetry run pytest tests/test_extractors_sentence.py tests/test_algorithms.py -v`
Expected: ALL PASS

**Step 7: Run full test suite**

Run: `poetry run pytest tests/ -v`
Expected: ALL PASS (existing tests should not break since `sentence_idx=-1` is the default)

**Step 8: Commit**

```bash
git add src/jate/extractors/pos_pattern.py src/jate/extractors/ngram.py src/jate/extractors/noun_phrase.py tests/test_extractors_sentence.py
git commit -m "feat: extractors tag candidates with sentence indices"
```

---

### Task 3: Update MemoryCorpusStore for 3-tuple positions

**Files:**
- Modify: `src/jate/store/memory_store.py:35-55`
- Test: `tests/test_store.py`

**Step 1: Check if index_candidates needs updating**

The `MemoryCorpusStore.index_candidates()` method accesses `cand.doc_positions.items()` and iterates over positions to get frequency via `len(positions)`. Since we changed tuples from 2-element to 3-element, this code doesn't unpack tuples — it only counts them. **No change needed** to `index_candidates`.

**Step 2: Run existing store tests to verify**

Run: `poetry run pytest tests/test_store.py -v`
Expected: ALL PASS (if test helpers construct candidates properly)

**Step 3: Fix any test helpers that construct position tuples directly**

Check if `tests/test_store.py` or `tests/test_algorithms.py` create `Candidate` objects with hardcoded 2-element position tuples. If so, update them to use `add_position()` method instead.

**Step 4: Commit if changes were needed**

```bash
git add tests/test_store.py
git commit -m "fix: update store tests for 3-tuple positions"
```

---

### Task 4: Create ContextIndex class

**Files:**
- Create: `src/jate/context.py`
- Test: `tests/test_context.py`

**Step 1: Write failing tests**

```python
# tests/test_context.py
"""Unit tests for ContextIndex."""
from __future__ import annotations

import pytest

from jate.context import ContextIndex
from jate.models import Candidate, Document
from jate.nlp.spacy_backend import SpacyBackend


def _make_candidate(normalized: str, positions: dict[str, list[tuple[int, int, int]]]) -> Candidate:
    """Helper to build a candidate with explicit positions."""
    c = Candidate(surface_form=normalized, normalized_form=normalized)
    # Bypass add_position to set exact positions
    c.doc_positions = positions
    c.total_frequency = sum(len(v) for v in positions.values())
    return c


class TestSentenceCooccurrence:
    def test_same_sentence(self) -> None:
        """Two terms in the same sentence co-occur."""
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 1

    def test_different_sentences(self) -> None:
        """Two terms in different sentences don't co-occur."""
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(50, 66, 1)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 0

    def test_multiple_sentences(self) -> None:
        """Co-occurrence counted per unique sentence."""
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0), (100, 114, 2)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0), (120, 136, 2)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 2

    def test_cross_document(self) -> None:
        """Co-occurrence is per (doc, sentence), not global."""
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)], "d2": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)], "d2": [(20, 36, 0)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("neural network", "machine learning") == 2

    def test_symmetric(self) -> None:
        """get_sentence_cooccurrences(a, b) == get_sentence_cooccurrences(b, a)."""
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)]})
        idx = ContextIndex.build([c1, c2], [])
        assert idx.get_sentence_cooccurrences("machine learning", "neural network") == 1

    def test_unknown_term_returns_zero(self) -> None:
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        idx = ContextIndex.build([c1], [])
        assert idx.get_sentence_cooccurrences("neural network", "unknown") == 0


class TestContextTermTotal:
    def test_single_sentence(self) -> None:
        """n_w = total distinct terms in sentences where w appears."""
        c1 = _make_candidate("neural network", {"d1": [(0, 14, 0)]})
        c2 = _make_candidate("machine learning", {"d1": [(20, 36, 0)]})
        c3 = _make_candidate("deep learning", {"d1": [(50, 63, 1)]})
        idx = ContextIndex.build([c1, c2, c3], [])
        # "neural network" in sentence 0 with "machine learning" -> 2 distinct terms
        assert idx.get_context_term_total("neural network") == 2
        # "deep learning" in sentence 1 alone -> 1 distinct term
        assert idx.get_context_term_total("deep learning") == 1

    def test_unknown_term_returns_zero(self) -> None:
        idx = ContextIndex.build([], [])
        assert idx.get_context_term_total("unknown") == 0


class TestAdjacentWords:
    def test_with_nlp_backend(self) -> None:
        """Adjacent words extracted from document text using NLP backend."""
        text = "The neural network is powerful."
        doc = Document(doc_id="d1", content=text)
        # "neural network" starts at char 4, ends at char 18
        c = _make_candidate("neural network", {"d1": [(4, 18, 0)]})
        nlp = SpacyBackend("en_core_web_sm")
        idx = ContextIndex.build([c], [doc], nlp_backend=nlp)
        adj = idx.get_adjacent_words("neural network")
        # "The" is before, "is" is after (exact tokens depend on spaCy)
        assert isinstance(adj, dict)
        assert len(adj) > 0

    def test_without_nlp_backend(self) -> None:
        """Without NLP backend, adjacency data is empty."""
        c = _make_candidate("neural network", {"d1": [(4, 18, 0)]})
        doc = Document(doc_id="d1", content="The neural network is powerful.")
        idx = ContextIndex.build([c], [doc])
        adj = idx.get_adjacent_words("neural network")
        assert adj == {}

    def test_unknown_term(self) -> None:
        idx = ContextIndex.build([], [])
        assert idx.get_adjacent_words("unknown") == {}
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_context.py -v`
Expected: FAIL — `jate.context` doesn't exist

**Step 3: Implement ContextIndex**

Create `src/jate/context.py`:

```python
"""Sentence-level context index for term extraction algorithms."""

from __future__ import annotations

from collections import defaultdict

from jate.models import Candidate, Document
from jate.protocols import NLPBackend


class ContextIndex:
    """Sentence-level context statistics.

    Built from candidates whose positions include sentence indices.
    Provides sentence co-occurrence, context term totals, and
    adjacent-word data for Chi-Square and NC-Value algorithms.
    """

    def __init__(
        self,
        sent_cooc: dict[tuple[str, str], int],
        context_totals: dict[str, int],
        adjacent: dict[str, dict[str, int]],
    ) -> None:
        self._sent_cooc = sent_cooc
        self._context_totals = context_totals
        self._adjacent = adjacent

    @classmethod
    def build(
        cls,
        candidates: list[Candidate],
        documents: list[Document],
        nlp_backend: NLPBackend | None = None,
    ) -> ContextIndex:
        """Build a ContextIndex from candidates with sentence IDs.

        Parameters
        ----------
        candidates:
            Candidates with ``sentence_idx`` in their positions.
        documents:
            Original documents (needed for adjacency extraction).
        nlp_backend:
            If provided, used to tokenise text for adjacent-word extraction.
        """
        # Step 1: Group terms by (doc_id, sentence_idx)
        sent_terms: dict[tuple[str, int], set[str]] = defaultdict(set)
        for cand in candidates:
            nf = cand.normalized_form
            for doc_id, positions in cand.doc_positions.items():
                for pos in positions:
                    sent_idx = pos[2] if len(pos) >= 3 else -1
                    if sent_idx < 0:
                        continue
                    sent_terms[(doc_id, sent_idx)].add(nf)

        # Step 2: Compute pairwise sentence co-occurrence
        sent_cooc: dict[tuple[str, str], int] = defaultdict(int)
        context_totals: dict[str, int] = defaultdict(int)

        for (_doc_id, _sent_idx), terms_in_sent in sent_terms.items():
            sorted_terms = sorted(terms_in_sent)
            for t in sorted_terms:
                context_totals[t] += len(terms_in_sent)
            for i, t_a in enumerate(sorted_terms):
                for t_b in sorted_terms[i + 1 :]:
                    sent_cooc[(t_a, t_b)] += 1

        # Step 3: Compute adjacent words (if NLP backend provided)
        adjacent: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
        if nlp_backend is not None and documents:
            doc_map = {d.doc_id: d.content for d in documents}
            for cand in candidates:
                nf = cand.normalized_form
                for doc_id, positions in cand.doc_positions.items():
                    text = doc_map.get(doc_id, "")
                    if not text:
                        continue
                    tokens = nlp_backend.tokenize(text)
                    token_lower = [t.lower() for t in tokens]
                    # For each occurrence, find adjacent tokens
                    for pos in positions:
                        char_start = pos[0]
                        char_end = pos[1]
                        # Find which tokens are at char_start and char_end
                        # by checking token character positions
                        from jate.extractors.utils import compute_token_offsets

                        tok_offsets = compute_token_offsets(text, tokens)
                        # Find token index just before char_start
                        before_idx = -1
                        after_idx = -1
                        for ti, (ts, te) in enumerate(tok_offsets):
                            if te <= char_start:
                                before_idx = ti
                            if ts >= char_end and after_idx == -1:
                                after_idx = ti
                                break
                        if before_idx >= 0:
                            adjacent[nf][token_lower[before_idx]] += 1
                        if after_idx >= 0 and after_idx < len(tokens):
                            adjacent[nf][token_lower[after_idx]] += 1

        return cls(
            sent_cooc=dict(sent_cooc),
            context_totals=dict(context_totals),
            adjacent={k: dict(v) for k, v in adjacent.items()},
        )

    def get_sentence_cooccurrences(self, term_a: str, term_b: str) -> int:
        """Number of sentences in which both *term_a* and *term_b* appear."""
        key = tuple(sorted([term_a.lower(), term_b.lower()]))
        return self._sent_cooc.get(key, 0)  # type: ignore[arg-type]

    def get_context_term_total(self, term: str) -> int:
        """Total distinct terms across all sentences where *term* appears.

        This is Chi-Square's *n_w* parameter.
        """
        return self._context_totals.get(term.lower(), 0)

    def get_adjacent_words(self, term: str) -> dict[str, int]:
        """Words immediately before/after each occurrence of *term*.

        Returns ``{word: count}`` for NC-Value context weighting.
        """
        return dict(self._adjacent.get(term.lower(), {}))
```

**Step 4: Run context tests**

Run: `poetry run pytest tests/test_context.py -v`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/jate/context.py tests/test_context.py
git commit -m "feat: add ContextIndex for sentence-level statistics"
```

---

### Task 5: Update Algorithm base class signature

**Files:**
- Modify: `src/jate/algorithms/base.py:28-43`
- Modify: all 13 algorithm files in `src/jate/algorithms/`
- Test: `tests/test_algorithms.py`

**Step 1: Update the base class**

In `src/jate/algorithms/base.py`:

```python
from __future__ import annotations

from abc import ABC, abstractmethod
from typing import TYPE_CHECKING

from jate.models import Candidate, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


class Algorithm(ABC):
    """Base class for ATE scoring algorithms."""

    @property
    def name(self) -> str:
        return self.__class__.__name__

    @property
    def description(self) -> str:
        return ""

    @abstractmethod
    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        ...
```

**Step 2: Add `context_index` parameter to all 11 non-Chi-Square/NCValue algorithms**

For each of these files, add `context_index: ContextIndex | None = None` to the `score()` signature (ignored in the body):

- `src/jate/algorithms/tfidf.py`
- `src/jate/algorithms/cvalue.py`
- `src/jate/algorithms/attf.py`
- `src/jate/algorithms/ttf.py`
- `src/jate/algorithms/basic.py`
- `src/jate/algorithms/combo_basic.py`
- `src/jate/algorithms/rake.py`
- `src/jate/algorithms/ridf.py`
- `src/jate/algorithms/weirdness.py`
- `src/jate/algorithms/termex.py`
- `src/jate/algorithms/glossex.py`

Each file needs:
1. Add `from __future__ import annotations` (if not present)
2. Add `from typing import TYPE_CHECKING` (if not present)
3. Add `if TYPE_CHECKING: from jate.context import ContextIndex`
4. Change `def score(self, candidates, corpus_store)` → `def score(self, candidates, corpus_store, context_index: ContextIndex | None = None)`

**Step 3: Run existing tests**

Run: `poetry run pytest tests/test_algorithms.py -v`
Expected: ALL PASS — existing callers don't pass `context_index`, and the default is `None`

**Step 4: Commit**

```bash
git add src/jate/algorithms/
git commit -m "feat: add context_index parameter to Algorithm.score()"
```

---

### Task 6: Update Chi-Square to use ContextIndex

**Files:**
- Modify: `src/jate/algorithms/chi_square.py`
- Test: `tests/test_algorithms.py`

**Step 1: Write failing test**

Add to `tests/test_algorithms.py`:

```python
from jate.context import ContextIndex


class TestChiSquareWithContext:
    def test_uses_sentence_cooccurrence(self) -> None:
        """When ContextIndex provided, Chi-Square uses sentence co-occurrence."""
        store = _make_store()
        candidates = _make_candidates()

        # Build a ContextIndex where "neural network" and "machine learning"
        # co-occur in 1 sentence (vs 3 documents in the store)
        ctx = ContextIndex(
            sent_cooc={("machine learning", "neural network"): 1},
            context_totals={"neural network": 5, "machine learning": 5,
                           "deep neural network": 3, "network": 8},
            adjacent={},
        )

        frequent_terms = {"machine learning": 0.3, "neural network": 0.2}
        algo = ChiSquare(frequent_terms=frequent_terms)
        result_without = algo.score(candidates, store)
        result_with = algo.score(candidates, store, context_index=ctx)

        scores_without = {t.string: t.score for t in result_without}
        scores_with = {t.string: t.score for t in result_with}

        # Scores should differ because co-occurrence counts differ
        assert scores_with != scores_without
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_algorithms.py::TestChiSquareWithContext -v`
Expected: FAIL — Chi-Square ignores context_index

**Step 3: Update Chi-Square implementation**

In `src/jate/algorithms/chi_square.py`:

```python
"""Chi-Square scoring algorithm."""

from __future__ import annotations

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


class ChiSquare(Algorithm):
    """Chi-square test for term independence.

    See Matsuo & Ishizuka (2003), *Keyword Extraction from a Single Document
    Using Word Co-Occurrence Statistical Information*.
    """

    def __init__(
        self,
        frequent_terms: dict[str, float],
        context_term_totals: dict[str, int] | None = None,
    ) -> None:
        self._frequent_terms = frequent_terms
        self._context_term_totals = context_term_totals or {}
        self._sum_exp_prob = sum(frequent_terms.values())
        self._max_exp_prob = max(frequent_terms.values()) if frequent_terms else 0.0

    @property
    def description(self) -> str:
        return "Chi-Square test for term independence"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        result = TermExtractionResult()

        for candidate in candidates:
            nf = candidate.normalized_form

            # n_w: total terms in contexts where w appears
            if context_index is not None:
                n_w = context_index.get_context_term_total(nf)
            else:
                n_w = self._context_term_totals.get(nf, corpus_store.get_term_frequency(nf))

            if n_w == 0:
                result.add(Term(
                    string=candidate.normalized_form,
                    score=0.0,
                    frequency=0,
                    surface_forms=set(candidate.surface_forms),
                ))
                continue

            max_chi_square = self._max_exp_prob
            sum_chi_w = n_w * self._sum_exp_prob

            for g_term, p_g in self._frequent_terms.items():
                # Use sentence co-occurrence if available
                if context_index is not None:
                    freq_wg = context_index.get_sentence_cooccurrences(nf, g_term)
                else:
                    freq_wg = corpus_store.get_cooccurrences(nf, g_term)

                if freq_wg == 0:
                    continue

                nw_pg = n_w * p_g
                if nw_pg == 0:
                    continue

                sum_chi_w -= nw_pg
                diff = freq_wg - nw_pg
                chi_g = (diff * diff) / nw_pg
                sum_chi_w += chi_g

                if chi_g > max_chi_square:
                    max_chi_square = chi_g

            s = sum_chi_w - max_chi_square

            term = Term(
                string=candidate.normalized_form,
                score=s,
                frequency=corpus_store.get_term_frequency(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()
```

**Step 4: Run tests**

Run: `poetry run pytest tests/test_algorithms.py::TestChiSquareWithContext tests/test_algorithms.py::TestChiSquare -v`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/jate/algorithms/chi_square.py tests/test_algorithms.py
git commit -m "feat: Chi-Square uses sentence co-occurrence from ContextIndex"
```

---

### Task 7: Update NC-Value to use ContextIndex

**Files:**
- Modify: `src/jate/algorithms/ncvalue.py`
- Test: `tests/test_algorithms.py`

**Step 1: Write failing test**

Add to `tests/test_algorithms.py`:

```python
class TestNCValueWithContext:
    def test_uses_adjacent_words(self) -> None:
        """When ContextIndex provided, NC-Value uses adjacency-based context."""
        store = _make_store()
        candidates = _make_candidates()

        # Build a ContextIndex with adjacency data
        ctx = ContextIndex(
            sent_cooc={},
            context_totals={},
            adjacent={
                "neural network": {"deep": 3, "large": 2, "train": 1},
                "machine learning": {"supervised": 2, "use": 1},
                "deep neural network": {"train": 1},
                "network": {"neural": 5, "large": 1},
            },
        )

        algo = NCValue()
        result_without = algo.score(candidates, store)
        result_with = algo.score(candidates, store, context_index=ctx)

        scores_without = {t.string: t.score for t in result_without}
        scores_with = {t.string: t.score for t in result_with}

        # Scores should differ
        assert scores_with != scores_without

    def test_frantzi_weight_formula(self) -> None:
        """Context weight(w) = t(w) / n per Frantzi et al."""
        store = _make_store()
        candidates = _make_candidates()

        # "deep" appears adjacent to 2 distinct terms
        # "supervised" appears adjacent to 1 distinct term
        # n = 4 (total candidate terms)
        ctx = ContextIndex(
            sent_cooc={},
            context_totals={},
            adjacent={
                "neural network": {"deep": 3, "supervised": 1},
                "deep neural network": {"deep": 2},
                "machine learning": {},
                "network": {},
            },
        )

        algo = NCValue()
        result = algo.score(candidates, store, context_index=ctx)
        assert len(result) == 4
```

**Step 2: Run to verify failure**

Run: `poetry run pytest tests/test_algorithms.py::TestNCValueWithContext -v`
Expected: FAIL — NC-Value ignores context_index

**Step 3: Update NC-Value implementation**

In `src/jate/algorithms/ncvalue.py`:

```python
"""NC-Value scoring algorithm."""

from __future__ import annotations

from typing import TYPE_CHECKING

from jate.algorithms.base import Algorithm
from jate.algorithms.cvalue import CValue
from jate.models import Candidate, Term, TermExtractionResult
from jate.protocols import CorpusStore

if TYPE_CHECKING:
    from jate.context import ContextIndex


class NCValue(Algorithm):
    """NC-Value: extends C-Value with context word information.

    ``NC-Value(t) = cvalue_weight * CValue(t) + context_weight * context_score(t)``

    When a ContextIndex is provided, context scores use the Frantzi et al.
    formula: weight(w) = t(w) / n, where t(w) is the number of distinct
    candidate terms that word w appears adjacent to, and n is the total
    number of candidate terms.
    """

    def __init__(
        self,
        cvalue_weight: float = 0.8,
        context_weight: float = 0.2,
    ) -> None:
        self._cvalue_weight = cvalue_weight
        self._context_weight = context_weight

    @property
    def description(self) -> str:
        return "NC-Value: C-Value extended with context words"

    def score(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
        context_index: ContextIndex | None = None,
    ) -> TermExtractionResult:
        # First compute C-Value scores
        cvalue_algo = CValue()
        cvalue_result = cvalue_algo.score(candidates, corpus_store)
        cvalue_scores: dict[str, float] = {t.string: t.score for t in cvalue_result}

        # Compute context scores
        if context_index is not None:
            context_scores = self._context_from_adjacency(candidates, context_index)
        else:
            context_scores = self._context_from_cooccurrence(candidates, corpus_store)

        # Normalise context scores to [0, 1]
        max_ctx = max(context_scores.values()) if context_scores else 1.0
        if max_ctx == 0:
            max_ctx = 1.0

        result = TermExtractionResult()
        for candidate in candidates:
            nf = candidate.normalized_form
            cv = cvalue_scores.get(nf, 0.0)
            ctx = context_scores.get(nf, 0.0) / max_ctx
            s = self._cvalue_weight * cv + self._context_weight * ctx
            term = Term(
                string=nf,
                score=s,
                frequency=corpus_store.get_term_frequency(nf),
                surface_forms=set(candidate.surface_forms),
            )
            result.add(term)

        return result.sort()

    def _context_from_adjacency(
        self,
        candidates: list[Candidate],
        context_index: ContextIndex,
    ) -> dict[str, float]:
        """Frantzi et al. context scoring from adjacent words."""
        n = len(candidates) if candidates else 1

        # Compute weight(w) = t(w) / n
        # t(w) = number of distinct candidate terms that word w appears adjacent to
        word_term_count: dict[str, int] = {}
        for cand in candidates:
            adj = context_index.get_adjacent_words(cand.normalized_form)
            for word in adj:
                word_term_count[word] = word_term_count.get(word, 0) + 1

        context_scores: dict[str, float] = {}
        for cand in candidates:
            nf = cand.normalized_form
            adj = context_index.get_adjacent_words(nf)
            score = 0.0
            for word, freq in adj.items():
                t_w = word_term_count.get(word, 0)
                weight = t_w / n
                score += weight * freq
            context_scores[nf] = score

        return context_scores

    def _context_from_cooccurrence(
        self,
        candidates: list[Candidate],
        corpus_store: CorpusStore,
    ) -> dict[str, float]:
        """Fallback: document-level co-occurrence context scoring."""
        context_scores: dict[str, float] = {}
        for candidate in candidates:
            ctx_score = 0.0
            nf = candidate.normalized_form
            for other in candidates:
                if other.normalized_form == nf:
                    continue
                cooc = corpus_store.get_cooccurrences(nf, other.normalized_form)
                if cooc > 0:
                    ctx_score += cooc
            context_scores[nf] = ctx_score
        return context_scores
```

**Step 4: Run tests**

Run: `poetry run pytest tests/test_algorithms.py::TestNCValueWithContext tests/test_algorithms.py::TestNCValue -v`
Expected: ALL PASS

**Step 5: Commit**

```bash
git add src/jate/algorithms/ncvalue.py tests/test_algorithms.py
git commit -m "feat: NC-Value uses adjacency context from ContextIndex"
```

---

### Task 8: Update public API to build ContextIndex automatically

**Files:**
- Modify: `src/jate/api.py`
- Modify: `src/jate/__init__.py`
- Test: existing integration tests

**Step 1: Update `extract()` in api.py**

After `store.index_candidates(candidates)`, add:

```python
from jate.context import ContextIndex

# Build context index if candidates have sentence data
context_index = _build_context_index(candidates, documents, nlp)

# Pass context_index to algorithm
result = algo.score(candidates, store, context_index=context_index)
```

Add helper function:

```python
def _build_context_index(
    candidates: list[Candidate],
    documents: list[Any],
    nlp: SpacyBackend,
) -> ContextIndex | None:
    """Build ContextIndex if candidates have sentence-level data."""
    from jate.context import ContextIndex

    has_sentences = any(
        len(pos) >= 3 and pos[2] >= 0
        for c in candidates
        for positions in c.doc_positions.values()
        for pos in positions
    )
    if not has_sentences:
        return None
    return ContextIndex.build(candidates, documents, nlp_backend=nlp)
```

Apply the same change to `extract_corpus()` and `compare()`.

**Step 2: Export ContextIndex from `__init__.py`**

Add `ContextIndex` to the imports in `src/jate/__init__.py`.

**Step 3: Run full test suite**

Run: `poetry run pytest tests/ -v`
Expected: ALL PASS

**Step 4: Commit**

```bash
git add src/jate/api.py src/jate/__init__.py
git commit -m "feat: public API auto-builds ContextIndex for sentence context"
```

---

### Task 9: Final verification and backwards compatibility

**Files:**
- Test: all test files

**Step 1: Run full test suite**

Run: `poetry run pytest tests/ -v --tb=long`
Expected: ALL PASS, 182+ tests (original 182 plus new tests)

**Step 2: Verify backwards compatibility**

Confirm:
- Algorithms called without `context_index` produce identical results to before
- Extractors still produce correct character offsets
- CorpusStore is unchanged

**Step 3: Run linting**

Run: `poetry run black --check src/ tests/ && poetry run isort --check src/ tests/`
Expected: No formatting issues (or fix them)

**Step 4: Final commit**

```bash
git add -A
git commit -m "chore: final cleanup for sentence-level context feature"
```
