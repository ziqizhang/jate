package uk.ac.shef.dcs.jate.core.feature;


import java.util.HashMap;
import java.util.Map;

/**
 * A feature store that contains information of term distributions over a reference corpus. It contains following
 * information:
 * <br>- total number of occurrences of all terms found in the reference corpus, which is the sum of occurrences of each term
 * <br>- number of occurrences of each term found in the reference corpus
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class FeatureRefCorpusTermFrequency extends AbstractFeature {

	private Map<String, Integer> _refTermFreqMap = new HashMap<String, Integer>();
	private int _totalCorpusTermFreq=0;

	protected FeatureRefCorpusTermFrequency(){
		_index=null;
	}


	public int getTotalRefCorpusTermFreq(){
		return _totalCorpusTermFreq;
	}
	public void setTotalRefCorpusTermFreq(int i){
		_totalCorpusTermFreq=i;
	}

		/**
	 * increment the number of occurrences of term t by i
	 * @param t
	 * @param i
	 */
	public void addToTermFreq(String t, int i){
		Integer freq = _refTermFreqMap.get(t);
		if(freq==null) _refTermFreqMap.put(t,i);
		else _refTermFreqMap.put(t,freq+i);
		_totalCorpusTermFreq+=i;
	}

	/**
	 * Get the number of occurrences of a term in the corpus
	 * @param t
	 * @return
	 */
	public int getTermFreq(String t){
		Integer freq = _refTermFreqMap.get(t);
		return freq==null?0:freq;
	}

	/**
	 * Get the normalised frequency of a term in the corpus, which is the number of occurrences of that term as a fraction
	 * of the total number of occurrences of all terms in the corpus.
	 * @param w the id of the term
	 * @return
	 */
	public double getNormalizedTermFreq(String w){
		return (double) getTermFreq(w) / ((double) getTotalRefCorpusTermFreq()+1);
	}

}
