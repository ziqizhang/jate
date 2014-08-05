package uk.ac.shef.dcs.oak.jate.model;

import java.util.List;
import java.util.Set;

/**
 * Represents a term.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */

public class NCTermwithOriginals extends Term { //implements Comparable<Term> {

	Set<String> _originals;

	/**
	 * Creates a term
	 * @param lemma the lemmatised text of the term
	 * @param confidence the relevance of the term to the corpus from which it is extracted
	 */
	public NCTermwithOriginals(java.lang.String lemma, Set<String> originals, double confidence) {
		super(lemma, confidence);
		_originals.addAll(originals);
	}

	public java.lang.String getConcept() {
		return _singular;
	}

	public double getConfidence() {
		return _confidence;
	}

	public void setConcept(java.lang.String singular){
		_singular = singular;
	}
	public void setConfidence(double c){
		_confidence=c;
	}
	
	public void setOriginals(Set<String> originals){
		_originals=originals;
	}
	public Set<String> getOriginals(){
		return _originals;
	}

	public boolean equals(Object o) {
		if (this == o) return true;
		if (o == null || getClass() != o.getClass()) return false;
		final Term that = (Term) o;

		return that.getConcept().equals(getConcept()) || that.getConcept().equals(getConcept());

	}

	public int hashCode(){
		return getConcept().hashCode();
	}

	public int compareTo(final Term c) {
      return getConfidence() > c.getConfidence() ? -1 : getConfidence() < c.getConfidence() ? 1 : 0;
   }
}
