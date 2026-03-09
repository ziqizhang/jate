# Installation

## From source

```bash
git clone https://github.com/ziqizhang/jate.git
cd jate
pip install .
```

For development:

```bash
pip install poetry
poetry install --with dev
```

## Requirements

- **Python 3.11+**
- **spaCy language model** — JATE uses spaCy for tokenisation, POS tagging, and lemmatisation

Download a spaCy model after installing:

```bash
python -m spacy download en_core_web_sm
```

For other languages, install the appropriate spaCy model (e.g. `de_core_news_sm` for German, `fr_core_news_sm` for French).

## Optional dependencies

JATE has optional dependency groups for specific features:

```bash
# Interactive web UI (Streamlit)
pip install ".[ui]"

# Neural methods (PyTorch, transformers)
pip install ".[neural]"

# LLM-augmented extraction (LangChain, LangGraph)
pip install ".[llm]"
```

> **Note:** PyPI publishing is planned but not yet available. Install from source for now.

## Verify installation

```python
import jate
print(jate.__version__)

# Quick smoke test
result = jate.extract("Automatic term extraction from text corpora.")
for term in result:
    print(term.string, term.score)
```
