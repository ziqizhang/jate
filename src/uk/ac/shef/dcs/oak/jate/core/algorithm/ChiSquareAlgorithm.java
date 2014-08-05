package uk.ac.shef.dcs.oak.jate.core.algorithm;

import java.io.IOException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.JATEProperties;
import uk.ac.shef.dcs.oak.jate.core.nlptools.NLPToolsControllerOpenNLP;
import uk.ac.shef.dcs.oak.jate.model.Corpus;
import uk.ac.shef.dcs.oak.jate.model.CorpusImpl;
import uk.ac.shef.dcs.oak.jate.model.Document;
import uk.ac.shef.dcs.oak.jate.model.Term;
import uk.ac.shef.dcs.oak.jate.util.Utility;
import uk.ac.shef.dcs.oak.jate.util.control.Lemmatizer;
import uk.ac.shef.dcs.oak.jate.util.control.StopList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
/**
 * An implementation of the Chi Square algorithm. See Matsuo, Y., Ishizuka, M. </i>
 * Keyword Extraction from a Single Document Using Word Co-Occurrence Statistical Information. </i>
 * Proc. 16th Intl. Florida AI Research Society, 2003, 392-396.
 */

public class ChiSquareAlgorithm implements Algorithm {
	
	private Map<String, Integer> cooccurence_map;	// freq(w,g) calculation => key:"term+frequentTerm" value: co-occurence freq 
	private Map<String, Set<String>> cooccurence_list; // nw and pg calculation => key:"lemmatized term" value:"set of terms in sentences where term appears"
	
	private Map<String, Set<String>> frequentTerms_variants_Map;
	private Set<String> all_terms_variants; 
	private List<Term> ngrams;
	private ChiSquareFeatureWrapper chiFeatureStore;
	StopList stoplist;
	Lemmatizer lemmatizer;
    private String inputCorpus;

	public ChiSquareAlgorithm(String input) throws IOException{
		ngrams = new ArrayList<Term>();
		cooccurence_map = new HashMap<String, Integer>();
		frequentTerms_variants_Map = new HashMap<String, Set<String>>() ;
		all_terms_variants = new HashSet<String>();
		cooccurence_list= new HashMap<String, Set<String>>();
		stoplist = new StopList(true);
		lemmatizer = new Lemmatizer();
        this.inputCorpus=input;
	}
	
	/**
	 * Identifies the variants of 'frequent' candidate terms from the Simple Frequency Alogrithm execution results.
	 * @throws IOException 
	 */
	public void getFrequentTerms() throws IOException{
		//Anurag for file
		//PrintWriter writer = new PrintWriter("the-file-name.txt", "UTF-8");
		FileWriter file;
		try {
			file = new FileWriter("n-gram_output.txt");
			BufferedWriter writer = new BufferedWriter(file);
			int percent_TopTerms = JATEProperties.getInstance().getPercentage();
			Term[] candidates = chiFeatureStore.getCandidateTerms();
			for(Term t: candidates){
				if(t.getConfidence() >= JATEProperties.getInstance().getMinFreq()) {
					ngrams.add(t);
					//System.out.println(t.getConcept() + " + " + t.getConfidence());
				}
			
				//Anurag to print the n-grams
				//System.out.println("The n--gram is:" + t.getConcept());
				writer.write(t.getConcept());
				writer.newLine();
			}
				
				int FrequentTerms_Count = percent_TopTerms*ngrams.size()/100;
				Term[] Frequent_Terms = Arrays.copyOf(ngrams.toArray(new Term[0]),FrequentTerms_Count);
				for(Term term: Frequent_Terms){
					Set<String> FrequentTerms_variants =  new HashSet<String>();
					FrequentTerms_variants.addAll(chiFeatureStore.getVariants(term.getConcept()));
					frequentTerms_variants_Map.put(term.getConcept(), FrequentTerms_variants);
				}
			
			writer.close();
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	      
			

	}
	
	/**
	 * Adds the variants of all the n-grams to the variable all_terms_variants.
	 */
	public void getAllTermsVariants(){
		for(Term term:ngrams){
			all_terms_variants.addAll(chiFeatureStore.getVariants(term.getConcept()));
		}		
	}
	
	/**
	 * Generates the maps of Co-occurence of a term and the frequent terms.
	 */
	public void generateMaps(Corpus corpus) throws IOException{
		getFrequentTerms();
		getAllTermsVariants();
		
		for (Document d : corpus) {
			for (String sent: NLPToolsControllerOpenNLP.getInstance().getSentenceSplitter().sentDetect(d.getContent())) {			
				sent = Utility.getModifiedSent(sent);				
				Set<String> sent_TermVariants = new HashSet<String>();
				Set<String> sent_freq_terms_lemma = new HashSet<String>();
				Set<String> sent_terms_lemma = new HashSet<String>();
				sent_TermVariants.addAll(Utility.getTermVariants_sent(sent, all_terms_variants));
				
				if(sent_TermVariants.size()<0){					
					continue;
				}
				
				Set<String> sent_TermLemma = new HashSet<String>();
				for(String sent_variant: sent_TermVariants){
					
					String lemma = 
							Utility.getLemmaChiSquare(sent_variant, stoplist, lemmatizer);
					
					if (lemma != null)
					{
						sent_TermLemma.add(lemma);
						
						if(frequentTerms_variants_Map.keySet().contains(lemma)){
							sent_freq_terms_lemma.add(lemma);
						}
						else{
							sent_terms_lemma.add(lemma);
						}
					}
					
				}
				
				for(String freqTerm: sent_freq_terms_lemma){
					for(String term: sent_terms_lemma){
						if(cooccurence_map.containsKey(freqTerm+"+"+term)){
							cooccurence_map.put(freqTerm+"+"+term, cooccurence_map.get(freqTerm+"+"+term) + 1);
						}
						else{
							cooccurence_map.put(freqTerm+"+"+term, 1);
						}
					}					
				}
				
				for(String freqTerm: sent_freq_terms_lemma){
					for(String term: sent_freq_terms_lemma){
						if(freqTerm.equals(term))
							continue;
						if(cooccurence_map.containsKey(freqTerm+"+"+term)){
							cooccurence_map.put(freqTerm+"+"+term, cooccurence_map.get(freqTerm+"+"+term) + 1);
						}
						else{
							cooccurence_map.put(freqTerm+"+"+term, 1);
						}
					}					
				}
				
				for(String lemma : sent_TermLemma){
					@SuppressWarnings("unchecked")
					Set<String> temp = (Set<String>)((HashSet<String>)sent_TermLemma).clone();
					temp.remove(lemma);
					
					if(!cooccurence_list.containsKey(lemma))
						cooccurence_list.put(lemma,temp);
					else
						cooccurence_list.get(lemma).addAll(temp);	
				}
				
			}			
			for(Map.Entry<String, Integer> e : cooccurence_map.entrySet()){
				//System.out.println(e.getKey()+ " " + e.getValue());
			}
		}
	}
	
	/**
	 * @param store
	 * @return Term[]
	 * @throws uk.ac.shef.dcs.oak.jate.JATEException
	 */
	@Override
	public Term[] execute(AbstractFeatureWrapper store) throws JATEException {
		// TODO Auto-generated method stub
		
		if (!(store instanceof ChiSquareFeatureWrapper)) throw new JATEException("" +
				"Required: ChiSquareFeatureWrapper");
		chiFeatureStore = (ChiSquareFeatureWrapper) store;
	
		try{
			generateMaps(new CorpusImpl(getInputCorpus()));
		}
		catch(IOException e){
			e.printStackTrace();
		}
		
		Set<Term> result = new HashSet<Term>();
		
		int freqwg;
		double nw, pg;
		double w_chiSqr;
		double max_chiSqValue;
		for(Entry<String, Set<String>> w: cooccurence_list.entrySet()){
			w_chiSqr = 0.0;
			nw = cooccurence_list.get(w.getKey()).size(); 
			max_chiSqValue = 0.0;
			
			for(Entry<String, Set<String>> g : frequentTerms_variants_Map.entrySet()){
				if(w.getKey().equals(g.getKey()))
					continue;
				
				if(cooccurence_map.containsKey(g.getKey()+"+"+w.getKey()))
						freqwg = cooccurence_map.get(g.getKey()+"+"+w.getKey());
				else
					freqwg = 0;
				pg = cooccurence_list.get(g.getKey()).size()/(double)ngrams.size();   //this line causes nullpointer exception
				
				w_chiSqr += Math.pow(freqwg - (nw*pg), 2)/(nw*pg);
				if(max_chiSqValue < Math.pow(freqwg - (nw*pg), 2)/(nw*pg))
					max_chiSqValue = Math.pow(freqwg - (nw*pg), 2)/(nw*pg);
			}
			w_chiSqr -= max_chiSqValue;
			result.add(new Term(w.getKey(), w_chiSqr));
		}			
		Term[] all = result.toArray(new Term[0]);		
		Arrays.sort(all);
		return all;
	}
	
	public String toString(){
		return "ChiSquare_ALGORITHM";
	}

    public String getInputCorpus() {
        return inputCorpus;
    }

    public void setInputCorpus(String inputCorpus) {
        this.inputCorpus = inputCorpus;
    }
}
