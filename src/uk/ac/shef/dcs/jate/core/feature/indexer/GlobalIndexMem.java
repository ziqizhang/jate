package uk.ac.shef.dcs.jate.core.feature.indexer;

import uk.ac.shef.dcs.jate.model.Document;

import java.util.*;


/**
 * GlobalIndexMem stores information (in-memory) of binary relations between candidate terms (word or phrase) and corpus. These
 * include:
 * <br>- candidate term canonical forms and their int ids
 * <br>- candidate term variant forms and their int ids
 * <br>- mapping from candidate term canonical form to variant forms
 * <br>- corpus elements (document) and their int ids
 * <br>- candidate term and their containing documents (id - ids)
 * <br>- document ids and their contained candidate terms (id - ids)
 * <p/>
 * In other words, a GlobalIndexMem contains mappings from a candidate term to its id, mappings from a candidate
 * term's canonical form to its variants found in the corpus,  a document to its id,
 * a lexical unit id to the document ids that contain the unit, a document id to the lexical unit ids which the document
 * contains.
 * <p/>
 * <br>Also credits to <b>pmclachlan@gmail.com</b> for revision for performance tweak </br>
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class GlobalIndexMem extends GlobalIndex {

    /**
     * Term to TermId map
     */
    protected HashMap<String, Integer> _termIdMap = new HashMap<String, Integer>();

    /**
     * TermId to Term map (array index is TermID)
     */
    protected ArrayList<String> _terms = new ArrayList<String>();

    /**
     * Variant to VariantId map
     */
    protected HashMap<String, Integer> _variantIdMap = new HashMap<String, Integer>();

    /**
     * VariantId to Variant map (array index is VariantID)
     */
    protected ArrayList<String> _variants = new ArrayList<String>();

    /**
     * Document to DocumentId map
     */
    protected HashMap<Document, Integer> _docIdMap = new HashMap<Document, Integer>();

    /**
     * DocumentId to Document map (array index is DocumentID)
     */
    protected ArrayList<Document> _documents = new ArrayList<Document>();


    protected Map<Integer, Set<Integer>> _term2Docs = new HashMap<Integer, Set<Integer>>();
    protected Map<Integer, Set<Integer>> _doc2Terms = new HashMap<Integer, Set<Integer>>();
    protected Map<Integer, Set<Integer>> _term2Variants = new HashMap<Integer, Set<Integer>>();
    protected Map<Integer, Integer> _variant2term = new HashMap<Integer, Integer>();

    public GlobalIndexMem() {
    }

    public Map<String, Integer> getTermIdMap() {
        return _termIdMap;
    }

    public Map<String, Integer> getVariantIdMap() {
        return _variantIdMap;
    }

    public Map<Document, Integer> getDocIdMap() {
        return _docIdMap;
    }

    public Map<Integer, Set<Integer>> getTerm2Docs() {
        return _term2Docs;
    }

    public Map<Integer, Set<Integer>> getDoc2Terms() {
        return _doc2Terms;
    }

    public Map<Integer, Set<Integer>> getTerm2Variants() {
        return _term2Variants;
    }

    public Map<Integer, Integer> getVariant2Term() {
        return _variant2term;
    }

    /**
     * Given a candidate term's canonical form, return its id. If the term has not been indexed, it will be indexed
     * with a new id and that id will be returned
     *
     * @param term
     * @return the id
     */
    protected int indexTermCanonical(String term) {
        Integer index = _termIdMap.get(term);
        //if (index == null) _termIdMap.put(term, index = _termCounter++);
        if (index == null) {
            _termIdMap.put(term, index = _termCounter++);
            _terms.add(term);
        }
        return index;
    }

    /**
     * Given a candidate term's canonical form, return its id. (If the term has not been index, -1 will be returned
     *
     * @param term
     * @return the id
     */
    public int retrieveTermCanonical(String term) {
        Integer index = _termIdMap.get(term);
        if (index == null) return -1;
        return index;
    }

    /**
     * Given an id, retrieve the candidate term's canonical form
     *
     * @param id
     * @return
     */
    public String retrieveTermCanonical(int id) {
        /*for (Map.Entry<String, Integer> en : _termIdMap.entrySet()) {
            if (en.getValue() == id) return en.getKey();
        }
        return null;*/

        return _terms.get(id);
    }

    /**
     * @return all indexed candidate term canonical form ids
     */
    public Set<Integer> getTermCanonicalIds() {
        return new HashSet<Integer>(_termIdMap.values());
    }

    /**
     * @return all candidate term canonical forms
     */
    public Set<String> getTermsCanonical() {
        return _termIdMap.keySet();
    }

    /**
     * Given a candidate term variant, index it and return its id.
     *
     * @param termV
     * @return the id
     */
    protected int indexTermVariant(String termV) {
        Integer index = _variantIdMap.get(termV);

        //if (index == null) _variantIdMap.put(termV, index = _variantCounter++);
        if (index == null) {
            index = _variantCounter++;
            _variantIdMap.put(termV, index);
            _variants.add(termV);
        }
        return index;
    }

    /**
     * Given an id of a candidate term variant, retrieve the text
     *
     * @param id
     * @return
     */
    protected String retrieveTermVariant(int id) {
        /*for (Map.Entry<String, Integer> en : _variantIdMap.entrySet()) {
            if (en.getValue() == id) return en.getKey();
        }
        return null;*/
        return _variants.get(id);
    }

    public Set<Integer> getTermVariantIds() {
        return new HashSet<Integer>(_variantIdMap.values());
    }

    public Set<String> getTermVariants() {
        return _variantIdMap.keySet();
    }

    /**
     * Given a document, return its id. If the document has not been indexed, index it and return the new id
     *
     * @param d
     * @return the id
     */
    protected int indexDocument(Document d) {
        Integer index = _docIdMap.get(d);
        //if (index == null) _docIdMap.put(d, index = _docCounter++);
        if (index == null) {
            _docIdMap.put(d, index = _docCounter++);
            _documents.add(d);
        }
        return index;
    }

    /**
     * Given a document, return its id. If the document has not been indexed, return -1
     *
     * @param d
     * @return the id
     */
    public int retrieveDocument(Document d) {
        Integer index = _docIdMap.get(d);
        if (index == null) return -1;
        return index;
    }

    /**
     * Given a document id return the document
     *
     * @param id
     * @return
     */
    public Document retrieveDocument(int id) {
        //          for (Map.Entry<Document, Integer> en : _docIdMap.entrySet()) {
        //        if (en.getValue() == id) return en.getKey();
        //      }
//        return null;
        return _documents.get(id);
    }

    /**
     * @return all indexed documents
     */
    public Set<Document> getDocuments() {
        return _docIdMap.keySet();
    }

    /**
     * @return return all indexed document ids
     */
    public Set<Integer> getDocumentIds() {
        return new HashSet<Integer>(_docIdMap.values());
    }

    /*
     term - - variant
      */

    /**
     * Given a map containing [term canonical form - term variant forms], index the mapping, plus the mapping from
     * term variant to term canonical
     *
     * @param map
     */
    protected void indexTermWithVariant(Map<String, Set<String>> map) {
        for (Map.Entry<String, Set<String>> e : map.entrySet()) {
            int termid = indexTermCanonical(e.getKey());

            Set<Integer> variants = _term2Variants.get(termid);
            if (variants == null) variants = new HashSet<Integer>();
            for (String v : e.getValue()) {
                int varid = indexTermVariant(v);
                variants.add(varid);
                _variant2term.put(varid, termid);
            }
            _term2Variants.put(termid, variants);
        }
    }

    /**
     * Given a term canonical form, retrieve its variant forms found in the corpus
     *
     * @param term
     * @return
     */
    public Set<String> retrieveVariantsOfTermCanonical(String term) {
        Set<String> variants = new HashSet<String>();
        Set<Integer> vIds = _term2Variants.get(retrieveTermCanonical(term));
        if (vIds == null) return variants;

        Iterator<Integer> it = vIds.iterator();
        while (it.hasNext())
            variants.add(retrieveTermVariant(it.next()));
        return variants;
    }

    /**
     * Given a term variant form, retrieve its canonical form
     *
     * @param termVar
     * @return
     */
    public int retrieveCanonicalOfTermVariant(String termVar) {
        Integer varId = _variantIdMap.get(termVar);
        if (varId == null)
            return -1;
        Integer termId = _variant2term.get(varId);
        if (termId == null)
            return -1;
        return termId;
    }

    /*
         term - - docs
          */

    /**
     * Given a candidate term's canonical form t found in document d, index the binary relation "t found_in d"
     *
     * @param t
     * @param d
     */
    protected void indexTermCanonicalInDoc(String t, Document d) {
        indexTermCanonicalInDoc(indexTermCanonical(t), indexDocument(d));
    }

    /**
     * Given a candidate term's canonical form id t found in document with id d, index the binary relation "t found_in d"
     *
     * @param t
     * @param d
     */
    protected void indexTermCanonicalInDoc(int t, int d) {
        Set<Integer> docs = _term2Docs.get(t);
        if (docs == null) docs = new HashSet<Integer>();
        docs.add(d);
        _term2Docs.put(t, docs);
    }

    /**
     * @param t the candidate term's canonical form in question
     * @return the document ids of which documents contain the candidate term t
     */
    public Set<Integer> retrieveDocIdsContainingTermCanonical(String t) {
        return retrieveDocIdsContainingTermCanonical(indexTermCanonical(t));
    }

    /**
     * @param id the candidate term's canonical form in questoin
     * @return the document ids of which documents contain the candidate term t
     */
    public Set<Integer> retrieveDocIdsContainingTermCanonical(int id) {
        Set<Integer> res = _term2Docs.get(id);
        return res == null ? new HashSet<Integer>() : res;
    }

    /**
     * @param t the candidate term's canonical form in question
     * @return the documents which contain the candidate term t
     */
    public Set<Document> retrieveDocsContainingTermCanonical(String t) {
        return retrieveDocsContainingTermCanonical(indexTermCanonical(t));
    }

    /**
     * @param t the candidate term's canonical form id in question
     * @return the documents which contain the candidate term t
     */
    public Set<Document> retrieveDocsContainingTermCanonical(int t) {
        Set<Document> docs = new HashSet<Document>();
        Set<Integer> docids = retrieveDocIdsContainingTermCanonical(t);
        Iterator<Integer> it = docids.iterator();
        while (it.hasNext()) docs.add(retrieveDocument(it.next()));
        return docs;
    }

    /**
     * @param t the candidate term's canonical form
     * @return number of documents that contain the candidate term (any variants)
     */
    public int sizeTermInDocs(String t) {
        return retrieveDocIdsContainingTermCanonical(t).size();
    }

    /**
     * @param t the id of candidate term's canonical form
     * @return number of documents that contain the candidate term (any variants)
     */
    public int sizeTermInDocs(int t) {
        return retrieveDocIdsContainingTermCanonical(t).size();
    }

    /*
         doc - - terms
          */

    /**
     * Given a document d which contains a set of terms (canonical form), index the binary relation "document contains term canonical"
     *
     * @param d
     * @param terms canonical forms of candidate terms found in document d
     */
    protected void indexDocWithTermsCanonical(Document d, Set<String> terms) {
        Set<Integer> termids = new HashSet<Integer>();
        for (String t : terms) termids.add(indexTermCanonical(t));
        indexDocWithTermsCanonical(indexDocument(d), termids);
    }

    /**
     * Given a document with id d which contains a set of terms (canonical form), index the binary relation "document contains term canonical"
     *
     * @param d     id of document
     * @param terms canonical forms of candidate terms found in document d
     */
    protected void indexDocWithTermsCanonical(int d, Set<Integer> terms) {
        Set<Integer> termids = _doc2Terms.get(d);
        if (termids != null) termids.addAll(terms);
        _doc2Terms.put(d, terms);
    }

    /**
     * @param d
     * @return the ids of canonical forms of terms found in the document d
     */
    public Set<Integer> retrieveTermCanonicalIdsInDoc(Document d) {
        return retrieveTermCanonicalIdsInDoc(indexDocument(d));
    }

    /**
     * @param d
     * @return the ids of canonical forms of terms found in the document d
     */
    public Set<Integer> retrieveTermCanonicalIdsInDoc(int d) {
        Set<Integer> res = _doc2Terms.get(d);
        return res == null ? new HashSet<Integer>() : res;
    }

    /**
     * @param d
     * @return the canonical form of terms found in the document d
     */
    public Set<String> retrieveTermsCanonicalInDoc(Document d) {
        return retrieveTermCanonicalInDoc(indexDocument(d));
    }

    /**
     * @param d
     * @return the canonical form of terms found in the document d
     */
    public Set<String> retrieveTermCanonicalInDoc(int d) {
        Set<String> res = new HashSet<String>();
        Set<Integer> termids = retrieveTermCanonicalIdsInDoc(d);
        Iterator<Integer> it = termids.iterator();
        while (it.hasNext()) {
            res.add(retrieveTermCanonical(it.next()));
        }
        return res;
    }

    /**
     * @param d
     * @return number of unique candidate terms (canonical form) found in document d
     */
    public int sizeDocHasTerms(Document d) {
        return retrieveTermsCanonicalInDoc(d).size();
    }

    /**
     * @param d
     * @return number of unique candidate terms (canonical form) found in document d
     */
    public int sizeDocHasTerms(int d) {
        return retrieveTermCanonicalInDoc(d).size();
    }
    
  /*  public void updateVariantsOfTermCanonical(String termCanonical, Set<String> variants_term_additional){
    	Set<String> variants = retrieveVariantsOfTermCanonical(termCanonical);
    	if(variants_term_additional.size()>0){
    		variants.addAll(variants_term_additional);
    	}
    	
    }
    */
    
    public void updateIndexTermWithVariant(Map<String, Set<String>> additional_variants_map) {
        for (Map.Entry<String, Set<String>> e : additional_variants_map.entrySet()) {
            int termid = indexTermCanonical(e.getKey());

            Set<Integer> variants = _term2Variants.get(termid);
            
            for (String v : e.getValue()) {
                int varid = indexTermVariant(v);
                variants.add(varid);
                _variant2term.put(varid, termid);
            }
            _term2Variants.put(termid, variants);
        }
    }



}
