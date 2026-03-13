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
