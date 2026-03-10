"""Noun-phrase-based candidate term extractor."""

from __future__ import annotations

from typing import Any

from jate.extractors.base import CandidateExtractorBase
from jate.extractors.pos_pattern import STOPWORDS
from jate.extractors.utils import batch_process_docs
from jate.models import Candidate, Document
from jate.protocols import CorpusStore, NLPBackend


class NounPhraseExtractor(CandidateExtractorBase):
    """Extract candidate terms via the NLP backend's noun-chunk detector."""

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
        for doc, spacy_doc in zip(valid_docs, spacy_docs):
            for sent_idx, sent in enumerate(spacy_doc.sents):
                sent_offset = sent.start_char

                # Get noun chunks that fall within this sentence.
                sent_chunks = [
                    chunk for chunk in spacy_doc.noun_chunks if chunk.start >= sent.start and chunk.end <= sent.end
                ]
                lemma_map = {token.text: token.lemma_ for token in sent}

                sent_search_start = 0
                for chunk in sent_chunks:
                    chunk_stripped = chunk.text.strip()
                    if not chunk_stripped:
                        continue

                    self._process_chunk(
                        doc,
                        sent_idx,
                        sent_offset,
                        sent.text,
                        chunk_stripped,
                        lemma_map,
                        sent_search_start,
                        merged,
                    )
                    # Advance search start past this chunk in the sentence.
                    local_start = sent.text.find(chunk_stripped, sent_search_start)
                    if local_start != -1:
                        sent_search_start = local_start + len(chunk_stripped)

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

                chunks: list[str] = nlp_backend.noun_chunks(sent_text)
                lemma_map = dict(nlp_backend.lemmatize(sent_text))

                sent_search_start = 0
                for chunk in chunks:
                    chunk_stripped = chunk.strip()
                    if not chunk_stripped:
                        continue

                    self._process_chunk(
                        doc,
                        sent_idx,
                        sent_offset,
                        sent_text,
                        chunk_stripped,
                        lemma_map,
                        sent_search_start,
                        merged,
                    )
                    local_start = sent_text.find(chunk_stripped, sent_search_start)
                    if local_start != -1:
                        sent_search_start = local_start + len(chunk_stripped)

    # ------------------------------------------------------------------
    # Shared chunk processing
    # ------------------------------------------------------------------

    @staticmethod
    def _process_chunk(
        doc: Document,
        sent_idx: int,
        sent_offset: int,
        sent_text: str,
        chunk_stripped: str,
        lemma_map: dict[str, str],
        sent_search_start: int,
        merged: dict[str, Candidate],
    ) -> None:
        # Strip leading/trailing stopwords (e.g. determiners).
        words = chunk_stripped.split()
        while words and words[0].lower() in STOPWORDS:
            words.pop(0)
        while words and words[-1].lower() in STOPWORDS:
            words.pop()
        if not words:
            return

        surface = " ".join(words)
        # Java JATE behaviour: only lemmatise the rightmost word.
        if len(words) == 1:
            normalized = lemma_map.get(words[0], words[0]).lower()
        else:
            normalized = " ".join([w.lower() for w in words[:-1]] + [lemma_map.get(words[-1], words[-1]).lower()])

        # Find character offset in the sentence text,
        # advancing past previously found occurrences.
        local_start = sent_text.find(chunk_stripped, sent_search_start)
        if local_start != -1:
            local_end = local_start + len(chunk_stripped)
        else:
            local_start = 0
            local_end = 0

        # Convert to document-level offsets.
        doc_start = sent_offset + local_start
        doc_end = sent_offset + local_end

        if normalized in merged:
            merged[normalized].add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx, surface=surface)
        else:
            cand = Candidate(
                surface_form=surface,
                normalized_form=normalized,
            )
            cand.add_position(doc.doc_id, doc_start, doc_end, sentence_idx=sent_idx)
            merged[normalized] = cand
