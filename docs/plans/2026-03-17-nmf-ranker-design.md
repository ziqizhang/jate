# NMF Ranker — Design Spec

Issue: #92 (P2.3)
Date: 2026-03-17

## Summary

Add an unsupervised NMF (Non-negative Matrix Factorization) ranker as a new ATERanker. Applies topic modelling to the document-candidate matrix, extracts top-T candidates per topic, and unions them into a ranked term list.

Based on: Section 3.1 of Nugumanova et al. (2024), "Semantic Non-Negative Matrix Factorization for Term Extraction", Big Data and Cognitive Computing. The standard NMF method described there replicates Nugumanova et al. (2022), "NMF-based approach to automatic term extraction", Expert Systems with Applications — which was evaluated on ACTER (TermEval 2020) and reported as second only to supervised methods.

## Algorithm (aligned with Section 3.1 of the paper)

### Input

- Document-candidate frequency matrix **A** (m × n):
  - m = number of documents
  - n = number of candidate terms (from JATE's POS-pattern extractor)
  - a_ij = raw frequency of candidate j in document i (NOT TF-IDF — paper uses raw counts)

**Adaptation note**: The paper uses single words as matrix columns. We substitute JATE's multi-word POS-pattern candidates, which serve the same role as the paper's n-gram + linguistic filter extraction step. This means multi-word terms like "machine learning" get direct NMF scores without word-level aggregation.

### Decomposition

Apply NMF: **A ≈ WH** where:
- W (m × k) = document-topic matrix
- H (k × n) = topic-candidate matrix
- k = number of topics (user parameter, default 20)

Uses Frobenius norm cost function with multiplicative update rules (Lee & Seung algorithm). This is sklearn's default `NMF(solver='mu', beta_loss='frobenius')`.

### Term extraction (from H)

1. Transpose H to get H^T (n × k) — each row is a candidate, each column is a topic
2. For each of the k topics, extract the top **T** candidates by their weight in that topic
3. Union all extracted candidates across topics (deduplicate)
4. Score each candidate = its coefficient in the topic where it ranked highest
5. Return ranked list sorted by score

This yields up to k × T unique candidates (less after deduplication).

### Parameters

| Parameter | Default | Description |
|-----------|---------|-------------|
| `n_topics` (k) | 20 | Number of latent topics. Paper tested 5, 10, 20, 50, 100. |
| `top_n_per_topic` (T) | 50 | Top candidates extracted per topic. Paper varied this. |

Both are user-configurable. If `top_n_per_topic` is None, all candidates are scored by max weight (no top-T filtering).

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Matrix columns | JATE multi-word candidates | Adapts paper's single-word approach to use our existing candidate pipeline |
| Matrix weighting | Raw frequency (not TF-IDF) | Matches Section 3.1 of the paper |
| NMF algorithm | Frobenius norm, multiplicative updates | Matches paper (Lee & Seung), sklearn default |
| Term extraction | Top-T per topic, union, score = max coefficient | Matches paper's Section 3.1, Figure 3 |
| Dependency | scikit-learn (core) | Lightweight, NMF built in, numpy/scipy already in JATE |

## Implementation

### File: `src/jate/algorithms/nmf.py`

```python
class NMFRanker(ATERanker):
    def __init__(self, n_topics: int = 20, top_n_per_topic: int = 50):
        self._n_topics = n_topics
        self._top_n = top_n_per_topic

    def output_capabilities(self):
        return OutputCapabilities(
            produces_scores=True,
            produces_ranking=True,
            requires_corpus=True,
        )

    def _score(self, candidates, term_freq, **kwargs):
        # 1. Build sparse doc-candidate matrix from term_freq.term2fid
        #    (raw frequency, not TF-IDF)
        # 2. Run sklearn.decomposition.NMF(n_components=self._n_topics)
        # 3. From H (topic-candidate matrix):
        #    - For each topic, find top-T candidates by weight
        #    - Union across all topics
        #    - Score = candidate's max coefficient across topics
        # 4. Build and return TermExtractionResult
```

### Matrix construction

- Rows = documents (unique doc_ids from term_freq.term2fid)
- Columns = candidate terms (normalised forms)
- Values = raw term frequency in that document (from term_freq.term2fid)
- Built as scipy sparse CSR matrix for memory efficiency

### Registration

- `_ALGORITHM_NAMES["nmf"] = NMFRanker` in `api.py`
- No new features needed — uses only `term_freq` which all rankers receive
- No changes to `FeatureCache` or `_FEATURE_NEEDS`

## Files changed

| File | Change |
|------|--------|
| `src/jate/algorithms/nmf.py` | New — NMFRanker class |
| `src/jate/algorithms/__init__.py` | Export NMFRanker |
| `src/jate/api.py` | Register `"nmf"` in `_ALGORITHM_NAMES` |
| `pyproject.toml` | Add `scikit-learn >= 1.0` to core dependencies |
| `tests/test_nmf.py` | New — unit tests |

## Tests

- ATERanker subclass check (auto via structural tests)
- output_capabilities() returns correct values
- Produces ranked results on acl_rdtec_mini fixture (real data, no mocks needed)
- Handles empty candidates gracefully
- Different n_topics and top_n_per_topic values produce different rankings
- Works via `jate.extract_corpus(docs, algorithm="nmf")`
- Works via CLI: `jate corpus path/ --algorithm nmf`
- doc_level_compatibility warns on single document (inherited from ATERanker base)

## What this does NOT include

- Semantic NMF with word embeddings and seed words (Section 3.2 of Paper B) — deferred, can be added later as optional parameter
- Custom NMF solvers beyond sklearn's default
- GPU acceleration
