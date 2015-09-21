package uk.ac.shef.dcs.jate.v2.feature;

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

    protected int termCounter =-1;

    public Cooccurrence(int numTerms){
        cooccurrence =new SparseIntMatrix2D(numTerms, numTerms);
    }

    public int getNumTerms(){
        return cooccurrence.rows();
    }

    Set<String> getTerms(){
        return mapTerm2Idx.keySet();
    }

    protected int lookupAndIndex(String term){
        Integer idx = mapTerm2Idx.get(term);
        if(idx==null) {
            termCounter++;
            mapIdx2Term.put(termCounter, term);
            mapTerm2Idx.put(term, termCounter);
            return termCounter;
        }
        return idx;
    }

    protected void increment(int term1Idx, int term2Idx, int freq){
        int newFreq=cooccurrence.getQuick(term1Idx, term2Idx)+freq;
        cooccurrence.setQuick(term1Idx, term2Idx,
                newFreq);
        cooccurrence.setQuick(term2Idx, term1Idx,
                newFreq);
    }

    public String lookup(int index){
        return mapIdx2Term.get(index);
    }

    public Map<Integer, Integer> getCoocurrence(String term){
        int index= lookupAndIndex(term);
        return getCooccurrence(index);
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
            int v = nzValues.get(idx);
            result.put(idx, v);
        }
        return result;
    }

    protected int getCooccurrence(int term1Id, int term2Id){
        return cooccurrence.getQuick(term1Id, term2Id);
    }

}
