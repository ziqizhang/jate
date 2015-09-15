package uk.ac.shef.dcs.jate.deprecated.core.feature.indexer;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zqz on 15/12/2014.
 */
class TermVariantIndex<String> extends Index<String> {
    protected Map<Integer, Integer> variant2term;

    public TermVariantIndex(){
        variant2term=new HashMap<Integer, Integer>();
    }

    protected synchronized int getTermCanonicalOfVariant(int termVarId){
        Integer termId = variant2term.get(termVarId);
        return termId==null?-1:termId;
    }

    protected synchronized void addVariantOfTermCanonical(int termVarId, int termId){
        variant2term.put(termVarId, termId);
    }
}
