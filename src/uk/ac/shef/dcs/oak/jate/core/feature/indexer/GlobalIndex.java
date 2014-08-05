package uk.ac.shef.dcs.oak.jate.core.feature.indexer;

import uk.ac.shef.dcs.oak.jate.model.Document;

import java.util.Map;
import java.util.Set;

/**
 * Index terms (term canonical form, and variants), documents, and the containing/occur-in relation between terms and documents
 */
public abstract class GlobalIndex {
    protected int _termCounter = 0;
	protected int _variantCounter = 0;
	protected int _docCounter = 0;


    /**
	 * Given a candidate term's canonical form, index it and return its id.
	 *
	 * @param term
	 * @return the id
	 */
	protected abstract int indexTermCanonical(String term);

	/**
	 * Given an id, retrieve the candidate term's canonical form
	 *
	 * @param id
	 * @return
	 */
	public abstract String retrieveTermCanonical(int id);

    /**
	 * Given a candidate term's canonical form, return its id. (If the term has not been index, -1 will be returned
	 *
	 * @param term
	 * @return
	 */
	public abstract int retrieveTermCanonical(String term);

	/**
	 * @return all indexed candidate term canonical form ids
	 */
	public abstract Set<Integer> getTermCanonicalIds() ;

	/**
	 * @return all candidate term canonical forms
	 */
	public abstract Set<String> getTermsCanonical();

	/**
	 * Given a candidate term variant, index it and return its id.
	 *
	 * @param termV
	 * @return the id
	 */
	protected abstract int indexTermVariant(String termV) ;

	/**
	 * Given an id of a candidate term variant, retrieve the text
	 *
	 * @param id
	 * @return
	 */
	protected abstract String retrieveTermVariant(int id) ;

    /**
     * Given a term variant form, retrieve its canonical form
     *
     * @param termVar
     * @return
     */
    public abstract int retrieveCanonicalOfTermVariant(String termVar);

	public abstract Set<Integer> getTermVariantIds() ;

	public abstract Set<String> getTermVariants() ;

	/**
	 * Given a document, index it and return its id.
	 *
	 * @param d
	 * @return the id
	 */
	protected abstract int indexDocument(Document d);

	/**
	 * Given a document id return the document
	 *
	 * @param id
	 * @return
	 */
	public abstract Document retrieveDocument(int id);

    /**
     * Given a document, return its id. If the document has not been indexed, return -1
     *
     * @param d
     * @return the id
     */
    public abstract int retrieveDocument(Document d);

	/**
	 * @return all indexed documents
	 */
	public abstract Set<Document> getDocuments();

	/**
	 * @return return all indexed document ids
	 */
	public abstract Set<Integer> getDocumentIds();

	/*
	term - - variant
	 */
	/**
	 * Given a map containing [term canonical form - term variant forms], index the mapping
	 * @param map
	 */
	protected abstract void indexTermWithVariant(Map<String, Set<String>> map);

	/**
	 * Given a term canonical form, retrieve its variant forms found in the corpus
	 * @param term
	 * @return
	 */
	public abstract Set<String> retrieveVariantsOfTermCanonical(String term);

	/*
		term - - docs
		 */

	/**
	 * Given a candidate term's canonical form t found in document d, index the binary relation "t found_in d"
	 *
	 * @param t
	 * @param d
	 */
	protected abstract void indexTermCanonicalInDoc(String t, Document d);

	/**
	 * Given a candidate term's canonical form id t found in document with id d, index the binary relation "t found_in d"
	 *
	 * @param t
	 * @param d
	 */
	protected abstract void indexTermCanonicalInDoc(int t, int d) ;

	/**
	 * @param t the candidate term's canonical form in question
	 * @return the document ids of which documents contain the candidate term t
	 */
	public abstract Set<Integer> retrieveDocIdsContainingTermCanonical(String t);
	/**
	 * @param id the candidate term's canonical form in questoin
	 * @return the document ids of which documents contain the candidate term t
	 */
	public abstract Set<Integer> retrieveDocIdsContainingTermCanonical(int id);

	/**
	 * @param t the candidate term's canonical form in question
	 * @return the documents which contain the candidate term t
	 */
	public abstract Set<Document> retrieveDocsContainingTermCanonical(String t);

	/**
	 * @param t the candidate term's canonical form id in question
	 * @return the documents which contain the candidate term t
	 */
	public abstract Set<Document> retrieveDocsContainingTermCanonical(int t) ;

	/**
	 * @param t the candidate term's canonical form
	 * @return number of documents that contain the candidate term (any variants)
	 */
	public abstract int sizeTermInDocs(String t);

	/**
	 * @param t the id of candidate term's canonical form
	 * @return number of documents that contain the candidate term (any variants)
	 */
	public abstract int sizeTermInDocs(int t);

	/*
		doc - - terms
		 */

	/**
	 * Given a document d which contains a set of terms (canonical form), index the binary relation "document contains term canonical"
	 *
	 * @param d
	 * @param terms canonical forms of candidate terms found in document d
	 */
	protected abstract  void indexDocWithTermsCanonical(Document d, Set<String> terms);

	/**
	 * Given a document with id d which contains a set of terms (canonical form), index the binary relation "document contains term canonical"
	 *
	 * @param d id of document
	 * @param terms canonical forms of candidate terms found in document d
	 */
	protected abstract void indexDocWithTermsCanonical(int d, Set<Integer> terms);

	/**
	 * @param d
	 * @return the ids of canonical forms of terms found in the document d
	 */
	public abstract Set<Integer> retrieveTermCanonicalIdsInDoc(Document d) ;

	/**
	 * @param d
	 * @return the ids of canonical forms of terms found in the document d
	 */
	public abstract Set<Integer> retrieveTermCanonicalIdsInDoc(int d);

	/**
	 * @param d
	 * @return the canonical form of terms found in the document d
	 */
	public abstract Set<String> retrieveTermsCanonicalInDoc(Document d);
	/**
	 * @param d
	 * @return the canonical form of terms found in the document d
	 */
	public abstract Set<String> retrieveTermCanonicalInDoc(int d);

	/**
	 * @param d
	 * @return number of unique candidate terms (canonical form) found in document d
	 */
	public abstract int sizeDocHasTerms(Document d);

	/**
	 * @param d
	 * @return number of unique candidate terms (canonical form) found in document d
	 */
	public abstract int sizeDocHasTerms(int d);
}
