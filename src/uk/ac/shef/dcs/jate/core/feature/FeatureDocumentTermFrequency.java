package uk.ac.shef.dcs.jate.core.feature;

import uk.ac.shef.dcs.jate.model.Document;
import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndex;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * A feature store that contains information of term distributions in each document. It contains following information:
 * <br>- total number of occurrences of all terms found in the corpus, which is the sum of occurrences of each term
 * <br>- number of occurrences of each term in each document
 * <br>- existence of terms in documents
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class FeatureDocumentTermFrequency extends AbstractFeature {

    private Map<Integer, Map<Integer, Integer>> _termInDocFreqMap = new ConcurrentHashMap<Integer, Map<Integer, Integer>>();

    private int _totalTermFreq = 0;

    protected FeatureDocumentTermFrequency(GlobalIndex index) {
        _index = index;
    }

    /**
     * Set total number of occurrences of all terms in the corpus
     *
     * @param i
     */
    public void setTotalCorpusTermFreq(int i) {
        _totalTermFreq = i;
    }

    public int getTotalCorpusTermFreq() {
        return _totalTermFreq;
    }

    /**
     * Increment term t's number of occurrences in d by freq
     *
     * @param t
     * @param d
     * @param freq
     */
    public void addToTermFreqInDoc(String t, Document d, int freq) {
        int termId = _index.retrieveTermCanonical(t);
        int docId = _index.retrieveDocument(d);
        if (termId == -1) {
            //System.err.println("Term (" + t + ") has not been indexed! Ignored.");
        } else if(docId==-1){
            //System.err.println("Document (" + d.getUrl() + ") has not been indexed! Ignored.");
        }
        else {
            addToTermFreqInDoc(termId, docId, freq);
        }
    }

    /**
     * Increment term t (id) number of occurrences in d by freq
     *
     * @param t
     * @param d
     * @param freq
     */
    public void addToTermFreqInDoc(int t, int d, int freq) {
        Map<Integer, Integer> freqs = _termInDocFreqMap.get(t);
        if (freqs == null) freqs = new HashMap<Integer, Integer>();
        freqs.put(d, freq);
        _termInDocFreqMap.put(t, freqs);
    }

    /**
     * @param term
     * @param d
     * @return number of occurrences of a term t in a document d
     */
    public int getTermFreqInDoc(String term, Document d) {
        int termId = _index.retrieveTermCanonical(term);
        int docId = _index.retrieveDocument(d);
        if (termId == -1) {
            //System.err.println("Term (" + term + ") has not been indexed! Ignored.");
            return 0;
        } else if(docId==-1){
           //System.err.println("Document (" + d.getUrl() + ") has not been indexed! Ignored.");
            return 0;
        }
        else {
            return getTermFreqInDoc(termId, docId);
        }
    }

    /**
     * @param t
     * @param d
     * @return number of occurrences of a term identified by id t in a document identified by id d
     */
    public int getTermFreqInDoc(int t, int d) {
        Map<Integer, Integer> freqInDocs = _termInDocFreqMap.get(t);
        if (freqInDocs == null) return 0;
        return freqInDocs.get(d);
    }

    /**
     * @param t
     * @param d
     * @return number of occurrences of a term t in a document identified by id d
     */
    public int getTermFreqInDoc(String t, int d) {
        int termId = _index.retrieveTermCanonical(t);
        if (termId == -1) {
            //System.err.println("Term (" + t + ") has not been indexed! Ignored.");
            return 0;
        } else {
            return getTermFreqInDoc(termId, d);
        }
    }

    /**
     * @param t
     * @return the id's of documents in which term t are found
     */
    public int[] getTermAppear(String t) {
        int termId = _index.retrieveTermCanonical(t);
        if (termId == -1) {
            //System.err.println("Term (" + t + ") has not been indexed! Ignored.");
            return new int[0];
        } else {
            if(_termInDocFreqMap.get(termId)==null)
                System.out.println();
            Set<Integer> keys = _termInDocFreqMap.get(termId).keySet();
            int[] rs = new int[keys.size()];
            int c = 0;
            for (Integer k : keys) {
                rs[c] = k;
                c++;
            }
            return rs;
        }
    }

    /**
     * @param term
     * @return number of occurrences of a term in all documents
     */
    public int getSumTermFreqInDocs(String term) {
        int termId = _index.retrieveTermCanonical(term);
        if (termId == -1) {
            //System.err.println("Term (" + term + ") has not been indexed! Ignored.");
            return 0;
        } else {
            return getSumTermFreqInDocs(termId);
        }
    }

    /**
     * @param term
     * @return number of occurrences of a term in all documents
     */
    public int getSumTermFreqInDocs(int term) {
        Set<Integer> docs = _index.retrieveDocIdsContainingTermCanonical(term);
        int sum = 0;
        Iterator<Integer> it = docs.iterator();
        while (it.hasNext())
            sum += getTermFreqInDoc(term, it.next());

        return sum;
    }

}
