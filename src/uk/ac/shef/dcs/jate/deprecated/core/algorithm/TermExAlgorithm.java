package uk.ac.shef.dcs.jate.deprecated.core.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.model.Term;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * An implementation of the TermEx term recognition algorithm. See Sclano e. al 2007, <i>
 * TermExtractor: a Web application to learn the shared terminology of emergent web communities</i>
 * <p>
 * In the formula w(t,Di ) =a* DR + B* DC + Y* LC, default values of a, B, and Y are 0.33.
 * </p>
 *
 * This is the implementation of the scoring formula <b>only</b> and does not include the analysis of document structure
 * as discussed in the paper.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class TermExAlgorithm implements Algorithm {

	private final double _alpha;
	private final double _beta;
	private final double _zeta;

	public TermExAlgorithm() {
		this(0.33,0.33,0.34);
	}

	public TermExAlgorithm(double alpha, double beta, double zeta) {
		_alpha = alpha;
		_beta = beta;
		_zeta = zeta;
	}

	public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if (!(store instanceof TermExFeatureWrapper)) throw new JATEException("" +
				"Required: TermExFeatureWrapper");
		TermExFeatureWrapper gFeatureStore = (TermExFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();

        double totalCorpusTermFreq= gFeatureStore.getTotalCorpusTermFreq();
		for (String s : gFeatureStore.getTerms()) {
			double score;
			String[] elements = s.split(" ");
			double T = (double) elements.length;
			double SUMwi = 0.0;
			double SUMfwi = 0.0;

			for (int i = 0; i < T; i++) {
				String wi = elements[i];
				SUMwi += (double) gFeatureStore.getWordFreq(wi) / totalCorpusTermFreq /
						(gFeatureStore.getRefWordFreqNorm(wi) + ((double) gFeatureStore.getWordFreq(wi) / totalCorpusTermFreq));
				SUMfwi += (double) gFeatureStore.getWordFreq(wi);
			}

			//calc DC
			int[] docs = gFeatureStore.getTermAppear(s);
			double sum = 0;
			for (int i : docs) {
				double norm = gFeatureStore.getNormFreqInDoc(s, i);
				if (norm == 0) sum += 0;
				else {
					sum += norm * Math.log(norm + 0.1);
				}
			}

			double DR = SUMwi;
			double DC = sum;
			double LC = (T * Math.log(gFeatureStore.getTermFreq(s) + 1) * gFeatureStore.getTermFreq(s)) / SUMfwi;

			//System.out.println(DR+"------"+DC+"------"+LC);
			score = _alpha * DR + _beta * DC + _zeta * LC;
			result.add(new Term(s, score));
		}

		Term[] all = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}

	public String toString() {
		return "TermEx_ALGORITHM";
	}
}
