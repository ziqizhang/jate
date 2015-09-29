package uk.ac.shef.dcs.jate.feature;
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



public class FrequencyTermBased extends AbstractFeature {

    //term and its total freq in corpus
    private Map<String, Integer> term2TTF = new ConcurrentHashMap<>();
    //term and its freq in each document, stored as a map
    private Map<String, Map<Integer, Integer>> term2FID = new ConcurrentHashMap<>();
    private int corpusTotal = 0;
    private int totalDocs=0;

    protected FrequencyTermBased() {
    }

    public Map<String, Integer> getMapTerm2TTF(){
        return term2TTF;
    }

    public int getCorpusTotal() {
        if(corpusTotal ==0){
            for(int i: term2TTF.values())
                corpusTotal +=i;
        }
        return corpusTotal;
    }

    public int getTotalDocs(){
        return totalDocs;
    }

    protected void setTotalDocs(int totalDocs){
        this.totalDocs=totalDocs;
    }

    public int getTTF(String term){
        Integer freq = term2TTF.get(term);
        if(freq==null)
            freq=0;
        return freq;
    }
    /**
     * Get the normalised frequency of a term in the corpus, which is the number of occurrences of that term as a fraction
     * of the total number of occurrences of all terms in the corpus.
     *
     * @param term
     * @return
     */
    public double getTTFNorm(String term) {
        return (double) getTTF(term) / ((double) getCorpusTotal() + 1);
    }

    /**
     * increment the number of occurrences of term by i
     *
     * @param term
     * @param i
     */
    protected void increment(String term, int i) {
        Integer freq = term2TTF.get(term);
        if(freq==null)
            freq=0;
        freq+=i;
        term2TTF.put(term, freq);
    }



    protected void incrementTermFrequencyInDocument(String term, int luceneDocId, int freq){
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
