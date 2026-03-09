"""spaCy-based NLP backend for JATE."""

from __future__ import annotations

from typing import Any

import spacy
from spacy.language import Language
from spacy.tokens import Doc


class SpacyBackend:
    """NLP backend powered by spaCy.

    Satisfies the :class:`~jate.protocols.NLPBackend` protocol.

    Parameters
    ----------
    model_name:
        Name of the spaCy model to load (default ``"en_core_web_sm"``).
    """

    __slots__ = ("_nlp", "_cached_text", "_cached_doc")

    def __init__(self, model_name: str = "en_core_web_sm") -> None:
        try:
            self._nlp: Language = spacy.load(model_name)
        except OSError as exc:
            raise OSError(
                f"spaCy model {model_name!r} is not installed. "
                f"Install it with: python -m spacy download {model_name}"
            ) from exc
        self._cached_text: str | None = None
        self._cached_doc: Doc | None = None

    # -- Single-entry cache ---------------------------------------------------

    def process(self, text: str) -> Doc:
        """Process *text* through the spaCy pipeline, with single-entry caching.

        If *text* is the same as the last call, the cached doc is returned
        without re-processing.
        """
        if text != self._cached_text or self._cached_doc is None:
            self._cached_doc = self._nlp(text)
            self._cached_text = text
        return self._cached_doc

    # -- Batch processing -----------------------------------------------------

    def process_batch(self, texts: list[str], batch_size: int = 256) -> list[Doc]:
        """Process multiple texts efficiently using spaCy's ``nlp.pipe()``.

        Uses spaCy's built-in batching which releases the GIL during
        C-level parsing, enabling multi-threaded speedup.

        Parameters
        ----------
        texts:
            List of text strings to process.
        batch_size:
            Number of texts to buffer (default ``256``).
        """
        if not texts:
            return []
        return list(self._nlp.pipe(texts, batch_size=batch_size))

    # -- NLPBackend protocol methods ------------------------------------------

    def tokenize(self, text: str) -> list[str]:
        """Split *text* into tokens."""
        doc = self.process(text)
        return [token.text for token in doc]

    def pos_tag(self, text: str) -> list[tuple[str, str]]:
        """Return ``(token, universal-POS-tag)`` pairs."""
        doc = self.process(text)
        return [(token.text, token.pos_) for token in doc]

    def lemmatize(self, text: str) -> list[tuple[str, str]]:
        """Return ``(token, lemma)`` pairs."""
        doc = self.process(text)
        return [(token.text, token.lemma_) for token in doc]

    def sentence_split(self, text: str) -> list[str]:
        """Split *text* into sentences."""
        doc = self.process(text)
        return [sent.text for sent in doc.sents]

    def noun_chunks(self, text: str) -> list[str]:
        """Extract noun-phrase chunks from *text*."""
        doc = self.process(text)
        return [chunk.text for chunk in doc.noun_chunks]
