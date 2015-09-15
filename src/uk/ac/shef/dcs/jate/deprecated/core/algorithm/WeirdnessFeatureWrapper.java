package uk.ac.shef.dcs.jate.deprecated.core.algorithm;

import uk.ac.shef.dcs.jate.deprecated.core.feature.FeatureRefCorpusTermFrequency;
import uk.ac.shef.dcs.jate.deprecated.core.feature.FeatureCorpusTermFrequency;

import java.util.Set;

/**
 * WeirdnessFeatureWrapper wraps an instance of FeatureCorpusTermFrequency, which tells a candidate term's distribution over a corpus;
 * another instance of FeatureCorpusTermFrequency which tells individual words' distributions over corpus;
 * and an instance of FeatureRefCorpusTermFrequency, which tells individual words' distributions in a reference corpus.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class WeirdnessFeatureWrapper extends AbstractFeatureWrapper {
	private FeatureCorpusTermFrequency _wordFreq;
	private FeatureCorpusTermFrequency _termFreq;
	private FeatureRefCorpusTermFrequency _refWordFreq;

	public WeirdnessFeatureWrapper(FeatureCorpusTermFrequency wordFreq,
	                               FeatureCorpusTermFrequency termFreq,
	                               FeatureRefCorpusTermFrequency refWordFreq){
		_wordFreq=wordFreq;
		_termFreq=termFreq;
		_refWordFreq=refWordFreq;
	}

	/**
	 * @return total number of words in the corpus
	 */
	public int getTotalCorpusWordFreq(){
		return _wordFreq.getTotalCorpusTermFreq();
	}

	/**
	 * @param word
	 * @return the number of occurrences of a word in the corpus
	 */
	public int getWordFreq(String word){
		Integer freq = _wordFreq.getTermFreq(word);
        if(freq==0){ //the query word could have not been normalized. This is because, for GlossEx algorithm,
                        //an NP is split into words on-the-fly, then words are queried for their frequency
                        //however, there may be the case where only canonical forms of words have been processed
                        //and stored in a GlobalIndex
            //find corresponding canonical form
            //System.out.println("Searching for alternative canonical form for "+word);
            int termid=_wordFreq.getGlobalIndex().retrieveCanonicalOfTermVariant(word);
            //System.out.println("term id "+termid);
            freq=_wordFreq.getTermFreq(termid);
        }

		return freq==0?1:freq;
	}

	/**
	 * @param word
	 * @return the normalised frequency of a word in the reference corpus. It is equal to freq of word w divided by
	 * total frequencies
	 */
	public double getRefWordFreqNorm(String word){
		Double value = _refWordFreq.getNormalizedTermFreq(word);
		return value==0?1.0:value;
	}


	public Set<String> getTerms(){
		return _termFreq.getGlobalIndex().getTermsCanonical();
	}
}
