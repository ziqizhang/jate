"""Benchmark runner for evaluating JATE algorithms against gold standards."""

from __future__ import annotations

import os
import sys
import time
from pathlib import Path
from typing import Any

from jate.evaluation import EvaluationResult, Evaluator
from jate.models import Document


def _log(msg: str) -> None:
    """Print a timestamped progress message to stderr and flush immediately."""
    from datetime import datetime

    ts = datetime.now().strftime("%H:%M:%S")
    print(f"[{ts}] {msg}", file=sys.stderr, flush=True)


# Bundled BNC reference frequency file.
_BNC_REF_PATH = Path(__file__).resolve().parent / "datasets" / "reference" / "bnc_unifrqs.normal"

# Algorithms that require a reference corpus.
_REF_ALGORITHMS = frozenset({"weirdness", "glossex", "termex"})

# Feature names for progress reporting.
_FEATURE_LABELS: dict[str, str] = {
    "containment": "containment (nested term frequencies)",
    "child_containment": "child containment (reverse nesting)",
    "context_freq": "context frequency (sentence co-occurrence)",
    "word_freq": "word frequency (token-level counts)",
    "term_component_index": "term component index (word-level decomposition)",
    "ref_freq": "reference frequency (BNC corpus comparison)",
    "ref_freqs": "reference frequencies (multi-corpus comparison)",
    "cooccurrence": "co-occurrence matrix",
    "chi_square_freq_terms": "chi-square frequent terms",
}


class _ProgressNlpWrapper:
    """Wrapper around SpacyBackend that reports batch progress."""

    def __init__(self, nlp: Any) -> None:
        self._nlp = nlp

    def __getattr__(self, name: str) -> Any:
        return getattr(self._nlp, name)

    def process_batch(self, texts: list[str], batch_size: int = 256) -> list[Any]:
        if len(texts) <= batch_size:
            _log(f"  NLP batch 1/1: processing {len(texts)} documents ...")
            return self._nlp.process_batch(texts, batch_size=batch_size)

        results: list[Any] = []
        total_batches = (len(texts) + batch_size - 1) // batch_size
        for batch_idx in range(0, len(texts), batch_size):
            batch_num = batch_idx // batch_size + 1
            batch = texts[batch_idx : batch_idx + batch_size]
            _log(
                f"  NLP batch {batch_num}/{total_batches}: "
                f"processing documents {batch_idx + 1}-{batch_idx + len(batch)} "
                f"of {len(texts)} ..."
            )
            results.extend(self._nlp.process_batch(batch, batch_size=batch_size))
        return results


def _build_features_with_progress(
    algo: Any,
    candidates: list[Any],
    documents: list[Any],
    nlp: Any,
    term_freq: Any,
    config: Any,
) -> dict[str, Any]:
    """Wrap _build_features to report which features are being computed."""
    from jate.api import _build_features

    t_feat = time.time()
    _log("    Building features ...")
    kwargs = _build_features(algo, candidates, documents, nlp, term_freq, config)
    for key in kwargs:
        label = _FEATURE_LABELS.get(key, key)
        _log(f"      Built: {label}")
    _log(f"    Features ready in {time.time() - t_feat:.1f}s")
    return kwargs


class BenchmarkRunner:
    """Run multiple algorithms on a dataset and evaluate the results."""

    def run(
        self,
        documents: list[Document],
        gold_terms: set[str],
        algorithms: list[str] | None = None,
        *,
        extractor: str = "pos_pattern",
        model: str = "en_core_web_sm",
        top_k: int | None = None,
        min_frequency: int = 1,
        reference_frequency_file: str | None = None,
        **algo_kwargs: Any,
    ) -> dict[str, EvaluationResult]:
        """Run algorithms against *documents* and evaluate against *gold_terms*.

        Parameters
        ----------
        documents:
            Corpus documents.
        gold_terms:
            Gold-standard terms for evaluation.
        algorithms:
            Algorithm names to benchmark. Defaults to a representative set.
        extractor:
            Candidate extractor name.
        model:
            spaCy model name.
        top_k:
            If set, evaluate only the top-K terms per algorithm.
        min_frequency:
            Minimum term frequency filter.
        reference_frequency_file:
            Path to a reference frequency file. If *None*, uses the bundled
            BNC frequency list. If that is also missing, falls back to
            self-reference (with a warning).
        **algo_kwargs:
            Extra kwargs forwarded to each algorithm constructor.

        Returns
        -------
        dict[str, EvaluationResult]
            Mapping of algorithm name to its evaluation result.
        """
        from jate.api import _resolve_algorithm, _resolve_extractor
        from jate.config import JATEConfig
        from jate.features import TermFrequency
        from jate.nlp.spacy_backend import SpacyBackend
        from jate.store.memory_store import MemoryCorpusStore

        if algorithms is None:
            algorithms = ["tfidf", "cvalue", "rake", "ridf", "basic"]

        # Resolve reference frequency file for algorithms that need it
        needs_ref = bool(_REF_ALGORITHMS & set(algorithms))
        ref_path = reference_frequency_file
        if ref_path is None and needs_ref:
            if _BNC_REF_PATH.exists():
                ref_path = str(_BNC_REF_PATH)
                _log(f"Using bundled BNC reference corpus: {_BNC_REF_PATH.name}")
            else:
                _log(
                    "WARNING: No reference frequency file found. "
                    "Algorithms requiring a reference corpus (weirdness, glossex, termex) "
                    "will fall back to self-reference, which produces weaker results. "
                    f"Expected: {_BNC_REF_PATH}"
                )

        config = JATEConfig(reference_frequency_file=ref_path) if ref_path else None

        cpu_count = os.cpu_count() or 1
        t0 = time.time()

        _log(f"System: {cpu_count} CPUs available")
        _log(f"Loading spaCy model '{model}' ...")
        nlp = SpacyBackend(model)
        _log(f"  spaCy uses multi-threaded processing within each batch " f"(batch_size=256, {cpu_count} CPUs)")

        # Wrap NLP backend for progress reporting
        nlp = _ProgressNlpWrapper(nlp)

        # Determine if any algorithm needs store-level co-occurrences.
        # Currently no algorithm reads store.get_cooccurrences() in the
        # benchmark pipeline — co-occurrence features are built separately
        # in _build_features via ContextFrequency. Skip the O(n²) computation
        # unless explicitly needed in the future.
        needs_cooc = False

        _log(f"Extracting candidates from {len(documents)} documents " f"(extractor={extractor}) ...")
        t_extract = time.time()
        store = MemoryCorpusStore()
        ext = _resolve_extractor(extractor)
        candidates = ext.extract(documents, nlp, store)
        t_index = time.time()
        if needs_cooc:
            _log(f"  Indexing candidates with co-occurrence computation " f"(max_workers={cpu_count}) ...")
            store.index_candidates(
                candidates,
                max_workers=cpu_count,
                compute_cooccurrences=True,
            )
        else:
            _log("  Indexing candidates (co-occurrences skipped — not needed) ...")
            store.index_candidates(
                candidates,
                compute_cooccurrences=False,
            )
        _log(
            f"  Extracted {len(candidates)} candidates in "
            f"{time.time() - t_extract:.1f}s "
            f"(indexing: {time.time() - t_index:.1f}s)"
        )

        _log("Building term frequencies ...")
        term_freq = TermFrequency.build(candidates, len(documents))
        _log(f"  {len(term_freq.term2ttf)} unique terms across {len(documents)} documents")

        evaluator = Evaluator(gold_terms)
        eval_mode = f"top-{top_k}" if top_k else "all candidates"
        _log(f"Evaluating {len(algorithms)} algorithm(s) against " f"{len(gold_terms)} gold terms ({eval_mode})")
        results: dict[str, EvaluationResult] = {}

        for i, algo_name in enumerate(algorithms, 1):
            t_algo = time.time()
            _log(f"  [{i}/{len(algorithms)}] Running {algo_name} ...")
            algo = _resolve_algorithm(algo_name, **algo_kwargs)
            score_kwargs = _build_features_with_progress(algo, candidates, documents, nlp, term_freq, config)
            _log(f"    Scoring {len(candidates)} candidates ...")
            result = algo.score(candidates, term_freq, **score_kwargs)
            result = result.filter_by_frequency(min_frequency)
            # Assign ranks
            for j, term in enumerate(result):
                term.rank = j + 1

            if top_k is not None:
                ev = evaluator.evaluate_at_k(result, top_k)
            else:
                ev = evaluator.evaluate(result)
            results[algo_name] = ev
            _log(
                f"  [{i}/{len(algorithms)}] {algo_name} done in "
                f"{time.time() - t_algo:.1f}s — "
                f"P={ev.precision:.4f} R={ev.recall:.4f} F1={ev.f1:.4f}"
            )

        _log(f"Benchmark complete in {time.time() - t0:.1f}s\n")
        return results

    def print_results(self, results: dict[str, EvaluationResult]) -> None:
        """Print a formatted comparison table to stdout."""
        print(format_results_table(results))


def format_results_table(results: dict[str, EvaluationResult]) -> str:
    """Format evaluation results as an aligned text table.

    Returns
    -------
    str
        Multi-line table string.
    """
    header = ("Algorithm", "Precision", "Recall", "F1", "#Predicted", "#TP", "#FP")
    rows: list[tuple[str, ...]] = []
    for name, ev in results.items():
        rows.append(
            (
                name,
                f"{ev.precision:.4f}",
                f"{ev.recall:.4f}",
                f"{ev.f1:.4f}",
                str(ev.total_predicted),
                str(ev.true_positives),
                str(ev.false_positives),
            )
        )

    widths = [len(h) for h in header]
    for row in rows:
        for j, cell in enumerate(row):
            widths[j] = max(widths[j], len(cell))

    fmt = "  ".join(f"{{:<{w}}}" for w in widths)
    lines = [fmt.format(*header), fmt.format(*("-" * w for w in widths))]
    for row in rows:
        lines.append(fmt.format(*row))
    return "\n".join(lines)
