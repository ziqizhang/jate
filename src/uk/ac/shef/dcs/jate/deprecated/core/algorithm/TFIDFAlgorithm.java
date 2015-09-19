package uk.ac.shef.dcs.jate.deprecated.core.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.model.Term;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * An implementation of the tf-idf algorithm applied to term recognition algorithm.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 *
 *  <br>Also credits to <b>pmclachlan@gmail.com</b> for revision for performance tweak </br>
 */

@Deprecated
public class TFIDFAlgorithm implements Algorithm {
	public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if(!(store instanceof TFIDFFeatureWrapper)) throw new JATEException("" +
				"Required: TFIDFFeatureWrapper");
		TFIDFFeatureWrapper tfidfFeatureStore = (TFIDFFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();

        double totalDocs = (double) tfidfFeatureStore.getTotalDocs();

		for(String s: tfidfFeatureStore.getTerms()){
/*			if(tfidfFeatureStore.getTermFreq(s)==0 || tfidfFeatureStore.getDocFreq(s)==0){
				System.out.println("ZERO: "+s+"-tf:"+tfidfFeatureStore.getTermFreq(s)+", df:"+tfidfFeatureStore.getDocFreq(s));
			}*/
			double tf =  (double)tfidfFeatureStore.getTermFreq(s)/((double) tfidfFeatureStore.getTotalTermFreq()+1.0);
			double df_i =  (double)tfidfFeatureStore.getDocFreq(s) ==0?1:(double)tfidfFeatureStore.getDocFreq(s);
			double idf = Math.log(totalDocs /df_i);
			result.add(new Term(s,tf*idf));
		}

		Term[] all  = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}

	public String toString(){
		return "TfIdf_ALGORITHM";
	}
}
