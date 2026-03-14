ARG PYTHON_VERSION=3.11

FROM python:${PYTHON_VERSION}-slim AS builder
WORKDIR /app
COPY pyproject.toml README.md ./
COPY src ./src
RUN pip install --no-cache-dir --upgrade pip build && \
    python -m build --wheel --outdir /dist


FROM python:${PYTHON_VERSION}-slim AS runtime
ENV PYTHONDONTWRITEBYTECODE=1 \
    PYTHONUNBUFFERED=1
WORKDIR /app
COPY --from=builder /dist/*.whl /tmp/
RUN pip install --no-cache-dir /tmp/*.whl && \
    pip install --no-cache-dir "fastapi>=0.121.1,<0.122.0" "uvicorn>=0.38.0,<0.39.0" && \
    pip install --no-cache-dir \
    "https://github.com/explosion/spacy-models/releases/download/en_core_web_sm-3.8.0/en_core_web_sm-3.8.0-py3-none-any.whl" && \
    rm -rf /tmp/*.whl
EXPOSE 8000
CMD ["jate"]

