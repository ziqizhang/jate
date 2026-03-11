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

## Running tests

```bash
poetry run pytest tests/
```

For API/container changes, run Docker smoke checks with runtime env vars:

```bash
bash scripts/docker_smoke_api.sh
```

From other directories, or with custom Dockerfile/context:

```bash
bash scripts/docker_smoke_api.sh /path/to/Dockerfile /path/to/build-context
```

If port conflicts exist, override host port:

```bash
JATE_SMOKE_HOST_PORT=18001 bash scripts/docker_smoke_api.sh
```

This smoke test validates all current API endpoints (`/health/live`, `/health/ready`, `/jate/api/v1/capabilities`, `/jate/api/v1/extract`) and key non-2xx error paths.

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

- `master` — release branch (triggers semantic-release)
- `dev` — development branch (submit PRs here)

1. Fork the repo and create a branch from `dev`
2. Make your changes
3. Add tests for new functionality
4. Ensure all tests pass: `poetry run pytest`
5. For API/container changes, ensure smoke checks pass: `bash scripts/docker_smoke_api.sh`
6. Ensure code passes linting: `poetry run pre-commit run --all-files`
7. Submit a PR to `dev`

## Adding a new algorithm

1. Create a new file in `src/jate/algorithms/` (e.g. `my_algo.py`)
2. Subclass `Algorithm` and implement `score(self, candidates, term_freq, **kwargs)` — receive feature objects via kwargs
3. Add the class to `src/jate/algorithms/__init__.py`
4. Add a name mapping in `src/jate/api.py` (`_ALGORITHM_NAMES`)
5. If the algorithm needs features beyond `TermFrequency`, add it to the appropriate `_NEEDS_*` tuple in `api.py` so features are built lazily
6. Export from `src/jate/__init__.py`
7. Add tests in `tests/test_algorithms.py`
8. Document in `docs/guide/algorithms.md`

## Reporting issues

- **Bug reports:** [GitHub Issues](https://github.com/ziqizhang/jate/issues/new?template=bug_report.yml)
- **Feature requests:** [GitHub Issues](https://github.com/ziqizhang/jate/issues/new?template=feature_request.yml)
