package uk.ac.shef.dcs.oak.jate.core.feature;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import uk.ac.shef.dcs.oak.jate.core.feature.indexer.GlobalIndexMem;
import uk.ac.shef.dcs.oak.jate.util.control.Lemmatizer;
import uk.ac.shef.dcs.oak.jate.util.control.StopList;

/**
 * Newly added for improving the variants detection for a term.
 */
public class TermVariantsUpdater{
	private GlobalIndexMem _index;
	
	private Map<String, Set<String>> additionalVariantsMap;
	
	public TermVariantsUpdater(GlobalIndexMem termDocIndex, StopList stoplist, Lemmatizer lemmatizer) {
		// TODO Auto-generated constructor stub
		_index = termDocIndex;
		additionalVariantsMap = new HashMap<String, Set<String>>();
	}

	/*no need to use lemmatizer to lemmatize the last word because 'canonical terms' are picked up from the global index itself which are already lemmatized.*/
	public GlobalIndexMem updateVariants(){
		for(String term:_index.getTermsCanonical()){
			//Set<String> v = _index.retrieveVariantsOfTermCanonical(term);
			boolean flag = false;
			Set<String> variants_term_additional = null;
			for(String lemmatized_term : _index.getTermsCanonical()){
				//lemmatized_term = lemmatized_term.toLowerCase();
				if(lemmatized_term.equals(term))
					continue;
				 Pattern p = Pattern.compile("\\b"+term+"\\b");
				 Matcher m = p.matcher(lemmatized_term);
	             if(!m.find()) {
	                	continue;                	
	             }
				else{
					int idx = lemmatized_term.indexOf(term);					
					int term_len = term.split(" ").length;
					Set<String> variants_term = _index.retrieveVariantsOfTermCanonical(term);					
					Set<String> variants_lemmatized_term = _index.retrieveVariantsOfTermCanonical(lemmatized_term);
					
					for(String variant:variants_lemmatized_term){
						String subString = variant.substring(idx);
						String[] split_subString = subString.split(" ");
						StringBuilder sb_substring = new StringBuilder();
						for(int i=0; i<term_len ;i++){
							sb_substring.append(split_subString[i]);
							sb_substring.append(" ");
						}
						if(variants_term.contains(sb_substring.toString().trim())){
							continue;
						}
						else{
							flag = true;
							if(variants_term_additional == null){
								variants_term_additional = new HashSet<String>();
							}
							variants_term_additional.add(sb_substring.toString().trim());
						}				
						
					}
				}
					
			}
			if(flag){
				additionalVariantsMap.put(term, variants_term_additional);
			}			
			
		}		
		if(additionalVariantsMap.size() > 0)
			_index.updateIndexTermWithVariant(additionalVariantsMap);
	
		return _index;
		
		
	}
	
}