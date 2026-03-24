"""Extract page routes — single-document term extraction."""

from __future__ import annotations

import re
import time
import warnings
from pathlib import Path
from typing import Any

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse, Response
from fastapi.templating import Jinja2Templates

from jate.ui.algo_registry import ALGO_REGISTRY, get_algorithms_for_mode

router = APIRouter()
templates = Jinja2Templates(directory=Path(__file__).parent / "templates")

# Simple in-memory store for last result (single-user local app).
_last_result: dict[str, Any] = {}


@router.get("/extract", response_class=HTMLResponse)
async def extract_page(request: Request) -> HTMLResponse:
    """Render the extract page with the algorithm dropdown."""
    algorithms = get_algorithms_for_mode("extract")
    return templates.TemplateResponse(
        "extract.html",
        {
            "request": request,
            "algorithms": algorithms,
            "selected_algo": "cvalue",
        },
    )


@router.post("/extract/run")
async def run_extract(request: Request) -> HTMLResponse:
    """Run term extraction and return a results partial."""
    form = await request.form()
    text = form.get("text", "")
    uploaded_file = form.get("file")
    algorithm = form.get("algorithm", "cvalue")
    _pattern = form.get("pattern", "default")  # noqa: F841 — reserved for future use
    min_frequency = int(form.get("min_frequency", "1"))
    top_n = int(form.get("top_n", "50"))

    # Read uploaded file if present.
    if uploaded_file and hasattr(uploaded_file, "read") and getattr(uploaded_file, "filename", None):
        content = await uploaded_file.read()
        text = content.decode("utf-8", errors="ignore")

    if not text or len(text.strip()) < 10:
        return HTMLResponse("<div class='error-banner'>Please enter at least 10 characters of text.</div>")

    # Collect algorithm-specific params from form fields prefixed algo_param_.
    algo_kwargs: dict[str, Any] = {}
    algo_info = ALGO_REGISTRY.get(algorithm, {})
    for param_name, param_def in algo_info.get("params", {}).items():
        val = form.get(f"algo_param_{param_name}")
        if val is not None and val != "":
            ptype = param_def["type"]
            if ptype == "float":
                algo_kwargs[param_name] = float(val)
            elif ptype == "int":
                algo_kwargs[param_name] = int(val)
            elif ptype == "bool":
                algo_kwargs[param_name] = val in ("on", "true", "True")

    # Reference corpus path (optional).
    ref_file = form.get("algo_param_reference_frequency_file")
    if ref_file and not ref_file.strip():
        ref_file = None

    # Run extraction.
    t0 = time.time()
    try:
        with warnings.catch_warnings():
            warnings.filterwarnings("ignore", category=UserWarning)

            from jate.api import _is_tagger, _resolve_tagger

            if _is_tagger(algorithm):
                tagger = _resolve_tagger(algorithm, **algo_kwargs)
                from jate.nlp.spacy_backend import SpacyBackend

                nlp = SpacyBackend("en_core_web_sm")
                doc = nlp.process(str(text))
                result = tagger.tag(doc)
                total_terms = len(result)
                terms = list(result)
            else:
                import jate
                from jate.config import JATEConfig

                config = JATEConfig(reference_frequency_file=ref_file) if ref_file else None
                result = jate.extract(
                    str(text),
                    algorithm=algorithm,
                    extractor="pos_pattern",
                    config=config,
                    min_frequency=min_frequency,
                    **algo_kwargs,
                )
                total_terms = len(result)
                terms = list(result)
    except Exception as e:
        return HTMLResponse(f"<div class='error-banner'>Error: {e}</div>")

    elapsed = time.time() - t0

    # Truncate to top_n for display.
    display_terms = terms[:top_n]

    # Store for export.
    _last_result["terms"] = terms
    _last_result["algorithm"] = algorithm

    # Build highlighted text from top terms.
    highlighted = _highlight_text(str(text), display_terms[:20])

    return templates.TemplateResponse(
        "partials/_results_table.html",
        {
            "request": request,
            "terms": display_terms,
            "algorithm": algorithm,
            "elapsed": f"{elapsed:.2f}",
            "total_terms": total_terms,
            "text": str(text),
            "highlighted_text": highlighted,
        },
    )


@router.get("/extract/export/{fmt}")
async def export_results(fmt: str) -> Response:
    """Download the last extraction result as CSV or JSON."""
    terms = _last_result.get("terms", [])
    if not terms:
        return Response("No results to export", status_code=404)

    from jate.models import TermExtractionResult

    result = TermExtractionResult(terms)

    if fmt == "csv":
        data = result.to_csv()
        return Response(
            content=data,
            media_type="text/csv",
            headers={"Content-Disposition": "attachment; filename=jate_results.csv"},
        )
    elif fmt == "json":
        data = result.to_json()
        return Response(
            content=data,
            media_type="application/json",
            headers={"Content-Disposition": "attachment; filename=jate_results.json"},
        )
    else:
        return Response(f"Unknown format: {fmt}", status_code=400)


def _highlight_text(text: str, terms: list[Any]) -> str:
    """Wrap top term occurrences in <mark> tags for the highlighted view."""
    if not terms:
        return text
    spans: list[tuple[int, int]] = []
    for term in terms:
        term_str = term.string if hasattr(term, "string") else str(term)
        for match in re.finditer(re.escape(term_str), text, re.IGNORECASE):
            spans.append((match.start(), match.end()))
    if not spans:
        return text
    # Sort and merge overlapping spans.
    spans.sort()
    merged = [spans[0]]
    for s, e in spans[1:]:
        if s <= merged[-1][1]:
            merged[-1] = (merged[-1][0], max(merged[-1][1], e))
        else:
            merged.append((s, e))
    parts: list[str] = []
    prev = 0
    for s, e in merged:
        parts.append(_escape_html(text[prev:s]))
        parts.append(f"<mark>{_escape_html(text[s:e])}</mark>")
        prev = e
    parts.append(_escape_html(text[prev:]))
    return "".join(parts)


def _escape_html(text: str) -> str:
    """Minimal HTML escaping for user text inserted into templates."""
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace('"', "&quot;")
