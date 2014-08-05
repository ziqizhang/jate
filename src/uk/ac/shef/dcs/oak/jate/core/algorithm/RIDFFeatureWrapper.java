package uk.ac.shef.dcs.oak.jate.core.algorithm;

import uk.ac.shef.dcs.oak.jate.core.feature.FeatureCorpusTermFrequency;

import java.util.Set;

/**
 * feature wrapper for the RIDF algorithm
 */
public class RIDFFeatureWrapper extends AbstractFeatureWrapper {
    private FeatureCorpusTermFrequency _termFreq;

	/**
	 * Default constructor
	 * @param termFreq
	 */
	public RIDFFeatureWrapper(FeatureCorpusTermFrequency termFreq){
		_termFreq=termFreq;
	}

	/**
	 * @param term
	 * @return frequency of a term in the corpus
	 */
	public int getTermFreqInCorpus(String term){
		int freq= _termFreq.getTermFreq(term);
		return freq==0?1:freq;
	}

    /**
	 * @return total number of documents in corpus
	 */
	public int getTotalDocs(){
		return _termFreq.getGlobalIndex().getDocumentIds().size();
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
