## Plan: Thin REST Server + Docker Image

Add a thin web server that exposes JATE extraction as a JSON endpoint, while still packaging JATE into an official Docker image for easy deployment. Keep the server adapter minimal and delegate extraction to existing core APIs.

### Steps
1. Finalize v1 API contract for extract and health endpoints.
2. Add a thin ASGI server module that calls existing extraction API functions.
3. Add JSON request/response models and error mapping.
4. Add Docker build/runtime assets for server deployment.
5. Add release-time image publishing workflow.
6. Add tests and docs for endpoint + container usage.

### API Contract
- Endpoint: POST /jate/api/v1/extract.
- Request fields: text (required), algorithm, extractor, model, top, min_frequency, min_words, max_words, algorithm_options.
- Response fields: algorithm, extractor, model, top, terms[].
- Term fields: rank, term, score, frequency, surface_forms, metadata.
- Health endpoints: GET /health/live and GET /health/ready.

### Implementation Notes
- Thin adapter over core API (`jate.api.extract`) with in-process execution.
- Do not shell out to CLI or parse console table output.
- Keep v1 synchronous.

### Docker
- Multi-stage Dockerfile.
- Install project wheel.
- Preinstall pinned spaCy model.
- Expose port 8000 and run ASGI server.

### Verification
1. POST /jate/api/v1/extract returns ranked JSON terms.
2. Invalid algorithm/extractor returns non-2xx JSON error.
3. Dockerized server responds on localhost:8000.
