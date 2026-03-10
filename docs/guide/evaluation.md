# Evaluation & Benchmarking

JATE includes built-in evaluation tools for measuring term extraction quality against gold standard term lists.

## Evaluation metrics

JATE uses **set-based precision, recall, and F1** — the standard evaluation approach for automatic term extraction, as used in TermEval 2020 and ACTER shared tasks.

- **Precision** = TP / (TP + FP) — what fraction of extracted terms are correct
- **Recall** = TP / (TP + FN) — what fraction of gold terms were found
- **F1** = 2 * P * R / (P + R) — harmonic mean

All comparisons are **case-insensitive exact match** on the normalised (lemmatised) term form.

## Basic evaluation

```python
import jate

# Extract terms
result = jate.extract_corpus(docs, algorithm="cvalue")

# Evaluate against a gold standard
gold = {"machine learning", "neural network", "text classification"}
evaluator = jate.Evaluator(gold_terms=gold)

eval_result = evaluator.evaluate(result)
print(eval_result.summary())
# P=0.2800  R=0.0644  F1=0.1047  TP=28  FP=72  FN=407  predicted=100  gold=435
```

## Precision at k

Evaluate only the top-k ranked terms. Useful for comparing algorithms at the same cutoff:

```python
for k in [50, 100, 200, 500]:
    ev = evaluator.evaluate_at_k(result, k=k)
    print(f"  @{k:4d}:  P={ev.precision:.4f}  R={ev.recall:.4f}  F1={ev.f1:.4f}")
```

## EvaluationResult

The `EvaluationResult` dataclass contains:

| Field | Type | Description |
|-------|------|-------------|
| `precision` | float | Precision score |
| `recall` | float | Recall score |
| `f1` | float | F1 score |
| `true_positives` | int | Terms correctly identified |
| `false_positives` | int | Extracted terms not in gold |
| `false_negatives` | int | Gold terms not extracted |
| `total_predicted` | int | Total extracted terms |
| `total_gold` | int | Total gold standard terms |

## Benchmarking

The `BenchmarkRunner` runs multiple algorithms on a dataset and prints a comparison table:

```python
from jate import BenchmarkRunner
from jate.datasets import AclRdtecMini

dataset = AclRdtecMini()
runner = BenchmarkRunner(
    algorithms=["cvalue", "tfidf", "rake", "ridf", "basic"],
    top_k=100,
)
runner.run(dataset)
```

Output:

```
Algorithm     P        R        F1       #Pred  #TP  #FP
cvalue        0.2800   0.0644   0.1047   100    28   72
tfidf         0.2800   0.0644   0.1047   100    28   72
rake          0.1300   0.0299   0.0486   100    13   87
ridf          0.2800   0.0644   0.1047   100    28   72
basic         0.2800   0.0644   0.1047   100    28   72
```

### CLI benchmark

```bash
jate benchmark --top 100
```

## Built-in datasets

| Dataset | Description |
|---------|-------------|
| `AclRdtecMini` | Small subset of ACL RD-TEC (3 documents, 435 gold terms) |

More datasets (ACTER, GENIA, CoastTerm) are planned.

## Loading your own gold standard

Gold standard files are typically one term per line:

```
machine learning
neural network
text classification
deep learning
```

Load them:

```python
gold_terms = set()
with open("gold.txt") as f:
    for line in f:
        term = line.strip()
        if term:
            gold_terms.add(term)

evaluator = jate.Evaluator(gold_terms=gold_terms)
```

## How normalisation affects evaluation

JATE evaluates using the **normalised (lemmatised, lowercased) form** of each term. This means:

- Gold term `"neural networks"` matches extracted term `"neural network"` (after lemmatisation)
- Comparison is case-insensitive: `"Machine Learning"` matches `"machine learning"`

This follows the approach used in the original Java JATE and aligns with standard ATE evaluation practice (TermEval 2020, ACTER).

!!! note
    If your gold standard contains non-lemmatised terms, JATE will normalise them via lowercasing (but not lemmatisation) during comparison. For best results, ensure your gold standard uses the same normalisation as your extraction pipeline, or provide lemmatised gold terms.
