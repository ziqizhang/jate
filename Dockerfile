# -------- Builder stage --------
FROM python:3.11-slim AS builder

WORKDIR /app

RUN pip install --upgrade pip

# install package
RUN pip install jate spacy

# install spaCy model required by README
RUN python -m spacy download en_core_web_sm


# -------- Runtime stage --------
FROM python:3.11-slim

WORKDIR /app

COPY --from=builder /usr/local /usr/local

ENTRYPOINT ["jate"]