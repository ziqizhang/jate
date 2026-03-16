"""Transformer-based token classification taggers for ATE."""

from __future__ import annotations

from typing import Any

from jate.algorithms.base import ATETagger, OutputCapabilities
from jate.models import Term, TermExtractionResult, TermSpan


def _ensure_transformers() -> Any:
    """Check that transformers is installed."""
    try:
        import transformers  # noqa: F401

        return transformers
    except ImportError:
        raise ImportError(
            "Transformer-based taggers require the 'transformers' package. " "Install with: pip install 'jate[neural]'"
        ) from None


class BertTagger(ATETagger):
    """Base class for transformer token classification taggers.

    Uses a HuggingFace token classification model to tag term boundaries
    (BIO scheme) in a single document. Any model compatible with
    AutoModelForTokenClassification can be used.

    Parameters
    ----------
    model:
        HuggingFace Hub model ID or local path to a fine-tuned model.
    device:
        Device for inference. -1 for CPU (default), 0+ for GPU.
    """

    def __init__(self, model: str, *, device: int = -1) -> None:
        self._model_name = model
        self._device = device
        self._pipeline: Any = None  # lazy loaded

    @property
    def name(self) -> str:
        return self.__class__.__name__

    @property
    def description(self) -> str:
        return f"Transformer token classification tagger ({self._model_name})"

    def output_capabilities(self) -> OutputCapabilities:
        return OutputCapabilities(
            produces_scores=True,
            produces_ranking=False,
            produces_offsets=True,
            produces_labels=False,
        )

    def _ensure_pipeline(self) -> Any:
        """Lazy-load the HuggingFace pipeline on first use."""
        if self._pipeline is None:
            transformers = _ensure_transformers()
            self._pipeline = transformers.pipeline(
                "token-classification",
                model=self._model_name,
                aggregation_strategy="simple",
                device=self._device,
            )
        return self._pipeline

    def tag(self, doc: Any) -> TermExtractionResult:
        """Extract terms from a document using token classification.

        Parameters
        ----------
        doc:
            A spaCy Doc, or any object with a .text attribute,
            or a plain string.
        """
        pipe = self._ensure_pipeline()

        # Get text from doc
        if isinstance(doc, str):
            text = doc
            doc_id = f"doc_{id(doc)}"
        else:
            text = doc.text
            # Try to get doc_id from spaCy custom extension
            _doc_id: str | None = None
            if hasattr(doc, "_") and hasattr(doc._, "doc_id"):
                _doc_id = doc._.doc_id
            doc_id = _doc_id if _doc_id is not None else f"doc_{id(doc)}"

        if not text.strip():
            return TermExtractionResult()

        # Run HuggingFace pipeline
        entities = pipe(text)

        # Group into Terms with spans, deduplicating by normalised form
        term_map: dict[str, Term] = {}
        for entity in entities:
            surface = text[entity["start"] : entity["end"]]
            normalised = surface.lower().strip()
            if not normalised:
                continue

            span = TermSpan(
                doc_id=doc_id,
                start=entity["start"],
                end=entity["end"],
            )
            score = entity.get("score", 0.0)

            if normalised in term_map:
                term_map[normalised].spans.append(span)
                term_map[normalised].frequency += 1
                term_map[normalised].surface_forms.add(surface)
                # Keep highest confidence score
                if score > term_map[normalised].score:
                    term_map[normalised].score = score
            else:
                term_map[normalised] = Term(
                    string=normalised,
                    score=score,
                    frequency=1,
                    spans=[span],
                    surface_forms={surface},
                )

        result = TermExtractionResult()
        for term in term_map.values():
            result.add(term)

        # Emit corpus-level warning
        warning_msg = self.corpus_level_warning()
        if warning_msg:
            import warnings

            warnings.warn(warning_msg, stacklevel=2)

        return result


class XLMRTagger(BertTagger):
    """XLM-RoBERTa tagger for automatic term extraction.

    Multilingual model supporting 100 languages. Based on Lang et al. (2021),
    "Transforming Term Extraction", Findings of ACL.

    Default model: ziqizhang/jate-ate-xlmr (trained on ACTER).
    """

    def __init__(self, model: str = "ziqizhang/jate-ate-xlmr", *, device: int = -1) -> None:
        super().__init__(model, device=device)


class RoBERTaTagger(BertTagger):
    """RoBERTa tagger for automatic term extraction.

    English-only model with faster inference than XLM-R.

    Default model: ziqizhang/jate-ate-roberta (trained on ACTER).
    """

    def __init__(self, model: str = "ziqizhang/jate-ate-roberta", *, device: int = -1) -> None:
        super().__init__(model, device=device)
