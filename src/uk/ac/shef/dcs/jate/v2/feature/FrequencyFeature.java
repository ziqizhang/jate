package uk.ac.shef.dcs.jate.v2.feature;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A feature store that contains information of term distributions over a corpus. It contains following information:
 * <br>- total number of occurrences of all terms found in the corpus, which is the sum of occurrences of each term
 * <br>- number of occurrences of each term found in the corpus
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class FrequencyFeature extends AbstractFeature {

    //term and its total freq in corpus
    private Map<String, Integer> term2TTF = new ConcurrentHashMap<>();
    //term and its freq in each document, stored as a map
    private Map<String, Map<Integer, Integer>> term2FID = new ConcurrentHashMap<>();
    private int totalWords = 0;

    protected FrequencyFeature() {
    }

    public Map<String, Integer> getMapTerm2TTF(){
        return term2TTF;
    }

    public int getTotalTerms() {
        if(totalWords==0){
            for(int i: term2TTF.values())
                totalWords+=i;
        }
        return totalWords;
    }

    protected void setTotalTerms(int i) {
        totalWords = i;
    }

    public int getTTF(String luceneTerm){
        Integer freq = term2TTF.get(luceneTerm);
        if(freq==null)
            freq=0;
        return freq;
    }
    /**
     * Get the normalised frequency of a term in the corpus, which is the number of occurrences of that term as a fraction
     * of the total number of occurrences of all terms in the corpus.
     *
     * @param luceneTerm
     * @return
     */
    public double getTTFNorm(String luceneTerm) {
        return (double) getTTF(luceneTerm) / ((double) getTotalTerms() + 1);
    }

    /**
     * increment the number of occurrences of term by i
     *
     * @param luceneTerm
     * @param i
     */
    protected void add(String luceneTerm, int i) {
        term2TTF.put(luceneTerm, i);
    }



    protected void addTermFrequencyInDocument(String term, int luceneDocId, int freq){
        Map<Integer, Integer> freqMap = term2FID.get(term);
        if(freqMap==null)
            freqMap = new HashMap<>();
        freqMap.put(luceneDocId, freq);
        term2FID.put(term, freqMap);
    }

    public Map<Integer, Integer> getTermFrequencyInDocument(String term){
        return term2FID.get(term);
    }

}
