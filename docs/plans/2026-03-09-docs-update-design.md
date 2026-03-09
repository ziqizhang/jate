# Documentation Update Design

**Date:** 2026-03-09
**Status:** Approved

## Problem

Recent features (sentence-level context, parallelism) are not reflected in the mkdocs documentation, changelog, or README. Code examples in quickstart have not been verified against the current codebase.

## Approach

1. Verify all quickstart code snippets work as-is
2. Update existing docs to cover new features — no new pages
3. Verify any new examples added

## Files to Update

| File | Change |
|------|--------|
| `docs/changelog.md` | Add sentence-context and parallelism entries |
| `docs/guide/architecture.md` | Update module map (config.py, parallel.py, features.py, context.py); update pipeline diagram with parallel stages and ContextIndex |
| `docs/guide/algorithms.md` | Note sentence-level context for Chi-Square and NC-Value |
| `docs/getting-started/quickstart.md` | Add parallelism example to compare() section |
| `docs/reference/api.md` | Add JATEConfig, ContextIndex to mkdocstrings |
| `README.md` | Brief mention of parallelism config |
| `docs/index.md` | Add parallelism and sentence context to feature list |

## What Stays the Same

- No new documentation pages
- No automated doc testing
- No restructuring of existing docs
