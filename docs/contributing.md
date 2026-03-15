# Contributing

Thank you for your interest in contributing to JATE.

## Development setup

```bash
git clone https://github.com/ziqizhang/jate.git
cd jate
pip install poetry
poetry install --with dev,docs
python -m spacy download en_core_web_sm
```

## Running checks

Use the Makefile for all common operations:

```bash
make check          # lint + test + typecheck — run after every change
make lint           # pre-commit (black + isort + flake8)
make test           # pytest (includes structural and entropy tests)
make typecheck      # mypy (strict mode)
```

For API/container changes, also run Docker smoke checks:

```bash
bash scripts/docker_smoke_test.sh
```

## Code style

JATE uses pre-commit hooks for consistent formatting:

```bash
poetry run pre-commit install
poetry run pre-commit run --all-files
```

- **black** — code formatting
- **isort** — import sorting
- **flake8** — linting

## Branch workflow

> **Important**: Always target `dev` for pull requests, never `master`.

- `master` — release branch. Pushes to `master` can trigger semantic-release and publish a new version to PyPI (if commits use `fix:` or `feat:` prefixes). Do not submit PRs to `master` unless you intend to cut a release.
- `dev` — development branch. All feature work, bug fixes, and improvements go here.

1. Fork the repo and create a branch from `dev`
2. Make your changes
3. Add tests for new functionality
4. Run `make check` and ensure everything passes
5. For API/container changes, ensure smoke checks pass: `bash scripts/docker_smoke_test.sh`
6. Submit a PR to **`dev`** — the PR body **must** reference at least one issue (e.g., `Resolves #67`, `Fixes #42`, or `Related to #10`). CI will reject PRs without an issue reference.

## Agentic coding harness

This repo uses an **agentic coding harness** — structured infrastructure that helps AI coding agents work productively and safely within the codebase. This follows the principles outlined in OpenAI's [Harness Engineering](https://openai.com/index/harness-engineering/), where agents need clear boundaries, mechanical guardrails, and discoverable context to operate effectively.

### Harness files

The following files form the harness. They **must not be deleted or broken** by contributions. If your changes alter the architecture (e.g., adding a new top-level module, changing module boundaries), you must update these files to reflect the new state:

| File | Purpose |
|------|---------|
| `AGENTS.md` | Tier 1 context — project overview, module map, commands, architectural rules. Loaded by AI agents at the start of every session. Keep under 150 lines. |
| `docs/architecture-agent.md` | Tier 2 context — detailed architecture reference. Module responsibilities, pipeline flow, extension guides. Pulled in by agents when working on cross-module changes. |
| `tests/test_architecture.py` | Structural tests — enforces module boundary rules via AST import analysis. If you add a new module or change import dependencies, these tests will catch violations. |
| `tests/test_entropy.py` | Entropy tests — catches codebase decay: duplicate function names, vague TODOs, oversized files, documentation drift. |
| `Makefile` | Universal command menu. `make check` runs lint + test + typecheck. Agents and humans use the same commands. |

**When to update harness files:**

- **Added a new top-level module** under `src/jate/` → update the module map in `AGENTS.md` and the module responsibilities section in `docs/architecture-agent.md`
- **Changed module boundaries** (e.g., a new dependency between modules) → update the architectural rules in both docs and, if needed, the allowed/forbidden import lists in `tests/test_architecture.py`
- **Added a new feature builder or algorithm** → register it in `api.py::_FEATURE_NEEDS` and ensure `FeatureCache` handles it. New features must be built once and shared, never per-algorithm.
- **Added a pipeline step that processes documents/candidates in bulk** → add timestamped progress logging to stderr (see `docs/architecture-agent.md` § Logging). Consider parallelism for CPU-bound work (see § Efficiency).
- **Added a large file** (>500 lines) that is intentional → add it to `KNOWN_LARGE_FILES` in `tests/test_entropy.py`

### Optional: agentic harness toolkit

An optional skill package is available for contributors who use AI coding tools (Claude Code, Cursor, Windsurf, Copilot, etc.). It contains:

- **SKILL.md** — a structured skill definition that teaches your AI agent how to assess, set up, and audit agentic coding harnesses. It covers context engineering (tiered documentation), architectural guardrails (automated enforcement via tests and linters), and entropy management (detecting codebase decay).
- **harness-engineering-guide.md** — the full reference guide on harness engineering principles and practices.

**Download**: grab `agentic-harness-toolkit.zip` from the [latest release](https://github.com/ziqizhang/jate/releases/latest) assets.

**Deployment**: how you deploy the skill depends on your AI coding tool. In general:

1. Unzip the archive
2. Place the files in your tool's skill/rules directory (e.g., `.claude/skills/`, `.cursor/rules/`, or wherever your tool loads custom instructions from)
3. Invoke the skill when you want to assess or audit the harness (e.g., in Claude Code: `/agentic-harness`)

Consult your tool's documentation for the exact location and format it expects for custom skills or instructions.

## Reporting issues

- **Bug reports:** [GitHub Issues](https://github.com/ziqizhang/jate/issues/new?template=bug_report.yml)
- **Feature requests:** [GitHub Issues](https://github.com/ziqizhang/jate/issues/new?template=feature_request.yml)
