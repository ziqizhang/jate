package uk.ac.shef.dcs.jate.core.algorithm;

import uk.ac.shef.dcs.jate.core.feature.FeatureCorpusTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.FeatureTermNest;

import java.util.Set;


/**
 * CValueFeatureWrapper wraps an instance of FeatureCorpusTermFrequency, which tells a term's distribution over a corpus;
 * and an instance of FeatureTermNest, which tells term-nested-in-term relations
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */
public class CValueFeatureWrapper extends AbstractFeatureWrapper {

	private FeatureCorpusTermFrequency _termFreq;
	private FeatureTermNest _termNest;

	/**
	 * Creates an instance. CValueFeatureWrapper wraps an instance of FeatureCorpusTermFrequency and an instance of FeatureTermNest
	 * @param termFreq
	 * @param termNest
	 */
	public CValueFeatureWrapper(FeatureCorpusTermFrequency termFreq, FeatureTermNest termNest){
		_termFreq=termFreq;
		_termNest=termNest;
	}

	/**
	 * @param term
	 * @return number of occurrences of a term in a corpus. If a term cannot be found it always returns 1.
	 */
	public int getTermFreq(String term){
		int freq= _termFreq.getTermFreq(term);
		return freq==0?1:freq;
	}

	/**
	 * @param id - the id of the term in question
	 * @return number of occurrences of a term in a corpus. If a term cannot be found it always returns 1.
	 */
	public int getTermFreq(int id){
		int freq= _termFreq.getTermFreq(id);
		return freq==0?1:freq;
	}

	/**
	 * @param nested
	 * @return ids of terms in which the provided term nests in
	 */
	public int[] getNestsOf(String nested){
		Set<Integer> res = _termNest.getNestIdsOf(nested);
		if (res==null)
            return new int[0];
        int[] result = new int[res.size()];
        int c=0;
        for(int i: res){
            result[c]=i;
            c++;
        }
        return result;
	}
	
	//modified code for NC-Value begins
	
	public String getTerm(int id){
		return _termFreq.getGlobalIndex().retrieveTermCanonical(id);
	}
	
	//modified code ends

	public Set<String> getTerms(){
		return _termFreq.getGlobalIndex().getTermsCanonical();
	}

}
