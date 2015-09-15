package uk.ac.shef.dcs.jate.deprecated.core.feature.indexer;

import java.util.*;

/**
 * Created by zqz on 15/12/2014.
 */
class DocumentIndex<Document> extends Index<Document>{
    protected Map<Integer, Set<Integer>> doc2Terms;

    public DocumentIndex(){
        doc2Terms=new HashMap<Integer, Set<Integer>>();
    }

    protected synchronized Set<Integer> getTermsInDoc(int docId){
        return doc2Terms.get(docId);
    }

    protected synchronized void updateTermsInDoc(int docId, Collection<Integer> termIds){
        Set<Integer> existing = getTermsInDoc(docId);
        boolean put=false;
        if(existing==null) {
            existing = new HashSet<Integer>();
            put=true;
        }
        existing.addAll(termIds);
        if(put)
            doc2Terms.put(docId, existing);
    }

}
