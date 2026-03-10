"""Configuration for JATE pipeline."""

from __future__ import annotations

from dataclasses import dataclass


@dataclass
class JATEConfig:
    """Pipeline configuration.

    Parameters
    ----------
    max_workers:
        Maximum number of worker processes for parallel computation.
        1 = sequential (no overhead). N > 1 = use ProcessPoolExecutor.
    """

    max_workers: int = 1
    reference_frequency_file: str | None = None
