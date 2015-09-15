package uk.ac.shef.dcs.jate.deprecated.model;

/**
 * Represents a term.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */

public class Term implements Comparable<Term> {

	protected String _singular;
	protected double _confidence;

	/**
	 * Creates a term
	 * @param lemma the lemmatised text of the term
	 * @param confidence the relevance of the term to the corpus from which it is extracted
	 */
	public Term(java.lang.String lemma, double confidence) {
		_singular = lemma;
		_confidence = confidence;
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
