"""N-gram-based candidate term extractor."""

from __future__ import annotations

import re

from jate.extractors.base import CandidateExtractorBase
from jate.extractors.pos_pattern import STOPWORDS
from jate.extractors.utils import batch_process_docs, compute_token_offsets
from jate.models import Candidate, Document
from jate.protocols import CorpusStore, NLPBackend

_PUNCT_RE = re.compile(r"^[^\w]+$", re.UNICODE)


class NGramExtractor(CandidateExtractorBase):
    """Extract candidate terms as contiguous token n-grams.

    Parameters
    ----------
    min_n:
        Minimum number of tokens in an n-gram.
    max_n:
        Maximum number of tokens in an n-gram.
    """

    def __init__(self, min_n: int = 1, max_n: int = 5) -> None:
        if min_n < 1:
            raise ValueError("min_n must be >= 1")
        if max_n < min_n:
            raise ValueError("max_n must be >= min_n")
        self._min_n = min_n
        self._max_n = max_n

    # ------------------------------------------------------------------

    def extract(
        self,
        documents: list[Document],
        nlp_backend: NLPBackend,
        corpus_store: CorpusStore,
    ) -> list[Candidate]:
        merged: dict[str, Candidate] = {}

        # Add ALL documents to the store (including empty ones).
        for doc in documents:
            corpus_store.add_document(doc)

        valid_docs, spacy_docs = batch_process_docs(documents, nlp_backend)

        if spacy_docs is not None:
            self._extract_from_spacy_docs(valid_docs, spacy_docs, merged)
        else:
            self._extract_legacy(valid_docs, nlp_backend, merged)

        return list(merged.values())

    # ------------------------------------------------------------------
    # Fast path: pre-processed spaCy Docs
    # ------------------------------------------------------------------

    def _extract_from_spacy_docs(
        self,
        valid_docs: list[Document],
        spacy_docs: list,
        merged: dict[str, Candidate],
    ) -> None:
        for doc, spacy_doc in zip(valid_docs, spacy_docs):
            for sent_idx, sent in enumerate(spacy_doc.sents):
                sent_offset = sent.start_char

                tokens: list[str] = [token.text for token in sent]
                lemmas: list[str] = [token.lemma_ for token in sent]
                token_offsets = compute_token_offsets(sent.text, tokens)

                self._process_sentence(
                    doc, sent_idx, sent_offset,
                    tokens, lemmas, token_offsets, merged,
                )

    # ------------------------------------------------------------------
    # Legacy path: per-sentence NLP calls (backward compat)
    # ------------------------------------------------------------------

    def _extract_legacy(
        self,
        valid_docs: list[Document],
        nlp_backend: NLPBackend,
        merged: dict[str, Candidate],
    ) -> None:
        for doc in valid_docs:
            text = doc.content

            sentences: list[str] = nlp_backend.sentence_split(text)
            sent_char_offsets: list[int] = []
            search_from = 0
            for sent_text in sentences:
                idx = text.find(sent_text, search_from)
                if idx == -1:
                    idx = search_from
                sent_char_offsets.append(idx)
                search_from = idx + len(sent_text)

            for sent_idx, sent_text in enumerate(sentences):
                if not sent_text.strip():
                    continue
                sent_offset = sent_char_offsets[sent_idx]

                tokens: list[str] = nlp_backend.tokenize(sent_text)
                lemma_pairs: list[tuple[str, str]] = nlp_backend.lemmatize(sent_text)
                lemmas: list[str] = [lem for _, lem in lemma_pairs]
                token_offsets = compute_token_offsets(sent_text, tokens)

                self._process_sentence(
                    doc, sent_idx, sent_offset,
                    tokens, lemmas, token_offsets, merged,
                )

    # ------------------------------------------------------------------
    # Shared sentence processing
    # ------------------------------------------------------------------

    def _process_sentence(
        self,
        doc: Document,
        sent_idx: int,
        sent_offset: int,
        tokens: list[str],
        lemmas: list[str],
        token_offsets: list[tuple[int, int]],
        merged: dict[str, Candidate],
    ) -> None:
        for n in range(self._min_n, self._max_n + 1):
            for i in range(len(tokens) - n + 1):
                span_tokens = tokens[i : i + n]
                span_lemmas = lemmas[i : i + n]

                # Skip n-grams starting or ending with stopwords or punctuation.
                if _is_stop_or_punct(span_tokens[0]) or _is_stop_or_punct(span_tokens[-1]):
                    continue

                surface = " ".join(span_tokens)
                # Java JATE behaviour: only lemmatise the rightmost word.
                if len(span_tokens) == 1:
                    normalized = span_lemmas[0].lower()
                else:
                    normalized = " ".join(
                        [t.lower() for t in span_tokens[:-1]] + [span_lemmas[-1].lower()]
                    )

                # Character offsets converted to document-level.
                doc_start = sent_offset + token_offsets[i][0]
                doc_end = sent_offset + token_offsets[i + n - 1][1]

                if normalized in merged:
                    merged[normalized].add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx, surface=surface)
                else:
                    cand = Candidate(
                        surface_form=surface,
                        normalized_form=normalized,
                    )
                    cand.add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx)
                    merged[normalized] = cand


# ------------------------------------------------------------------
# Helpers
# ------------------------------------------------------------------


def _is_stop_or_punct(token: str) -> bool:
    return token.lower() in STOPWORDS or bool(_PUNCT_RE.match(token))
