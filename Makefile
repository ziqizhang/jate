.PHONY: lint test typecheck check build clean

# Format and style check (via pre-commit)
lint:
	poetry run pre-commit run --all-files

# Unit tests — the fast feedback loop
test:
	poetry run pytest tests/ -v --tb=short -q

# Static type checking (mypy, strict mode)
typecheck:
	poetry run mypy src/jate/ || true

# Run all fast checks — the single command after every change
check: lint test typecheck

# Build distributable package
build:
	poetry build

# Remove generated files
clean:
	rm -rf dist/ build/ .mypy_cache/ .pytest_cache/ **/__pycache__/
