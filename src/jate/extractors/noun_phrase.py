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

            chunks: list[str] = nlp_backend.noun_chunks(text)
            lemma_map = dict(nlp_backend.lemmatize(text))

            search_start = 0
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

                # Find character offset in the document text,
                # advancing past previously found occurrences.
                start = text.find(chunk_stripped, search_start)
                if start != -1:
                    end = start + len(chunk_stripped)
                    search_start = end
                else:
                    start = 0
                    end = 0

                if normalized in merged:
                    merged[normalized].add_position(doc.doc_id, start, end, surface=surface)
                else:
                    cand = Candidate(
                        surface_form=surface,
                        normalized_form=normalized,
                    )
                    cand.add_position(doc.doc_id, start, end)
                    merged[normalized] = cand

        return list(merged.values())
