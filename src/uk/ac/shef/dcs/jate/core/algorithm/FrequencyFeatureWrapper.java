package uk.ac.shef.dcs.jate.core.algorithm;

import uk.ac.shef.dcs.jate.core.feature.FeatureCorpusTermFrequency;

import java.util.Set;

/**
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */
public class FrequencyFeatureWrapper extends AbstractFeatureWrapper{

	private FeatureCorpusTermFrequency _termFreq;

	/**
	 * Default constructor
	 * @param termFreq
	 */
	public FrequencyFeatureWrapper(FeatureCorpusTermFrequency termFreq){
		_termFreq=termFreq;
	}

	/**
	 * @return total term frequency
	 */
	public int getTotalTermFreq(){
		return _termFreq.getTotalCorpusTermFreq();
	}

	/**
	 * @param term
	 * @return frequency of a term in the corpus
	 */
	public int getTermFreq(String term){
		int freq= _termFreq.getTermFreq(term);
		return freq==0?1:freq;
	}

	public Set<String> getTerms(){
		return _termFreq.getGlobalIndex().getTermsCanonical();
	}
}
