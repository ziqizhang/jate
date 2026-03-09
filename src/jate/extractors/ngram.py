"""N-gram-based candidate term extractor."""

from __future__ import annotations

import re

from jate.extractors.base import CandidateExtractorBase
from jate.extractors.pos_pattern import STOPWORDS
from jate.extractors.utils import compute_token_offsets
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

        for doc in documents:
            corpus_store.add_document(doc)
            text = doc.content
            if not text.strip():
                continue

            tokens: list[str] = nlp_backend.tokenize(text)
            lemma_pairs: list[tuple[str, str]] = nlp_backend.lemmatize(text)
            lemmas: list[str] = [lem for _, lem in lemma_pairs]

            # Compute character offsets for tokens in the original text.
            token_offsets = compute_token_offsets(text, tokens)

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

                    doc_start = token_offsets[i][0]
                    doc_end = token_offsets[i + n - 1][1]

                    if normalized in merged:
                        merged[normalized].add_position(doc.doc_id, doc_start, doc_end, surface=surface)
                    else:
                        cand = Candidate(
                            surface_form=surface,
                            normalized_form=normalized,
                        )
                        cand.add_position(doc.doc_id, doc_start, doc_end)
                        merged[normalized] = cand

        return list(merged.values())


# ------------------------------------------------------------------
# Helpers
# ------------------------------------------------------------------


def _is_stop_or_punct(token: str) -> bool:
    return token.lower() in STOPWORDS or bool(_PUNCT_RE.match(token))
