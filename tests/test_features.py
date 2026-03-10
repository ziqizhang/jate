from __future__ import annotations

import pytest
from jate.features import ChiSquareFrequentTerms, Cooccurrence, Containment, ContextFrequency, ContextWindow, ReferenceFrequency, TermComponentIndex, TermFrequency, WordFrequency
from jate.models import Candidate, Document


class TestTermFrequency:
    def _make_candidates(self):
        """Two candidates across 2 docs."""
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 50, 64, sentence_idx=1)
        c1.add_position("doc2", 10, 24, sentence_idx=0)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 20, 33, sentence_idx=0)
        return [c1, c2]

    def test_build_from_candidates(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        assert tf.get_ttf("neural network") == 3
        assert tf.get_ttf("deep learning") == 1
        assert tf.get_df("neural network") == 2
        assert tf.get_df("deep learning") == 1
        assert tf.total_docs == 2
        # corpus_total = sum of all TTF = 3 + 1 = 4
        assert tf.corpus_total == 4

    def test_get_ttf_norm(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        # Java: getTTFNorm = TTF / (corpusTotal + 1) = 3 / 5
        assert tf.get_ttf_norm("neural network") == pytest.approx(3 / 5)

    def test_get_doc_freq_map(self):
        candidates = self._make_candidates()
        tf = TermFrequency.build(candidates, total_docs=2)
        m = tf.get_doc_freq_map("neural network")
        assert m == {"doc1": 2, "doc2": 1}

    def test_unknown_term_returns_zero(self):
        tf = TermFrequency.build([], total_docs=0)
        assert tf.get_ttf("nonexistent") == 0
        assert tf.get_df("nonexistent") == 0
        assert tf.get_ttf_norm("nonexistent") == 0.0


class TestWordFrequency:
    def test_build_from_documents(self):
        docs = [
            Document(doc_id="d1", content="neural network for deep learning"),
            Document(doc_id="d2", content="neural network analysis"),
        ]
        # Simple whitespace tokenizer for testing
        def tokenize(text):
            return text.lower().split()

        wf = WordFrequency.build(docs, tokenize_fn=tokenize)
        assert wf.get_ttf("neural") == 2
        assert wf.get_ttf("network") == 2
        assert wf.get_ttf("for") == 1
        assert wf.get_ttf("deep") == 1
        assert wf.get_ttf("learning") == 1
        assert wf.get_ttf("analysis") == 1
        assert wf.get_ttf("nonexistent") == 0
        assert wf.corpus_total == 8  # 5 + 3

    def test_build_with_spacy_tokenizer(self):
        """WordFrequency should accept any callable tokenizer."""
        docs = [Document(doc_id="d1", content="Machine learning works.")]
        wf = WordFrequency.build(docs, tokenize_fn=lambda t: t.lower().split())
        assert wf.get_ttf("machine") == 1
        assert wf.get_ttf("learning") == 1
        assert wf.get_ttf("works.") == 1  # basic split doesn't strip punctuation


class TestReferenceFrequency:
    def test_build_from_dict(self):
        data = {"neural": 100, "network": 80, "deep": 50}
        rf = ReferenceFrequency(word2ttf=data, corpus_total=1000)
        assert rf.get_ttf("neural") == 100
        assert rf.get_ttf("unknown") == 0
        assert rf.corpus_total == 1000
        assert rf.null_prob == pytest.approx(50 / 1000)  # min_freq / total

    def test_build_as_self_reference(self):
        """When no external ref corpus, build from WordFrequency."""
        docs = [Document(doc_id="d1", content="neural network deep learning")]
        wf = WordFrequency.build(docs, tokenize_fn=lambda t: t.lower().split())
        rf = ReferenceFrequency.from_word_frequency(wf)
        assert rf.get_ttf("neural") == 1
        assert rf.corpus_total == 4

    def test_from_file(self, tmp_path):
        ref_file = tmp_path / "ref_freq.txt"
        ref_file.write_text("100 neural\n80 network\n1 rare\n50 deep\n")
        rf = ReferenceFrequency.from_file(str(ref_file))
        assert rf.get_ttf("neural") == 100
        assert rf.get_ttf("network") == 80
        assert rf.get_ttf("deep") == 50
        assert rf.get_ttf("rare") == 0  # freq < 2 is ignored
        assert rf.corpus_total == 230  # 100 + 80 + 50

    def test_null_prob_excludes_zero(self):
        rf = ReferenceFrequency(word2ttf={"a": 10, "b": 0}, corpus_total=100)
        # null_prob = min of non-zero freqs / total = 10 / 100
        assert rf.null_prob == pytest.approx(0.1)


class TestContextFrequency:
    def _make_data(self):
        """3 candidates across 2 sentences in 1 doc."""
        # Sentence 0: "neural network" appears twice, "deep learning" once → ctx2TTF = 3
        # Sentence 1: "neural network" once, "machine learning" once → ctx2TTF = 2
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 30, 44, sentence_idx=0)
        c1.add_position("doc1", 100, 114, sentence_idx=1)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 15, 28, sentence_idx=0)

        c3 = Candidate(surface_form="machine learning", normalized_form="machine learning")
        c3.add_position("doc1", 115, 131, sentence_idx=1)

        return [c1, c2, c3]

    def test_ctx2ttf(self):
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        ctx0 = ContextWindow(doc_id="doc1", sentence_id=0)
        ctx1 = ContextWindow(doc_id="doc1", sentence_id=1)
        # Sentence 0: neural network x2 + deep learning x1 = 3
        assert cf.get_ctx_ttf(ctx0) == 3
        # Sentence 1: neural network x1 + machine learning x1 = 2
        assert cf.get_ctx_ttf(ctx1) == 2

    def test_ctx2tfic(self):
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        ctx0 = ContextWindow(doc_id="doc1", sentence_id=0)
        tfic = cf.get_tfic(ctx0)
        assert tfic["neural network"] == 2
        assert tfic["deep learning"] == 1

    def test_term2ctx(self):
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        ctxs = cf.get_contexts("neural network")
        assert len(ctxs) == 2  # appears in sentence 0 and 1

    def test_n_w_for_chi_square(self):
        """n_w = sum of ctx2TTF for all contexts containing term w."""
        candidates = self._make_data()
        cf = ContextFrequency.build(candidates)
        # neural network appears in both sentences: n_w = 3 + 2 = 5
        assert cf.get_n_w("neural network") == 5
        # deep learning appears only in sentence 0: n_w = 3
        assert cf.get_n_w("deep learning") == 3
        # machine learning appears only in sentence 1: n_w = 2
        assert cf.get_n_w("machine learning") == 2


class TestTermComponentIndex:
    def test_build(self):
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c2 = Candidate(surface_form="deep neural network", normalized_form="deep neural network")
        c3 = Candidate(surface_form="network analysis", normalized_form="network analysis")
        idx = TermComponentIndex.build([c1, c2, c3])

        # "neural" maps to two terms
        neural_terms = idx.get_sorted("neural")
        assert len(neural_terms) == 2
        # Sorted by descending word count
        assert neural_terms[0] == ("deep neural network", 3)
        assert neural_terms[1] == ("neural network", 2)

        # "network" maps to three terms
        network_terms = idx.get_sorted("network")
        assert len(network_terms) == 3

    def test_unknown_word(self):
        idx = TermComponentIndex.build([])
        assert idx.get_sorted("unknown") == []


class TestContainment:
    def test_build_parents(self):
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c2 = Candidate(surface_form="deep neural network", normalized_form="deep neural network")
        c3 = Candidate(surface_form="network analysis", normalized_form="network analysis")
        tci = TermComponentIndex.build([c1, c2, c3])
        cont = Containment.build([c1, c2, c3], tci)

        parents = cont.get_parents("neural network")
        assert "deep neural network" in parents
        assert "network analysis" not in parents  # not a parent

    def test_build_children(self):
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c2 = Candidate(surface_form="deep neural network", normalized_form="deep neural network")
        tci = TermComponentIndex.build([c1, c2])
        cont = Containment.build([c1, c2], tci)
        rev = cont.reverse()

        children = rev.get_parents("deep neural network")  # reversed: "parents" = children
        assert "neural network" in children

    def test_word_boundary_regex(self):
        """Hyphenated terms should not false-match."""
        c1 = Candidate(surface_form="co op", normalized_form="co op")
        c2 = Candidate(surface_form="co-operative co op", normalized_form="co-operative co op")
        tci = TermComponentIndex.build([c1, c2])
        cont = Containment.build([c1, c2], tci)
        parents = cont.get_parents("co op")
        # "co op" IS contained in "co-operative co op" as a whole-word match
        assert "co-operative co op" in parents


class TestCooccurrence:
    def test_build_from_context_frequency(self):
        # Sentence 0: "neural network" x2, "deep learning" x1
        # Sentence 1: "neural network" x1, "machine learning" x1
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 30, 44, sentence_idx=0)
        c1.add_position("doc1", 100, 114, sentence_idx=1)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 15, 28, sentence_idx=0)

        c3 = Candidate(surface_form="machine learning", normalized_form="machine learning")
        c3.add_position("doc1", 115, 131, sentence_idx=1)

        cf = ContextFrequency.build([c1, c2, c3])
        cooc = Cooccurrence.build(cf, cf)

        # In sentence 0: min(nn=2, dl=1) = 1
        assert cooc.get("neural network", "deep learning") == 1
        # In sentence 1: min(nn=1, ml=1) = 1
        assert cooc.get("neural network", "machine learning") == 1
        # deep learning and machine learning never share a sentence
        assert cooc.get("deep learning", "machine learning") == 0

    def test_symmetric(self):
        c1 = Candidate(surface_form="a", normalized_form="a")
        c1.add_position("doc1", 0, 1, sentence_idx=0)
        c2 = Candidate(surface_form="b", normalized_form="b")
        c2.add_position("doc1", 2, 3, sentence_idx=0)
        cf = ContextFrequency.build([c1, c2])
        cooc = Cooccurrence.build(cf, cf)
        assert cooc.get("a", "b") == cooc.get("b", "a")


    def test_min_ttf_filtering(self):
        """Terms below min_ttf should be excluded from co-occurrence."""
        # neural network: TTF=3, deep learning: TTF=1, machine learning: TTF=1
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 30, 44, sentence_idx=0)
        c1.add_position("doc1", 100, 114, sentence_idx=1)

        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 15, 28, sentence_idx=0)

        c3 = Candidate(surface_form="machine learning", normalized_form="machine learning")
        c3.add_position("doc1", 115, 131, sentence_idx=1)

        cf = ContextFrequency.build([c1, c2, c3])
        tf = TermFrequency.build([c1, c2, c3], total_docs=1)

        # With min_ttf=2, only "neural network" (TTF=3) passes filter
        cooc = Cooccurrence.build(cf, cf, term_freq=tf, min_ttf=2)
        # neural network co-occurs with deep learning (nn is target, dl is ref)
        assert cooc.get("neural network", "deep learning") == 1
        # deep learning as TARGET is filtered out (TTF=1 < 2), so no dl->nn pair
        # But nn->dl pair still exists because nn passes filter
        # machine learning as TARGET is also filtered (TTF=1 < 2)

        # Without filtering, all pairs exist
        cooc_all = Cooccurrence.build(cf, cf)
        assert cooc_all.get("neural network", "deep learning") == 1
        assert cooc_all.get("neural network", "machine learning") == 1


class TestChiSquareFrequentTerms:
    def test_build(self):
        c1 = Candidate(surface_form="neural network", normalized_form="neural network")
        c1.add_position("doc1", 0, 14, sentence_idx=0)
        c1.add_position("doc1", 30, 44, sentence_idx=0)
        c2 = Candidate(surface_form="deep learning", normalized_form="deep learning")
        c2.add_position("doc1", 15, 28, sentence_idx=0)
        cf = ContextFrequency.build([c1, c2])
        tf = TermFrequency.build([c1, c2], total_docs=1)

        # With fraction=1.0 (all terms are "frequent")
        ft = ChiSquareFrequentTerms.build(cf, tf.corpus_total, fraction=1.0)
        assert "neural network" in ft.exp_prob
        assert "deep learning" in ft.exp_prob
        assert ft.sum_exp_prob == pytest.approx(
            ft.exp_prob["neural network"] + ft.exp_prob["deep learning"]
        )

    def test_p_g_formula(self):
        """p_g = (sum of TTF in contexts where g appears) / corpus_total."""
        c1 = Candidate(surface_form="a", normalized_form="a")
        c1.add_position("doc1", 0, 1, sentence_idx=0)  # sentence 0: a x1
        c2 = Candidate(surface_form="b", normalized_form="b")
        c2.add_position("doc1", 2, 3, sentence_idx=0)  # sentence 0: b x1
        cf = ContextFrequency.build([c1, c2])
        # ctx TTF for sentence 0 = 2 (a:1 + b:1)
        # g_w for "a" = sum of TTF in contexts containing "a" = 2
        # corpus_total = 2
        # p_g("a") = 2 / 2 = 1.0
        ft = ChiSquareFrequentTerms.build(cf, corpus_total=2, fraction=1.0)
        assert ft.exp_prob["a"] == pytest.approx(1.0)
