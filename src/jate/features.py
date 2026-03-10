# src/jate/features.py
"""Shared feature builders for term extraction algorithms."""

from __future__ import annotations

import re
from collections import defaultdict
from dataclasses import dataclass, field
from typing import TYPE_CHECKING, NamedTuple

from jate.models import Candidate, Document
from jate.parallel import parallel_map, split_range

if TYPE_CHECKING:
    from jate.protocols import NLPBackend


@dataclass(slots=True)
class TermFrequency:
    """Term-level corpus frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` (feature type 0).

    Attributes
    ----------
    term2ttf : dict[str, int]
        Term → total term frequency across corpus.
    term2fid : dict[str, dict[str, int]]
        Term → {doc_id → frequency in that document}.
    corpus_total : int
        Sum of all TTF values.
    total_docs : int
        Total number of documents.
    """

    term2ttf: dict[str, int] = field(default_factory=dict)
    term2fid: dict[str, dict[str, int]] = field(default_factory=dict)
    corpus_total: int = 0
    total_docs: int = 0

    @classmethod
    def build(cls, candidates: list[Candidate], total_docs: int) -> TermFrequency:
        term2ttf: dict[str, int] = defaultdict(int)
        term2fid: dict[str, dict[str, int]] = defaultdict(dict)

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                freq = len(positions)
                term2ttf[term] += freq
                term2fid[term][doc_id] = term2fid[term].get(doc_id, 0) + freq

        corpus_total = sum(term2ttf.values())
        return cls(
            term2ttf=dict(term2ttf),
            term2fid=dict(term2fid),
            corpus_total=corpus_total,
            total_docs=total_docs,
        )

    def get_ttf(self, term: str) -> int:
        return self.term2ttf.get(term.lower(), 0)

    def get_df(self, term: str) -> int:
        return len(self.term2fid.get(term.lower(), {}))

    def get_ttf_norm(self, term: str) -> float:
        """Java: getTTFNorm = TTF / (corpusTotal + 1)."""
        ttf = self.get_ttf(term)
        return ttf / (self.corpus_total + 1)

    def get_doc_freq_map(self, term: str) -> dict[str, int]:
        return dict(self.term2fid.get(term.lower(), {}))

    def get_doc_totals(self) -> dict[str, int]:
        """Total candidate term occurrences per document.

        Mirrors Java's document-level ``ctx2TTF`` from ``FrequencyCtxDocBased``.
        """
        doc_totals: dict[str, int] = defaultdict(int)
        for doc_freqs in self.term2fid.values():
            for doc_id, freq in doc_freqs.items():
                doc_totals[doc_id] += freq
        return dict(doc_totals)


@dataclass(slots=True)
class WordFrequency:
    """Word-level (unigram) corpus frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` (feature type 1).

    Built by tokenizing every document and counting individual word occurrences.
    Unlike TermFrequency, this counts ALL tokens, not just candidate terms.
    """

    word2ttf: dict[str, int] = field(default_factory=dict)
    word2fid: dict[str, dict[str, int]] = field(default_factory=dict)
    corpus_total: int = 0

    @classmethod
    def build(
        cls,
        documents: list[Document],
        tokenize_fn: callable,
    ) -> WordFrequency:
        word2ttf: dict[str, int] = defaultdict(int)
        word2fid: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))

        for doc in documents:
            tokens = tokenize_fn(doc.content)
            for token in tokens:
                word = token.lower()
                word2ttf[word] += 1
                word2fid[word][doc.doc_id] += 1

        corpus_total = sum(word2ttf.values())
        return cls(
            word2ttf=dict(word2ttf),
            word2fid={k: dict(v) for k, v in word2fid.items()},
            corpus_total=corpus_total,
        )

    def get_ttf(self, word: str) -> int:
        return self.word2ttf.get(word.lower(), 0)

    def get_df(self, word: str) -> int:
        return len(self.word2fid.get(word.lower(), {}))

    def get_ttf_norm(self, word: str) -> float:
        """Java: getTTFNorm = TTF / (corpusTotal + 1)."""
        ttf = self.get_ttf(word)
        return ttf / (self.corpus_total + 1)


@dataclass(slots=True)
class ReferenceFrequency:
    """Reference corpus word frequency statistics.

    Mirrors Java's ``FrequencyTermBased`` loaded from a reference file.
    """

    word2ttf: dict[str, int] = field(default_factory=dict)
    corpus_total: int = 0

    @property
    def null_prob(self) -> float:
        """Minimum non-zero probability. Java: setNullWordProbInReference."""
        nonzero = [v for v in self.word2ttf.values() if v > 0]
        if nonzero and self.corpus_total > 0:
            return min(nonzero) / self.corpus_total
        return 0.1

    def get_ttf(self, word: str) -> int:
        return self.word2ttf.get(word.lower(), 0)

    def get_ttf_norm(self, word: str) -> float:
        return self.get_ttf(word) / (self.corpus_total + 1)

    @classmethod
    def from_file(cls, path: str) -> ReferenceFrequency:
        """Load reference frequencies from a file.

        File format (one per line): ``freq term``
        Terms with frequency < 2 are ignored (matches Java TTFReferenceFeatureFileBuilder).
        """
        word2ttf: dict[str, int] = {}
        with open(path) as f:
            for line in f:
                line = line.strip()
                if not line:
                    continue
                parts = line.split(None, 1)
                if len(parts) < 2:
                    continue
                freq = int(parts[0])
                if freq < 2:
                    continue
                word2ttf[parts[1].strip().lower()] = freq
        corpus_total = sum(word2ttf.values())
        return cls(word2ttf=word2ttf, corpus_total=corpus_total)

    @classmethod
    def from_word_frequency(cls, wf: WordFrequency) -> ReferenceFrequency:
        """Build self-reference from target corpus word frequencies."""
        filtered = {k: v for k, v in wf.word2ttf.items() if v > 0}
        total = sum(filtered.values()) if filtered else 1
        return cls(word2ttf=filtered, corpus_total=total)


class ContextWindow(NamedTuple):
    """Identifies a context (sentence within a document).

    Mirrors Java's ``ContextWindow``. For sentence-level contexts,
    ``doc_id`` and ``sentence_id`` are set.
    """

    doc_id: str
    sentence_id: int = -1


@dataclass(slots=True)
class ContextFrequency:
    """Context-aware frequency statistics.

    Mirrors Java's ``FrequencyCtxBased`` at sentence granularity.

    Attributes
    ----------
    ctx2ttf : dict[ContextWindow, int]
        Context -> total candidate term occurrences in that context.
    ctx2tfic : dict[ContextWindow, dict[str, int]]
        Context -> {term -> frequency in context}.
    term2ctx : dict[str, set[ContextWindow]]
        Term -> set of contexts where it appears.
    adjacent : dict[str, dict[str, int]]
        Term -> {word -> count} for adjacent words (NC-Value).
    """

    ctx2ttf: dict[ContextWindow, int] = field(default_factory=dict)
    ctx2tfic: dict[ContextWindow, dict[str, int]] = field(default_factory=dict)
    term2ctx: dict[str, set[ContextWindow]] = field(default_factory=dict)
    adjacent: dict[str, dict[str, int]] = field(default_factory=dict)

    @classmethod
    def build(
        cls,
        candidates: list[Candidate],
        documents: list[Document] | None = None,
        nlp_backend: NLPBackend | None = None,
    ) -> ContextFrequency:
        ctx2ttf: dict[ContextWindow, int] = defaultdict(int)
        ctx2tfic: dict[ContextWindow, dict[str, int]] = defaultdict(lambda: defaultdict(int))
        term2ctx: dict[str, set[ContextWindow]] = defaultdict(set)

        for cand in candidates:
            term = cand.normalized_form.lower()
            for doc_id, positions in cand.doc_positions.items():
                for _start, _end, sent_idx in positions:
                    if sent_idx < 0:
                        continue
                    ctx = ContextWindow(doc_id=doc_id, sentence_id=sent_idx)
                    ctx2ttf[ctx] += 1
                    ctx2tfic[ctx][term] += 1
                    term2ctx[term].add(ctx)

        # Adjacent words (for NC-Value) — same logic as old ContextIndex
        adjacent: dict[str, dict[str, int]] = defaultdict(lambda: defaultdict(int))
        if nlp_backend is not None and documents is not None:
            from jate.extractors.utils import compute_token_offsets

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
                        before_token = None
                        after_token = None
                        for i, (t_start, t_end) in enumerate(token_offsets):
                            if t_end <= char_start:
                                before_token = tokens[i]
                            if t_start >= char_end:
                                after_token = tokens[i]
                                break
                        if before_token:
                            adjacent[norm][before_token.lower()] += 1
                        if after_token:
                            adjacent[norm][after_token.lower()] += 1

        return cls(
            ctx2ttf=dict(ctx2ttf),
            ctx2tfic={k: dict(v) for k, v in ctx2tfic.items()},
            term2ctx={k: set(v) for k, v in term2ctx.items()},
            adjacent={k: dict(v) for k, v in adjacent.items()},
        )

    def get_ctx_ttf(self, ctx: ContextWindow) -> int:
        return self.ctx2ttf.get(ctx, 0)

    def get_tfic(self, ctx: ContextWindow) -> dict[str, int]:
        return dict(self.ctx2tfic.get(ctx, {}))

    def get_contexts(self, term: str) -> set[ContextWindow]:
        return self.term2ctx.get(term.lower(), set())

    def get_n_w(self, term: str) -> int:
        """Chi-Square's n_w: sum of TTF across all contexts containing term."""
        total = 0
        for ctx in self.get_contexts(term):
            total += self.ctx2ttf.get(ctx, 0)
        return total

    def get_adjacent_words(self, term: str) -> dict[str, int]:
        """Adjacent words for NC-Value."""
        return self.adjacent.get(term.lower(), {})

    def copy_top_fraction(
        self, term_freq: TermFrequency, fraction: float = 0.3
    ) -> ContextFrequency:
        """Java's FrequencyCtxBasedCopier: filter to top-fraction most frequent terms.

        Returns a new ContextFrequency containing only terms whose TTF
        is at or above the given percentile threshold.
        """
        all_ttfs = sorted(term_freq.term2ttf.values())
        if not all_ttfs:
            return ContextFrequency()
        threshold_idx = int(len(all_ttfs) * (1 - fraction))
        threshold = all_ttfs[min(threshold_idx, len(all_ttfs) - 1)]

        kept_terms = {t for t, f in term_freq.term2ttf.items() if f >= threshold}

        new_ctx2ttf: dict[ContextWindow, int] = {}
        new_ctx2tfic: dict[ContextWindow, dict[str, int]] = {}
        new_term2ctx: dict[str, set[ContextWindow]] = defaultdict(set)

        for ctx, tfic in self.ctx2tfic.items():
            filtered_tfic = {t: f for t, f in tfic.items() if t in kept_terms}
            if not filtered_tfic:
                continue
            new_ctx2tfic[ctx] = filtered_tfic
            new_ctx2ttf[ctx] = sum(filtered_tfic.values())
            for t in filtered_tfic:
                new_term2ctx[t].add(ctx)

        return ContextFrequency(
            ctx2ttf=new_ctx2ttf,
            ctx2tfic=new_ctx2tfic,
            term2ctx=dict(new_term2ctx),
            adjacent={},
        )


@dataclass(slots=True)
class TermComponentIndex:
    """Maps unigrams to candidate terms containing them.

    Mirrors Java's ``TermComponentIndex``. Used by RAKE and
    Containment for efficient lookup.
    """

    index: dict[str, list[tuple[str, int]]] = field(default_factory=dict)

    @classmethod
    def build(cls, candidates: list[Candidate]) -> TermComponentIndex:
        raw: dict[str, list[tuple[str, int]]] = defaultdict(list)
        for cand in candidates:
            term = cand.normalized_form.lower()
            words = term.split()
            wc = len(words)
            for w in words:
                raw[w].append((term, wc))
        # Sort each entry by descending word count
        for word in raw:
            raw[word].sort(key=lambda x: -x[1])
        return cls(index=dict(raw))

    def get_sorted(self, word: str) -> list[tuple[str, int]]:
        """Return [(term, word_count)] sorted by descending word count."""
        return list(self.index.get(word.lower(), []))


@dataclass(slots=True)
class Containment:
    """Parent/child containment relationships between terms.

    Mirrors Java's ``Containment``. Built using TermComponentIndex
    for efficient lookup and regex for precise word-boundary matching.
    """

    term2parents: dict[str, set[str]] = field(default_factory=dict)

    @classmethod
    def build(
        cls,
        candidates: list[Candidate],
        term_component_index: TermComponentIndex,
        max_workers: int = 1,
    ) -> Containment:
        term2parents: dict[str, set[str]] = {}
        all_terms = {c.normalized_form.lower() for c in candidates}

        for cand in candidates:
            term = cand.normalized_form.lower()
            words = term.split()
            term_wc = len(words)

            # Collect candidate parents via TermComponentIndex
            compare_candidates: set[str] = set()
            for w in words:
                for parent_term, parent_wc in term_component_index.get_sorted(w):
                    if parent_wc <= term_wc:
                        break  # sorted descending, no more longer terms
                    if parent_term in all_terms:
                        compare_candidates.add(parent_term)

            # Regex word-boundary check (Java: (?<!\w)term(?!\w))
            pattern = re.compile(r"(?<!\w)" + re.escape(term) + r"(?!\w)")
            parents: set[str] = set()
            for parent in compare_candidates:
                if pattern.search(parent):
                    parents.add(parent)

            term2parents[term] = parents

        return cls(term2parents=term2parents)

    def get_parents(self, term: str) -> set[str]:
        return self.term2parents.get(term.lower(), set())

    def reverse(self) -> Containment:
        """Java's ContainmentReverseBuilder: invert parent->child to child->parent."""
        reversed_map: dict[str, set[str]] = defaultdict(set)
        for child, parents in self.term2parents.items():
            for parent in parents:
                reversed_map[parent].add(child)
        return Containment(term2parents=dict(reversed_map))


@dataclass(slots=True)
class Cooccurrence:
    """Term co-occurrence based on shared contexts.

    Mirrors Java's ``Cooccurrence``. For each context where both a target
    term and a reference term appear, the co-occurrence count is incremented
    by ``min(freq_target_in_ctx, freq_ref_in_ctx)``.
    """

    matrix: dict[tuple[str, str], int] = field(default_factory=dict)

    @classmethod
    def build(
        cls,
        target_ctx: ContextFrequency,
        ref_ctx: ContextFrequency,
        term_freq: TermFrequency | None = None,
        min_ttf: int = 0,
        min_tcf: int = 0,
    ) -> Cooccurrence:
        """Build co-occurrence matrix from context frequencies.

        Parameters
        ----------
        target_ctx : ContextFrequency
            Context frequencies for target candidate terms.
        ref_ctx : ContextFrequency
            Context frequencies for reference terms.
        term_freq : TermFrequency, optional
            Required when ``min_ttf > 0`` to filter by total term frequency.
        min_ttf : int
            Minimum total term frequency for a target term to be included.
            Java: ``prefilterMinTTF``. Default 0 (no filtering).
        min_tcf : int
            Minimum number of contexts a target term must appear in.
            Java: ``prefilterMinTCF``. Default 0 (no filtering).
        """
        matrix: dict[tuple[str, str], int] = defaultdict(int)

        # Collect raw directed co-occurrence, then combine
        raw: dict[tuple[str, str], int] = defaultdict(int)
        for ctx, target_tfic in target_ctx.ctx2tfic.items():
            ref_tfic = ref_ctx.ctx2tfic.get(ctx)
            if ref_tfic is None:
                continue
            for target_term, target_freq in target_tfic.items():
                # Java: filter target terms by minTTF and minTCF
                if min_ttf > 0 or min_tcf > 0:
                    if min_ttf > 0 and term_freq is not None:
                        if term_freq.get_ttf(target_term) < min_ttf:
                            continue
                    if min_tcf > 0:
                        if len(target_ctx.get_contexts(target_term)) < min_tcf:
                            continue

                for ref_term, ref_freq in ref_tfic.items():
                    if target_term == ref_term:
                        continue
                    # Store as directed pair (target -> ref)
                    raw[(target_term, ref_term)] += min(target_freq, ref_freq)

        # Merge directed pairs into undirected (sorted key), taking max
        # to avoid double-counting when target_ctx is ref_ctx
        seen: set[tuple[str, str]] = set()
        for (a, b), count in raw.items():
            key = tuple(sorted([a, b]))
            if key not in seen:
                seen.add(key)
                matrix[key] = count

        return cls(matrix=dict(matrix))

    def get(self, term_a: str, term_b: str) -> int:
        key = tuple(sorted([term_a.lower(), term_b.lower()]))
        return self.matrix.get(key, 0)

    def get_cooccurrences_for(self, term: str) -> dict[str, int]:
        """All co-occurring terms and their counts for a given term."""
        t = term.lower()
        result: dict[str, int] = {}
        for (a, b), count in self.matrix.items():
            if a == t:
                result[b] = count
            elif b == t:
                result[a] = count
        return result


@dataclass(slots=True)
class ChiSquareFrequentTerms:
    """Expected probabilities for frequent reference terms.

    Mirrors Java's ``ChiSquareFrequentTerms``.
    ``p_g = (sum of TTF in contexts where g appears) / ttfInCorpus``
    """

    exp_prob: dict[str, float] = field(default_factory=dict)
    sum_exp_prob: float = 0.0
    max_exp_prob: float = 0.0

    @classmethod
    def build(
        cls,
        ref_ctx_freq: ContextFrequency,
        corpus_total: int,
        fraction: float = 0.3,
    ) -> ChiSquareFrequentTerms:
        if corpus_total <= 0:
            corpus_total = 1

        exp_prob: dict[str, float] = {}
        for term, contexts in ref_ctx_freq.term2ctx.items():
            g_w = sum(ref_ctx_freq.ctx2ttf.get(ctx, 0) for ctx in contexts)
            p_g = g_w / corpus_total
            exp_prob[term] = p_g

        sum_exp_prob = sum(exp_prob.values())
        max_exp_prob = max(exp_prob.values()) if exp_prob else 0.0

        return cls(
            exp_prob=exp_prob,
            sum_exp_prob=sum_exp_prob,
            max_exp_prob=max_exp_prob,
        )


def build_containment_index(
    candidates: list[Candidate],
    max_workers: int = 1,
) -> dict[str, list[str]]:
    """Build parent containment index: term -> [longer terms containing it].

    Backward-compatible wrapper around Containment.build().
    """
    if not candidates:
        return {}
    tci = TermComponentIndex.build(candidates)
    cont = Containment.build(candidates, tci, max_workers=max_workers)
    return {term: list(parents) for term, parents in cont.term2parents.items()}


def build_child_containment_index(
    candidates: list[Candidate],
    max_workers: int = 1,
) -> dict[str, list[str]]:
    """Build child containment index: term -> [shorter terms contained in it].

    Backward-compatible wrapper around Containment.build() + reverse().
    """
    if not candidates:
        return {}
    tci = TermComponentIndex.build(candidates)
    cont = Containment.build(candidates, tci, max_workers=max_workers)
    rev = cont.reverse()
    return {term: list(children) for term, children in rev.term2parents.items()}
