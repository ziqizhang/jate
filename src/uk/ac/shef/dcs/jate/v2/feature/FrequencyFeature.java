package uk.ac.shef.dcs.jate.v2.feature;


import org.apache.lucene.util.BytesRef;

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
    private Map<BytesRef, Integer> term2TTF = new ConcurrentHashMap<>();
    //term and its freq in each document, stored as a map
    private Map<BytesRef, Map<Integer, Integer>> term2FID = new ConcurrentHashMap<>();
    private int totalWords = 0;

    protected FrequencyFeature() {
    }

    public int getTotalWords() {
        if(totalWords==0){
            for(int i: term2TTF.values())
                totalWords+=i;
        }
        return totalWords;
    }

    protected void setTotalWords(int i) {
        totalWords = i;
    }

    public int getTTF(BytesRef luceneTerm){
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
    public double getTTFNorm(BytesRef luceneTerm) {
        return (double) getTTF(luceneTerm) / ((double) getTotalWords() + 1);
    }

    /**
     * increment the number of occurrences of term by i
     *
     * @param luceneTerm
     * @param i
     */
    protected void add(BytesRef luceneTerm, int i) {
        term2TTF.put(luceneTerm, i);
    }



    protected void addTermFrequencyInDocument(BytesRef luceneTerm, int luceneDocId, int freq){
        Map<Integer, Integer> freqMap = term2FID.get(luceneTerm);
        if(freqMap==null)
            freqMap = new HashMap<>();
        freqMap.put(luceneDocId, freq);
        term2FID.put(luceneTerm, freqMap);
    }

}
