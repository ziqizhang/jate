"""Corpus page routes — multi-document, multi-algorithm term extraction."""

from __future__ import annotations

import asyncio
import csv
import io
import json
import threading
import time
import uuid
import warnings
from collections.abc import AsyncGenerator
from pathlib import Path
from typing import Any

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, Response, StreamingResponse
from fastapi.templating import Jinja2Templates

from jate.ui.algo_registry import ALGO_REGISTRY

router = APIRouter()
templates = Jinja2Templates(directory=Path(__file__).parent / "templates")

# In-memory job store (single-user local app).
_jobs: dict[str, dict[str, Any]] = {}


@router.get("/corpus", response_class=HTMLResponse)
async def corpus_page(request: Request) -> HTMLResponse:
    """Render the corpus page with all algorithms from registry."""
    return templates.TemplateResponse(
        "corpus.html",
        {
            "request": request,
            "algorithms": ALGO_REGISTRY,
        },
    )


@router.post("/corpus/scan")
async def scan_directory(request: Request) -> HTMLResponse:
    """Scan a directory and return file stats as a partial."""
    form = await request.form()
    directory = str(form.get("directory", "")).strip()

    if not directory:
        return HTMLResponse("<div class='error-banner'>Please enter a directory path.</div>")

    path = Path(directory)
    if not path.is_dir():
        return HTMLResponse(f"<div class='error-banner'>Directory not found: {directory}</div>")

    valid_files: list[tuple[str, int]] = []
    large_files: list[tuple[str, int]] = []
    skipped_files: list[str] = []
    total_size = 0

    for f in sorted(path.iterdir()):
        if not f.is_file():
            continue
        if f.suffix.lower() != ".txt":
            skipped_files.append(f.name)
            continue
        size = f.stat().st_size
        total_size += size
        if size > 1_048_576:  # 1 MB
            large_files.append((f.name, size))
        valid_files.append((f.name, size))

    return templates.TemplateResponse(
        "partials/_file_scan.html",
        {
            "request": request,
            "valid_files": valid_files,
            "large_files": large_files,
            "skipped_files": skipped_files,
            "total_size": total_size,
        },
    )


@router.post("/corpus/run")
async def run_corpus(request: Request) -> HTMLResponse:
    """Start corpus extraction in a background thread."""
    form = await request.form()
    directory = str(form.get("directory", "")).strip()

    # Algorithm chips submitted as multiple values with same name.
    algorithms: list[str] = [str(a) for a in form.getlist("algorithms")]
    if not algorithms:
        algo_str = str(form.get("algorithms", ""))
        algorithms = [a.strip() for a in algo_str.split(",") if a.strip()]

    if not algorithms:
        return HTMLResponse("<div class='error-banner'>Please select at least one algorithm.</div>")

    if not directory or not Path(directory).is_dir():
        return HTMLResponse("<div class='error-banner'>Please enter a valid directory path.</div>")

    if not any(Path(directory).glob("*.txt")):
        return HTMLResponse("<div class='error-banner'>No .txt files found in the specified directory.</div>")

    top_n = int(str(form.get("top_n", "20")))
    min_frequency = int(str(form.get("min_frequency", "2")))
    pattern = str(form.get("pattern", "default"))

    # Collect per-algorithm params.
    algo_params: dict[str, dict[str, Any]] = {}
    for algo_name in algorithms:
        algo_info = ALGO_REGISTRY.get(algo_name, {})
        algo_param_defs = algo_info.get("params", {})
        assert isinstance(algo_param_defs, dict)
        params: dict[str, Any] = {}
        for param_name, param_def in algo_param_defs.items():
            val = form.get(f"algo_param_{algo_name}_{param_name}")
            if val is not None:
                val_str = str(val)
                if param_def["type"] == "float":
                    params[param_name] = float(val_str)
                elif param_def["type"] == "int":
                    params[param_name] = int(val_str)
                elif param_def["type"] == "bool":
                    params[param_name] = val_str in ("on", "true", "True")
        if params:
            algo_params[algo_name] = params

    ref_file_raw = str(form.get("reference_frequency_file", "")).strip()
    ref_file: str | None = ref_file_raw or None

    job_id = str(uuid.uuid4())[:8]
    _jobs[job_id] = {
        "done": False,
        "progress": 0,
        "messages": [],
        "results": {},
        "stats": {},
        "error": None,
        "start_time": time.time(),
    }

    thread = threading.Thread(
        target=_run_corpus_job,
        args=(
            job_id,
            directory,
            algorithms,
            pattern,
            min_frequency,
            top_n,
            ref_file,
            algo_params,
        ),
        daemon=True,
    )
    thread.start()

    return templates.TemplateResponse(
        "partials/_progress.html",
        {
            "request": request,
            "job_id": job_id,
        },
    )


def _run_corpus_job(
    job_id: str,
    directory: str,
    algorithms: list[str],
    pattern: str,
    min_frequency: int,
    top_n: int,
    ref_file: str | None,
    algo_params: dict[str, dict[str, Any]],
) -> None:
    """Run corpus extraction in a background thread."""
    job = _jobs[job_id]

    def log(msg: str) -> None:
        from datetime import datetime

        ts = datetime.now().strftime("%H:%M:%S")
        job["messages"].append(f"[{ts}] {msg}")

    try:
        from jate.api import FeatureCache, _is_tagger, _resolve_algorithm, _resolve_extractor, _resolve_tagger
        from jate.config import JATEConfig
        from jate.features import TermFrequency
        from jate.models import Document
        from jate.nlp.spacy_backend import SpacyBackend
        from jate.store.memory_store import MemoryCorpusStore

        # Read files.
        log("Reading corpus files...")
        job["progress"] = 5
        path = Path(directory)
        texts: list[str] = []
        for f in sorted(path.glob("*.txt")):
            content = f.read_text(encoding="utf-8", errors="ignore").strip()
            if content:
                texts.append(content)

        if not texts:
            job["error"] = "No valid text content found in .txt files."
            job["done"] = True
            return

        log(f"Loaded {len(texts)} documents")
        job["progress"] = 10

        # Split algorithms into rankers and taggers.
        tagger_algos: list[str] = []
        ranker_algos: list[str] = []
        for algo in algorithms:
            if _is_tagger(algo):
                tagger_algos.append(algo)
            else:
                ranker_algos.append(algo)

        results: dict[str, list[Any]] = {}
        n_candidates = 0

        # Process rankers (shared NLP pipeline).
        if ranker_algos:
            log("Initializing NLP pipeline...")
            job["progress"] = 15

            documents = [Document(doc_id=f"doc_{i}", content=t) for i, t in enumerate(texts)]
            nlp = SpacyBackend("en_core_web_sm")
            store = MemoryCorpusStore()
            ext = _resolve_extractor("pos_pattern", pattern=pattern)

            log(f"Extracting candidates from {len(documents)} documents...")
            job["progress"] = 20

            with warnings.catch_warnings():
                warnings.filterwarnings("ignore", category=UserWarning)
                candidates = ext.extract(documents, nlp, store)
            store.index_candidates(candidates, compute_cooccurrences=False)

            n_candidates = len(candidates)
            log(f"Extracted {n_candidates} candidates")
            job["progress"] = 40

            term_freq = TermFrequency.build(candidates, len(documents))

            config = JATEConfig(reference_frequency_file=ref_file) if ref_file else None

            # Build features once for all rankers.
            log(f"Building features for {len(ranker_algos)} algorithm(s)...")
            algo_instances = {}
            for name in ranker_algos:
                kw = algo_params.get(name, {})
                algo_instances[name] = _resolve_algorithm(name, **kw)

            feature_cache = FeatureCache.build_for_algorithms(
                list(algo_instances.values()),
                candidates,
                documents,
                nlp,
                term_freq,
                config,
            )
            job["progress"] = 60
            log("Features built")

            # Score each ranker.
            for i, algo_name in enumerate(ranker_algos):
                log(f"Scoring with {algo_name}...")
                algo_inst = algo_instances[algo_name]
                score_kwargs = feature_cache.get_features(algo_inst)

                with warnings.catch_warnings():
                    warnings.filterwarnings("ignore", category=UserWarning)
                    result = algo_inst.score(candidates, term_freq, **score_kwargs)

                result = result.filter_by_frequency(min_frequency)
                for j, term in enumerate(result):
                    term.rank = j + 1
                results[algo_name] = list(result)

                pct = 60 + 30 * (i + 1) / len(ranker_algos)
                job["progress"] = int(pct)
                log(f"{algo_name}: {len(results[algo_name])} terms")

        # Process taggers.
        for tagger_name in tagger_algos:
            log(f"Running {tagger_name} on {len(texts)} documents...")
            tagger = _resolve_tagger(tagger_name)
            all_terms: dict[str, Any] = {}
            with warnings.catch_warnings():
                warnings.filterwarnings("ignore", category=UserWarning)
                for doc_idx, text in enumerate(texts):
                    result = tagger.tag(text)
                    for term in result:
                        key = term.string.lower()
                        if key not in all_terms:
                            all_terms[key] = term
                        else:
                            all_terms[key].frequency += 1
                    if doc_idx % max(1, len(texts) // 5) == 0:
                        log(f"  {tagger_name}: {doc_idx + 1}/{len(texts)} documents")

            results[tagger_name] = sorted(all_terms.values(), key=lambda t: t.score, reverse=True)
            log(f"{tagger_name}: {len(results[tagger_name])} terms")

        job["results"] = results
        job["stats"] = {
            "n_algorithms": len(algorithms),
            "n_documents": len(texts),
            "n_candidates": n_candidates,
            "top_n": top_n,
        }
        job["progress"] = 100
        log("Done!")

    except Exception as e:
        log(f"Error: {e}")
        job["error"] = str(e)
    finally:
        job["done"] = True


@router.get("/corpus/progress/{job_id}")
async def progress_stream(job_id: str) -> StreamingResponse:
    """SSE endpoint streaming progress updates for a corpus job."""

    async def event_generator() -> AsyncGenerator[str, None]:
        last_msg_count = 0
        while True:
            job = _jobs.get(job_id)
            if not job:
                yield "event: error\ndata: Job not found\n\n"
                break

            messages = job["messages"]
            new_messages = messages[last_msg_count:]
            last_msg_count = len(messages)

            data = json.dumps(
                {
                    "progress": job["progress"],
                    "messages": new_messages,
                    "done": job["done"],
                    "error": job.get("error"),
                }
            )
            yield f"data: {data}\n\n"

            if job["done"]:
                break

            await asyncio.sleep(0.5)

    return StreamingResponse(
        event_generator(),
        media_type="text/event-stream",
        headers={"Cache-Control": "no-cache", "X-Accel-Buffering": "no"},
    )


@router.get("/corpus/results/{job_id}")
async def corpus_results(job_id: str, request: Request) -> HTMLResponse:
    """Return the results partial when a job is done."""
    job = _jobs.get(job_id)
    if not job:
        return HTMLResponse("<div class='error-banner'>Job not found.</div>")

    if job.get("error"):
        return HTMLResponse(f"<div class='error-banner'>Error: {job['error']}</div>")

    top_n = job["stats"].get("top_n", 20)
    preview_results = {algo: terms[:top_n] for algo, terms in job["results"].items()}

    elapsed_secs = time.time() - job.get("start_time", time.time())
    elapsed = f"{elapsed_secs:.1f}"

    return templates.TemplateResponse(
        "partials/_corpus_results.html",
        {
            "request": request,
            "results": preview_results,
            "full_counts": {algo: len(terms) for algo, terms in job["results"].items()},
            "job_id": job_id,
            "stats": job["stats"],
            "elapsed": elapsed,
        },
    )


@router.get("/corpus/export/{job_id}/{algorithm}/{fmt}")
async def export_corpus(job_id: str, algorithm: str, fmt: str) -> Response:
    """Download full results for one algorithm as CSV or JSON."""
    job = _jobs.get(job_id)
    if not job or algorithm not in job.get("results", {}):
        return HTMLResponse("Not found", status_code=404)

    terms = job["results"][algorithm]

    if fmt == "csv":
        output = io.StringIO()
        writer = csv.writer(output)
        writer.writerow(["rank", "term", "score", "frequency", "surface_forms"])
        for i, t in enumerate(terms, 1):
            writer.writerow(
                [
                    i,
                    t.string,
                    f"{t.score:.6f}",
                    t.frequency,
                    "; ".join(t.surface_forms),
                ]
            )
        return Response(
            content=output.getvalue(),
            media_type="text/csv",
            headers={"Content-Disposition": f"attachment; filename={algorithm}_results.csv"},
        )
    elif fmt == "json":
        data = [
            {
                "rank": i,
                "term": t.string,
                "score": t.score,
                "frequency": t.frequency,
                "surface_forms": list(t.surface_forms),
            }
            for i, t in enumerate(terms, 1)
        ]
        return Response(
            content=json.dumps(data, indent=2),
            media_type="application/json",
            headers={"Content-Disposition": f"attachment; filename={algorithm}_results.json"},
        )
    else:
        return Response(f"Unknown format: {fmt}", status_code=400)
