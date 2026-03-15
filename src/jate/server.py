"""Thin REST server exposing JATE extraction endpoints."""

from __future__ import annotations

import os
from contextlib import asynccontextmanager
from functools import lru_cache
from typing import Any, AsyncIterator

try:
    from fastapi import FastAPI, HTTPException
    from pydantic import BaseModel, Field
except ImportError as exc:  # pragma: no cover - exercised only when extras missing
    raise ImportError(
        "Server dependencies are optional. Install with `pip install jate[server]` to use jate.server."
    ) from exc

from jate.api import _ALGORITHM_NAMES, _EXTRACTOR_NAMES, extract
from jate.nlp.spacy_backend import SpacyBackend

DEFAULT_MODEL = "en_core_web_sm"


@asynccontextmanager
async def lifespan(_: FastAPI) -> AsyncIterator[None]:
    """Warm up the default model in each worker process at startup."""
    try:
        get_cached_backend(DEFAULT_MODEL)
    except OSError:
        # Keep startup non-fatal; readiness endpoint reports model availability.
        pass
    yield


app = FastAPI(title="JATE API", version="1.0.0", lifespan=lifespan)


@lru_cache(maxsize=4)
def get_cached_backend(model: str) -> SpacyBackend:
    """Load and cache a spaCy backend per model name."""
    return SpacyBackend(model)


class ExtractRequest(BaseModel):
    """Request payload for term extraction."""

    text: str = Field(min_length=1)
    algorithm: str = "cvalue"
    extractor: str = "pos_pattern"
    model: str = DEFAULT_MODEL
    top: int | None = Field(default=None, ge=1)
    min_frequency: int = Field(default=1, ge=1)
    min_words: int = Field(default=1, ge=1)
    max_words: int | None = Field(default=None, ge=1)
    algorithm_options: dict[str, Any] = Field(default_factory=dict)


class TermResponse(BaseModel):
    """Single ranked term in API output."""

    rank: int
    term: str
    score: float
    frequency: int
    surface_forms: list[str]
    metadata: dict[str, Any]


class ExtractResponse(BaseModel):
    """Response payload for extraction endpoint."""

    algorithm: str
    extractor: str
    model: str
    top: int | None
    terms: list[TermResponse]


@app.get("/health/live")
def health_live() -> dict[str, str]:
    """Simple liveness check."""
    return {"status": "ok"}


@app.get("/health/ready")
def health_ready(model: str = DEFAULT_MODEL) -> dict[str, str]:
    """Readiness check validating spaCy model availability."""
    try:
        get_cached_backend(model)
    except OSError as exc:
        raise HTTPException(status_code=503, detail=f"Model not ready: {model}") from exc
    return {"status": "ready", "model": model}


@app.get("/jate/api/v1/capabilities")
def capabilities() -> dict[str, list[str] | str]:
    """List supported algorithms and extractors."""
    return {
        "algorithms": sorted(_ALGORITHM_NAMES.keys()),
        "extractors": sorted(_EXTRACTOR_NAMES.keys()),
        "default_model": DEFAULT_MODEL,
    }


@app.post("/jate/api/v1/extract", response_model=ExtractResponse)
def extract_terms(payload: ExtractRequest) -> ExtractResponse:
    """Extract terms and return ranked JSON results."""
    try:
        backend = get_cached_backend(payload.model)
        result = extract(
            payload.text,
            algorithm=payload.algorithm,
            extractor=payload.extractor,
            model=payload.model,
            nlp_backend=backend,
            min_frequency=payload.min_frequency,
            min_words=payload.min_words,
            max_words=payload.max_words,
            **payload.algorithm_options,
        )
    except ValueError as exc:
        raise HTTPException(status_code=400, detail=str(exc)) from exc
    except OSError as exc:
        raise HTTPException(status_code=503, detail=f"Model not ready: {payload.model}") from exc

    terms = list(result)
    if payload.top is not None:
        terms = terms[: payload.top]

    return ExtractResponse(
        algorithm=payload.algorithm,
        extractor=payload.extractor,
        model=payload.model,
        top=payload.top,
        terms=[
            TermResponse(
                rank=t.rank,
                term=t.string,
                score=t.score,
                frequency=t.frequency,
                surface_forms=sorted(t.surface_forms) if t.surface_forms else [],
                metadata=t.metadata,
            )
            for t in terms
        ],
    )


def run() -> None:
    """Run API server."""
    import uvicorn

    host = os.getenv("JATE_API_HOST", "0.0.0.0")
    port = int(os.getenv("JATE_API_PORT", "8000"))
    workers = int(os.getenv("JATE_API_WORKERS", "1"))
    keepalive = int(os.getenv("JATE_API_TIMEOUT_KEEP_ALIVE", "5"))

    uvicorn.run(
        "jate.server:app",
        host=host,
        port=port,
        workers=workers,
        timeout_keep_alive=keepalive,
    )
