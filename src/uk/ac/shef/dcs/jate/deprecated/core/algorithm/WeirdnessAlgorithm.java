package uk.ac.shef.dcs.jate.deprecated.core.algorithm;

import uk.ac.shef.dcs.jate.deprecated.JATEException;
import uk.ac.shef.dcs.jate.deprecated.model.Term;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of the word weirdness algorithm applied to term recognition algorithm. See
 * Ahmad et al 1999, <i>Surrey Participation in TREC8: Weirdness Indexing for Logical Document Extrapolation
 * and Retrieval</i>
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class WeirdnessAlgorithm implements Algorithm {
	public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if (!(store instanceof WeirdnessFeatureWrapper)) throw new JATEException("" +
				"Required: WeirdnessFeatureWrapper");
		WeirdnessFeatureWrapper gFeatureStore = (WeirdnessFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();

        double totalCorpusWordFreq = gFeatureStore.getTotalCorpusWordFreq();
		for (String s : gFeatureStore.getTerms()) {
			String[] elements = s.split(" ");
			double T = (double) elements.length;
			double SUMwi = 0.0;

			for (int i = 0; i < T; i++) {
				String wi = elements[i];
				double v=(double) gFeatureStore.getWordFreq(wi) / totalCorpusWordFreq / gFeatureStore.getRefWordFreqNorm(wi);
				//SUMwi += Math.log((double) gFeatureStore.getWordFreq(wi) / (double) gFeatureStore.getTotalCorpusWordFreq() / gFeatureStore.getRefWordFreqNorm(wi));
				SUMwi+=Math.log(v);
			}

			double TD = SUMwi / T;
			result.add(new Term(s, TD));
		}

		Term[] all = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}

	public String toString() {
		return "Weirdness_ALGORITHM";
	}
}
