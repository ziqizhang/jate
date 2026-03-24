# Web Demo (Gradio + HuggingFace Spaces) — Design Spec

Issue: #66
Date: 2026-03-23

## Summary

Build a Gradio web demo for interactive term extraction, deployable to HuggingFace Spaces (free hosting) and runnable locally. Three tabs: single text extraction, small corpus mode, and static benchmark visualisation.

## Decisions

| Decision | Choice | Rationale |
|----------|--------|-----------|
| Framework | Gradio | Better default aesthetics, built-in examples, native HuggingFace Spaces support |
| Hosting | HuggingFace Spaces (free tier: 2 vCPU, 16GB RAM) | Zero cost, always-on demo |
| Local run | `pip install "jate[demo]"` then `python demo/app.py` | Self-contained, optional dependency |
| CLI integration | None | Demo is a standalone app, not a CLI command |
| Corpus limit | 10 files, each under 10KB, .txt only | Fits free tier resources |
| Algorithm limit | Up to 3 per run | Keeps response time reasonable |

## Tab 1: "Extract Terms"

Single text input for quick term extraction.

**Inputs:**
- Text area with min length (~50 chars) and max length (~5,000 chars) enforcement
- Algorithm multi-select: up to 3 algorithms. TF-IDF and other corpus-only algorithms greyed out with tooltip ("Requires multiple documents — use Corpus Mode tab")
- Pre-loaded example buttons: biomedical, legal, tech, coastal science texts

**Available algorithms (Tab 1):**
- Enabled: cvalue, ncvalue, basic, combobasic, attf, ttf, ridf, rake, chi_square, weirdness, glossex, termex, nmf, xlmr-tagger
- Disabled (greyed out): tfidf (IDF=0 on single doc)

**Outputs:**
- Highlighted text showing where extracted terms appear in the source
- Ranked results table per algorithm (term, score, frequency)
- If multiple algorithms selected: side-by-side comparison

**Performance:** 2-5 seconds per ranker, ~2 seconds for tagger.

## Tab 2: "Corpus Mode"

Small corpus upload for corpus-level extraction.

**Inputs:**
- Drag-and-drop file upload: .txt files only, up to 10 files, each under 10KB
- Algorithm multi-select: up to 3 algorithms. All algorithms available (including tfidf)
- File validation with clear error messages

**Available algorithms (Tab 2):**
- All 14 rankers + xlmr-tagger enabled

**Outputs:**
- Ranked results table per algorithm (term, score, frequency)
- If multiple algorithms selected: comparison view showing overlap and differences
- Summary: number of documents, candidates extracted, terms per algorithm

**Performance:** 10-30 seconds depending on corpus size and algorithms.

## Tab 3: "Benchmarks"

Static visualisation of pre-computed benchmark results.

**Content:**
- Interactive bar charts: P@K (100, 500, 1000) by algorithm, grouped by dataset
- Dataset selector: GENIA, ACL RD-TEC, ACTER, CoastTerm
- Algorithm comparison across datasets
- XLM-R tagger results (set-based P/R/F1)
- NMF results
- Link to full benchmark methodology in docs/benchmark-results.md

**Data source:** Pre-computed from docs/benchmark-results.md, hardcoded in the app (no live computation).

**Performance:** Instant (static data).

## File structure

```
demo/
  app.py              # Main Gradio application
  requirements.txt    # Dependencies for HuggingFace Spaces deployment
```

`requirements.txt` for the Space:
```
jate @ git+https://github.com/ziqizhang/jate.git@dev
gradio>=4.0
plotly>=5.0
```

## Dependency

In `pyproject.toml`:
```toml
[tool.poetry.extras]
demo = ["gradio", "plotly"]
```

Local run:
```bash
pip install "jate[demo]"
python demo/app.py
```

## Algorithm selection logic

**Tab 1 (single text):**
- Suppress corpus-level warnings (user already knows it's single-doc mode)
- Disable tfidf (returns empty results)
- Run selected algorithms, catch `AlgorithmIncompatibleError` gracefully
- For tagger: call `tag()` directly
- For rankers: call `extract()` which handles single-doc mode

**Tab 2 (corpus):**
- All algorithms enabled
- Build corpus from uploaded files
- Call `compare()` with selected algorithms (uses FeatureCache — features built once)
- For tagger: process each document, union results

**Both tabs:**
- Maximum 3 algorithms per run
- Run algorithms in sequence (Gradio runs in a single thread)
- Show progress indicator during computation

## Pre-loaded examples (Tab 1)

4-5 example texts from different domains, each ~200-500 words:

- **Biomedical**: excerpt about gene expression and cell signalling
- **Computer Science**: excerpt about machine learning and neural networks
- **Legal/Corruption**: excerpt about anti-corruption measures
- **Coastal Science**: excerpt about marine ecosystems
- **General**: news article or Wikipedia passage

These let users see results instantly without typing, and demonstrate domain-specific extraction.

## Benchmark data (Tab 3)

Hardcoded from current benchmark-results.md:

```python
BENCHMARK_DATA = {
    "genia": {
        "attf": {"P@100": 0.79, "P@500": 0.74, "P@1000": 0.74},
        "ridf": {"P@100": 0.77, "P@500": 0.76, "P@1000": 0.77},
        ...
    },
    ...
}
```

Rendered as Plotly bar charts with dataset selector dropdown.

## Implementation steps

1. Create `demo/app.py` with three-tab Gradio interface
2. Implement Tab 1 (Extract Terms) — text input, algorithm selection, highlighted output
3. Implement Tab 2 (Corpus Mode) — file upload, validation, comparison output
4. Implement Tab 3 (Benchmarks) — static Plotly charts from hardcoded data
5. Add `gradio` and `plotly` as optional demo dependencies in pyproject.toml
6. Create `demo/requirements.txt` for HuggingFace Spaces
7. Test locally
8. Deploy to HuggingFace Spaces
9. Add "Try it live" badge to README

## What this does NOT include

- Local-only full-featured UI (separate future project)
- GPU support for tagger
- Large corpus processing
- User accounts or saved results
- Word cloud visualisation (may add later)
