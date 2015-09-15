package uk.ac.shef.dcs.jate.deprecated.core.extractor;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.deprecated.JATEProperties;
import uk.ac.shef.dcs.jate.deprecated.core.nlptools.NLPToolsControllerOpenNLP;
import uk.ac.shef.dcs.jate.deprecated.model.Corpus;
import uk.ac.shef.dcs.jate.deprecated.model.Document;
import uk.ac.shef.dcs.jate.deprecated.util.control.Normalizer;
import uk.ac.shef.dcs.jate.deprecated.util.control.StopList;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;


public class PhraseExtractor {
	protected StopList _stoplist;
	protected Normalizer _normaliser;

    private boolean _removeStop = true;
    private int _minCharsInWord = 2;

    /**
     * Creates an instance with specified stopwords list and normaliser.
     * By default, stopwords are ignored; words that have less than 2 characters
     * are ignored.
     *
     * @param stop       a list of words which are unlikely to occur in a domain specific candidate term
     * @param normaliser an instance of a Normalizer which returns candidate term to canonical form
     */
    public PhraseExtractor(StopList stop, Normalizer normaliser) {
        _stoplist = stop;
        _normaliser = normaliser;
    }

    /**
     * Creates an instance with specified stopwords list and normaliser.
     *
     * @param stop       a list of words which are unlikely to occur in a domain specific candidate term
     * @param normaliser an instance of a Normalizer which returns candidate term to canonical form
     * @param removeStop whether stop words should be ignored in the extracted words
     * @param minCharsInWord words that contain less than this number of characters (non-white space) are ignored in the
     * extracted words
     */
    public PhraseExtractor(StopList stop, Normalizer normaliser, boolean removeStop, int minCharsInWord) {
        _stoplist = stop;
        _normaliser = normaliser;
        _removeStop=removeStop;
        _minCharsInWord=minCharsInWord;
    }

    public List<String> extract(Corpus c) throws JATEException {
        List<String> res = new ArrayList<String>();
        for (Document d : c) {
            res.addAll(extract(d.getContent())) ;
        }
        return res;
    }

   

    public List<String> extract(String content) throws JATEException {
        List<String> phrases = new ArrayList<String>();
        try
        {
        	String[] words = NLPToolsControllerOpenNLP.getInstance().getTokeniser().tokenize(content);
        	
		    int wordCounter = 0;
	
		    StringBuilder sb = new StringBuilder();
		        
		    for (String temp_w : words) {
		        	
			    String w = CandidateTermExtractor.applyCharacterReplacement(temp_w, JATEProperties.TERM_CLEAN_PATTERN_RAKE);
			    String nw = w.trim().toLowerCase();
			    
			    /* To handle the case where tokenizer tokenizes the word "i.e." as 'i', '.e', '.'.
			     * Thus, 'i' being 1 character in length is rejected as a candidate term but, '.e' is not, and therefore, appears as a candidate term.
			     **/
			    if(nw.equals(".e")){
			    	continue;
			    }
			    wordCounter++;
			    
			    /*To handle the case of two consecutive sentences such as: ...using FAST. Display the content... 
			     * Here, tokenizer recognizes 'FAST.' as a token and 'Display' as another token ie, it fails to recognize 'FAST' and '.' as two different tokens.. 
			     * Thus, while generating the candidates, it is to made sure that 'Display' and 'FAST' are in two different candidates as a full stop appears in between them in the corpus.
			     * */
			    
			    if(sb.length() > 0 && sb.toString().endsWith(".")){
			    	phrases.add(_normaliser.normalize(sb.toString().replace('.', ' ').trim()).trim());
			    	sb.setLength(0);
			    }
			    
			    /*To handle the case of Apostophe s for eg. tokenizer genarates two tokens, namely "user" and "'s" for the word "user's".
			     * Thus, it is to be made sure that in the candidate, there exists no space in between "user" and "'s" 
			     * */
			    
			    if(nw.startsWith("'")){
			    	sb.append(nw);
			    	continue;
			    }		    
			    
			           
			    if ((!containsLetter(nw) && !containsDigit(nw))||(nw.length() < _minCharsInWord)||(_removeStop && (_stoplist.isStopWord(nw) || _stoplist.isStopWord(w.trim())))) 
			    {
			    	if(sb.length()==0)
			    		continue;
			        else
			        {     		        		
			            phrases.add(_normaliser.normalize(sb.toString().trim()).trim());
			            sb.setLength(0);
			         }            		            		
			     }
			    
			     else
			     {
			    	 sb.append(" " + nw);
			     }
			            
			     if(wordCounter == words.length)
			     {
			    	 if (sb.length() > 0) {	
			    		 phrases.add(_normaliser.normalize(sb.toString().trim()).trim());
			    		 sb.setLength(0);
			    	 }                 
			 	}	        
		    } 
        }catch (IOException e) {
            throw new JATEException(e);
        }
        return phrases;
    }
    
    public static boolean containsLetter(String string) {
		char[] chars = string.toCharArray();
		for (char c : chars) {
			if (Character.isLetter(c)) return true;
		}
		return false;
	}
    
    public static boolean containsDigit(String string) {
		for (char c : string.toCharArray()) {
			if (Character.isDigit(c)) return true;
		}
		return false;
	}
    
}
