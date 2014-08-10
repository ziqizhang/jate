package uk.ac.shef.dcs.jate.core.algorithm;

import java.util.Set;

import uk.ac.shef.dcs.jate.model.Term;
import uk.ac.shef.dcs.jate.test.AlgorithmTester;

/**
 * ChiSquareFeatureWrapper wraps an instance of AlgorithmTester, which enables to get the terms along with their confidence values identified from the execution of that AlgorithmTester Instance.
 */

public class ChiSquareFeatureWrapper extends AbstractFeatureWrapper {
	
	private AlgorithmTester _tester;
	
	
	public ChiSquareFeatureWrapper(AlgorithmTester tester){
		_tester = tester;
	}

	public AlgorithmTester getTesterObject(){
		return _tester;
	}
	
	public Term[] getCandidateTerms() {
		// TODO Auto-generated method stub		
		return _tester.getTerms();
	}
	
	
	public Set<String> getVariants(String term){
		return _tester.getIndex().retrieveVariantsOfTermCanonical(term);
	}
	
	@Override
	public Set<String> getTerms() {
		// TODO Auto-generated method stub
		
		return null;
	}

}
