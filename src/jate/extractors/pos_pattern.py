"""POS-pattern-based candidate term extractor."""

from __future__ import annotations

import re
from typing import Any

from jate.extractors.base import CandidateExtractorBase
from jate.extractors.utils import batch_process_docs, compute_token_offsets
from jate.models import Candidate, Document
from jate.protocols import CorpusStore, NLPBackend

# Minimal English stopword set (sufficient for stripping leading/trailing).
STOPWORDS: frozenset[str] = frozenset(
    {
        "a",
        "an",
        "the",
        "this",
        "that",
        "these",
        "those",
        "is",
        "are",
        "was",
        "were",
        "be",
        "been",
        "being",
        "have",
        "has",
        "had",
        "do",
        "does",
        "did",
        "will",
        "would",
        "shall",
        "should",
        "may",
        "might",
        "must",
        "can",
        "could",
        "of",
        "in",
        "to",
        "for",
        "with",
        "on",
        "at",
        "from",
        "by",
        "about",
        "as",
        "into",
        "through",
        "during",
        "before",
        "after",
        "above",
        "below",
        "between",
        "and",
        "but",
        "or",
        "nor",
        "not",
        "so",
        "yet",
        "both",
        "either",
        "neither",
        "each",
        "every",
        "all",
        "any",
        "few",
        "more",
        "most",
        "other",
        "some",
        "such",
        "no",
        "only",
        "own",
        "same",
        "than",
        "too",
        "very",
        "just",
        "because",
        "if",
        "when",
        "where",
        "how",
        "what",
        "which",
        "who",
        "whom",
        "its",
        "it",
        "i",
        "me",
        "my",
        "we",
        "our",
        "you",
        "your",
        "he",
        "him",
        "his",
        "she",
        "her",
        "they",
        "them",
        "their",
    }
)


class PosPatternExtractor(CandidateExtractorBase):
    """Extract candidate terms by matching regex patterns over POS-tag sequences.

    The *pattern* is a regular expression applied to a space-separated string
    of Universal POS tags (e.g. ``"ADJ NOUN NOUN"``).  Matching spans are
    mapped back to the original tokens to recover surface forms.
    """

    def __init__(self, pattern: str = r"(ADJ |VERB )*(NOUN )+") -> None:
        self._pattern = re.compile(pattern)

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
        spacy_docs: list[Any],
        merged: dict[str, Candidate],
    ) -> None:
        total = len(valid_docs)
        report_every = max(1, total // 10)  # report ~10 times
        for doc_idx, (doc, spacy_doc) in enumerate(zip(valid_docs, spacy_docs)):
            if total > 100 and doc_idx % report_every == 0 and doc_idx > 0:
                from datetime import datetime

                ts = datetime.now().strftime("%H:%M:%S")
                print(
                    f"[{ts}]   Candidate extraction: {doc_idx}/{total} documents ...",
                    file=__import__("sys").stderr,
                    flush=True,
                )
            for sent_idx, sent in enumerate(spacy_doc.sents):
                sent_offset = sent.start_char

                tokens: list[str] = [token.text for token in sent]
                pos_tags: list[str] = [token.pos_ for token in sent]
                lemmas: list[str] = [token.lemma_ for token in sent]

                self._process_sentence(
                    doc,
                    sent_idx,
                    sent_offset,
                    sent.text,
                    tokens,
                    pos_tags,
                    lemmas,
                    merged,
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

                tagged: list[tuple[str, str]] = nlp_backend.pos_tag(sent_text)
                lemmas_pairs: list[tuple[str, str]] = nlp_backend.lemmatize(sent_text)

                pos_tags: list[str] = [tag for _, tag in tagged]
                tokens: list[str] = [tok for tok, _ in tagged]
                lemmas: list[str] = [lem for _, lem in lemmas_pairs]

                self._process_sentence(
                    doc,
                    sent_idx,
                    sent_offset,
                    sent_text,
                    tokens,
                    pos_tags,
                    lemmas,
                    merged,
                )

    # ------------------------------------------------------------------
    # Shared sentence processing
    # ------------------------------------------------------------------

    def _process_sentence(
        self,
        doc: Document,
        sent_idx: int,
        sent_offset: int,
        sent_text: str,
        tokens: list[str],
        pos_tags: list[str],
        lemmas: list[str],
        merged: dict[str, Candidate],
    ) -> None:
        # Build a POS string with one tag per token, space-separated,
        # **with a trailing space** so patterns ending with ``(NOUN )+``
        # can match the last token.
        pos_string = " ".join(pos_tags) + " "

        # We need a mapping from character offset in pos_string to token index.
        # Each tag occupies len(tag)+1 characters (tag + space).
        tag_char_starts: list[int] = []
        offset = 0
        for tag in pos_tags:
            tag_char_starts.append(offset)
            offset += len(tag) + 1  # +1 for the space

        # Compute character offsets of tokens in the sentence text.
        token_char_offsets: list[tuple[int, int]] = compute_token_offsets(sent_text, tokens)

        for m in self._pattern.finditer(pos_string):
            start_char = m.start()
            end_char = m.end()  # exclusive, may point into trailing space

            # Map POS-string char offsets to token indices.
            tok_start = _char_to_token_index(tag_char_starts, start_char)
            # end_char-1 because the match includes trailing space
            tok_end = _char_to_token_index(tag_char_starts, end_char - 1)
            span_tokens = tokens[tok_start : tok_end + 1]
            span_lemmas = lemmas[tok_start : tok_end + 1]
            span_pos = pos_tags[tok_start : tok_end + 1]

            # Strip leading/trailing stopwords.
            span_tokens, span_lemmas, span_pos, strip_left = _strip_stopwords(span_tokens, span_lemmas, span_pos)
            if not span_tokens:
                continue

            actual_tok_start = tok_start + strip_left
            actual_tok_end = actual_tok_start + len(span_tokens) - 1

            surface = " ".join(span_tokens)
            # Java JATE behaviour: only lemmatise the rightmost word.
            if len(span_tokens) == 1:
                normalized = span_lemmas[0].lower()
            else:
                normalized = " ".join([t.lower() for t in span_tokens[:-1]] + [span_lemmas[-1].lower()])
            pos_pat = " ".join(span_pos)

            # Character offsets converted to document-level.
            doc_start = sent_offset + token_char_offsets[actual_tok_start][0]
            doc_end = sent_offset + token_char_offsets[actual_tok_end][1]

            if normalized in merged:
                merged[normalized].add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx, surface=surface)
            else:
                cand = Candidate(
                    surface_form=surface,
                    normalized_form=normalized,
                    pos_pattern=pos_pat,
                )
                cand.add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx)
                merged[normalized] = cand


# ------------------------------------------------------------------
# Helpers
# ------------------------------------------------------------------


def _char_to_token_index(tag_char_starts: list[int], char_pos: int) -> int:
    """Binary-search *tag_char_starts* to find which token owns *char_pos*."""
    lo, hi = 0, len(tag_char_starts) - 1
    while lo <= hi:
        mid = (lo + hi) // 2
        if tag_char_starts[mid] <= char_pos:
            lo = mid + 1
        else:
            hi = mid - 1
    return hi


def _strip_stopwords(
    tokens: list[str],
    lemmas: list[str],
    pos_tags: list[str],
) -> tuple[list[str], list[str], list[str], int]:
    """Strip leading and trailing stopwords; return trimmed lists + count stripped from left."""
    left = 0
    while left < len(tokens) and tokens[left].lower() in STOPWORDS:
        left += 1
    right = len(tokens)
    while right > left and tokens[right - 1].lower() in STOPWORDS:
        right -= 1
    return tokens[left:right], lemmas[left:right], pos_tags[left:right], left
