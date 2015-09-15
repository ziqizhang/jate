package uk.ac.shef.dcs.jate.deprecated.core.feature;


import uk.ac.shef.dcs.jate.deprecated.core.feature.indexer.GlobalIndex;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A feature store that contains information of term distributions over a corpus. It contains following information:
 * <br>- total number of occurrences of all terms found in the corpus, which is the sum of occurrences of each term
 * <br>- number of occurrences of each term found in the corpus
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class FeatureCorpusTermFrequency extends AbstractFeature {

    private Map<Integer, Integer> _termFreqMap = new ConcurrentHashMap<Integer, Integer>();
    private int _totalCorpusTermFreq = 0;

    protected FeatureCorpusTermFrequency(GlobalIndex index) {
        _index = index;
    }

    public int getTotalCorpusTermFreq() {
        return _totalCorpusTermFreq;
    }

    public void setTotalCorpusTermFreq(int i) {
        _totalCorpusTermFreq = i;
    }

    /**
     * increment the number of occurrences of term by i
     *
     * @param term
     * @param i
     */
    public void addToTermFreq(String term, int i) {
        int termId = _index.retrieveTermCanonical(term);
        if (termId == -1) {
            //System.err.println("Term (" + term + ") has not been indexed!");
        } else {
            addToTermFreq(termId, i);
        }
    }

    /**
     * increment the number of occurrences of term with id t by i
     *
     * @param t
     * @param i
     */
    public void addToTermFreq(int t, int i) {
        Integer freq = _termFreqMap.get(t);
        if (freq == null) freq = 0;
        _termFreqMap.put(t, freq + i);
    }

    /**
     * Set the number of occurrences of a term
     *
     * @param term
     * @param freq
     */
    public void setTermFreq(String term, int freq) {
        int termId = _index.retrieveTermCanonical(term);
        if (termId == -1) {
            //System.err.println("Term (" + term + ") has not been indexed!");
        } else {
            setTermFreq(termId, freq);
        }
    }

    /**
     * Set the number of occurrences of a term with id t
     *
     * @param t
     * @param freq
     */
    public void setTermFreq(int t, int freq) {
        _termFreqMap.put(t, freq);
    }

    /**
     * Get the number of occurrences of a term in the corpus
     *
     * @param term
     * @return
     */
    public int getTermFreq(String term) {
        int termId = _index.retrieveTermCanonical(term);
        if (termId == -1) {
            //System.err.println("Term (" + term + ") has not been indexed!");
            return 0;
        } else {
            return getTermFreq(termId);
        }
    }

    /**
     * Get the number of occurrences of a term in the corpus
     *
     * @param t the id of the term
     * @return
     */
    public int getTermFreq(int t) {
        Integer freq = _termFreqMap.get(t);
        if (freq == null) {
            freq = 0;
        }
        return freq;
    }

    /**
     * Get the normalised frequency of a term in the corpus, which is the number of occurrences of that term as a fraction
     * of the total number of occurrences of all terms in the corpus.
     *
     * @param term
     * @return
     */
    public double getNormalizedTermFreq(String term) {
        int termId = _index.retrieveTermCanonical(term);
        if (termId == -1) {
            //System.err.println("Term (" + term + ") has not been indexed!");
            return 0.0;
        } else {
            return getNormalizedTermFreq(termId);
        }
    }

    /**
     * Get the normalised frequency of a term in the corpus, which is the number of occurrences of that term as a fraction
     * of the total number of occurrences of all terms in the corpus.
     *
     * @param t the id of the term
     * @return
     */
    public double getNormalizedTermFreq(int t) {
        return (double) getTermFreq(t) / ((double) getTotalCorpusTermFreq() + 1);
    }
}
