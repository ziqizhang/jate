package uk.ac.shef.dcs.jate.core.algorithm;

import java.util.Map;
import java.util.Set;

import uk.ac.shef.dcs.jate.test.AlgorithmTester;


public class NCValueFeatureWrapper extends AbstractFeatureWrapper {
	
	private Map<String, Double> _contextWords;
	private AlgorithmTester _tester;
	
	
	public NCValueFeatureWrapper(Map<String, Double> contextWords, AlgorithmTester tester){
		_contextWords = contextWords;
		_tester = tester;
	}

	
	public Map<String, Double> getContextWordsMap(){
		return _contextWords;
	}

	public AlgorithmTester getTesterObject(){
		return _tester;
	}


	@Override
	public Set<String> getTerms() {
		// TODO Auto-generated method stub
		return null;
	}

}
