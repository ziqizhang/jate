"""Download manager for GitHub-hosted benchmark datasets.

Downloads GitHub repositories as zipballs and caches them locally
under ``~/.jate/datasets/<name>/``.
"""

from __future__ import annotations

import shutil
import urllib.request
import zipfile
from pathlib import Path


def _get_cache_dir(name: str) -> Path:
    """Return the local cache directory for a dataset.

    Parameters
    ----------
    name:
        Dataset identifier used as the subdirectory name.

    Returns
    -------
    Path
        ``~/.jate/datasets/<name>``
    """
    return Path.home() / ".jate" / "datasets" / name


def download_dataset(
    name: str,
    repo_url: str,
    *,
    branch: str = "main",
    force: bool = False,
) -> Path:
    """Download a GitHub repo zipball and cache it locally.

    Parameters
    ----------
    name:
        Short identifier used for the cache subdirectory.
    repo_url:
        GitHub repo URL, e.g. ``https://github.com/owner/repo``.
    branch:
        Branch to download (default ``main``).
    force:
        If *True*, re-download even when the cache directory exists.

    Returns
    -------
    Path
        Path to the extracted dataset directory.
    """
    cache_dir = _get_cache_dir(name)

    if cache_dir.exists() and not force:
        return cache_dir

    # Clean up if forcing
    if cache_dir.exists() and force:
        shutil.rmtree(cache_dir)

    # Normalise repo URL
    repo_url = repo_url.rstrip("/")
    zip_url = f"{repo_url}/archive/refs/heads/{branch}.zip"

    print(f"Downloading {name} from {zip_url} ...")

    # Download to a temp file
    cache_dir.parent.mkdir(parents=True, exist_ok=True)
    zip_path = cache_dir.parent / f"{name}.zip"

    try:
        req = urllib.request.Request(zip_url, headers={"User-Agent": "jate-dataset-downloader"})
        with urllib.request.urlopen(req) as resp:  # noqa: S310
            total = resp.headers.get("Content-Length")
            if total:
                print(f"  File size: {int(total) / 1024 / 1024:.1f} MB")
            else:
                print("  File size: unknown")

            with open(zip_path, "wb") as fh:
                downloaded = 0
                while True:
                    chunk = resp.read(8192)
                    if not chunk:
                        break
                    fh.write(chunk)
                    downloaded += len(chunk)

            print(f"  Downloaded {downloaded / 1024 / 1024:.1f} MB")

        # Extract
        print(f"  Extracting to {cache_dir} ...")
        with zipfile.ZipFile(zip_path) as zf:
            zf.extractall(cache_dir.parent / f"{name}_tmp")  # noqa: S202

        # Flatten: the zip contains a top-level directory like repo-branch/
        tmp_dir = cache_dir.parent / f"{name}_tmp"
        top_dirs = list(tmp_dir.iterdir())
        if len(top_dirs) == 1 and top_dirs[0].is_dir():
            # Move the inner directory to the final cache_dir
            top_dirs[0].rename(cache_dir)
            tmp_dir.rmdir()
        else:
            # No single top-level dir, just rename tmp as cache
            tmp_dir.rename(cache_dir)

        print(f"  Cached at {cache_dir}")
        return cache_dir

    finally:
        if zip_path.exists():
            zip_path.unlink()
