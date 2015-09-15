package uk.ac.shef.dcs.jate.deprecated.core.context;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import uk.ac.shef.dcs.jate.deprecated.JATEProperties;
import uk.ac.shef.dcs.jate.deprecated.core.nlptools.NLPToolsControllerOpenNLP;
import uk.ac.shef.dcs.jate.deprecated.model.Corpus;
import uk.ac.shef.dcs.jate.deprecated.model.CorpusImpl;
import uk.ac.shef.dcs.jate.deprecated.model.Document;
import uk.ac.shef.dcs.jate.deprecated.model.Term;
import uk.ac.shef.dcs.jate.deprecated.test.AlgorithmTester;
import uk.ac.shef.dcs.jate.deprecated.util.Utility;
import uk.ac.shef.dcs.jate.deprecated.util.control.StopList;

/**
 * This class extracts the term context words along with their weights using the 'top' candidate terms from C-Value result. 
 */

public class ContextExtraction{
	private AlgorithmTester tester;		
	private StopList stoplist; 
	
	private Set<String> TopTerms_variants;
	private int TopTerms_Count;
	private Term[] Top_Terms;
	
	private Map<String,Set<String>> ContextWord_to_Term_Map;	
	private Map<String, Double> ContextWord_Map;
	
	
	public ContextExtraction(AlgorithmTester tester) throws IOException
	{
		// To obtain reference of tester object for CValue Algorithm.
		this.tester = tester;
		//stoplist initialization
		stoplist = new StopList(true);
		TopTerms_variants = new HashSet<String>();
	}
	
	/** Returns the candidate terms along with their C-Value scores identified by the C-Value Algorithm */
	public Term[] getTerms(){
		return tester.getTerms();
	}
	
	
	/** Returns the variants of 'top' candidate terms from the C-Value Resultant terms for identification of context words. */
	public Set<String> getTopTerms(int percent_TopTerms){
		TopTerms_Count = percent_TopTerms*tester.getTerms().length/100;
		Top_Terms = Arrays.copyOf(tester.getTerms(),TopTerms_Count);
		for(Term term: Top_Terms){
			TopTerms_variants.addAll(tester.getIndex().retrieveVariantsOfTermCanonical(term.getConcept()));			
		}
		return TopTerms_variants;
	}
	
	/** Main function for this class. */
	public Map<String, Double> Extract(String input) throws IOException
	{
		/* Get the percentage of the C-Value Terms which are to be considered as 'top candidate terms' for context words identification.
		 * This percentage value is configurable in jate.properties file.
		 */
		int percent_TopTerms = JATEProperties.getInstance().getPercentage();
		getTopTerms(percent_TopTerms);
		ContextIdentification(new CorpusImpl(input));
		Calculate_CW_Weight();
		return ContextWord_Map;		
	}
	
	/** This function makes calls to other functions for identification of the context words by making use of the 'top' candidate terms identified earlier.  */
	public void ContextIdentification(Corpus corpus) throws IOException {
		for (Document d : corpus) {
			int i=0;
			for (String sent: NLPToolsControllerOpenNLP.getInstance().getSentenceSplitter().sentDetect(d.getContent())) {
				i++;				
				sent = Utility.getModifiedSent(sent);		
				Set<String> sent_TopTermVariants = new HashSet<String>();
				sent_TopTermVariants.addAll(Utility.getTermVariants_sent(sent, TopTerms_variants));
				
				if(sent_TopTermVariants.size()>0){					
					Set<String> ContextWords = ExtractContextWords(sent, sent_TopTermVariants);					
					if( ContextWords.size() > 0 ){ 
						CreateContext_To_TermMap(Utility.getLemmatizedWordSet(ContextWords), getCanonicalTermSet(sent_TopTermVariants));
						//System.out.println(i);
					}
				}
			}
		}
	}
	
				
	/** Calculates weights of all the context words identified and creates a mapping for the same. */
	private void Calculate_CW_Weight() {		
		for(Map.Entry<String, Set<String>> e : ContextWord_to_Term_Map.entrySet()){			
			double weight = e.getValue().size()/(double)TopTerms_Count;
			if(ContextWord_Map==null)
				ContextWord_Map = new HashMap<String, Double>();
			ContextWord_Map.put(e.getKey(), weight);	
		}	
		//System.out.println("Weights calculated");
	}

	
	private Set<String> getCanonicalTermSet(Set<String> variants) {
		Set<String> CValueTerms_Set = new HashSet<String>();
		for(String s: variants){
			CValueTerms_Set.add(tester.getIndex().retrieveTermCanonical(tester.getIndex().retrieveCanonicalOfTermVariant(s)));
		}		
		return CValueTerms_Set;
	}
	
	/** 
	 * Generates a mapping between the context word and the corresponding set of top terms with which they co-occur in the entire corpus. 
	 */
	private void CreateContext_To_TermMap(Set<String> contextWords,
			Set<String> terms) throws IOException {
		
		if(ContextWord_to_Term_Map==null){
			ContextWord_to_Term_Map=new HashMap<String,Set<String>>();
		}
		for(String context:contextWords){
			if(ContextWord_to_Term_Map.containsKey(context)){
				Set<String> CW_terms = ContextWord_to_Term_Map.get(context);           
                CW_terms.addAll(terms);
                ContextWord_to_Term_Map.put(context, CW_terms);
			}
			else{
				ContextWord_to_Term_Map.put(context, terms);
			}
		}
	}	

	/**
	 * Returns the set of context words identified in the input sentence with respect to the top candidate variants present in that particular sentence. 
	 */
	public Set<String> ExtractContextWords(String sent, Set<String> sent_variants) throws IOException {
		String[] tokens = NLPToolsControllerOpenNLP.getInstance().getTokeniser().tokenize(sent);
		String POSTags[] = NLPToolsControllerOpenNLP.getInstance().getPosTagger().tag(tokens);
		
		List<String> Noun_Tags = Arrays.asList("NN","NNS","NNP","NNPS");
		List<String> Verb_Tags = Arrays.asList("VB", "VBD", "VBG", "VBN", "VBP", "VBZ");
		List<String> Adj_Tags =  Arrays.asList("JJ");		
		
		Set<String> Sent_ContextWords = new HashSet<String>();
		
		outermostLoop:
		for(int i = 0; i<POSTags.length ; ){
			if(!Noun_Tags.contains(POSTags[i]) && !Verb_Tags.contains(POSTags[i]) && !Adj_Tags.contains(POSTags[i])) {
				i++;
				continue;
			}				
			else if(stoplist.isStopWord(tokens[i])){
				i++;
				continue;
			}
			
			else{
				//for single word terms
				if(sent_variants.contains(tokens[i])){
					i++;				
					continue;
				}
				else{
					int idx = i;
					int v_idx=0;
					variantLoop:						
						for(String variant: sent_variants) {
							v_idx++;
							if(variant.indexOf(tokens[idx])==-1){
								if(v_idx == sent_variants.size()){											
									Sent_ContextWords.add(tokens[idx]);	
									i++;
									continue outermostLoop;
								}
								continue variantLoop;								
							}
							else if(variant.startsWith(tokens[idx]) && variant.contains(" ")){
									
									String[] split_variant = variant.split(" ");
									
									int j;
									for(j=0; j<split_variant.length;){
										if(idx < tokens.length && split_variant[j].equals(tokens[idx])){
											idx++;
											j++;
											continue;											
										}
										else{
											idx=i;
											j=0;
											continue variantLoop;
										}											
									}
									if(j==split_variant.length){
										i+=j;
										continue outermostLoop;
									}
									else{
										Sent_ContextWords.add(tokens[i]);
										i++;
										continue outermostLoop;
									}										
								}																				
						}
						Sent_ContextWords.add(tokens[i]);
						i++;
						continue outermostLoop;					
				}				
			}			
		}	
		return Sent_ContextWords;		
	}	
	
}