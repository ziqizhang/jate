# Algorithms

JATE provides 13 automatic term extraction algorithms. All algorithms implement the same interface: they take a list of candidates and a corpus store, and return a scored, sorted `TermExtractionResult`.

## Choosing an algorithm

| If you need... | Use |
|----------------|-----|
| Good general-purpose multi-word extraction | `cvalue` or `ncvalue` |
| Simple, fast baseline | `tfidf`, `basic`, or `ttf` |
| Keyword extraction (single doc focus) | `rake` |
| Comparison against a reference corpus | `weirdness`, `termex`, or `glossex` |
| Statistical independence testing | `chi_square` |
| Terms that deviate from expected distribution | `ridf` |

## Algorithm reference

### TFIDF

**TF-IDF at corpus level.** `score = tf * log(N / df)` where tf is total term frequency and df is document frequency.

```python
result = jate.extract_corpus(docs, algorithm="tfidf")
```

Simple and effective baseline. Works well when you have enough documents for IDF to be meaningful.

---

### C-Value

**Multi-word term extraction via nested term frequency.** From Frantzi et al. (2000).

`score = log2(|t| + 0.1) * f(t)` for terms not contained in longer terms, adjusted for containment otherwise.

```python
result = jate.extract_corpus(docs, algorithm="cvalue")
```

The default algorithm. Excels at finding multi-word terms by penalising candidates that only appear as parts of longer terms.

---

### NC-Value

**C-Value extended with context word information.** Also from Frantzi et al. (2000).

`score = 0.8 * CValue(t) + 0.2 * context_score(t)`

```python
result = jate.extract_corpus(docs, algorithm="ncvalue")
```

Improves on C-Value by considering which other terms co-occur with the candidate. When sentence-level position data is available (default with all extractors), NC-Value uses **adjacency-based context** — words immediately before/after term occurrences — as described in the original paper. The context score uses the formula `weight(w) = t(w) / n` where `t(w)` is how many unique terms word `w` appears adjacent to and `n` is the total number of unique terms with context.

---

### Basic

**Frequency + containment scoring.** From Bordea, Buitelaar & Polajnar (2013).

`score = |t| * log(f(t)) + alpha * e_t` where e_t is the number of longer terms containing t.

```python
result = jate.extract_corpus(docs, algorithm="basic")
```

---

### ComboBasic

**Basic with parent and child containment.** Also from Bordea et al. (2013).

`score = |t| * log(f(t)) + alpha * e_t + beta * e'_t` where e'_t counts shorter terms contained in t.

```python
result = jate.extract_corpus(docs, algorithm="combobasic")
```

---

### ATTF

**Average Total Term Frequency.** `score = ttf / df`.

```python
result = jate.extract_corpus(docs, algorithm="attf")
```

Rewards terms that appear frequently but are concentrated in fewer documents.

---

### TTF

**Total Term Frequency.** `score = ttf` (raw count).

```python
result = jate.extract_corpus(docs, algorithm="ttf")
```

The simplest possible baseline — pure frequency.

---

### RIDF

**Residual IDF.** From Church & Gale (1995).

`score = IDF_observed - IDF_expected` where the expected IDF is derived from a Poisson model. Terms that appear in fewer documents than their frequency would predict score higher.

```python
result = jate.extract_corpus(docs, algorithm="ridf")
```

---

### RAKE

**Rapid Automatic Keyword Extraction.** From Rose et al. (2010).

For each component word: `word_score = degree(word) / freq(word)`. The candidate score is the sum of word scores.

```python
result = jate.extract_corpus(docs, algorithm="rake")
```

Originally designed for keyword extraction from individual documents. In JATE, it operates at corpus level.

---

### Chi-Square

**Chi-square test for term independence.** From Matsuo & Ishizuka (2003).

Tests whether the co-occurrence of a candidate with frequent reference terms deviates from what would be expected by chance.

```python
result = jate.extract_corpus(docs, algorithm="chi_square")
```

Requires reference term frequencies, which JATE auto-constructs from the corpus. When sentence-level position data is available (default with all extractors), Chi-Square uses **sentence co-occurrence** rather than document co-occurrence, aligning with the original paper (Matsuo & Ishizuka 2003).

---

### Weirdness

**Target vs reference corpus frequency ratio.** From Ahmad et al. (1999).

`score = (1/T) * sum(log(freq_target(w) / freq_reference(w)))` for each component word w.

```python
result = jate.extract_corpus(docs, algorithm="weirdness")
```

Requires a reference corpus. JATE auto-constructs one from the target corpus word frequencies.

---

### TermEx

**Domain pertinence + context + lexical cohesion.** From Sclano et al. (2007).

`score = alpha * DP + beta * DC + zeta * LC` combining domain pertinence (target vs reference), domain consensus (entropy), and lexical cohesion.

```python
result = jate.extract_corpus(docs, algorithm="termex")
```

---

### GlossEx

**Domain specificity via glossary comparison.** From Park et al. (2002).

`score = alpha * TD + beta * TC` where TD is average word-level domain specificity and TC is a term cohesion measure.

```python
result = jate.extract_corpus(docs, algorithm="glossex")
```

Uses different weight ratios for single-word vs multi-word terms.

## Using algorithms directly

For full control, instantiate algorithm classes directly:

```python
from jate import CValue, NCValue, MemoryCorpusStore, PosPatternExtractor, SpacyBackend
from jate.nlp import DocumentLoader

# Load documents
docs = DocumentLoader.load_directory("corpus/")

# Extract candidates
nlp = SpacyBackend()
store = MemoryCorpusStore()
extractor = PosPatternExtractor()
candidates = extractor.extract(docs, nlp, store)
store.index_candidates(candidates)

# Score with multiple algorithms
for algo in [CValue(), NCValue()]:
    result = algo.score(candidates, store)
    print(f"\n{algo.__class__.__name__}:")
    for term in list(result)[:5]:
        print(f"  {term.string:30s}  {term.score:.4f}")
```
