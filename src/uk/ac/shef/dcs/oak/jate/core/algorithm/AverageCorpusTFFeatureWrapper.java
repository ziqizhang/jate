package uk.ac.shef.dcs.oak.jate.core.algorithm;

import uk.ac.shef.dcs.oak.jate.core.feature.FeatureCorpusTermFrequency;

import java.util.Set;

/**
 * Feature wrapper for AverageCorpusTFAlgorithm
 */
public class AverageCorpusTFFeatureWrapper extends AbstractFeatureWrapper{
    private FeatureCorpusTermFrequency _termFreq;

	/**
	 * Default constructor
	 * @param termFreq
	 */
	public AverageCorpusTFFeatureWrapper(FeatureCorpusTermFrequency termFreq){
		_termFreq=termFreq;
	}


	/**
	 * @param term
	 * @return frequency of a term in the corpus
	 */
	public int getTermFreq(String term){
		int freq= _termFreq.getTermFreq(term);
		return freq==0?1:freq;
	}

	/**
	 * @param term
	 * @return document frequency, which is the number of documents in which the term is found
	 */
	public int getDocFreq(String term){
		return _termFreq.getGlobalIndex().sizeTermInDocs(term);
	}

	public Set<String> getTerms(){
		return _termFreq.getGlobalIndex().getTermsCanonical();
	}
}
