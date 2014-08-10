package uk.ac.shef.dcs.jate.core.algorithm;

import uk.ac.shef.dcs.jate.core.feature.FeatureCorpusTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.FeatureDocumentTermFrequency;
import uk.ac.shef.dcs.jate.core.feature.FeatureRefCorpusTermFrequency;

import java.util.Set;

/**
 * TermExFeatureWrapper wraps an instance of FeatureDocumentTermFrequency, which tells a candidate term's distribution over a corpus,
 * each document in the corpus, and existence in documents;
 * another instance of FeatureCorpusTermFrequency which tells individual words' distributions over corpus;
 * and an instance of FeatureRefCorpusTermFrequency, which tells individual words' distributions in a reference corpus.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class TermExFeatureWrapper extends AbstractFeatureWrapper {

	private FeatureDocumentTermFrequency _termFreq;
	private FeatureRefCorpusTermFrequency _refWordFreq;
	private FeatureCorpusTermFrequency _wordFreq;

	/**
	 * Default constructor
	 * @param termfreq
	 * @param wordfreq
	 * @param ref
	 */
	public TermExFeatureWrapper(FeatureDocumentTermFrequency termfreq, FeatureCorpusTermFrequency wordfreq, FeatureRefCorpusTermFrequency ref) {
		_termFreq = termfreq;
		_wordFreq = wordfreq;
		_refWordFreq = ref;

	}

	/**
	 * @return total number of occurrences of terms in the corpus
	 */
	public int getTotalCorpusTermFreq() {
		return _termFreq.getTotalCorpusTermFreq();
	}

	/**
	 * @param term
	 * @return the number of occurrences of a candidate term in the corpus
	 */
	public int getTermFreq(String term) {
		int freq = _termFreq.getSumTermFreqInDocs(term);
		return freq == 0 ? 1 : freq;
	}

	/**
	 * @param term
	 * @param d
	 * @return the term's frequency in document with id=d
	 */
	public int getTermFreqInDoc(String term, int d) {
		return _termFreq.getTermFreqInDoc(term, d);
	}

	/**
	 * @param t
	 * @return the ids of documents in which term t is found
	 */
	public int[] getTermAppear(String t) {
		return _termFreq.getTermAppear(t);
	}

	/**
	 * @param term
	 * @return total number of occurrences of a term in the documents in which it is found
	 */
	public int getSumTermFreqInDocs(String term) {
		int[] docs = getTermAppear(term);
		int sum = 0;
		for (int i : docs) {
			sum += getTermFreqInDoc(term, i);
		}
		return sum == 0 ? 1 : sum;
	}

	/**
	 * @param t
	 * @param d
	 * @return normalised term frequency in a document with id=d. It is equal to freq of term t in d divided by
	 * total term frequency in d.
	 */
	public double getNormFreqInDoc(String t, int d) {
		return (getTermFreqInDoc(t, d) + 0.1) / (getSumTermFreqInDocs(t) + 1);
	}

	/**
	 * @param word
	 * @return the number of occurrences of a word in the corpus
	 */
	public int getWordFreq(String word) {
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
	public double getRefWordFreqNorm(String word) {
		return _refWordFreq.getNormalizedTermFreq(word);
	}

	public Set<String> getTerms() {
		return _termFreq.getGlobalIndex().getTermsCanonical();
	}
}
