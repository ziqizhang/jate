package uk.ac.shef.dcs.jate.v2.feature;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Created by zqz on 20/09/2015.
 */
public class FrequencyDocBased extends AbstractFeature {

    //doc and total term freq in doc
    private Map<Integer, Integer> doc2TTF = new HashMap<>();
    //doc and its contained term with their Freq In Doc
    private Map<Integer, Map<String, Integer>> doc2TFID = new HashMap<>();
    private int corpusTotal = 0;
    private int totalDocs=0;

    protected FrequencyDocBased() {
    }

    public Map<Integer, Integer> getMapDoc2TTF(){
        return doc2TTF;
    }

    protected Map<Integer, Map<String, Integer>> getMapDoc2TFID(){
        return doc2TFID;
    }

    public int getCorpusTotal() {
        if(corpusTotal ==0){
            for(int i: doc2TTF.values())
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

    public Map<String, Integer> getTFID(int docId){
        Map<String, Integer> result = doc2TFID.get(docId);
        if(result==null)
            return new HashMap<>();
        return result;
    }

    /**
     * increment the number of occurrences of term by i
     *
     */
    protected void increment(int docId, String term, int tf) {
        Map<String, Integer> tfidMap = doc2TFID.get(docId);
        if(tfidMap==null)
            tfidMap = new HashMap<>();

        Integer f = tfidMap.get(term);
        if(f==null)
            f=0;
        f+=tf;
        tfidMap.put(term, f);
        doc2TFID.put(docId, tfidMap);
    }

    protected void increment(int docId, int freq){
        Integer f = doc2TTF.get(docId);
        if(f==null)
            f=0;
        f+=freq;
        doc2TTF.put(docId, f);
    }

}
