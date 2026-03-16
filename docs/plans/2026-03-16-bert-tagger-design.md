# BERT Token Classification Tagger — Design Spec

Issue: #92 (P1.1)
Date: 2026-03-16

## Summary

Add a transformer-based token classification tagger as the first ATETagger implementation. Fine-tune XLM-R (or any HuggingFace model) on ACTER IOB annotations for BIO sequence labelling. Provide a training script + Colab notebook as a fine-tuning framework, and a pre-trained model on HuggingFace Hub as a starting point.

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Primary use case | Fine-tuning framework | Domain-specific models always beat general-purpose |
| Training entry point | Script + Colab notebook | Script for power users, notebook for quick start |
| Training data format | ACTER TSV only (token\tBIO_label) | Simple, matches existing data. YAGNI on converters. |
| Base model | XLM-R default, any HuggingFace model supported | Matches literature, `AutoModel` handles any model |
| Architecture | Thin wrapper — training separate from inference | JATE core stays clean, optional deps minimal |

## File structure

```
src/jate/
  algorithms/
    bert_tagger.py          # BertTagger base + XLMRTagger + RoBERTaTagger

scripts/
  train_tagger.py           # Standalone training script

examples/
  train_bert_tagger.ipynb   # Colab notebook wrapping the script
```

## Dependencies

| What | Packages | When needed | Install |
|------|----------|-------------|---------|
| Inference | `transformers` | `tag()` called | `pip install "jate[neural]"` |
| Training | `transformers`, `torch`, `datasets`, `seqeval` | Running train script | `pip install "jate[neural]"` or Colab |

In `pyproject.toml`:
```toml
[tool.poetry.extras]
neural = ["transformers", "torch", "datasets", "seqeval"]
```

Lazy import in `bert_tagger.py` — if user tries `algorithm="xlmr-tagger"` without extras, clear error message.

## Training script

**CLI:**
```bash
python scripts/train_tagger.py \
  --dataset acter \
  --model xlm-roberta-base \
  --output ./models/jate-ate-xlmr \
  --eval-domain htfl \
  --epochs 10 \
  --batch-size 16 \
  --learning-rate 5e-5
```

**Custom data:**
```bash
python scripts/train_tagger.py \
  --train-file ./my-data/train.tsv \
  --eval-file ./my-data/eval.tsv \
  --model xlm-roberta-base \
  --output ./my-model
```

**Flow:**
1. Load ACTER IOB annotations (or custom TSV files)
2. Split: 3 domains train, 1 held-out eval (default: htfl for TermEval 2020 comparability)
3. Tokenise with HuggingFace tokeniser, handle subword alignment (first subtoken gets label, rest get -100)
4. Fine-tune with HuggingFace `Trainer`, 10 epochs
5. Evaluate on held-out domain, print token-level P/R/F1 via `seqeval`
6. Save model to output directory

**After training**, user uploads:
```bash
huggingface-cli upload ziqizhang/jate-ate-xlmr ./models/jate-ate-xlmr
```

## ATETagger implementation

### Class hierarchy

```python
class BertTagger(ATETagger):
    """Base class for transformer token classification taggers."""
    def __init__(self, model: str):
        self._model_name = model
        self._pipeline = None  # lazy loaded

    def tag(self, doc) -> TermExtractionResult:
        # 1. Lazy load HuggingFace pipeline("token-classification")
        # 2. Run inference on doc.text
        # 3. Group B-I sequences into term spans
        # 4. Deduplicate (same normalised term → merge spans)
        # 5. Return TermExtractionResult with spans populated

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(
            produces_scores=True,     # model confidence
            produces_ranking=False,   # no natural ranking
            produces_offsets=True,    # char-level spans
            produces_labels=False,    # single class (TERM)
        )

    def corpus_level_warning(self) -> str | None:
        # Warns that frequency/rank/corpus stats not available


class XLMRTagger(BertTagger):
    """XLM-RoBERTa tagger (Lang et al. 2021). Multilingual."""
    def __init__(self, model="ziqizhang/jate-ate-xlmr"):
        super().__init__(model)

class RoBERTaTagger(BertTagger):
    """RoBERTa tagger. English only, faster."""
    def __init__(self, model="ziqizhang/jate-ate-roberta"):
        super().__init__(model)
```

### Registry

```python
_TAGGER_NAMES = {
    "xlmr-tagger": XLMRTagger,
    "roberta-tagger": RoBERTaTagger,
}
```

### Inference details

- Uses `transformers.pipeline("token-classification", model=..., aggregation_strategy="simple")`
- HuggingFace pipeline handles subword aggregation, returns spans with char offsets and scores
- Deduplication: same normalised term → one Term with multiple TermSpan entries

## corpus_level_warning (ATETagger base)

Parallel to ATERanker's `doc_level_compatibility`. Warns about attributes the tagger cannot fill meaningfully:

```python
def corpus_level_warning(self) -> str | None:
    return (
        f"{self.name} is a document-level tagger. "
        "Term frequency reflects single-document counts only. "
        "Score reflects model confidence, not statistical termhood. "
        "Rank is not meaningful. "
        "For corpus-level statistics, use an ATERanker with extract_corpus()."
    )
```

Called by the pipeline when tagger results are returned.

## Pipeline integration

### extract()

```python
def extract(text, *, algorithm="cvalue", ...):
    if algorithm in _TAGGER_NAMES:
        tagger = _resolve_tagger(algorithm)
        nlp = SpacyBackend(model)
        doc = nlp.process(text)
        return tagger.tag(doc)
    else:
        # existing ranker pipeline
```

### CLI

Same interface — `jate extract "text" --algorithm xlmr-tagger`

### spaCy component

Detects tagger vs ranker from algorithm name, calls `tag(doc)` directly.

### compare() and benchmark

- Rankers go through FeatureCache, taggers go through `tag()`
- Both produce TermExtractionResult
- Evaluation: set-based P/R/F1 for all, P@K only for rankers
- AlgorithmIncompatibleError catch handles failures

## Model distribution

- Pre-trained model hosted at `ziqizhang/jate-ate-xlmr` on HuggingFace Hub
- Downloaded on demand on first `tag()` call to `~/.cache/huggingface/`
- No impact on `pip install jate` size
- Training done on Google Colab (free T4 GPU, ~1 hour for ACTER)

## Implementation steps

1. Add `corpus_level_warning()` to ATETagger base class
2. Create `bert_tagger.py` with BertTagger, XLMRTagger, RoBERTaTagger
3. Add tagger registry and routing in `api.py`
4. Update spaCy component to handle taggers
5. Update CLI to route tagger algorithms
6. Write training script (`scripts/train_tagger.py`)
7. Write Colab notebook (`examples/train_bert_tagger.ipynb`)
8. Train model on Colab, upload to HuggingFace Hub
9. Add tests
10. Update docs (README, AGENTS.md, architecture-agent.md)
