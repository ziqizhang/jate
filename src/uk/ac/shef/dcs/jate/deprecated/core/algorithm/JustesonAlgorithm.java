package uk.ac.shef.dcs.jate.deprecated.core.algorithm;

import uk.ac.shef.dcs.jate.deprecated.model.Term;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.JATEProperties;

import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * <p>
 * Jutseson Algorithm : Simple algorithm that extracts the terms with frequency of 2 or more and thus, ranks them by their frequency in the corpus
 * <i> Justeson, John S., and Slava M. Katz. "Technical terminology: some linguistic properties and an algorithm for identification in text." Natural language engineering 1.1 (1995): 9-27.</i> 
 * </p>
 */

@Deprecated
public class JustesonAlgorithm implements Algorithm{

	public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		if(!(store instanceof FrequencyFeatureWrapper)) throw new JATEException("" +
				"Required: FrequencyFeatureWrapper");
		FrequencyFeatureWrapper fFeatureStore = (FrequencyFeatureWrapper) store;
		Set<Term> result = new HashSet<Term>();
		boolean Ignore_SingleWords = JATEProperties.getInstance().isIgnoringSingleWords();

		for(String s: fFeatureStore.getTerms()){
			if(Ignore_SingleWords && !s.contains(" ")){
				continue;				
			}
			
			double tf =  (double)fFeatureStore.getTermFreq(s);
			if(tf >= 1.0)											//terms with frequency 2 or more are the valid terms.
				result.add(new Term(s,tf));
		}

		Term[] all  = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}

	public String toString(){
		return "Justeson&Katz_ALGORITHM";
	}
}
