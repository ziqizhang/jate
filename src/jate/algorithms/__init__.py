"""ATE scoring algorithms."""

from __future__ import annotations

from jate.algorithms.attf import ATTF
from jate.algorithms.base import Algorithm
from jate.algorithms.basic import Basic
from jate.algorithms.chi_square import ChiSquare
from jate.algorithms.combo_basic import ComboBasic
from jate.algorithms.cvalue import CValue
from jate.algorithms.glossex import GlossEx
from jate.algorithms.ncvalue import NCValue
from jate.algorithms.rake import RAKE
from jate.algorithms.ridf import RIDF
from jate.algorithms.termex import TermEx
from jate.algorithms.tfidf import TFIDF
from jate.algorithms.ttf import TTF
from jate.algorithms.weirdness import Weirdness

__all__ = [
    "Algorithm",
    "ATTF",
    "Basic",
    "ChiSquare",
    "ComboBasic",
    "CValue",
    "GlossEx",
    "NCValue",
    "RAKE",
    "RIDF",
    "TermEx",
    "TFIDF",
    "TTF",
    "Weirdness",
]
