package uk.ac.shef.dcs.jate.core.algorithm;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.core.context.ContextExtraction;
import uk.ac.shef.dcs.jate.core.nlptools.NLPToolsControllerOpenNLP;
import uk.ac.shef.dcs.jate.model.Corpus;
import uk.ac.shef.dcs.jate.model.CorpusImpl;
import uk.ac.shef.dcs.jate.model.Document;
import uk.ac.shef.dcs.jate.model.Term;
import uk.ac.shef.dcs.jate.util.Utility;

/**
* An implementation of the NCValue term recognition algorithm. See Frantzi et. al 2000, <i>
* Automatic recognition of multi-word terms:. the C-value/NC-value method</i>
*/

public class NCValueAlgorithm implements Algorithm{
	
	private ContextExtraction contextExtraction;
	
	private Set<String> CValueTerms_Variants;
	
	private Map<String, Set<String>> Term_CW_Map;
	private Map<String, Integer> Term_CW_freqMap;
	
	private Map<String, Double> context_words;

	public NCValueAlgorithm(){
		CValueTerms_Variants = new HashSet<String>();
		Term_CW_Map = new HashMap<String,Set<String>>();
		/*Term_CW_freqMap maps the compound key (combination of term and the context word) to the frequency of co-occurrence of that term-context word pair.
		 * Assumption: The system should update the records.
		 * The beautiful system shall update the files.
		 * The frequency of co-occurence of term "system" and context word "update" is calculated as 2, whereby in one of the occurrence, "system" is found to be nested in another term "beautiful system".
		 *  */
		Term_CW_freqMap = new HashMap<String, Integer>();		
	}
	
	/**
	 * Initializes the private variables of the class.
	 */
	public void initialize(NCValueFeatureWrapper featureWrapper) throws IOException{
		this.contextExtraction = new ContextExtraction(featureWrapper.getTesterObject());		
		CValueTerms_Variants = contextExtraction.getTopTerms(100);		
		this.context_words = featureWrapper.getContextWordsMap();
	}
	
	
	/** Each candidate term from the C-value list appears in the corpus with a set of context words (may be an adjective, noun or a verb).
	 * This function identifies the context words with respect to all the candidates identified in the C-Value result set. 
	*/
	private void ContextIdentification_Terms() throws IOException {
		Corpus corpus = new CorpusImpl(JATEProperties.getInstance().getCorpusPath());
		for (Document d : corpus) {
			sentLoop:
			for (String sent: NLPToolsControllerOpenNLP.getInstance().getSentenceSplitter().sentDetect(d.getContent())) {
				//System.out.println(sent)	;			
				sent = Utility.getModifiedSent(sent);
				
				Set<String> sent_CValueTermVariants = new HashSet<String>();
				sent_CValueTermVariants.addAll(Utility.getTermVariants_sent(sent, CValueTerms_Variants));				
				
				if(sent_CValueTermVariants.size()>0){					
					Set<String> ContextWords = new HashSet<String>();
					ContextWords = contextExtraction.ExtractContextWords(sent, sent_CValueTermVariants);
					Map<String,Integer> freqMap = CalculateFrequency(sent, ContextWords);
					Map<String,Integer> freqMap_copy = new HashMap<String, Integer>();
					freqMap_copy.putAll(freqMap);
					Iterator<Entry<String, Integer>> it = freqMap_copy.entrySet().iterator();
					while (it.hasNext())
					{
					   Entry<String,Integer> e = it.next();
					   String lemmatized_Context = Utility.getLemma(e.getKey());
						if(lemmatized_Context!= null && !lemmatized_Context.equals(e.getKey())){
							freqMap.put(lemmatized_Context, e.getValue());
							freqMap.remove(e.getKey());				
						}					   
					}	
					
					for(String term : sent_CValueTermVariants){
						term = Utility.getLemma(term);
						if(freqMap.size() <= 0)
							continue sentLoop;
						/*
						if(!Term_CW_freqMap.containsKey(term)){
							Term_CW_freqMap.put(term, freqMap);
						}
						*/
						//else {
							for(String contextWord:freqMap.keySet()){
								/*if(Term_CW_freqMap.get(term).keySet().contains(contextWord)){
									Term_CW_freqMap.get(term).put(contextWord, Term_CW_freqMap.get(term).get(contextWord) + freqMap.get(contextWord));
								}
								else {
									Term_CW_freqMap.get(term).put(contextWord, freqMap.get(contextWord));
								}
							}*/	
								if(!Term_CW_Map.containsKey(term)){
									Set<String> contextWordSet = new HashSet<String>();
									contextWordSet.add(contextWord);
									Term_CW_Map.put(term, contextWordSet);
								}
								else{
									Term_CW_Map.get(term).add(contextWord);
								}
								
								if(!Term_CW_freqMap.containsKey(term+"+"+contextWord)){
									Term_CW_freqMap.put(term+"+"+contextWord, freqMap.get(contextWord));
								}
								else{
									Term_CW_freqMap.put(term+"+"+contextWord,Term_CW_freqMap.get(term+"+"+contextWord)+freqMap.get(contextWord));
									
								}						
								
						}
					}		
				}
			}
		}		
	}
	
	
	/*Calculates the frequency of context words in a sentence.*/

	private  Map<String, Integer> CalculateFrequency(String sent, Set<String> contextWords) {
		Map<String,Integer> freqMap = new HashMap<String,Integer>();
		// TODO Auto-generated method stub
		if(contextWords.size()<=0){
			return freqMap;
		}
		
		String[] split_sent = sent.split(" ");		
		for(String word: contextWords){
			int count = 0;
			for(String s :split_sent){
				if(word.equals(s))
					count++;
			}
			if(freqMap.containsKey(word)){
				freqMap.put(word, freqMap.get(word)+count);
			}
			else
				freqMap.put(word, count);
		}			
		return freqMap;
	}
	
	/**
	 * @param store
	 * @return Term[]
	 * @throws uk.ac.shef.dcs.jate.JATEException
	 */
	public Term[] execute(AbstractFeatureWrapper store) throws JATEException{
		if (!(store instanceof NCValueFeatureWrapper)) throw new JATEException("" +
				"Required: NCValueFeatureWrapper");
		NCValueFeatureWrapper ncFeatureStore = (NCValueFeatureWrapper) store;
		
		try {
			initialize(ncFeatureStore);
			ContextIdentification_Terms();
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		
		return CalculateNCValue();		
	}

	/**
	 * Calculates the NC-Value of all the C-Value candidate terms and re-orders them in the decreasing order of their NC-Value score. 
	 * */	
	private Term[] CalculateNCValue() {
		// TODO Auto-generated method stub
		Term[] CValueTerms = contextExtraction.getTerms();
		
		Set<Term> result = new HashSet<Term>();
		
		for(Term term : CValueTerms){
			double summation = 0.0;
			
			//Map<String,Integer> CW_map = Term_CW_freqMap.get(term.getConcept());
			
			/*if(CW_map !=null)
			{
				for(Map.Entry<String, Integer> e : CW_map.entrySet()){
					double weight_e;
					if(context_words.containsKey(e.getKey()))
						weight_e = context_words.get(e.getKey());					
					else
						weight_e = 0.0;		// For the context words that are not present in the list of context words identified in phase 2 are assigned a weight 0.0
					summation += weight_e * e.getValue();		
				}
			}
			*/
			
			Set<String> ContextWords = Term_CW_Map.get(term.getConcept());
			
			if(ContextWords != null){
				for(String C_word:ContextWords){
					double weight_e;
					if(context_words.containsKey(C_word))
						weight_e = context_words.get(C_word);					
					else
						weight_e = 0.0;		// For the context words that are not present in the list of context words identified in phase 2 are assigned a weight 0.0
					summation += weight_e * Term_CW_freqMap.get(term.getConcept()+"+"+C_word);				
					
				}
			}
				
			
			double score = 0.8*term.getConfidence()+ 0.2 * summation;
			result.add(new Term(term.getConcept(), score));				
		}
		
		Term[] all = result.toArray(new Term[0]);
		Arrays.sort(all);
		return all;	
		
	}
	
	public String toString(){
		return "NCValue_ALGORITHM";
	}

}



