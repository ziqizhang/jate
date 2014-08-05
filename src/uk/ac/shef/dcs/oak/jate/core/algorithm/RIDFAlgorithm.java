package uk.ac.shef.dcs.oak.jate.core.algorithm;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.model.Term;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * The RIDF algorithm, see
 * Church, K. and Gale, W. 1995a. Inverse Document Frequency (IDF): A Measure of Deviation from Poisson. In Proceedings of the 3rd Workshop on Very Large Corpora. Cambridge, Massachusetts, USA, pp.121-30.
 *
 */
public class RIDFAlgorithm implements Algorithm {
    public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if(!(store instanceof RIDFFeatureWrapper)) throw new JATEException("" +
				"Required: RIDFFeatureWrapper");
		RIDFFeatureWrapper ridfFeatureStore = (RIDFFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();

        double totalDocs = (double) ridfFeatureStore.getTotalDocs();

		for(String s: ridfFeatureStore.getTerms()){
/*			if(tfidfFeatureStore.getTermFreqInCorpus(s)==0 || tfidfFeatureStore.getDocFreq(s)==0){
				System.out.println("ZERO: "+s+"-tf:"+tfidfFeatureStore.getTermFreqInCorpus(s)+", df:"+tfidfFeatureStore.getDocFreq(s));
			}*/

            int tfInCorpus = ridfFeatureStore.getTermFreqInCorpus(s);
            double cf_over_N = (double) tfInCorpus / totalDocs;
            double exponential = Math.exp(0 - cf_over_N);
            double nominator = totalDocs * (1 - exponential);
            double denominator = (double) ridfFeatureStore.getDocFreq(s);

            if (denominator == 0) {
                denominator=1; //this shouldnt occur. a term that is firstly extracted from the corpus must have a source
            }
            double ridf = Math.log(nominator / denominator) / Math.log(2.0);

			result.add(new Term(s,ridf));
		}

		Term[] all  = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}

	public String toString(){
		return "RIDF_ALGORITHM";
	}
}
