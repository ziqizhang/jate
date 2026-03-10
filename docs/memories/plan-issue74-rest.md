## Plan: Issue 74 API-First Container

Expose JATE capabilities through a JSON REST API in a Docker image, instead of wrapping/parsing CLI console output.

### Response Mapping Example
CLI table columns map to JSON fields:
- Rank -> rank
- Term -> term
- Score -> score
- Frequency -> frequency

Suggested response envelope:
- algorithm
- extractor
- model
- top
- terms
