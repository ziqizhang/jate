"""Shared utilities for reference-corpus-based algorithms.

Ports the Java ``ReferenceBased`` helper methods.
"""

from __future__ import annotations

import math


def _match_orders_of_magnitude(
    word_frequencies: dict[str, int],
    reference_frequencies: dict[str, int],
    reference_total: int,
) -> float:
    """Compute a scalar to align reference probabilities with domain probabilities.

    Ports Java ``ReferenceBased.matchOrdersOfMagnitude``.

    Computes the mean normalised word frequency in each corpus, then returns
    ``10^(oom_domain - oom_reference)`` so that multiplying reference
    probabilities by this scalar brings them to the same order of magnitude
    as the domain corpus.
    """
    # Mean normalised frequency in reference corpus
    if not reference_frequencies or reference_total <= 0:
        return 1.0
    ref_corpus_total = reference_total + 1  # +1 matches Java getTTFNorm denominator
    total_ref_score = sum(f / ref_corpus_total for f in reference_frequencies.values())
    mean_ref = total_ref_score / len(reference_frequencies)

    # Mean normalised frequency in domain corpus
    if not word_frequencies:
        return 1.0
    domain_total = sum(word_frequencies.values())
    domain_corpus_total = domain_total + 1
    total_domain_score = sum(f / domain_corpus_total for f in word_frequencies.values())
    mean_domain = total_domain_score / len(word_frequencies)

    if not math.isfinite(mean_ref) or mean_ref <= 0 or mean_domain <= 0:
        return 1.0

    oom_ref = int(math.log10(mean_ref))
    oom_domain = int(math.log10(mean_domain))
    return math.pow(10, oom_domain - oom_ref)
