"""Tests for thin REST server endpoints."""

from __future__ import annotations

import pytest
from fastapi.testclient import TestClient

from jate.models import Term, TermExtractionResult
from jate.server import DEFAULT_MODEL, app

client = TestClient(app)


def test_health_live() -> None:
    resp = client.get("/health/live")
    assert resp.status_code == 200
    assert resp.json()["status"] == "ok"


def test_extract_returns_ranked_json(monkeypatch: pytest.MonkeyPatch) -> None:
    def _fake_extract(*args: object, **kwargs: object) -> TermExtractionResult:
        _ = (args, kwargs)
        return TermExtractionResult(
            [
                Term(string="local governor", score=1.0704, frequency=1, rank=1),
                Term(string="consulate", score=0.1375, frequency=1, rank=2),
            ]
        )

    monkeypatch.setattr("jate.server.extract", _fake_extract)

    payload = {
        "text": "sample",
        "algorithm": "cvalue",
        "extractor": "pos_pattern",
        "top": 2,
    }

    resp = client.post("/jate/api/v1/extract", json=payload)
    assert resp.status_code == 200
    data = resp.json()

    assert data["algorithm"] == "cvalue"
    assert data["extractor"] == "pos_pattern"
    assert data["top"] == 2
    assert len(data["terms"]) == 2
    assert data["terms"][0]["term"] == "local governor"
    assert data["terms"][0]["score"] == 1.0704
    assert data["terms"][0]["frequency"] == 1
    assert data["terms"][0]["rank"] == 1


def test_extract_rejects_empty_text() -> None:
    resp = client.post("/jate/api/v1/extract", json={"text": ""})
    assert resp.status_code == 422


def test_capabilities() -> None:
    resp = client.get("/jate/api/v1/capabilities")
    assert resp.status_code == 200
    data = resp.json()
    assert "cvalue" in data["algorithms"]
    assert "pos_pattern" in data["extractors"]


def test_health_ready_model_unavailable(monkeypatch: pytest.MonkeyPatch) -> None:
    def _raise_backend(_: str) -> object:
        raise OSError("missing model")

    monkeypatch.setattr("jate.server.get_cached_backend", _raise_backend)

    resp = client.get("/health/ready")
    assert resp.status_code == 503


def test_extract_model_unavailable(monkeypatch: pytest.MonkeyPatch) -> None:
    def _raise_backend(_: str) -> object:
        raise OSError("missing model")

    monkeypatch.setattr("jate.server.get_cached_backend", _raise_backend)

    resp = client.post(
        "/jate/api/v1/extract",
        json={"text": "sample text", "algorithm": "cvalue"},
    )
    assert resp.status_code == 503


def test_startup_warms_default_model(monkeypatch: pytest.MonkeyPatch) -> None:
    calls: list[str] = []

    def _fake_backend(model: str) -> object:
        calls.append(model)
        return object()

    monkeypatch.setattr("jate.server.get_cached_backend", _fake_backend)

    with TestClient(app) as local_client:
        resp = local_client.get("/health/live")
        assert resp.status_code == 200

    assert DEFAULT_MODEL in calls


def test_startup_warmup_fail_open(monkeypatch: pytest.MonkeyPatch) -> None:
    def _raise_backend(_: str) -> object:
        raise OSError("missing model")

    monkeypatch.setattr("jate.server.get_cached_backend", _raise_backend)

    # Startup should not fail even if warmup cannot load the model.
    with TestClient(app) as local_client:
        resp = local_client.get("/health/live")
        assert resp.status_code == 200
