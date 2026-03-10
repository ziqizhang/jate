"""Built-in datasets for benchmarking JATE term extraction."""

from __future__ import annotations

from jate.datasets.acl_rdtec import load_acl_rdtec_mini
from jate.datasets.base import Dataset

__all__ = [
    "Dataset",
    "load_acl_rdtec_mini",
]
