package uk.ac.shef.dcs.jate.v2.feature;


import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A feature store that contains information of term distributions over a corpus. It contains following information:
 * <br>- total number of occurrences of all terms found in the corpus, which is the sum of occurrences of each term
 * <br>- number of occurrences of each term found in the corpus
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class FeatureTotalTermFrequency extends AbstractFeature {

    private Map<String, Integer> term2Frequency = new ConcurrentHashMap<>();
    private int totalWords = 0;

    protected FeatureTotalTermFrequency() {

    }

    public int getTotalWords() {
        return totalWords;
    }

    protected void setTotalWords(int i) {
        totalWords = i;
    }


    public int getFrequency(String term){
        Integer freq = term2Frequency.get(term);
        if(freq==null)
            freq=0;
        return freq;
    }
    /**
     * increment the number of occurrences of term by i
     *
     * @param term
     * @param i
     */
    protected void add(String term, int i) {
        term2Frequency.put(term, i);
    }



    /**
     * Get the normalised frequency of a term in the corpus, which is the number of occurrences of that term as a fraction
     * of the total number of occurrences of all terms in the corpus.
     *
     * @param term
     * @return
     */
    public double getNormalizedFrequency(String term) {
        return (double) getFrequency(term) / ((double) getTotalWords() + 1);
    }

}
