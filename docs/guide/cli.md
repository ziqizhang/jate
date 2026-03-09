# CLI Reference

JATE provides a command-line interface for extracting, comparing, and benchmarking term extraction algorithms.

## Commands

### `jate extract`

Extract terms from inline text.

```bash
jate extract "Your text here" --algorithm cvalue --top 20
```

| Flag | Default | Description |
|------|---------|-------------|
| `--algorithm` | `cvalue` | Algorithm to use |
| `--extractor` | `pos_pattern` | Candidate extractor (`pos_pattern`, `ngram`, `noun_phrase`) |
| `--top` | (all) | Limit output to top N terms |
| `--output` | `table` | Output format: `table`, `csv`, or `json` |
| `--min-frequency` | (none) | Minimum term frequency filter |
| `--min-words` | (none) | Minimum number of words per term |

### `jate corpus`

Extract terms from a directory of text files.

```bash
jate corpus path/to/docs/ --algorithm tfidf --top 50 --output csv
```

| Flag | Default | Description |
|------|---------|-------------|
| `--algorithm` | `cvalue` | Algorithm to use |
| `--extractor` | `pos_pattern` | Candidate extractor |
| `--top` | (all) | Limit output |
| `--output` | `table` | Output format |
| `--min-frequency` | (none) | Minimum frequency filter |
| `--min-words` | (none) | Minimum word count filter |

### `jate compare`

Run multiple algorithms on a corpus and compare results side by side.

```bash
jate compare path/to/docs/ --algorithms cvalue tfidf rake
```

| Flag | Default | Description |
|------|---------|-------------|
| `--algorithms` | `cvalue tfidf` | Algorithms to compare |
| `--top` | `20` | Top N terms per algorithm |
| `--output` | `table` | Output format |

### `jate benchmark`

Run all algorithms on the built-in ACL RD-TEC Mini dataset and print evaluation results.

```bash
jate benchmark --top 100
```

| Flag | Default | Description |
|------|---------|-------------|
| `--top` | `100` | Evaluate top N terms |

### `jate demo`

Launch the interactive web demo (requires `jate[ui]`).

```bash
jate demo
```

## Output formats

### Table (default)

```
Rank  Term                           Score      Frequency
   1  machine learning               12.4567    45
   2  neural network                  9.8765    32
```

### CSV

```
term,score,frequency,rank
machine learning,12.4567,45,1
neural network,9.8765,32,2
```

### JSON

```json
[
  {"term": "machine learning", "score": 12.4567, "frequency": 45, "rank": 1},
  {"term": "neural network", "score": 9.8765, "frequency": 32, "rank": 2}
]
```

## Examples

```bash
# Quick extraction from text
jate extract "Deep learning models use neural networks for classification" --top 5

# Corpus extraction with frequency filter
jate corpus ./papers/ --algorithm cvalue --min-frequency 3 --top 50

# Compare algorithms as CSV
jate compare ./papers/ --algorithms cvalue tfidf weirdness --output csv

# Benchmark with top 200
jate benchmark --top 200
```
