"""Tests for the NMF ranker algorithm."""

from jate.algorithms.base import ATERanker, OutputCapabilities
from jate.algorithms.nmf import NMFRanker
from jate.models import TermExtractionResult


class TestNMFRankerClass:
    def test_is_ate_ranker(self):
        assert issubclass(NMFRanker, ATERanker)

    def test_output_capabilities(self):
        ranker = NMFRanker()
        caps = ranker.output_capabilities()
        assert isinstance(caps, OutputCapabilities)
        assert caps.produces_scores is True
        assert caps.produces_ranking is True
        assert caps.requires_corpus is True

    def test_default_params(self):
        ranker = NMFRanker()
        assert ranker._n_topics == 20
        assert ranker._top_n == 50

    def test_custom_params(self):
        ranker = NMFRanker(n_topics=10, top_n_per_topic=30)
        assert ranker._n_topics == 10
        assert ranker._top_n == 30

    def test_description(self):
        ranker = NMFRanker(n_topics=10, top_n_per_topic=30)
        assert "10" in ranker.description
        assert "30" in ranker.description


class TestNMFScoring:
    """Test NMF scoring with real data from acl_rdtec_mini."""

    def setup_method(self):
        from jate.api import _resolve_extractor
        from jate.datasets.acl_rdtec import AclRdtecMini
        from jate.features import TermFrequency
        from jate.nlp.spacy_backend import SpacyBackend
        from jate.store.memory_store import MemoryCorpusStore

        ds = AclRdtecMini()
        nlp = SpacyBackend("en_core_web_sm")
        store = MemoryCorpusStore()
        ext = _resolve_extractor("pos_pattern")
        self.candidates = ext.extract(ds.documents, nlp, store)
        store.index_candidates(self.candidates, compute_cooccurrences=False)
        self.term_freq = TermFrequency.build(self.candidates, len(ds.documents))

    def test_produces_results(self):
        ranker = NMFRanker(n_topics=3, top_n_per_topic=50)
        import warnings

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = ranker.score(self.candidates, self.term_freq)
        assert isinstance(result, TermExtractionResult)
        assert len(result) > 0

    def test_results_have_scores(self):
        ranker = NMFRanker(n_topics=3, top_n_per_topic=50)
        import warnings

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = ranker.score(self.candidates, self.term_freq)
        terms = list(result)
        scored = [t for t in terms if t.score > 0]
        assert len(scored) > 0

    def test_results_sorted_by_score(self):
        ranker = NMFRanker(n_topics=3, top_n_per_topic=50)
        import warnings

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = ranker.score(self.candidates, self.term_freq)
        terms = list(result)
        for i in range(len(terms) - 1):
            assert terms[i].score >= terms[i + 1].score

    def test_different_n_topics(self):
        """Different k values produce different results."""
        import warnings

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            r1 = NMFRanker(n_topics=2, top_n_per_topic=20).score(self.candidates, self.term_freq)
            r2 = NMFRanker(n_topics=5, top_n_per_topic=20).score(self.candidates, self.term_freq)
        s1 = {t.string for t in r1}
        s2 = {t.string for t in r2}
        # Not identical (different topic structures)
        assert s1 != s2 or len(r1) != len(r2)

    def test_top_n_none_scores_all(self):
        """top_n_per_topic=None scores all candidates."""
        ranker = NMFRanker(n_topics=3, top_n_per_topic=None)
        import warnings

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = ranker.score(self.candidates, self.term_freq)
        # Should have more terms than top-N filtered version
        ranker_filtered = NMFRanker(n_topics=3, top_n_per_topic=10)
        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result_filtered = ranker_filtered.score(self.candidates, self.term_freq)
        assert len(result) >= len(result_filtered)

    def test_empty_candidates(self):
        ranker = NMFRanker()
        result = ranker._score([], self.term_freq)
        assert len(result) == 0


class TestNMFIntegration:
    """Test NMF via the public API."""

    def test_extract_corpus_with_nmf(self):
        import warnings

        from jate.api import extract_corpus

        with warnings.catch_warnings():
            warnings.simplefilter("ignore")
            result = extract_corpus(
                [
                    "Machine learning and neural networks.",
                    "Deep learning models improve results.",
                    "Neural network architectures are complex.",
                ],
                algorithm="nmf",
                min_frequency=1,
            )
        assert len(result) > 0

    def test_registered_in_algorithm_names(self):
        from jate.api import _ALGORITHM_NAMES

        assert "nmf" in _ALGORITHM_NAMES
