"""Configuration for JATE pipeline."""

from __future__ import annotations

from dataclasses import dataclass

# Fraction of highest-frequency terms used to build the chi-square reference
# context (Java: FrequencyCtxBasedCopier top-fraction parameter).
_CHI_SQUARE_TOP_FRACTION: float = 0.3


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
    reference_frequency_file: str | list[str] | None = None
    prefilter_min_ttf: int = 0
    prefilter_min_tcf: int = 0
