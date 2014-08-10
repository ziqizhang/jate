package uk.ac.shef.dcs.jate.core.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.Term;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Average corpus tf = tf_in_corpus(t)/doc freq (t).
 */
public class AverageCorpusTFAlgorithm implements Algorithm{
    public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if(!(store instanceof AverageCorpusTFFeatureWrapper)) throw new JATEException("" +
				"Required: AverageCorpusTFFeatureWrapper");
		AverageCorpusTFFeatureWrapper burstnessFeatureStore = (AverageCorpusTFFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();

		for(String s: burstnessFeatureStore.getTerms()){
/*			if(tfidfFeatureStore.getTermFreqInCorpus(s)==0 || tfidfFeatureStore.getDocFreq(s)==0){
				System.out.println("ZERO: "+s+"-tf:"+tfidfFeatureStore.getTermFreqInCorpus(s)+", df:"+tfidfFeatureStore.getDocFreq(s));
			}*/
			double tf =  (double)burstnessFeatureStore.getTermFreq(s);
			double df_i =  (double)burstnessFeatureStore.getDocFreq(s) ==0?1:(double)burstnessFeatureStore.getDocFreq(s);
			result.add(new Term(s,tf/df_i));
		}

		Term[] all  = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}

	public String toString(){
		return "AverageCorpusTF_ALGORITHM";
	}
}
