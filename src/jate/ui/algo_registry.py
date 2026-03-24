"""Algorithm metadata registry for the JATE UI."""

ALGO_REGISTRY = {
    "tfidf": {
        "name": "TF-IDF",
        "description": "TF-IDF at corpus level — requires multiple documents",
        "type": "ranker",
        "params": {},
        "needs_reference": False,
        "single_doc": False,
    },
    "cvalue": {
        "name": "C-Value",
        "description": "Multi-word term extraction via nested term frequency",
        "type": "ranker",
        "params": {},
        "needs_reference": False,
        "single_doc": True,
    },
    "ncvalue": {
        "name": "NC-Value",
        "description": "C-Value extended with context word weighting",
        "type": "ranker",
        "params": {
            "cvalue_weight": {
                "type": "float",
                "default": 0.8,
                "label": "C-Value Weight",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
            "context_weight": {
                "type": "float",
                "default": 0.2,
                "label": "Context Weight",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
        },
        "needs_reference": False,
        "single_doc": True,
    },
    "basic": {
        "name": "Basic",
        "description": "Frequency + containment scoring",
        "type": "ranker",
        "params": {
            "alpha": {
                "type": "float",
                "default": 0.72,
                "label": "Alpha",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
        },
        "needs_reference": False,
        "single_doc": True,
    },
    "combobasic": {
        "name": "ComboBasic",
        "description": "Basic scoring with combined frequency and containment weights",
        "type": "ranker",
        "params": {
            "alpha": {
                "type": "float",
                "default": 0.75,
                "label": "Alpha",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
            "beta": {
                "type": "float",
                "default": 0.1,
                "label": "Beta",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
        },
        "needs_reference": False,
        "single_doc": True,
    },
    "attf": {
        "name": "ATTF",
        "description": "Average term frequency normalised by document length",
        "type": "ranker",
        "params": {},
        "needs_reference": False,
        "single_doc": True,
    },
    "ttf": {
        "name": "TTF",
        "description": "Total term frequency across the corpus",
        "type": "ranker",
        "params": {},
        "needs_reference": False,
        "single_doc": True,
    },
    "ridf": {
        "name": "RIDF",
        "description": "Residual IDF — discounts terms whose frequency matches a Poisson baseline",
        "type": "ranker",
        "params": {},
        "needs_reference": False,
        "single_doc": True,
    },
    "rake": {
        "name": "RAKE",
        "description": "Rapid Automatic Keyword Extraction using word co-occurrence scores",
        "type": "ranker",
        "params": {},
        "needs_reference": False,
        "single_doc": True,
    },
    "chi_square": {
        "name": "Chi-Square",
        "description": "Statistical association between term and document class",
        "type": "ranker",
        "params": {},
        "needs_reference": False,
        "single_doc": True,
    },
    "weirdness": {
        "name": "Weirdness",
        "description": "Ratio of term frequency in domain corpus vs. reference corpus",
        "type": "ranker",
        "params": {
            "match_oom": {
                "type": "bool",
                "default": True,
                "label": "Match Order of Magnitude",
            },
        },
        "needs_reference": True,
        "single_doc": True,
    },
    "glossex": {
        "name": "GlossEx",
        "description": "Glossary extraction combining frequency and reference corpus contrast",
        "type": "ranker",
        "params": {
            "alpha": {
                "type": "float",
                "default": 0.2,
                "label": "Alpha",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
            "beta": {
                "type": "float",
                "default": 0.8,
                "label": "Beta",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
        },
        "needs_reference": True,
        "single_doc": True,
    },
    "termex": {
        "name": "TermEx",
        "description": "Multi-feature term extraction with reference corpus contrast",
        "type": "ranker",
        "params": {
            "alpha": {
                "type": "float",
                "default": 0.33,
                "label": "Alpha",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
            "beta": {
                "type": "float",
                "default": 0.33,
                "label": "Beta",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
            "zeta": {
                "type": "float",
                "default": 0.34,
                "label": "Zeta",
                "min": 0.0,
                "max": 1.0,
                "step": 0.01,
            },
            "match_oom": {
                "type": "bool",
                "default": True,
                "label": "Match Order of Magnitude",
            },
        },
        "needs_reference": True,
        "single_doc": True,
    },
    "nmf": {
        "name": "NMF",
        "description": "Non-negative Matrix Factorisation topic-based term ranking",
        "type": "ranker",
        "params": {
            "n_topics": {
                "type": "int",
                "default": 20,
                "label": "Number of Topics",
                "min": 1,
                "max": 200,
                "step": 1,
            },
            "top_n_per_topic": {
                "type": "int",
                "default": 50,
                "label": "Top N Terms per Topic",
                "min": 1,
                "max": 500,
                "step": 1,
            },
        },
        "needs_reference": False,
        "single_doc": True,
    },
    "xlmr-tagger": {
        "name": "XLM-R Tagger",
        "description": "Neural sequence tagger using XLM-RoBERTa — slow on CPU, GPU recommended",
        "type": "tagger",
        "params": {},
        "needs_reference": False,
        "single_doc": True,
    },
}


def get_algorithms_for_mode(mode: str) -> dict:
    """Return algorithms available for 'extract' (single doc) or 'corpus' mode."""
    if mode == "extract":
        return {k: v for k, v in ALGO_REGISTRY.items() if v["single_doc"]}
    return ALGO_REGISTRY


def get_algo_params(algo_name: str) -> dict:
    """Return parameter definitions for an algorithm."""
    algo = ALGO_REGISTRY.get(algo_name, {})
    return algo.get("params", {})


def needs_reference_corpus(algo_names: list[str]) -> bool:
    """Check if any algorithm in the list needs a reference corpus."""
    return any(ALGO_REGISTRY.get(name, {}).get("needs_reference", False) for name in algo_names)
