#!/usr/bin/env python
"""Train a transformer token classification model for automatic term extraction.

Fine-tunes a HuggingFace model (default: XLM-R) on ACTER IOB annotations
for BIO sequence labelling. Produces a model that can be used with JATE's
BertTagger / XLMRTagger.

Usage (with ACTER dataset):
    python scripts/train_tagger.py \\
        --dataset acter \\
        --model xlm-roberta-base \\
        --output ./models/jate-ate-xlmr \\
        --eval-domain htfl \\
        --epochs 10

Usage (with custom TSV files):
    python scripts/train_tagger.py \\
        --train-file ./my-data/train.tsv \\
        --eval-file ./my-data/eval.tsv \\
        --model xlm-roberta-base \\
        --output ./my-model

TSV format: one token per line, tab-separated (token\\tBIO_label).
Blank lines separate sentences. Labels: B, I, O.

After training, upload to HuggingFace Hub:
    huggingface-cli upload ziqizhang/jate-ate-xlmr ./models/jate-ate-xlmr

Requires: pip install transformers torch datasets seqeval
  (or: pip install "jate[neural]")
"""

from __future__ import annotations

import argparse
import sys
import time
from pathlib import Path

# ---------------------------------------------------------------------------
# Data loading
# ---------------------------------------------------------------------------

LABEL_LIST = ["O", "B", "I"]
LABEL2ID = {label: i for i, label in enumerate(LABEL_LIST)}
ID2LABEL = {i: label for i, label in enumerate(LABEL_LIST)}

ACTER_DOMAINS = ("corp", "equi", "htfl", "wind")


def load_iob_file(path: Path) -> list[dict]:
    """Load a single IOB TSV file into a list of sentence dicts.

    Each sentence dict has:
        - tokens: list[str]
        - labels: list[str]  (B, I, O)
    """
    sentences: list[dict] = []
    tokens: list[str] = []
    labels: list[str] = []

    for line in path.read_text(encoding="utf-8").splitlines():
        line = line.strip()
        if not line:
            if tokens:
                sentences.append({"tokens": tokens, "labels": labels})
                tokens = []
                labels = []
            continue
        parts = line.split("\t")
        if len(parts) >= 2:
            tokens.append(parts[0])
            label = parts[1].strip()
            if label not in LABEL2ID:
                label = "O"
            labels.append(label)
        elif len(parts) == 1:
            # Token with no label — treat as O
            tokens.append(parts[0])
            labels.append("O")

    if tokens:
        sentences.append({"tokens": tokens, "labels": labels})

    return sentences


def load_acter_domain(domain: str, acter_dir: Path) -> list[dict]:
    """Load all IOB files for one ACTER domain."""
    iob_dir = (
        acter_dir
        / "en"
        / domain
        / "annotated"
        / "annotations"
        / "sequential_annotations"
        / "iob_annotations"
        / "without_named_entities"
    )
    if not iob_dir.is_dir():
        print(f"WARNING: IOB directory not found: {iob_dir}", file=sys.stderr)
        return []

    sentences: list[dict] = []
    for tsv_path in sorted(iob_dir.glob("*.tsv")):
        sentences.extend(load_iob_file(tsv_path))

    return sentences


def load_acter_dataset(acter_dir: Path, eval_domain: str = "htfl") -> tuple[list[dict], list[dict]]:
    """Load ACTER IOB data, split by domain.

    Returns (train_sentences, eval_sentences).
    Training uses all domains except eval_domain.
    """
    train_sentences: list[dict] = []
    eval_sentences: list[dict] = []

    for domain in ACTER_DOMAINS:
        sentences = load_acter_domain(domain, acter_dir)
        if domain == eval_domain:
            eval_sentences.extend(sentences)
        else:
            train_sentences.extend(sentences)

    return train_sentences, eval_sentences


# ---------------------------------------------------------------------------
# Tokenisation and alignment
# ---------------------------------------------------------------------------


def tokenize_and_align_labels(examples, tokenizer):
    """Tokenise sentences and align BIO labels to subword tokens.

    For each word split into subword tokens, the first subtoken gets
    the original label, the rest get -100 (ignored in loss).
    """
    tokenized = tokenizer(
        examples["tokens"],
        truncation=True,
        is_split_into_words=True,
        max_length=512,
    )

    all_labels = []
    for i, labels in enumerate(examples["labels"]):
        word_ids = tokenized.word_ids(batch_index=i)
        label_ids = []
        prev_word_id = None
        for word_id in word_ids:
            if word_id is None:
                label_ids.append(-100)
            elif word_id != prev_word_id:
                label_ids.append(LABEL2ID[labels[word_id]])
            else:
                # Subword continuation — ignore in loss
                label_ids.append(-100)
            prev_word_id = word_id
        all_labels.append(label_ids)

    tokenized["labels"] = all_labels
    return tokenized


# ---------------------------------------------------------------------------
# Metrics
# ---------------------------------------------------------------------------


def compute_metrics(eval_pred):
    """Compute token-level P/R/F1 using seqeval."""
    from seqeval.metrics import classification_report, f1_score, precision_score, recall_score

    predictions, labels = eval_pred
    import numpy as np

    predictions = np.argmax(predictions, axis=2)

    # Convert IDs back to label strings, ignoring -100
    true_labels = []
    pred_labels = []
    for pred_seq, label_seq in zip(predictions, labels):
        true_sent = []
        pred_sent = []
        for p, l in zip(pred_seq, label_seq):
            if l == -100:
                continue
            true_sent.append(ID2LABEL[l])
            pred_sent.append(ID2LABEL[p])
        true_labels.append(true_sent)
        pred_labels.append(pred_sent)

    p = precision_score(true_labels, pred_labels)
    r = recall_score(true_labels, pred_labels)
    f1 = f1_score(true_labels, pred_labels)

    # Print detailed report
    print("\n" + classification_report(true_labels, pred_labels), file=sys.stderr)

    return {"precision": p, "recall": r, "f1": f1}


# ---------------------------------------------------------------------------
# Main
# ---------------------------------------------------------------------------


def main():
    parser = argparse.ArgumentParser(description="Train a transformer tagger for automatic term extraction")

    # Data source (ACTER or custom files)
    data_group = parser.add_argument_group("Data source")
    data_group.add_argument(
        "--dataset",
        default=None,
        help="Dataset name (currently: 'acter'). Uses JATE's dataset cache.",
    )
    data_group.add_argument(
        "--eval-domain",
        default="htfl",
        help="ACTER domain to hold out for evaluation (default: htfl)",
    )
    data_group.add_argument(
        "--train-file",
        default=None,
        help="Custom training TSV file (token\\tBIO_label, blank-line separated)",
    )
    data_group.add_argument(
        "--eval-file",
        default=None,
        help="Custom evaluation TSV file",
    )

    # Model
    model_group = parser.add_argument_group("Model")
    model_group.add_argument(
        "--model",
        default="xlm-roberta-base",
        help="HuggingFace model name or path (default: xlm-roberta-base)",
    )
    model_group.add_argument(
        "--output",
        required=True,
        help="Output directory for the trained model",
    )

    # Training hyperparameters
    train_group = parser.add_argument_group("Training")
    train_group.add_argument("--epochs", type=int, default=10, help="Number of epochs (default: 10)")
    train_group.add_argument("--batch-size", type=int, default=16, help="Batch size (default: 16)")
    train_group.add_argument("--learning-rate", type=float, default=5e-5, help="Learning rate (default: 5e-5)")
    train_group.add_argument("--weight-decay", type=float, default=0.01, help="Weight decay (default: 0.01)")

    args = parser.parse_args()

    # Validate args
    if args.dataset is None and args.train_file is None:
        parser.error("Provide either --dataset or --train-file")
    if args.dataset and args.train_file:
        parser.error("Provide either --dataset or --train-file, not both")

    # ---------------------------------------------------------------------------
    # Load data
    # ---------------------------------------------------------------------------
    t0 = time.time()

    if args.dataset:
        if args.dataset.lower() != "acter":
            parser.error(f"Unknown dataset: {args.dataset}. Currently supported: acter")

        # Find ACTER data
        acter_dir = Path.home() / ".jate" / "datasets" / "acter"
        if not acter_dir.is_dir():
            print(
                "ACTER dataset not found. Download it first:\n"
                "  poetry run jate benchmark --dataset acter --list-datasets\n"
                "  poetry run jate benchmark --dataset acter -y  # triggers download",
                file=sys.stderr,
            )
            sys.exit(1)

        print(f"Loading ACTER dataset (eval domain: {args.eval_domain}) ...", file=sys.stderr)
        train_sents, eval_sents = load_acter_dataset(acter_dir, args.eval_domain)
    else:
        print(f"Loading custom data from {args.train_file} ...", file=sys.stderr)
        train_sents = load_iob_file(Path(args.train_file))
        eval_sents = load_iob_file(Path(args.eval_file)) if args.eval_file else []

    print(
        f"  Train: {len(train_sents)} sentences, " f"Eval: {len(eval_sents)} sentences",
        file=sys.stderr,
    )

    if not train_sents:
        print("ERROR: No training data found.", file=sys.stderr)
        sys.exit(1)

    # ---------------------------------------------------------------------------
    # Prepare HuggingFace datasets
    # ---------------------------------------------------------------------------
    from datasets import Dataset as HFDataset

    train_ds = HFDataset.from_list(train_sents)
    eval_ds = HFDataset.from_list(eval_sents) if eval_sents else None

    # ---------------------------------------------------------------------------
    # Load tokeniser and model
    # ---------------------------------------------------------------------------
    from transformers import (
        AutoModelForTokenClassification,
        AutoTokenizer,
        DataCollatorForTokenClassification,
        Trainer,
        TrainingArguments,
    )

    print(f"Loading model: {args.model} ...", file=sys.stderr)
    tokenizer = AutoTokenizer.from_pretrained(args.model)
    model = AutoModelForTokenClassification.from_pretrained(
        args.model,
        num_labels=len(LABEL_LIST),
        id2label=ID2LABEL,
        label2id=LABEL2ID,
    )

    # ---------------------------------------------------------------------------
    # Tokenise and align labels
    # ---------------------------------------------------------------------------
    print("Tokenising ...", file=sys.stderr)
    train_tokenized = train_ds.map(
        lambda ex: tokenize_and_align_labels(ex, tokenizer),
        batched=True,
        remove_columns=train_ds.column_names,
    )
    eval_tokenized = None
    if eval_ds:
        eval_tokenized = eval_ds.map(
            lambda ex: tokenize_and_align_labels(ex, tokenizer),
            batched=True,
            remove_columns=eval_ds.column_names,
        )

    data_collator = DataCollatorForTokenClassification(tokenizer=tokenizer)

    # ---------------------------------------------------------------------------
    # Training
    # ---------------------------------------------------------------------------
    output_dir = Path(args.output)

    training_args = TrainingArguments(
        output_dir=str(output_dir / "checkpoints"),
        eval_strategy="epoch" if eval_tokenized else "no",
        save_strategy="epoch",
        learning_rate=args.learning_rate,
        per_device_train_batch_size=args.batch_size,
        per_device_eval_batch_size=args.batch_size,
        num_train_epochs=args.epochs,
        weight_decay=args.weight_decay,
        logging_dir=str(output_dir / "logs"),
        logging_steps=50,
        load_best_model_at_end=True if eval_tokenized else False,
        metric_for_best_model="f1" if eval_tokenized else None,
        save_total_limit=2,
        report_to="none",
    )

    trainer = Trainer(
        model=model,
        args=training_args,
        train_dataset=train_tokenized,
        eval_dataset=eval_tokenized,
        tokenizer=tokenizer,
        data_collator=data_collator,
        compute_metrics=compute_metrics if eval_tokenized else None,
    )

    print(f"Training for {args.epochs} epochs ...", file=sys.stderr)
    trainer.train()

    # ---------------------------------------------------------------------------
    # Final evaluation
    # ---------------------------------------------------------------------------
    if eval_tokenized:
        print("\nFinal evaluation:", file=sys.stderr)
        metrics = trainer.evaluate()
        for k, v in metrics.items():
            if isinstance(v, float):
                print(f"  {k}: {v:.4f}", file=sys.stderr)

    # ---------------------------------------------------------------------------
    # Save
    # ---------------------------------------------------------------------------
    save_dir = output_dir / "model"
    print(f"\nSaving model to {save_dir} ...", file=sys.stderr)
    trainer.save_model(str(save_dir))
    tokenizer.save_pretrained(str(save_dir))

    elapsed = time.time() - t0
    print(f"\nDone in {elapsed / 60:.1f} minutes.", file=sys.stderr)
    print("\nTo use in JATE:", file=sys.stderr)
    print("  from jate.algorithms.bert_tagger import BertTagger", file=sys.stderr)
    print(f'  tagger = BertTagger("{save_dir}")', file=sys.stderr)
    print('  result = tagger.tag("Your text here")', file=sys.stderr)
    print("\nTo upload to HuggingFace Hub:", file=sys.stderr)
    print(f"  huggingface-cli upload ziqizhang/jate-ate-xlmr {save_dir}", file=sys.stderr)


if __name__ == "__main__":
    main()
