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
    reference_frequency_file:
        Path(s) to reference frequency files for algorithms that compare
        target vs reference corpus (weirdness, glossex, termex). If not
        provided, falls back to self-reference.
    prefilter_min_ttf:
        Minimum total term frequency for chi-square prefiltering.
    prefilter_min_tcf:
        Minimum term context frequency for chi-square prefiltering.
    """

    reference_frequency_file: str | list[str] | None = None
    prefilter_min_ttf: int = 0
    prefilter_min_tcf: int = 0
