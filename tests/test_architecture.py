"""Structural tests — enforce module boundaries and architectural rules.

These tests read source code (AST imports) to verify that architectural
constraints hold.  They run in < 1s and require no external services.

Architectural rules enforced:
1. algorithms/ are pure scorers — no imports from extractors, nlp, store, cli, server
2. extractors/ are independent — no imports from algorithms or store implementations
3. store/ is infrastructure — no imports from algorithms, extractors, features, nlp
4. nlp/ is infrastructure — no imports from algorithms, extractors, features, store
"""

import ast
import os
from pathlib import Path
from typing import Set

import pytest

SRC_ROOT = Path(__file__).resolve().parents[1] / "src" / "jate"
PROJECT_ROOT = Path(__file__).resolve().parents[1]


def _python_files(directory: Path):
    """Yield all .py files under a directory, recursively."""
    for root, _dirs, files in os.walk(directory):
        for f in files:
            if f.endswith(".py"):
                yield Path(root) / f


def _get_imports(filepath: Path) -> Set[str]:
    """Extract all import module names from a Python file."""
    try:
        tree = ast.parse(filepath.read_text(), filename=str(filepath))
    except SyntaxError:
        return set()

    imports = set()
    for node in ast.walk(tree):
        if isinstance(node, ast.Import):
            for alias in node.names:
                imports.add(alias.name)
        elif isinstance(node, ast.ImportFrom):
            if node.module:
                imports.add(node.module)
    return imports


class TestAlgorithmPurity:
    """algorithms/ must be pure scorers — no I/O, NLP, or extraction imports."""

    FORBIDDEN_PREFIXES = (
        "jate.extractors",
        "jate.nlp",
        "jate.store",
        "jate.server",
        "jate.cli",
    )

    def test_no_forbidden_imports(self):
        algo_dir = SRC_ROOT / "algorithms"
        violations = []
        for py_file in _python_files(algo_dir):
            rel = py_file.relative_to(SRC_ROOT)
            for imp in _get_imports(py_file):
                for prefix in self.FORBIDDEN_PREFIXES:
                    if imp.startswith(prefix):
                        violations.append(f"  {rel} imports {imp}")

        assert not violations, "algorithms/ must not import extractors, nlp, store, cli, or server:\n" + "\n".join(
            violations
        )


class TestExtractorIndependence:
    """extractors/ must not import from algorithms/ or store/ implementations."""

    FORBIDDEN_PREFIXES = (
        "jate.algorithms",
        "jate.store",
    )

    def test_no_forbidden_imports(self):
        ext_dir = SRC_ROOT / "extractors"
        violations = []
        for py_file in _python_files(ext_dir):
            rel = py_file.relative_to(SRC_ROOT)
            for imp in _get_imports(py_file):
                for prefix in self.FORBIDDEN_PREFIXES:
                    if imp.startswith(prefix):
                        violations.append(f"  {rel} imports {imp}")

        assert not violations, "extractors/ must not import algorithms or store:\n" + "\n".join(violations)


class TestInfraIndependence:
    """store/ and nlp/ must not import domain modules."""

    FORBIDDEN_PREFIXES = (
        "jate.algorithms",
        "jate.extractors",
        "jate.features",
    )

    @pytest.mark.parametrize("module", ["store", "nlp"])
    def test_no_domain_imports(self, module: str):
        module_dir = SRC_ROOT / module
        if not module_dir.exists():
            pytest.skip(f"{module}/ does not exist")

        violations = []
        for py_file in _python_files(module_dir):
            rel = py_file.relative_to(SRC_ROOT)
            for imp in _get_imports(py_file):
                for prefix in self.FORBIDDEN_PREFIXES:
                    if imp.startswith(prefix):
                        violations.append(f"  {rel} imports {imp}")

        # Additionally: store must not import nlp, nlp must not import store
        cross_infra = "jate.nlp" if module == "store" else "jate.store"
        for py_file in _python_files(module_dir):
            rel = py_file.relative_to(SRC_ROOT)
            for imp in _get_imports(py_file):
                if imp.startswith(cross_infra):
                    violations.append(f"  {rel} imports {imp}")

        assert not violations, f"{module}/ must not import domain modules or cross-infra:\n" + "\n".join(violations)


class TestDocFreshness:
    """Verify that documentation references match the actual codebase."""

    def test_agents_md_module_map_matches_disk(self):
        """Every top-level dir/file under src/jate/ should appear in AGENTS.md module map."""
        agents_md = (PROJECT_ROOT / "AGENTS.md").read_text()

        actual_entries = set()
        for entry in SRC_ROOT.iterdir():
            if entry.name.startswith(("_", ".")):
                continue
            if entry.is_dir():
                actual_entries.add(f"{entry.name}/")
            elif entry.suffix == ".py":
                actual_entries.add(entry.name)

        missing_from_docs = []
        for name in sorted(actual_entries):
            # Check if the name (without trailing /) appears in AGENTS.md
            check_name = name.rstrip("/")
            if check_name not in agents_md:
                missing_from_docs.append(name)

        assert not missing_from_docs, (
            "Entries under src/jate/ not mentioned in AGENTS.md:\n"
            + "\n".join(f"  {e}" for e in missing_from_docs)
            + "\nUpdate the Module Map section in AGENTS.md."
        )

    def test_architecture_doc_exists(self):
        """Tier 2 architecture doc must exist."""
        arch_doc = PROJECT_ROOT / "docs" / "architecture-agent.md"
        assert arch_doc.exists(), (
            "docs/architecture-agent.md not found. " "This is the Tier 2 architecture reference for agents."
        )


class TestAlgorithmAbstraction:
    """Verify the ATERanker/ATETagger abstraction is correctly applied."""

    def test_all_algorithms_are_ranker_or_tagger(self):
        """Every Algorithm subclass in algorithms/ must be ATERanker or ATETagger."""
        algo_dir = SRC_ROOT / "algorithms"
        # Collect all class names defined in algorithm files and their bases
        exclude = {"Algorithm", "ATERanker", "ATETagger"}
        violations = []

        for py_file in _python_files(algo_dir):
            try:
                tree = ast.parse(py_file.read_text(), filename=str(py_file))
            except SyntaxError:
                continue
            for node in ast.walk(tree):
                if not isinstance(node, ast.ClassDef):
                    continue
                if node.name in exclude:
                    continue
                # Check if any base is Algorithm (directly or indirectly)
                base_names = []
                for base in node.bases:
                    if isinstance(base, ast.Name):
                        base_names.append(base.id)
                    elif isinstance(base, ast.Attribute):
                        base_names.append(base.attr)
                # Only check classes that inherit from Algorithm, ATERanker, or ATETagger
                algo_bases = {"Algorithm", "ATERanker", "ATETagger"}
                if not any(b in algo_bases for b in base_names):
                    continue
                # Must be ATERanker or ATETagger, not raw Algorithm
                if not any(b in {"ATERanker", "ATETagger"} for b in base_names):
                    violations.append(
                        f"  {py_file.relative_to(SRC_ROOT)}: {node.name} "
                        f"inherits from Algorithm directly instead of ATERanker/ATETagger"
                    )

        assert not violations, "All Algorithm subclasses must use ATERanker or ATETagger:\n" + "\n".join(violations)

    def test_all_algorithms_have_output_capabilities(self):
        """Every registered algorithm must return an OutputCapabilities instance."""
        from jate.algorithms.base import OutputCapabilities
        from jate.api import _ALGORITHM_NAMES

        for name, cls in _ALGORITHM_NAMES.items():
            algo = cls()
            caps = algo.output_capabilities()
            assert isinstance(caps, OutputCapabilities), (
                f"Algorithm {name!r} ({cls.__name__}).output_capabilities() "
                f"returned {type(caps).__name__}, expected OutputCapabilities"
            )

    def test_tfidf_raises_on_single_doc(self):
        """TF-IDF must raise AlgorithmIncompatibleError on a single-document corpus."""
        from jate.algorithms.base import AlgorithmIncompatibleError
        from jate.algorithms.tfidf import TFIDF
        from jate.features import TermFrequency
        from jate.models import Candidate

        tf = TermFrequency(
            term2ttf={"test": 5},
            term2fid={"test": {"doc1": 5}},
            corpus_total=5,
            total_docs=1,
        )
        candidates = [Candidate(surface_form="test")]
        tfidf = TFIDF()
        with pytest.raises(AlgorithmIncompatibleError):
            tfidf.score(candidates, tf)

    def test_attf_warns_on_single_doc(self):
        """ATTF must emit a warning on a single-document corpus."""
        from jate.algorithms.attf import ATTF
        from jate.features import TermFrequency
        from jate.models import Candidate

        tf = TermFrequency(
            term2ttf={"test": 5},
            term2fid={"test": {"doc1": 5}},
            corpus_total=5,
            total_docs=1,
        )
        candidates = [Candidate(surface_form="test")]
        attf = ATTF()
        with pytest.warns(UserWarning, match="ATTF degrades to TTF"):
            attf.score(candidates, tf)
