package uk.ac.shef.dcs.jate.v2.feature;

import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 *
 */
public class Cooccurrence extends AbstractFeature {
    protected IntMatrix2D coocurrence;
    protected Map<Integer, String> mapIdx2Term = new HashMap<>();
    protected Map<String, Integer> mapTerm2Idx = new HashMap<>();

    protected Map<Integer, Set<Integer>> mapTermIdx2CtxIdx = new HashMap<>();
    protected Map<Integer, Integer> mapCtxIdx2TermFreq = new ConcurrentHashMap<>();

    protected int termCounter =0;
    protected int contextCounter=0;

    public Cooccurrence(){}
    public Cooccurrence(Set<String> terms){
        for(String t: terms){
            mapIdx2Term.put(termCounter, t);
            mapTerm2Idx.put(t, termCounter);
            termCounter++;
        }
    }

    protected void index(String term){
        mapIdx2Term.put(termCounter, term);
        mapTerm2Idx.put(term, termCounter);
        termCounter++;
    }

    public String lookup(int index){
        return mapIdx2Term.get(index);
    }

    public int lookup(String term){
        return mapTerm2Idx.get(term);
    }

    public Map<Integer, Integer> getCoocurrence(String term){
        int index= lookup(term);
        IntMatrix1D cooccur=coocurrence.viewRow(index);
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


    public Set<Integer> getContexts(int termId){
        Set<Integer> ctxIds=mapTermIdx2CtxIdx.get(termId);
        if(ctxIds==null)
            ctxIds=new HashSet<>();
        return ctxIds;
    }

    public int getTTFOfContext(int ctxId){
        Integer freq = mapCtxIdx2TermFreq.get(ctxId);
        if(freq==null)
            freq=0;
        return freq;
    }
}
