"""Sentence-level context statistics for Chi-Square and NC-Value algorithms."""

from __future__ import annotations

from collections import defaultdict
from itertools import combinations

from jate.extractors.utils import compute_token_offsets
from jate.models import Candidate, Document
from jate.protocols import NLPBackend


class ContextIndex:
    """Sentence-level context statistics for term extraction algorithms."""

    def __init__(
        self,
        sent_cooc: dict[tuple[str, str], int],
        context_totals: dict[str, int],
        adjacent: dict[str, dict[str, int]],
    ) -> None:
        self._sent_cooc = sent_cooc
        self._context_totals = context_totals
        self._adjacent = adjacent

    @classmethod
    def build(
        cls,
        candidates: list[Candidate],
        documents: list[Document],
        nlp_backend: NLPBackend | None = None,
    ) -> ContextIndex:
        """Build a ContextIndex from candidates and documents.

        Parameters
        ----------
        candidates:
            List of candidate terms with position information.
        documents:
            List of documents (needed for adjacent word extraction).
        nlp_backend:
            Optional NLP backend for tokenization (needed for adjacent words).
        """
        # Step 1: Group terms by (doc_id, sentence_idx)
        # sentence_groups[(doc_id, sent_idx)] = set of normalized term strings
        sentence_groups: dict[tuple[str, int], set[str]] = defaultdict(set)

        for cand in candidates:
            norm = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                for _start, _end, sent_idx in positions:
                    if sent_idx >= 0:
                        sentence_groups[(doc_id, sent_idx)].add(norm)

        # Step 2: Sentence co-occurrence (pairwise within each sentence group)
        sent_cooc: dict[tuple[str, str], int] = defaultdict(int)
        for terms_in_sent in sentence_groups.values():
            for a, b in combinations(sorted(terms_in_sent), 2):
                key = tuple(sorted([a, b]))
                sent_cooc[key] += 1

        # Step 3: Context totals
        # For each term, sum the count of distinct terms in all sentences where it appears
        context_totals: dict[str, int] = defaultdict(int)
        for terms_in_sent in sentence_groups.values():
            n = len(terms_in_sent)
            for term in terms_in_sent:
                context_totals[term] += n

        # Step 4: Adjacent words (only if nlp_backend provided)
        adjacent: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))

        if nlp_backend is not None:
            doc_map = {doc.doc_id: doc for doc in documents}

            for cand in candidates:
                norm = cand.normalized_form.lower()
                for doc_id, positions in cand.doc_positions.items():
                    doc = doc_map.get(doc_id)
                    if doc is None:
                        continue
                    text = doc.content
                    tokens = nlp_backend.tokenize(text)
                    token_offsets = compute_token_offsets(text, tokens)

                    for char_start, char_end, _sent_idx in positions:
                        # Find the token immediately before char_start
                        before_token: str | None = None
                        after_token: str | None = None

                        for i, (t_start, t_end) in enumerate(token_offsets):
                            # Token that ends at or before the candidate start
                            if t_end <= char_start:
                                before_token = tokens[i]
                            # Token that starts at or after the candidate end
                            if t_start >= char_end:
                                after_token = tokens[i]
                                break

                        if before_token:
                            adjacent[norm][before_token.lower()] += 1
                        if after_token:
                            adjacent[norm][after_token.lower()] += 1

        # Convert defaultdicts to regular dicts
        return cls(
            sent_cooc=dict(sent_cooc),
            context_totals=dict(context_totals),
            adjacent={k: dict(v) for k, v in adjacent.items()},
        )

    def get_sentence_cooccurrences(self, term_a: str, term_b: str) -> int:
        """Number of sentences in which both terms appear."""
        key = tuple(sorted([term_a.lower(), term_b.lower()]))
        return self._sent_cooc.get(key, 0)

    def get_context_term_total(self, term: str) -> int:
        """Total number of distinct terms across sentences where term appears.

        Chi-Square's n_w parameter.
        """
        return self._context_totals.get(term.lower(), 0)

    def get_adjacent_words(self, term: str) -> dict[str, int]:
        """Words immediately before/after each occurrence of term.

        Returns {word: count} for NC-Value.
        """
        return self._adjacent.get(term.lower(), {})
