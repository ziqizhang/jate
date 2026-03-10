"""Benchmark runner for evaluating JATE algorithms against gold standards."""

from __future__ import annotations

from typing import Any

from jate.evaluation import EvaluationResult, Evaluator
from jate.models import Document


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
        **algo_kwargs:
            Extra kwargs forwarded to each algorithm constructor.

        Returns
        -------
        dict[str, EvaluationResult]
            Mapping of algorithm name to its evaluation result.
        """
        from jate.api import _build_features, _resolve_algorithm, _resolve_extractor
        from jate.features import TermFrequency
        from jate.nlp.spacy_backend import SpacyBackend
        from jate.store.memory_store import MemoryCorpusStore

        if algorithms is None:
            algorithms = ["tfidf", "cvalue", "rake", "ridf", "basic"]

        # Shared NLP pipeline and candidate extraction
        nlp = SpacyBackend(model)
        store = MemoryCorpusStore()
        ext = _resolve_extractor(extractor)
        candidates = ext.extract(documents, nlp, store)
        store.index_candidates(candidates)

        # Build shared term frequency
        total_docs = len(documents)
        term_freq = TermFrequency.build(candidates, total_docs)

        evaluator = Evaluator(gold_terms)
        results: dict[str, EvaluationResult] = {}

        for algo_name in algorithms:
            algo = _resolve_algorithm(algo_name, **algo_kwargs)
            score_kwargs = _build_features(algo, candidates, documents, nlp, term_freq)
            result = algo.score(candidates, term_freq, **score_kwargs)
            result = result.filter_by_frequency(min_frequency)
            # Assign ranks
            for i, term in enumerate(result):
                term.rank = i + 1

            if top_k is not None:
                ev = evaluator.evaluate_at_k(result, top_k)
            else:
                ev = evaluator.evaluate(result)
            results[algo_name] = ev

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
