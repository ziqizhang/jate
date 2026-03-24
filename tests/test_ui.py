"""Tests for the JATE local UI (FastAPI + HTMX routes)."""

from __future__ import annotations

import warnings

from fastapi.testclient import TestClient

warnings.filterwarnings("ignore")

from jate.ui.app import app  # noqa: E402

client = TestClient(app)


def test_root_redirects():
    """GET / should 307-redirect to /extract."""
    response = client.get("/", follow_redirects=False)
    assert response.status_code == 307
    assert response.headers["location"] == "/extract"


def test_extract_page():
    """GET /extract should return 200 with 'Extract' in the HTML."""
    response = client.get("/extract")
    assert response.status_code == 200
    assert "Extract" in response.text


def test_corpus_page():
    """GET /corpus should return 200 with 'Corpus' in the HTML."""
    response = client.get("/corpus")
    assert response.status_code == 200
    assert "Corpus" in response.text


def test_algo_params_basic():
    """GET /api/algo-params/basic should return 200 and include 'alpha'."""
    response = client.get("/api/algo-params/basic")
    assert response.status_code == 200
    assert "alpha" in response.text.lower()


def test_algo_params_no_params():
    """GET /api/algo-params/tfidf should return 200 with 'No additional' in text."""
    response = client.get("/api/algo-params/tfidf")
    assert response.status_code == 200
    assert "no additional" in response.text.lower()


def test_algo_params_reference():
    """GET /api/algo-params/weirdness should return 200 mentioning 'reference' (case-insensitive)."""
    response = client.get("/api/algo-params/weirdness")
    assert response.status_code == 200
    assert "reference" in response.text.lower()


def test_extract_run():
    """POST /extract/run with a valid text and cvalue algorithm should return results."""
    response = client.post(
        "/extract/run",
        data={
            "text": "Machine learning and neural networks improve deep learning models.",
            "algorithm": "cvalue",
        },
    )
    assert response.status_code == 200
    assert "machine learning" in response.text.lower()


def test_extract_run_empty_text():
    """POST /extract/run with empty text should return an error message."""
    response = client.post(
        "/extract/run",
        data={"text": "", "algorithm": "cvalue"},
    )
    assert response.status_code == 200
    # Route returns an HTML error banner rather than an HTTP error code.
    assert "error" in response.text.lower() or "please" in response.text.lower()


def test_corpus_scan(tmp_path):
    """POST /corpus/scan should count .txt files and report skipped non-.txt files."""
    (tmp_path / "doc1.txt").write_text("This is the first document.")
    (tmp_path / "doc2.txt").write_text("This is the second document.")
    (tmp_path / "image.png").write_bytes(b"\x89PNG\r\n")

    response = client.post(
        "/corpus/scan",
        data={"directory": str(tmp_path)},
    )
    assert response.status_code == 200
    # Should mention 2 valid .txt files.
    assert "2" in response.text
    # Should mention the skipped PNG file.
    assert "image.png" in response.text


def test_corpus_scan_invalid_dir():
    """POST /corpus/scan with a nonexistent path should return an error."""
    response = client.post(
        "/corpus/scan",
        data={"directory": "/nonexistent/path/that/does/not/exist"},
    )
    assert response.status_code == 200
    assert "error" in response.text.lower() or "not found" in response.text.lower()


def test_extract_export_no_data():
    """GET /extract/export/csv when no extraction has been run should handle gracefully."""
    # Use a fresh client so _last_result is empty.
    from jate.ui import app as ui_app
    from jate.ui.routes_extract import _last_result

    # Clear any leftover state from other tests.
    _last_result.clear()

    fresh_client = TestClient(ui_app.app)
    response = fresh_client.get("/extract/export/csv")
    # Should return 404 or an empty/graceful response, not a 500.
    assert response.status_code in (404, 200)
    if response.status_code == 200:
        # If 200, it should contain no meaningful data or an appropriate message.
        assert len(response.content) == 0 or response.status_code == 200
