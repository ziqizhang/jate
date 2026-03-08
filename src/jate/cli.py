"""JATE command-line interface."""

from __future__ import annotations

import argparse
import json
import sys
from pathlib import Path


def _build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(
        prog="jate",
        description="JATE -- Just Automatic Term Extraction",
    )
    subparsers = parser.add_subparsers(dest="command", help="Available commands")

    # --- extract -----------------------------------------------------------
    p_extract = subparsers.add_parser("extract", help="Extract terms from a file or text")
    p_extract.add_argument("input", help="File path or raw text string")
    p_extract.add_argument("--algorithm", default="cvalue", help="Scoring algorithm (default: cvalue)")
    p_extract.add_argument("--extractor", default="pos_pattern", help="Candidate extractor (default: pos_pattern)")
    p_extract.add_argument("--model", default="en_core_web_sm", help="spaCy model (default: en_core_web_sm)")
    p_extract.add_argument("--min-frequency", type=int, default=1, help="Minimum term frequency (default: 1)")
    p_extract.add_argument("--min-words", type=int, default=1, help="Minimum words per term (default: 1)")
    p_extract.add_argument("--max-words", type=int, default=None, help="Maximum words per term")
    p_extract.add_argument("--output", choices=["table", "csv", "json"], default="table", help="Output format")
    p_extract.add_argument("--top", type=int, default=None, help="Limit to top N terms")

    # --- corpus ------------------------------------------------------------
    p_corpus = subparsers.add_parser("corpus", help="Extract terms from a directory of files")
    p_corpus.add_argument("directory", help="Directory containing text files")
    p_corpus.add_argument("--algorithm", default="cvalue", help="Scoring algorithm (default: cvalue)")
    p_corpus.add_argument("--extractor", default="pos_pattern", help="Candidate extractor (default: pos_pattern)")
    p_corpus.add_argument("--model", default="en_core_web_sm", help="spaCy model (default: en_core_web_sm)")
    p_corpus.add_argument("--db", default=None, help="SQLite database path for persistence")
    p_corpus.add_argument("--min-frequency", type=int, default=2, help="Minimum term frequency (default: 2)")
    p_corpus.add_argument("--min-words", type=int, default=1, help="Minimum words per term (default: 1)")
    p_corpus.add_argument("--max-words", type=int, default=None, help="Maximum words per term")
    p_corpus.add_argument("--output", choices=["table", "csv", "json"], default="table", help="Output format")
    p_corpus.add_argument("--top", type=int, default=None, help="Limit to top N terms")

    # --- compare -----------------------------------------------------------
    p_compare = subparsers.add_parser("compare", help="Compare multiple algorithms on the same input")
    p_compare.add_argument("input", help="Directory or file path")
    p_compare.add_argument("--algorithms", default=None, help="Comma-separated algorithm names")
    p_compare.add_argument("--extractor", default="pos_pattern", help="Candidate extractor (default: pos_pattern)")
    p_compare.add_argument("--model", default="en_core_web_sm", help="spaCy model (default: en_core_web_sm)")
    p_compare.add_argument("--top", type=int, default=10, help="Top N terms per algorithm (default: 10)")

    # --- demo --------------------------------------------------------------
    subparsers.add_parser("demo", help="Launch the demo UI")

    return parser


def _format_table(result: object, top: int | None = None) -> str:
    """Format a TermExtractionResult as an aligned text table."""
    from jate.models import TermExtractionResult

    assert isinstance(result, TermExtractionResult)
    terms = list(result)
    if top is not None:
        terms = terms[:top]

    if not terms:
        return "No terms found."

    # Compute column widths
    header = ("Rank", "Term", "Score", "Frequency")
    rows: list[tuple[str, str, str, str]] = []
    for i, t in enumerate(terms):
        rows.append((str(i + 1), t.string, f"{t.score:.4f}", str(t.frequency)))

    widths = [len(h) for h in header]
    for row in rows:
        for j, cell in enumerate(row):
            widths[j] = max(widths[j], len(cell))

    fmt = "  ".join(f"{{:<{w}}}" for w in widths)
    lines = [fmt.format(*header), fmt.format(*("-" * w for w in widths))]
    for row in rows:
        lines.append(fmt.format(*row))
    return "\n".join(lines)


def main() -> None:
    """Entry point for the ``jate`` CLI."""
    parser = _build_parser()
    args = parser.parse_args()

    if args.command is None:
        parser.print_help()
        return

    if args.command == "demo":
        print("Demo UI not yet available. Coming soon!")
        return

    if args.command == "extract":
        _cmd_extract(args)
    elif args.command == "corpus":
        _cmd_corpus(args)
    elif args.command == "compare":
        _cmd_compare(args)


def _cmd_extract(args: argparse.Namespace) -> None:
    from jate.api import extract, extract_corpus

    input_path = Path(args.input)
    if input_path.is_file():
        result = extract_corpus(
            [str(input_path)],
            algorithm=args.algorithm,
            extractor=args.extractor,
            model=args.model,
            min_frequency=args.min_frequency,
            min_words=args.min_words,
            max_words=args.max_words,
        )
    else:
        result = extract(
            args.input,
            algorithm=args.algorithm,
            extractor=args.extractor,
            model=args.model,
            min_frequency=args.min_frequency,
            min_words=args.min_words,
            max_words=args.max_words,
        )

    _print_result(result, args.output, args.top)


def _cmd_corpus(args: argparse.Namespace) -> None:
    from jate.api import extract_corpus

    result = extract_corpus(
        args.directory,
        algorithm=args.algorithm,
        extractor=args.extractor,
        model=args.model,
        db_path=args.db,
        min_frequency=args.min_frequency,
        min_words=args.min_words,
        max_words=args.max_words,
    )
    _print_result(result, args.output, args.top)


def _cmd_compare(args: argparse.Namespace) -> None:
    from jate.api import compare

    algo_list: list[str] | None = None
    if args.algorithms:
        algo_list = [a.strip() for a in args.algorithms.split(",")]

    results = compare(
        args.input,
        algorithms=algo_list,
        extractor=args.extractor,
        model=args.model,
    )

    for algo_name, result in results.items():
        print(f"\n=== {algo_name.upper()} ===")
        print(_format_table(result, args.top))


def _print_result(result: object, output_format: str, top: int | None) -> None:
    from jate.models import TermExtractionResult

    assert isinstance(result, TermExtractionResult)
    if output_format == "csv":
        # Respect --top for csv too
        if top is not None:
            from jate.models import Term

            trimmed = TermExtractionResult(list(result)[:top])
            print(trimmed.to_csv(), end="")
        else:
            print(result.to_csv(), end="")
    elif output_format == "json":
        terms = list(result)
        if top is not None:
            terms = terms[:top]
        data = [
            {
                "term": t.string,
                "score": t.score,
                "frequency": t.frequency,
                "rank": t.rank,
                "metadata": t.metadata,
            }
            for t in terms
        ]
        print(json.dumps(data, ensure_ascii=False, indent=2))
    else:
        print(_format_table(result, top))


if __name__ == "__main__":
    main()
