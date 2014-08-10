package uk.ac.shef.dcs.jate.core.algorithm;


import java.util.ArrayList;
import java.util.List;
import java.util.Set;




public class RAKEFeatureWrapper extends AbstractFeatureWrapper {

	private List<String> _CandidateTerms;
	
	
	
	public RAKEFeatureWrapper(List<String> candidates){
		_CandidateTerms = new ArrayList<String>();
		_CandidateTerms.addAll(candidates);
	}

	/**
	 * @param term
	 * @return number of occurrences of a term in a corpus. If a term cannot be found it always returns 1.
	 */

	
	@Override
	public Set<String> getTerms() {
		// TODO Auto-generated method stub
		
		return null; //_CandidateTerms;
	}
	
	public List<String> getCandidateTerms(){
		return _CandidateTerms;
	}

}
