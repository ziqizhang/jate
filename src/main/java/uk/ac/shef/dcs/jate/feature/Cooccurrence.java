package uk.ac.shef.dcs.jate.feature;


import no.uib.cipr.matrix.sparse.FlexCompRowMatrix;
import no.uib.cipr.matrix.sparse.SparseVector;
import org.apache.log4j.Logger;

import java.util.*;

/**
 *
 */
public class Cooccurrence extends AbstractFeature {
    private static Logger LOG = Logger.getLogger(Cooccurrence.class.getSimpleName());

    protected FlexCompRowMatrix cooccurrence;
    protected Map<Integer, String> mapIdx2Term = new HashMap<>();
    protected Map<String, Integer> mapTerm2Idx = new HashMap<>();

    protected Map<Integer, String> mapIdx2RefTerm = new HashMap<>();
    protected Map<String, Integer> mapRefTerm2Idx = new HashMap<>();


    protected int termCounter =-1;
    protected int ctxTermCounter =-1;

    public Cooccurrence(int terms, int refTerms){
        cooccurrence =new FlexCompRowMatrix(terms, refTerms);
    }

    void deduce(int rowIndex, int colIndex, int value){
        double newValue = cooccurrence.get(rowIndex, colIndex);
        if(newValue==0) {
            LOG.debug(rowIndex + "|" + colIndex);
            LOG.debug(lookupTerm(rowIndex) + "|" + lookupRefTerm(colIndex));
        }
        newValue-=value;
        newValue=newValue<0?0:newValue;

        cooccurrence.set(rowIndex, colIndex, newValue);
    }

    public Set<String> getTerms(){
        return mapTerm2Idx.keySet();
    }
    public Set<String> getRefTerms() {return mapRefTerm2Idx.keySet();}

    protected synchronized int lookupAndIndexTerm(String term){
        Integer idx = mapTerm2Idx.get(term);
        if(idx==null) {
            termCounter++;
            mapIdx2Term.put(termCounter, term);
            mapTerm2Idx.put(term, termCounter);
            return termCounter;
        }
        return idx;
    }

    protected int lookupTerm(String term){
        Integer index= mapTerm2Idx.get(term);
        if(index==null) {
            return -1;
        }
        return index;
    }

    protected synchronized int lookupAndIndexRefTerm(String refTerm){
        Integer idx = mapRefTerm2Idx.get(refTerm);

        if(idx==null) {
            ctxTermCounter++;
            mapIdx2RefTerm.put(ctxTermCounter, refTerm);
            mapRefTerm2Idx.put(refTerm, ctxTermCounter);
            return ctxTermCounter;
        }
        return idx;
    }


    protected int lookupRefTerm(String refTerm){
        Integer index= mapRefTerm2Idx.get(refTerm);
        if(index==null) {
            return -1;
        }
        return index;
    }

    protected synchronized void increment(int termIdx, int refTermIdx, int freq){
        //try {
            double newFreq = cooccurrence.get(termIdx, refTermIdx) + freq;
            cooccurrence.set(termIdx, refTermIdx,
                    newFreq);
        /*}catch (Exception e){
            System.err.println("rows="+cooccurrence.numRows()+", columns="+cooccurrence.numColumns()+". row="+termIdx+"" +
                    " column="+refTermIdx);
            e.printStackTrace();

            System.err.println("\n\n\nTrying again, ");
            System.out.println("value="+cooccurrence.get(termIdx, refTermIdx));
        }*/
    }

    public String lookupTerm(int index){
        return mapIdx2Term.get(index);
    }

    public String lookupRefTerm(int index){return mapIdx2RefTerm.get(index);}

    /**
     * It is possible to have an invalid query term, usually created because of cross-sentence-boundary n-gram,
     * or phrases matched by POS patterns. In these cases, an empty map is returned
     *
     * @param term term string
     * @return Map the map of term index and co-ocurrence
     */
    public Map<Integer, Integer> getCoocurrence(String term){
        int termIdx= lookupTerm(term);
        if(termIdx==-1)
            return new HashMap<>();
        return getCooccurrence(termIdx);
    }

    Map<Integer, Integer> getCooccurrence(int index){
        Map<Integer, Integer> result = new HashMap<>();
        SparseVector vec=cooccurrence.getRow(index);
        int[] nonZeroIndexes=vec.getIndex();

        for(int i=0; i<nonZeroIndexes.length; i++){
            int idx=nonZeroIndexes[i];
            double v = cooccurrence.get(index,idx);
            result.put(idx, (int)v);
        }

        return result;
    }
}
