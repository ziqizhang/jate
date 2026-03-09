"""Noun-phrase-based candidate term extractor."""

from __future__ import annotations

from jate.extractors.base import CandidateExtractorBase
from jate.extractors.pos_pattern import STOPWORDS
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

        for doc in documents:
            corpus_store.add_document(doc)
            text = doc.content
            if not text.strip():
                continue

            # Split into sentences and compute document-level char offsets
            sentences: list[str] = nlp_backend.sentence_split(text)
            sent_char_offsets: list[int] = []
            search_from = 0
            for sent_text in sentences:
                idx = text.find(sent_text, search_from)
                if idx == -1:
                    idx = search_from  # fallback
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

                    # Strip leading/trailing stopwords (e.g. determiners).
                    words = chunk_stripped.split()
                    while words and words[0].lower() in STOPWORDS:
                        words.pop(0)
                    while words and words[-1].lower() in STOPWORDS:
                        words.pop()
                    if not words:
                        continue

                    surface = " ".join(words)
                    # Java JATE behaviour: only lemmatise the rightmost word.
                    if len(words) == 1:
                        normalized = lemma_map.get(words[0], words[0]).lower()
                    else:
                        normalized = " ".join(
                            [w.lower() for w in words[:-1]]
                            + [lemma_map.get(words[-1], words[-1]).lower()]
                        )

                    # Find character offset in the sentence text,
                    # advancing past previously found occurrences.
                    local_start = sent_text.find(chunk_stripped, sent_search_start)
                    if local_start != -1:
                        local_end = local_start + len(chunk_stripped)
                        sent_search_start = local_end
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

        return list(merged.values())
