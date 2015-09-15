package uk.ac.shef.dcs.jate.deprecated.core.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.model.Term;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * An implementation of the GlossEx term recognition algorithm. See Kozakov, et. al 2004, <i>
 * Glossary extraction and utilization in the information search and delivery system for IBM Technical Support</i>
 *. This is the implementation of the scoring formula <b>only</b>, and does not include the filtering algorithm as mentioned
 * in the paper.
 * <p>
 * In the equation C(T) = a* TD(T) + B*TC(T), default a=0.2, B = 0.8.
 * </p>
 *
 * You might need to modify the value of B by increasing it substaintially when the reference corpus is relatively
 * much bigger than the target corpus, such as the BNC corpus. For details, please refer to the paper.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */




public class GlossExAlgorithm implements Algorithm {

	private final double _alpha;
	private final double _beta;

	public GlossExAlgorithm(){
		this(0.2,0.8);
	}

	public GlossExAlgorithm(double alpha, double beta){
		_alpha = alpha;
		_beta = beta;
	}

	public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if (!(store instanceof GlossExFeatureWrapper)) throw new JATEException("" +
				"Required: GlossExFeatureWrapper");
		GlossExFeatureWrapper gFeatureStore = (GlossExFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();

        double totalCorpusTermFreq = gFeatureStore.getTotalCorpusTermFreq();
		for (String s : gFeatureStore.getTerms()) {
			double score;
			String[] elements = s.split(" ");
			double T = (double) elements.length;
			double SUMwi = 0.0;
			double SUMfwi = 0.0;

			for (int i = 0; i < T; i++) {
				String wi = elements[i];
				SUMwi += Math.log((double) gFeatureStore.getWordFreq(wi) / totalCorpusTermFreq / gFeatureStore.getRefWordFreqNorm(wi));
				SUMfwi += (double) gFeatureStore.getWordFreq(wi);
			}

			double TD = SUMwi / T;
			double TC = (T * Math.log10(gFeatureStore.getTermFreq(s) + 1) * gFeatureStore.getTermFreq(s)) / SUMfwi;

			if (T == 1) score= 0.9 * TD + 0.1 * TC;
			else score= _alpha * TD + _beta * TC;
			result.add(new Term(s, score));
		}

		Term[] all = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}

	public String toString() {
		return "IBM_GlossEx_ALGORITHM";
	}
}
