#!/usr/bin/env python
"""JATE spaCy Pipeline Demo.

Shows how to use JATE as a native spaCy pipeline component.
Terms are extracted with character-level offsets, so you can
trace each term back to its exact position in the source text.

Usage:
    python examples/spacy_demo.py
    python examples/spacy_demo.py --algorithm rake
    python examples/spacy_demo.py --text "Your custom text here"
"""

from __future__ import annotations

import argparse
import warnings

# Suppress spaCy model version warnings
warnings.filterwarnings("ignore", category=UserWarning, module="spacy")


def main() -> None:
    parser = argparse.ArgumentParser(description="JATE spaCy Pipeline Demo")
    parser.add_argument(
        "--algorithm",
        default="cvalue",
        help="Scoring algorithm (default: cvalue)",
    )
    parser.add_argument(
        "--model",
        default="en_core_web_sm",
        help="spaCy model (default: en_core_web_sm)",
    )
    parser.add_argument(
        "--text",
        default=None,
        help="Custom text to process (default: built-in example)",
    )
    parser.add_argument(
        "--top",
        type=int,
        default=10,
        help="Show top N terms (default: 10)",
    )
    args = parser.parse_args()

    import spacy

    import jate  # noqa: F401 — registers the spaCy factory

    # --- Setup ---
    print("=== JATE spaCy Pipeline Demo ===\n")
    print(f"Loading spaCy model '{args.model}' ...")
    nlp = spacy.load(args.model)

    print(f'Adding JATE: nlp.add_pipe("jate", config={{"algorithm": "{args.algorithm}"}})')
    nlp.add_pipe("jate", config={"algorithm": args.algorithm})
    print(f"Pipeline: {nlp.pipe_names}\n")

    # --- Process ---
    text = args.text or (
        "Machine learning and neural networks have transformed artificial intelligence. "
        "Deep learning models, particularly convolutional neural networks, excel at "
        "image recognition tasks. Recurrent neural networks handle sequential data "
        "processing, while transformer architectures have revolutionized natural "
        "language processing and natural language understanding."
    )

    print("--- Document ---")
    print(f'"{text}"\n')

    doc = nlp(text)

    terms = doc._.terms[: args.top]

    if not terms:
        print("No terms extracted.")
        if args.algorithm == "tfidf":
            print("(TF-IDF cannot score single documents — try cvalue or basic instead)")
        return

    # --- Display ---
    print(f"--- Extracted Terms (top {len(terms)}) ---\n")
    for term in terms:
        print(f"  {term.rank:2d}. {term.string:35s}  score={term.score:.4f}  freq={term.frequency}")
        for span in term.spans:
            surface = doc.text[span.start : span.end]
            print(f'      -> "{surface}" [{span.start}:{span.end}]')

    print("\n--- Summary ---")
    print(f"Algorithm: {args.algorithm}")
    print(f"Total terms extracted: {len(doc._.terms)}")
    print(f"Pipeline components: {nlp.pipe_names}")
    print("NLP processing: reused from spaCy (no double computation)")


if __name__ == "__main__":
    main()
