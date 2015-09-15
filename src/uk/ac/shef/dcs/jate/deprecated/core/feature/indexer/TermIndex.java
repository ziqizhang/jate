package uk.ac.shef.dcs.jate.deprecated.core.feature.indexer;

import java.util.*;

/**
 *
 */
class TermIndex<String> extends Index<String> {
    protected Map<Integer, Set<Integer>> term2Variants;
    protected Map<Integer, Set<Integer>> term2Docs;

    public TermIndex() {
        term2Variants = new HashMap<Integer, Set<Integer>>();
        term2Docs = new HashMap<Integer, Set<Integer>>();
    }

    protected synchronized Set<Integer> getVariantsOfTerm(int termId){
        return term2Variants.get(termId);
    }

    protected synchronized Set<Integer> getSourceDocsOfTerm(int termId){
        return term2Docs.get(termId);
    }

    protected synchronized void updateVariantsOfTerm(int termId, Collection<Integer> termVarIds){
        Set<Integer> existing = getVariantsOfTerm(termId);
        boolean put=false;
        if(existing==null) {
            existing = new HashSet<Integer>();
            put=true;
        }
        existing.addAll(termVarIds);
        if(put)
            term2Variants.put(termId, existing);
    }

    protected synchronized void updateSourceDocsOfTerm(int termId, Collection<Integer> docIds){
        Set<Integer> existing = getSourceDocsOfTerm(termId);
        boolean put=false;
        if(existing==null) {
            existing = new HashSet<Integer>();
            put=true;
        }
        existing.addAll(docIds);
        if(put)
            term2Docs.put(termId, existing);
    }

}
