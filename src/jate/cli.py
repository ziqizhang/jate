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

    # --- benchmark ---------------------------------------------------------
    p_bench = subparsers.add_parser("benchmark", help="Benchmark algorithms against a gold standard")
    p_bench.add_argument(
        "--dataset",
        default="acl_rdtec_mini",
        help="Dataset name (default: acl_rdtec_mini)",
    )
    p_bench.add_argument(
        "--algorithms",
        default=None,
        help="Comma-separated algorithm names (default: all common algorithms)",
    )
    p_bench.add_argument(
        "--extractor",
        default="pos_pattern",
        help="Candidate extractor (default: pos_pattern)",
    )
    p_bench.add_argument(
        "--model",
        default="en_core_web_sm",
        help="spaCy model (default: en_core_web_sm)",
    )
    p_bench.add_argument(
        "--top",
        type=int,
        default=None,
        help="Evaluate only top-K terms per algorithm",
    )
    p_bench.add_argument(
        "--list-datasets",
        action="store_true",
        help="List available datasets and exit",
    )
    p_bench.add_argument(
        "-y",
        "--yes",
        action="store_true",
        help="Skip confirmation prompt for large dataset downloads",
    )
    p_bench.add_argument(
        "--force-redownload",
        action="store_true",
        help="Force re-download even if dataset is already cached",
    )

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
    elif args.command == "benchmark":
        _cmd_benchmark(args)


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


_DATASET_INFO: dict[str, tuple[str, str]] = {
    "acl_rdtec_mini": ("ACL RD-TEC Mini", "3-document test fixture (bundled, no download)"),
    "acl_rdtec": ("ACL RD-TEC 2.0", "Full ACL RD-TEC 2.0 corpus (~35 MB download)"),
    "acter": ("ACTER v1.5", "All 4 English domains combined (~50 MB download)"),
    "acter_corp": ("ACTER v1.5 — Corruption", "Corruption domain only"),
    "acter_equi": ("ACTER v1.5 — Equitation", "Dressage/equitation domain only"),
    "acter_htfl": ("ACTER v1.5 — Heart Failure", "Heart failure domain only"),
    "acter_wind": ("ACTER v1.5 — Wind Energy", "Wind energy domain only"),
    "coastterm": ("CoastTerm", "Coastal area terminology (~10 MB download)"),
}

# Datasets that require download and user confirmation
_LARGE_DATASETS: dict[str, str] = {
    "acl_rdtec": "~35 MB",
    "acter": "~50 MB",
    "acter_corp": "~50 MB",
    "acter_equi": "~50 MB",
    "acter_htfl": "~50 MB",
    "acter_wind": "~50 MB",
    "coastterm": "~10 MB",
}


def _cmd_benchmark(args: argparse.Namespace) -> None:
    from jate.benchmark import BenchmarkRunner

    # Handle --list-datasets
    if getattr(args, "list_datasets", False):
        print("Available datasets:\n")
        for ds_id, (ds_name, ds_desc) in _DATASET_INFO.items():
            print(f"  {ds_id:<20s} {ds_name} — {ds_desc}")
        return

    # Resolve dataset
    dataset_name = args.dataset.lower().strip()

    if dataset_name not in _DATASET_INFO:
        print(f"Unknown dataset: {args.dataset!r}.")
        print("Use --list-datasets to see available datasets.")
        sys.exit(1)

    force_download = getattr(args, "force_redownload", False)

    # Confirmation prompt for large datasets
    if dataset_name in _LARGE_DATASETS and not getattr(args, "yes", False):
        size = _LARGE_DATASETS[dataset_name]
        print(f'Dataset "{dataset_name}" requires downloading {size} from GitHub.')
        print("Running benchmarks on large datasets may take 10-30 minutes.")
        try:
            answer = input("Continue? [y/N]: ").strip().lower()
        except (EOFError, KeyboardInterrupt):
            print("\nAborted.")
            sys.exit(0)
        if answer not in ("y", "yes"):
            print("Aborted.")
            sys.exit(0)

    ds = _resolve_dataset(dataset_name, force_download=force_download)
    documents = ds.documents
    gold_terms = ds.gold_terms

    algo_list: list[str] | None = None
    if args.algorithms:
        algo_list = [a.strip() for a in args.algorithms.split(",")]

    runner = BenchmarkRunner()
    results = runner.run(
        documents,
        gold_terms,
        algorithms=algo_list,
        extractor=args.extractor,
        model=args.model,
        top_k=args.top,
    )
    runner.print_results(results)


def _resolve_dataset(name: str, *, force_download: bool = False) -> object:
    """Instantiate a dataset loader by name.

    Returns an object satisfying the Dataset protocol.
    """
    if name == "acl_rdtec_mini":
        from jate.datasets.acl_rdtec import AclRdtecMini

        return AclRdtecMini()

    if name == "acl_rdtec":
        from jate.datasets.acl_rdtec_full import AclRdtecFull

        return AclRdtecFull(force_download=force_download)

    if name.startswith("acter"):
        from jate.datasets.acter import Acter

        domain = name.removeprefix("acter_") if name != "acter" else None
        return Acter(domain=domain, force_download=force_download)

    if name == "coastterm":
        from jate.datasets.coastterm import CoastTerm

        return CoastTerm(force_download=force_download)

    msg = f"Unknown dataset: {name!r}"
    raise ValueError(msg)


def _print_result(result: object, output_format: str, top: int | None) -> None:
    from jate.models import TermExtractionResult

    assert isinstance(result, TermExtractionResult)
    if output_format == "csv":
        # Respect --top for csv too
        if top is not None:
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
