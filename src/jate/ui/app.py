"""JATE Local UI -- FastAPI app with HTMX + Jinja2 templates."""

from __future__ import annotations

from pathlib import Path

from fastapi import FastAPI
from fastapi.responses import RedirectResponse
from fastapi.staticfiles import StaticFiles
from fastapi.templating import Jinja2Templates

from jate.ui.routes_api import router as api_router
from jate.ui.routes_corpus import router as corpus_router
from jate.ui.routes_extract import router as extract_router

UI_DIR = Path(__file__).parent
app = FastAPI(title="JATE UI")
app.mount("/static", StaticFiles(directory=UI_DIR / "static"), name="static")
templates = Jinja2Templates(directory=UI_DIR / "templates")

app.include_router(extract_router)
app.include_router(corpus_router)
app.include_router(api_router)


@app.get("/")
async def root() -> RedirectResponse:
    return RedirectResponse(url="/extract")
