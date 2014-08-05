package uk.ac.shef.dcs.oak.jate.core.algorithm;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.model.Term;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.Arrays;

/**
 * An Implementation of the RAKE (Rapid Automatic Keyword Extraction) 
 * <i> Rose, Stuart, et al. "Automatic keyword extraction from individual documents." Text Mining (2010): 1-20.
 * </i>
 *
 */
public class RAKEAlgorithm implements Algorithm {

	
	public Map<String, Double> calculateWordScores(List<String> phrases) {
		Map<String, Integer> wordfreq = new HashMap<String, Integer>();
		Map<String, Integer> worddegree = new HashMap<String, Integer>();
		Map<String, Double> wordscore = new HashMap<String, Double>();
		
		
		for(String phrase : phrases)
		{
			String[] wordlist = separatewords(phrase);
			int wordlistlength = wordlist.length;
			int wordlistdegree = wordlistlength - 1;
			for(String word:wordlist){
				int freq;
				if(wordfreq.containsKey(word)==false)
					wordfreq.put(word, 1);
				else
				{
					freq = wordfreq.get(word)+1;
					wordfreq.remove(word);
					wordfreq.put(word, freq);
				}
				
				if(worddegree.containsKey(word)==false)
					worddegree.put(word, wordlistdegree);
				else
				{
					int deg = worddegree.get(word)+wordlistdegree;
					worddegree.remove(word);
					worddegree.put(word, deg);
				}
				
			}			
				
		}
		
		for(Map.Entry<String, Integer> entry : worddegree.entrySet()){
			entry.setValue(entry.getValue()+ wordfreq.get(entry.getKey()));
		}
		
		for(Map.Entry<String, Integer> entry : wordfreq.entrySet()){
			wordscore.put(entry.getKey(), worddegree.get(entry.getKey())/(wordfreq.get(entry.getKey())*1.0));
		}
		return wordscore;				
	}
	
	
	public Term[] execute(AbstractFeatureWrapper store)throws JATEException {
		if (!(store instanceof RAKEFeatureWrapper)) throw new JATEException("" +
				"Required: RAKEFeatureWrapper");
		RAKEFeatureWrapper rakeFeatureStore = (RAKEFeatureWrapper) store;
		
		Set<Term> result = new HashSet<Term>();
		List<String> phrases = rakeFeatureStore.getCandidateTerms();
		Map<String, Double> wordscores = calculateWordScores(phrases);
		for(String candidate: phrases){
			String[] words = separatewords(candidate);
			double score = 0.0;
			for(String word : words){
				score+=wordscores.get(word);
			}
			result.add(new Term(candidate, score));			
		}
		Term[] all = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;
	}
	
	public String[] separatewords(String phrase){
		return(phrase.split(" "));
	}
	
	
	public String toString(){
		return "RAKE_ALGORITHM";
	}


	
}