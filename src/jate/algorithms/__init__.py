"""ATE scoring algorithms."""

from __future__ import annotations

from jate.algorithms.attf import ATTF
from jate.algorithms.base import Algorithm, AlgorithmIncompatibleError, ATERanker, ATETagger, OutputCapabilities
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
from jate.algorithms.voting import Voting
from jate.algorithms.weirdness import Weirdness

# Neural taggers (optional — requires jate[neural])
try:
    from jate.algorithms.bert_tagger import BertTagger, RoBERTaTagger, XLMRTagger
except ImportError:
    pass

__all__ = [
    "Algorithm",
    "AlgorithmIncompatibleError",
    "ATERanker",
    "ATETagger",
    "OutputCapabilities",
    "ATTF",
    "Basic",
    "BertTagger",
    "ChiSquare",
    "ComboBasic",
    "CValue",
    "GlossEx",
    "NCValue",
    "RAKE",
    "RIDF",
    "RoBERTaTagger",
    "TermEx",
    "TFIDF",
    "TTF",
    "Voting",
    "Weirdness",
    "XLMRTagger",
]
