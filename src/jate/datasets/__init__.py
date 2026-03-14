"""Built-in datasets for benchmarking JATE term extraction."""

from __future__ import annotations

from jate.datasets.acl_rdtec import AclRdtecMini, load_acl_rdtec_mini
from jate.datasets.acl_rdtec_full import AclRdtecFull
from jate.datasets.acter import Acter
from jate.datasets.base import Dataset
from jate.datasets.coastterm import CoastTerm
from jate.datasets.genia import Genia

__all__ = [
    "AclRdtecFull",
    "AclRdtecMini",
    "Acter",
    "CoastTerm",
    "Dataset",
    "Genia",
    "load_acl_rdtec_mini",
]
