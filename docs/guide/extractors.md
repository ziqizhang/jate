# Candidate Extractors

Candidate extraction is the first stage of the ATE pipeline — it identifies spans of text that *could* be terms, before any scoring happens. The quality of candidates directly affects what the algorithms can find.

JATE provides three extraction strategies.

## POS Pattern (default)

Matches regular expressions over Universal POS tag sequences. The default pattern `(ADJ )*(NOUN )+` captures the most common term structures in English: noun phrases optionally preceded by adjectives.

```python
result = jate.extract(text, extractor="pos_pattern")
```

### How it works

1. Text is POS-tagged by the NLP backend
2. POS tags are joined into a string (e.g. `"ADJ NOUN NOUN"`)
3. The regex is applied to find matching spans
4. Matching spans are mapped back to the original tokens
5. Leading/trailing stopwords are stripped

### Custom patterns

```python
from jate import PosPatternExtractor

# Only noun-noun compounds
extractor = PosPatternExtractor(pattern=r"(NOUN ){2,}")

# Include verbs (e.g. "machine learning")
extractor = PosPatternExtractor(pattern=r"(ADJ |NOUN |VERB )*(NOUN )+")
```

### When to use

- Best general-purpose extractor for English
- Good for multi-word terms
- Relies on accurate POS tagging

## N-gram

Extracts all contiguous token windows of size n (for configurable min_n to max_n). Filters out n-grams that start or end with stopwords or punctuation.

```python
result = jate.extract(text, extractor="ngram")
```

### Configuration

```python
from jate import NGramExtractor

# Only bigrams and trigrams
extractor = NGramExtractor(min_n=2, max_n=3)

# Unigrams through 5-grams (default)
extractor = NGramExtractor(min_n=1, max_n=5)
```

### When to use

- Language-agnostic (doesn't depend on POS tagging quality)
- Produces many more candidates — useful when combined with aggressive scoring algorithms
- Can capture terms that don't follow typical POS patterns

## Noun Phrase

Uses spaCy's built-in noun chunk detector, which identifies base noun phrases using dependency parsing.

```python
result = jate.extract(text, extractor="noun_phrase")
```

### How it works

1. spaCy processes the text and identifies noun chunks
2. Leading/trailing stopwords (determiners, etc.) are stripped
3. Each chunk is lemmatised to produce the normalised form

### When to use

- Leverages spaCy's dependency parsing for linguistically motivated chunks
- Good for languages where spaCy has strong parsing models
- Produces fewer but more precise candidates than POS patterns

## Comparison

| Extractor | Candidates | Precision | Language dependence |
|-----------|-----------|-----------|---------------------|
| POS Pattern | Medium | High | Needs POS tagger |
| N-gram | Many | Lower | Language-agnostic |
| Noun Phrase | Fewer | Highest | Needs dependency parser |

## Normalisation

All three extractors perform the same normalisation:

1. **Lemmatisation** — each token is lemmatised (e.g. "networks" → "network")
2. **Lowercasing** — the normalised form is lowercased
3. **Merging** — candidates with the same normalised form are merged into one entry
4. **Surface tracking** — all distinct surface variants are recorded in `candidate.surface_forms`

This means "Neural Networks", "neural network", and "neural networks" all become a single candidate with normalised form `"neural network"` and three surface forms.
