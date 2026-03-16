#!/usr/bin/env python
"""JATE BERT Tagger Demo.

Shows how to use the transformer-based term tagger for per-document
term extraction with character-level span offsets.

Downloads the pre-trained XLM-R model from HuggingFace on first run (~1GB).

Usage:
    python examples/tagger_demo.py
    python examples/tagger_demo.py --text "Your custom text here"
    python examples/tagger_demo.py --model /path/to/local/model
"""

from __future__ import annotations

import argparse
import warnings

# Suppress spaCy and transformers warnings
warnings.filterwarnings("ignore", category=UserWarning)
warnings.filterwarnings("ignore", category=FutureWarning)


def main() -> None:
    parser = argparse.ArgumentParser(description="JATE BERT Tagger Demo")
    parser.add_argument(
        "--model",
        default=None,
        help="HuggingFace model ID or local path (default: ziqizhang2026/jate-ate-xlmr)",
    )
    parser.add_argument(
        "--text",
        default=None,
        help="Custom text to process (default: built-in example)",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=15,
        help="Show top N terms (default: 15)",
    )
    args = parser.parse_args()

    from jate.algorithms.bert_tagger import BertTagger, XLMRTagger

    # --- Setup ---
    print("=== JATE BERT Tagger Demo ===\n")

    if args.model:
        print(f"Loading custom model: {args.model} ...")
        tagger = BertTagger(args.model)
    else:
        print("Loading XLM-R tagger (ziqizhang2026/jate-ate-xlmr) ...")
        print("(First run downloads ~1GB model from HuggingFace)\n")
        tagger = XLMRTagger()

    # --- Process ---
    text = args.text or (
        "Corruption in public procurement is a major challenge for governments worldwide. "
        "Bribery and money laundering undermine the rule of law and erode public trust. "
        "Anti-corruption agencies work to prevent conflicts of interest and promote "
        "transparency in government spending. International cooperation through mutual "
        "legal assistance treaties helps combat cross-border financial crime."
    )

    print("--- Document ---")
    print(f'"{text}"\n')

    print("Extracting terms ...")
    result = tagger.tag(text)
    terms = list(result)[: args.top]

    if not terms:
        print("No terms extracted.")
        return

    # --- Display ---
    print(f"\n--- Extracted Terms ({len(terms)}) ---\n")
    for i, term in enumerate(terms, 1):
        print(f"  {i:2d}. {term.string:35s}  confidence={term.score:.4f}  freq={term.frequency}")
        for span in term.spans:
            surface = text[span.start : span.end]
            print(f'      -> "{surface}" [{span.start}:{span.end}]')

    print("\n--- Summary ---")
    print(f"Total terms: {len(list(result))}")
    print(f"Model: {tagger._model_name}")
    print("Type: ATETagger (document-level, no corpus statistics needed)")
    print("Note: scores are model confidence, not statistical termhood")


if __name__ == "__main__":
    main()
