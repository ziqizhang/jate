package uk.ac.shef.dcs.jate.deprecated.core.algorithm;

import uk.ac.shef.dcs.jate.deprecated.core.feature.FeatureCorpusTermFrequency;

import java.util.Set;

/**
 * TermExFeatureWrapper wraps an instance of FeatureCorpusTermFrequency which tells each candidate term's distributions over corpus.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class TFIDFFeatureWrapper extends AbstractFeatureWrapper {

	private FeatureCorpusTermFrequency _termFreq;

	/**
	 * Default constructor
	 * @param termFreq
	 */
	public TFIDFFeatureWrapper(FeatureCorpusTermFrequency termFreq){
		_termFreq=termFreq;
	}

	/**
	 * @return total term frequency
	 */
	public int getTotalTermFreq(){
		return _termFreq.getTotalCorpusTermFreq();
	}

	/**
	 * @return total number of documents in corpus
	 */
	public int getTotalDocs(){
		return _termFreq.getGlobalIndex().getDocumentIds().size();
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
