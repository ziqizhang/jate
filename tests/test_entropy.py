"""Entropy tests — catch codebase decay automatically.

These tests detect common patterns of cruft that accumulate over time,
especially with agent-generated code. They run with `make test` and
require no external services.

What these catch:
- Duplicate public function names across modules (agent created a new one instead of reusing)
- TODO/FIXME without actionable context
- Files that exceed the 500-line guideline
- Documentation drift (AGENTS.md module map, architecture doc paths)

What these do NOT catch (use manual review for these):
- Whether two similar functions should be merged (needs human judgement)
- Whether abstractions are still justified
- Whether documentation content is accurate
"""

import ast
import os
import re
from collections import defaultdict
from pathlib import Path

import pytest

SRC_ROOT = Path(__file__).resolve().parents[1] / "src" / "jate"
PROJECT_ROOT = Path(__file__).resolve().parents[1]

SKIP_DIRS = {"__pycache__", ".mypy_cache", ".pytest_cache"}


def _python_files(directory: Path, skip_dirs=SKIP_DIRS):
    """Yield production .py files, skipping specified directories."""
    for root, dirs, files in os.walk(directory):
        dirs[:] = [d for d in dirs if d not in skip_dirs]
        for f in files:
            if f.endswith(".py"):
                yield Path(root) / f


class TestDuplicateFunctionNames:
    """Detect duplicate public function definitions across modules.

    Agents sometimes create new utility functions instead of reusing
    existing ones.  This test flags when the same public function name
    appears in 3+ files.
    """

    ALLOWED_DUPLICATES = {
        "__init__",
        "setup",
        "teardown",
        "main",
    }

    def test_no_unexpected_duplicate_module_level_functions(self):
        """Only checks module-level (non-method) functions.

        Class methods are excluded because ABC patterns intentionally
        define the same method name across subclasses.
        """
        function_locations = defaultdict(list)
        for py_file in _python_files(SRC_ROOT):
            try:
                tree = ast.parse(py_file.read_text(), filename=str(py_file))
            except SyntaxError:
                continue
            for node in ast.iter_child_nodes(tree):
                if isinstance(node, (ast.FunctionDef, ast.AsyncFunctionDef)):
                    if not node.name.startswith("_"):
                        rel = str(py_file.relative_to(SRC_ROOT))
                        function_locations[node.name].append(rel)

        duplicates = {
            name: locs
            for name, locs in function_locations.items()
            if len(locs) > 2 and name not in self.ALLOWED_DUPLICATES
        }

        assert not duplicates, (
            "Module-level function names appear in 3+ files "
            "(agent may have re-created existing utils):\n"
            + "\n".join(f"  {name} ({len(locs)}x): {', '.join(locs[:5])}" for name, locs in sorted(duplicates.items()))
        )


class TestTodoQuality:
    """TODOs/FIXMEs should have actionable context.

    Vague TODOs like '# TODO: fix later' accumulate and never get addressed.
    This test flags TODOs that lack meaningful description.
    """

    MIN_TODO_LENGTH = 15

    def test_no_vague_todos(self):
        vague_todos = []
        for py_file in _python_files(SRC_ROOT):
            try:
                lines = py_file.read_text().splitlines()
            except (UnicodeDecodeError, OSError):
                continue
            for line_num, line in enumerate(lines, 1):
                stripped = line.strip()
                if not stripped.startswith("#"):
                    continue
                upper = stripped.upper()
                if "TODO" not in upper and "FIXME" not in upper:
                    continue
                for keyword in ("TODO:", "FIXME:", "TODO ", "FIXME "):
                    idx = upper.find(keyword)
                    if idx >= 0:
                        content = stripped[idx + len(keyword) :].strip()
                        if len(content) < self.MIN_TODO_LENGTH:
                            rel = py_file.relative_to(SRC_ROOT)
                            vague_todos.append(f"  {rel}:{line_num}: {stripped[:80]}")
                        break

        assert not vague_todos, "Vague TODOs found (add actionable description):\n" + "\n".join(vague_todos)


class TestFileSizeLimit:
    """Production files should stay under 500 lines.

    Large files are harder for agents to navigate and reason about.
    """

    MAX_LINES = 500

    # Known large files (tracked tech debt)
    KNOWN_LARGE_FILES = {
        "api.py",
        "features.py",
    }

    def test_no_new_large_files(self):
        new_large = []
        for py_file in _python_files(SRC_ROOT):
            try:
                line_count = len(py_file.read_text().splitlines())
            except (UnicodeDecodeError, OSError):
                continue
            if line_count > self.MAX_LINES:
                rel = str(py_file.relative_to(SRC_ROOT))
                if rel not in self.KNOWN_LARGE_FILES:
                    new_large.append(f"  {rel}: {line_count} lines")

        assert (
            not new_large
        ), f"NEW files exceeding {self.MAX_LINES} lines " f"(add to KNOWN_LARGE_FILES or refactor):\n" + "\n".join(
            new_large
        )


class TestDocFreshness:
    """Verify documentation references match the actual codebase."""

    def test_architecture_doc_paths_exist(self):
        """File paths referenced in architecture doc should actually exist."""
        arch_doc_path = PROJECT_ROOT / "docs" / "architecture-agent.md"
        if not arch_doc_path.exists():
            pytest.skip("docs/architecture-agent.md not found")

        arch_doc = arch_doc_path.read_text()

        # Match backticked paths that look like Python source files
        path_pattern = re.compile(r"`((?:algorithms/|extractors/|nlp/|store/|datasets/)\S+\.py)`")
        referenced_paths = path_pattern.findall(arch_doc)

        missing = []
        for ref_path in referenced_paths:
            full_path = SRC_ROOT / ref_path
            if not full_path.exists():
                missing.append(f"  {ref_path}")

        assert not missing, (
            "File paths in docs/architecture-agent.md that don't exist:\n"
            + "\n".join(missing)
            + "\nFiles may have been moved or deleted."
        )
