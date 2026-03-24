"""API routes — lightweight endpoints returning HTMX partials."""

from __future__ import annotations

from pathlib import Path

from fastapi import APIRouter, Request
from fastapi.responses import HTMLResponse
from fastapi.templating import Jinja2Templates

from jate.ui.algo_registry import ALGO_REGISTRY

router = APIRouter()
templates = Jinja2Templates(directory=Path(__file__).parent / "templates")


@router.get("/api/algo-params/{algorithm}", response_class=HTMLResponse)
async def algo_params(algorithm: str, request: Request) -> HTMLResponse:
    """Return rendered _algo_params.html partial for the selected algorithm."""
    algo_info = ALGO_REGISTRY.get(algorithm, {})
    params = algo_info.get("params", {})
    needs_reference = algo_info.get("needs_reference", False)
    return templates.TemplateResponse(
        "partials/_algo_params.html",
        {
            "request": request,
            "algorithm": algorithm,
            "params": params,
            "needs_reference": needs_reference,
        },
    )
