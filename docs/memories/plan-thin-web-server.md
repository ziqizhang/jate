## Plan: Thin REST Server + Docker Image

### Scope
- Included: extract endpoint, health endpoints, JSON responses, Docker image, GHCR publish path.
- Excluded in v1: async jobs, compare/corpus endpoints, benchmark endpoint.

### Phases
1. API contract and schema.
2. Thin server adapter.
3. Runtime behavior and reliability.
4. Docker packaging.
5. Publish workflow and verification.

### Decisions
- JSON-first response contract.
- Thin adapter over core API.
- Docker remains a required deliverable.
