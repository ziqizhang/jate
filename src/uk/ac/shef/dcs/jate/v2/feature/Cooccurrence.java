package uk.ac.shef.dcs.jate.v2.feature;

import cern.colt.list.tdouble.DoubleArrayList;
import cern.colt.list.tint.IntArrayList;
import cern.colt.matrix.tint.IntMatrix1D;
import cern.colt.matrix.tint.IntMatrix2D;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class Cooccurrence extends AbstractFeature {
    protected IntMatrix2D coocurrence;
    protected Map<Integer, String> mapIdx2Term = new HashMap<>();
    protected Map<String, Integer> mapTerm2Idx = new HashMap<>();

    protected int counter=0;

    public Cooccurrence(){}
    public Cooccurrence(Set<String> terms){
        for(String t: terms){
            mapIdx2Term.put(counter, t);
            mapTerm2Idx.put(t, counter);
            counter++;
        }
    }

    protected void index(String term){
        mapIdx2Term.put(counter, term);
        mapTerm2Idx.put(term, counter);
        counter++;
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

}
