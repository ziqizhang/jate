# JATE — Just Automatic Term Extraction

Python library for automatic term extraction (ATE). 14 algorithms (13 classical + ensemble voting), corpus-level statistics, evaluation, CLI, and REST API. Built on spaCy.

## Key Paths

- Source: `src/jate/`
- Tests: `tests/`
- CI workflows: `.github/workflows/`
- Docs (MkDocs): `docs/`
- Pre-commit config: `.pre-commit-config.yaml`

## Development Commands

```
make check          # lint + test + typecheck — run after every change
make lint           # pre-commit (black + isort + flake8)
make test           # pytest (~19 test files, includes structural tests)
make typecheck      # mypy (strict mode)
make build          # poetry build
make clean          # remove generated files
```

## Module Map

```
src/jate/
├── algorithms/     # 14 scoring algorithms (base.py + one file per algo)
├── extractors/     # Candidate extractors (pos_pattern, ngram, noun_phrase)
│   └── patterns/   # POS pattern presets (default, genia, acl_rdtec)
├── nlp/            # spaCy backend, document loader
├── store/          # Corpus storage (memory, sqlite)
├── datasets/       # Built-in evaluation datasets (GENIA, ACL RD-TEC, ACTER, CoastTerm)
│   └── reference/  # Reference frequency files (BNC)
├── api.py          # Public API — extract() orchestrates the full pipeline
├── cli.py          # CLI entry point (jate command)
├── server.py       # REST API (FastAPI, jate-api command)
├── models.py       # Core data models (Term, TermExtractionResult, etc.)
├── features.py     # Corpus-level feature computation (TF, DF, co-occurrence, etc.)
├── context.py      # Context words/window extraction
├── corpus.py       # Corpus abstraction over stores
├── config.py       # Pipeline configuration (JATEConfig)
├── evaluation.py   # Precision/recall against gold standard
├── benchmark.py    # Built-in benchmarking harness
├── parallel.py     # Parallel processing utilities
└── protocols.py    # Protocol definitions (structural typing)
```

## Architectural Rules

1. **algorithms/ are pure scorers**: They receive pre-computed features and return scores. No I/O, no NLP, no extraction logic. Enforced by: `tests/test_architecture.py`
2. **extractors/ are independent**: They depend on nlp/ but not on algorithms/, store/, or each other. Enforced by: `tests/test_architecture.py`
3. **store/ has no domain logic**: Storage backends must not import algorithms, extractors, or features. Enforced by: `tests/test_architecture.py`
4. **nlp/ has no domain logic**: NLP backends must not import algorithms, extractors, features, or store. Enforced by: `tests/test_architecture.py`
5. **api.py is the orchestrator**: All pipeline composition happens in `api.py`. It wires algorithms, extractors, features, NLP, and store together.
6. **Features are built once**: When running multiple algorithms, use `FeatureCache` (in `api.py`) to build all features once for the union of algorithms. Never rebuild the same feature per algorithm. See: `api.py::FeatureCache`, `compare()`.
7. **Pipeline steps must log progress**: Any operation that processes documents or candidates in bulk must emit timestamped progress to stderr. Use the `_log()` pattern from `benchmark.py`. See: `docs/architecture-agent.md` § Logging.
8. **Data-intensive operations must consider parallelism**: Feature builders, candidate extraction, and scoring that iterate over large datasets should use `parallel.py::parallel_map` or `ProcessPoolExecutor` for CPU-bound work. Pre-tokenise/pre-compute shared data before parallel dispatch. See: `docs/architecture-agent.md` § Efficiency.
9. **New algorithms**: Subclass `Algorithm`, add to `algorithms/__init__.py`, register in `api.py::_FEATURE_NEEDS`. See `docs/contributing.md`.

## Conventions

- All commands via `poetry run <command>`
- Pre-commit: black (formatting), isort (imports), flake8 (linting)
- Type checking: mypy strict mode
- Branch workflow: `dev` for development, `master` for releases (triggers semantic-release)
- Tests must not require network access or external services

## Common Pitfalls

- spaCy model must be installed separately: `python -m spacy download en_core_web_sm`
- `features.py` is the largest file (577 lines) — read selectively, not in full
- `api.py::extract()` is the main entry point — start here when understanding the pipeline
- Never tokenise the same document twice in a loop — cache tokenisation results (see `features.py::_build_adjacent_words` for the pattern)
- Avoid O(n²) iterations over candidates/terms — use sparse lookups (see `chi_square.py` for the pattern)
- Use `FeatureCache` not `_build_features` when running multiple algorithms

## Tier 2 Docs

- Architecture deep-dive: `docs/architecture-agent.md`
- Contributing guide: `docs/contributing.md`
- Algorithm reference: `docs/guide/algorithms.md`
