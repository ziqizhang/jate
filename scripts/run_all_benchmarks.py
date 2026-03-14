"""Run all algorithms on all datasets and produce a results markdown file.

Reports P@K at multiple cutoffs: 100, 500, 1000, 5000, 10000.
Skips cutoffs larger than the number of candidates.
"""

from __future__ import annotations

import sys
import time
from pathlib import Path

# Add src to path
sys.path.insert(0, str(Path(__file__).resolve().parents[1] / "src"))

from jate.api import _build_features, _resolve_algorithm, _resolve_extractor  # noqa: E402
from jate.benchmark import _BNC_REF_PATH, _log  # noqa: E402
from jate.config import JATEConfig  # noqa: E402
from jate.datasets.acl_rdtec import AclRdtecMini  # noqa: E402
from jate.datasets.genia import Genia  # noqa: E402
from jate.evaluation import Evaluator  # noqa: E402
from jate.features import TermFrequency  # noqa: E402
from jate.nlp.spacy_backend import SpacyBackend  # noqa: E402
from jate.store.memory_store import MemoryCorpusStore  # noqa: E402

ALL_ALGORITHMS = [
    "tfidf",
    "cvalue",
    "ncvalue",
    "basic",
    "combobasic",
    "attf",
    "ttf",
    "ridf",
    "rake",
    "chi_square",
    "weirdness",
    "glossex",
    "termex",
]

K_VALUES = [100, 500, 1000, 5000, 10000]

DATASETS = [
    ("acl_rdtec_mini", lambda: AclRdtecMini()),
    ("genia", lambda: Genia()),
]

# Downloadable datasets
try:
    from jate.datasets.acl_rdtec_full import AclRdtecFull

    DATASETS.append(("acl_rdtec", lambda: AclRdtecFull()))
except Exception:
    pass

try:
    from jate.datasets.acter import Acter

    DATASETS.append(("acter", lambda: Acter()))
except Exception:
    pass

try:
    from jate.datasets.coastterm import CoastTerm

    DATASETS.append(("coastterm", lambda: CoastTerm()))
except Exception:
    pass


def run_dataset(ds_name, ds_loader, nlp, config, results_all):
    """Run all algorithms on one dataset."""
    _log(f"\n{'='*60}")
    _log(f"DATASET: {ds_name}")
    _log(f"{'='*60}")

    t_ds = time.time()
    ds = ds_loader()
    documents = ds.documents
    gold_terms = ds.gold_terms

    _log(f"  Documents: {len(documents)}")
    _log(f"  Gold terms: {len(gold_terms)}")

    # Shared extraction
    _log("  NLP processing ...")
    t_nlp = time.time()
    store = MemoryCorpusStore()
    ext = _resolve_extractor("pos_pattern")
    candidates = ext.extract(documents, nlp, store)
    store.index_candidates(candidates, compute_cooccurrences=False)
    nlp_time = time.time() - t_nlp
    _log(f"  NLP + extraction done in {nlp_time:.1f}s — {len(candidates)} candidates")

    # Build term frequency
    term_freq = TermFrequency.build(candidates, len(documents))
    evaluator = Evaluator(gold_terms)

    # Determine which K values are applicable for this dataset
    applicable_ks = [k for k in K_VALUES if k <= len(candidates)]
    _log(f"  P@K cutoffs: {applicable_ks} (max candidates: {len(candidates)})")

    ds_results = []
    for algo_name in ALL_ALGORITHMS:
        _log(f"  Running {algo_name} ...")
        try:
            algo = _resolve_algorithm(algo_name)

            t_feat = time.time()
            score_kwargs = _build_features(algo, candidates, documents, nlp, term_freq, config)
            feat_time = time.time() - t_feat

            t_score = time.time()
            result = algo.score(candidates, term_freq, **score_kwargs)
            score_time = time.time() - t_score

            result = result.filter_by_frequency(1)
            for i, term in enumerate(result):
                term.rank = i + 1

            # Evaluate at each K
            p_at_k = {}
            for k in applicable_ks:
                ev = evaluator.evaluate_at_k(result, k)
                p_at_k[k] = ev.precision

            ds_results.append(
                {
                    "algorithm": algo_name,
                    "p_at_k": p_at_k,
                    "score_time": score_time,
                    "feat_time": feat_time,
                    "error": None,
                }
            )
            p_str = "  ".join(f"P@{k}={p:.4f}" for k, p in p_at_k.items())
            _log(f"    {p_str}  score={score_time:.2f}s feat={feat_time:.2f}s")
        except Exception as e:
            _log(f"    ERROR: {e}")
            import traceback

            traceback.print_exc(file=sys.stderr)
            ds_results.append(
                {
                    "algorithm": algo_name,
                    "p_at_k": {},
                    "score_time": 0,
                    "feat_time": 0,
                    "error": str(e),
                }
            )

    total_ds_time = time.time() - t_ds
    _log(f"  Dataset total: {total_ds_time:.1f}s")

    results_all[ds_name] = {
        "n_docs": len(documents),
        "n_gold": len(gold_terms),
        "n_candidates": len(candidates),
        "nlp_time": nlp_time,
        "applicable_ks": applicable_ks,
        "algorithms": ds_results,
    }


def write_markdown(results_all, output_path):
    """Write results to a markdown file."""
    lines = []
    lines.append("# JATE Benchmark Results")
    lines.append("")
    lines.append(f"Generated: {time.strftime('%Y-%m-%d %H:%M')}")
    lines.append("")
    lines.append("Extractor: `pos_pattern` (default POS regex `(ADJ |VERB )*(NOUN )+`)")
    lines.append("")
    lines.append("spaCy model: `en_core_web_sm`")
    lines.append("")
    lines.append("Metric: **P@K** (precision at top-K ranked terms)")
    lines.append("")
    lines.append("K cutoffs: 100, 500, 1,000, 5,000, 10,000 (where applicable)")
    lines.append("")
    lines.append(
        "Reference corpus: BNC (British National Corpus) frequency list "
        "for algorithms requiring a reference (weirdness, glossex, termex)"
    )
    lines.append("")

    for ds_name, ds_data in results_all.items():
        lines.append(f"## {ds_name}")
        lines.append("")
        lines.append(f"- **Documents**: {ds_data['n_docs']:,}")
        lines.append(f"- **Gold terms**: {ds_data['n_gold']:,}")
        lines.append(f"- **Candidates extracted**: {ds_data['n_candidates']:,}")
        lines.append(f"- **NLP + extraction time**: {ds_data['nlp_time']:.1f}s")
        lines.append("")

        ks = ds_data["applicable_ks"]
        k_headers = " | ".join(f"P@{k:,}" for k in ks)
        lines.append(f"| Algorithm | {k_headers} | Score time |")
        lines.append(f"|-----------|{' | '.join('------' for _ in ks)} | ----------|")

        for r in ds_data["algorithms"]:
            if r["error"]:
                cells = " | ".join("—" for _ in ks)
                lines.append(f"| {r['algorithm']} | {cells} | {r['error']} |")
            else:
                cells = " | ".join(f"{r['p_at_k'].get(k, 0):.4f}" if k in r["p_at_k"] else "—" for k in ks)
                lines.append(f"| {r['algorithm']} | {cells} | {r['score_time']:.2f}s |")

        lines.append("")

    with open(output_path, "w") as f:
        f.write("\n".join(lines))

    _log(f"\nResults written to {output_path}")


def main():
    _log("Initializing spaCy model ...")
    nlp = SpacyBackend("en_core_web_sm")

    # Config with BNC reference
    ref_path = str(_BNC_REF_PATH) if _BNC_REF_PATH.exists() else None
    if ref_path:
        _log(f"Using BNC reference: {_BNC_REF_PATH.name}")
    else:
        _log("WARNING: BNC reference not found, " "reference-based algorithms may underperform")
    config = JATEConfig(reference_frequency_file=ref_path) if ref_path else None

    results_all = {}

    for ds_name, ds_loader in DATASETS:
        try:
            run_dataset(ds_name, ds_loader, nlp, config, results_all)
        except Exception as e:
            _log(f"DATASET {ds_name} FAILED: {e}")
            import traceback

            traceback.print_exc()

    output_path = Path(__file__).resolve().parents[1] / "docs" / "benchmark-results.md"
    write_markdown(results_all, output_path)
    _log("Done!")


if __name__ == "__main__":
    main()
