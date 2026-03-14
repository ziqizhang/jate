# API Reference

Auto-generated from source docstrings.

## REST API (v1)

JATE also provides a thin HTTP server for JSON-based extraction.

### Start server

Install server dependencies first:

```bash
pip install "jate[server]"
```

```bash
jate-api
```

### Endpoints

- `POST /jate/api/v1/extract`
- `GET /jate/api/v1/capabilities`
- `GET /health/live`
- `GET /health/ready`

### Example request

```bash
curl --header "Content-Type: application/json" \
	--request POST \
	--data '{"text":"text to process","algorithm":"cvalue"}' \
	http://localhost:8000/jate/api/v1/extract
```

### Example response

```json
{
	"algorithm": "cvalue",
	"extractor": "pos_pattern",
	"model": "en_core_web_sm",
	"top": 3,
	"terms": [
		{
			"rank": 1,
			"term": "local governor",
			"score": 1.0704,
			"frequency": 1,
			"surface_forms": [],
			"metadata": {}
		}
	]
}
```

## Public API functions

::: jate.api.extract

::: jate.api.extract_corpus

::: jate.api.compare

## Configuration

::: jate.config.JATEConfig

## Models

::: jate.models.Term

::: jate.models.Candidate

::: jate.models.Document

::: jate.models.TermExtractionResult

## Evaluation

::: jate.evaluation.Evaluator

::: jate.evaluation.EvaluationResult

## Benchmark

::: jate.benchmark.BenchmarkRunner

## Corpus

::: jate.corpus.Corpus

## Feature Classes

::: jate.features.TermFrequency

::: jate.features.WordFrequency

::: jate.features.ReferenceFrequency

::: jate.features.ContextWindow

::: jate.features.ContextFrequency

::: jate.features.TermComponentIndex

::: jate.features.Containment

::: jate.features.Cooccurrence

::: jate.features.ChiSquareFrequentTerms

## Algorithms

::: jate.algorithms.base.Algorithm

::: jate.algorithms.tfidf.TFIDF

::: jate.algorithms.cvalue.CValue

::: jate.algorithms.ncvalue.NCValue

::: jate.algorithms.basic.Basic

::: jate.algorithms.combo_basic.ComboBasic

::: jate.algorithms.attf.ATTF

::: jate.algorithms.ttf.TTF

::: jate.algorithms.ridf.RIDF

::: jate.algorithms.rake.RAKE

::: jate.algorithms.chi_square.ChiSquare

::: jate.algorithms.weirdness.Weirdness

::: jate.algorithms.termex.TermEx

::: jate.algorithms.glossex.GlossEx

::: jate.algorithms.voting.Voting

## Extractors

::: jate.extractors.pos_pattern.PosPatternExtractor

::: jate.extractors.ngram.NGramExtractor

::: jate.extractors.noun_phrase.NounPhraseExtractor

## NLP

::: jate.nlp.spacy_backend.SpacyBackend

::: jate.nlp.document_loader.DocumentLoader

## Stores

::: jate.store.memory_store.MemoryCorpusStore

::: jate.store.sqlite_store.SQLiteCorpusStore

## Parallel Utilities

::: jate.parallel.parallel_map

::: jate.parallel.split_range

## Protocols

::: jate.protocols.NLPBackend

::: jate.protocols.CandidateExtractor

::: jate.protocols.CorpusStore
