package uk.ac.shef.dcs.jate.feature;

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;
import cern.colt.matrix.tint.impl.SparseIntMatrix2D;

import java.util.*;

/**
 *
 */
public class Cooccurrence extends AbstractFeature {
    protected IntMatrix2D cooccurrence;
    protected Map<Integer, String> mapIdx2Term = new HashMap<>();
    protected Map<String, Integer> mapTerm2Idx = new HashMap<>();

    protected Map<Integer, String> mapIdx2RefTerm = new HashMap<>();
    protected Map<String, Integer> mapRefTerm2Idx = new HashMap<>();


    protected int termCounter =-1;
    protected int ctxTermCounter =-1;

    public Cooccurrence(int terms, int refTerms){
        cooccurrence =new SparseIntMatrix2D(terms, refTerms);
    }

    public int getNumTerms(){
        return cooccurrence.rows();
    }

    public Set<String> getTerms(){
        return mapTerm2Idx.keySet();
    }

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
        int newFreq=cooccurrence.getQuick(termIdx, refTermIdx)+freq;
        cooccurrence.setQuick(termIdx, refTermIdx,
                newFreq);
    }

    public String lookupTerm(int index){
        return mapIdx2Term.get(index);
    }

    public String lookupRefTerm(int index){return mapIdx2RefTerm.get(index);}

    /**
     * It is possible to have an invalid query term, usually created because of cross-sentence-boundary n-gram,
     * or phrases matched by POS patterns. In these cases, an empty map is returned
     *
     * @param term
     * @return
     */
    public Map<Integer, Integer> getCoocurrence(String term){
        int termIdx= lookupTerm(term);
        if(termIdx==-1)
            return new HashMap<>();
        return getCooccurrence(termIdx);
    }

    Map<Integer, Integer> getCooccurrence(int index){
        IntMatrix1D cooccur= cooccurrence.viewRow(index);
        IntArrayList nzIndexes= new IntArrayList();
        IntArrayList nzValues = new IntArrayList();
        cooccur.getNonZeros(nzIndexes, nzValues);
        Map<Integer, Integer> result = new HashMap<>();
        List index_elements=nzIndexes.toList();
        //System.out.println("\t"+getCurrentTimeStamp() + "row="+currentRow+" "+index_elements.size()+" non-zeros");
        for(int i=0; i<index_elements.size(); i++){
            int idx=(int)index_elements.get(i);
            int v = nzValues.get(i);
            result.put(idx, v);
        }
        return result;
    }

}
