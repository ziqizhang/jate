package uk.ac.shef.dcs.jate.core.extractor;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.core.nlptools.NLPToolsControllerOpenNLP;
import uk.ac.shef.dcs.jate.model.Corpus;
import uk.ac.shef.dcs.jate.model.Document;
import uk.ac.shef.dcs.jate.util.control.Normalizer;
import uk.ac.shef.dcs.jate.util.control.StopList;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Nounphrase extractor implemented with OpenNLP tools. It applies certain heuristics to clean a candidate noun phrase and
 * return it to the normalised root. These heuristics include:
 * <br>-Stopwords will be trimmed from the head and tails of a phrase. E.g, "the cat on the mat" becomes "cat on the mat".
 * <br>-phrases containing "or" "and" will be split, e.g., "Tom and Jerry" becomes "tom" "jerry"
 * <br>-must have letters
 * <br>-must have at least two characters
 * <br>-characters that do not match the pattern [a-zA-Z\-] are replaced with whitespaces.
 * <br>-may or may not have digits, this is set by the property file
 * <br>-must contain no more than N words, this is set by the property file
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class NounPhraseExtractorOpenNLP extends CandidateTermExtractor {

	
	
	private static Logger _logger = Logger.getLogger(NounPhraseExtractorOpenNLP.class.getName());
 
    /**
     * Creates an instance with specified stopwords list and norm
     *
     * @param stop
     * @param normaliser
     * @throws IOException
     */
    public NounPhraseExtractorOpenNLP(StopList stop, Normalizer normaliser) throws IOException {
        _stoplist = stop;
        _normaliser = normaliser;
        
    }

    public Map<String, Set<String>> extract(Corpus c) throws JATEException {
        Map<String, Set<String>> res = new HashMap<String, Set<String>>();
        for (Document d : c) {
            _logger.info("Extracting candidate NP... From Document " + d);
            for (Map.Entry<String, Set<String>> e : extract(d).entrySet()) {
                Set<String> variants = res.get(e.getKey());
                variants = variants == null ? new HashSet<String>() : variants;
                variants.addAll(e.getValue());
                res.put(e.getKey(), variants);
            }
        }
        return res;
    }
    
    
    //modified part begins..the first function only commented and the rest are new implementations

    public Map<String, Set<String>> extract(Document d) throws JATEException {
        Map<String, Set<String>> res = new HashMap<String, Set<String>>();
        try {
            for (String s : NLPToolsControllerOpenNLP.getInstance().getSentenceSplitter().sentDetect(d.getContent())) {
                for (Map.Entry<String, Set<String>> e : extract(s).entrySet()) {
                    Set<String> variants = res.get(e.getKey());
                    variants = variants == null ? new HashSet<String>() : variants;
                    variants.addAll(e.getValue());
                    res.put(e.getKey(), variants);
                }
            }
        } catch (IOException e) {
            throw new JATEException(e);
        }
       
        return res;
    }
    
    
    

   
    /*
    public Map<String, Set<String>> extract(Document d) throws JATEException {
        Map<String, Set<String>> res = new HashMap<String, Set<String>>();
        
        String[] candidateTerms = d.getContent().split("\\r?\\n"); 
        for (Map.Entry<String, Set<String>> e : extract(candidateTerms).entrySet()) {
        	Set<String> variants = res.get(e.getKey());
            variants = variants == null ? new HashSet<String>() : variants;
            variants.addAll(e.getValue());
            res.put(e.getKey(), variants);
        }       
        return res;
    }
    
    */


    public Map<String, Set<String>> extract(String[] candidates) throws JATEException {
        Map<String, Set<String>> nouns = new HashMap<String, Set<String>>(); 
        for (String c : candidates) {
        	
        	//modified code begins
        	c = applyCharacterReplacement(c,JATEProperties.REGEX_QUOTES);
            /*    c = applyCharacterReplacement(c, JATEProperties.TERM_CLEAN_PATTERN);
                String[] e = applySplitList(c);

                for (String str : e) {
                String str=c;
                   String stopremoved = applyTrimStopwords(str, _stoplist, _normaliser);
                    if (stopremoved == null) continue;
                */
        	String original = c;
            c = _normaliser.normalize(c.toLowerCase()).trim();
            if( c.equals(""))
            	continue;

                   /* String[] nelements = str.split("\\s+");
                    if (nelements.length < 1 || nelements.length >
                            Integer.valueOf(JATEProperties.getInstance().getMaxMultipleWords()))
                        continue;
                    if (JATEProperties.getInstance().isIgnoringDigits() &&
                            containsDigit(str))
                        continue;
                    if (!containsLetter(str)) continue;
                    if (!hasReasonableNumChars(str)) continue;				//needed or not?
*/
            //modified code ends
            if (c.toLowerCase().indexOf(c) != -1) {
            	Set<String> variants = nouns.get(c);
                variants = variants == null ? new HashSet<String>() : variants;
                variants.add(original);
                nouns.put(c, variants);
            }              
        }        
        return nouns;
    }
    
    
    
    
    
    //modified part ends
    
    


    public Map<String, Set<String>> extract(String content) throws JATEException {
      //  System.out.println(content+ "||" );
    	Map<String, Set<String>> nouns = new HashMap<String, Set<String>>();
        try {
            String[] tokens = NLPToolsControllerOpenNLP.getInstance().getTokeniser().tokenize(content);
            String[] pos = NLPToolsControllerOpenNLP.getInstance().getPosTagger().tag(tokens);
            String[] candidates = chunkNPs(tokens, pos);
            for (String c : candidates) {
                c = applyCharacterReplacement(c, JATEProperties.TERM_CLEAN_PATTERN);
                String[] e = applySplitList(c);

                for (String str : e) {
                    String stopremoved = applyTrimStopwords(str, _stoplist, _normaliser, true,true);
                    if (stopremoved == null) continue;
                    String original = stopremoved;
                    str = _normaliser.normalize(stopremoved.toLowerCase()).trim();

                    String[] nelements = str.split("\\s+");
                    if (nelements.length < 1 || nelements.length >
                            Integer.valueOf(JATEProperties.getInstance().getMaxMultipleWords()))
                        continue;
                    if (JATEProperties.getInstance().isIgnoringDigits() &&
                            containsDigit(str))
                        continue;
                    if (!containsLetter(str)) continue;
                    if (!hasReasonableNumChars(str)) continue;

                    if (c.toLowerCase().indexOf(str) != -1) {
                    	//System.out.print(original+"|");
                    	
                        Set<String> variants = nouns.get(str);
                        variants = variants == null ? new HashSet<String>() : variants;
                        variants.add(original);
                        nouns.put(str, variants);
                    }
                }
            }
          //  System.out.println("\n");
        } catch (IOException wte) {
            throw new JATEException(wte);
        }
       
        return nouns;
    }

    private String[] chunkNPs(String[] tokens, String[] pos) throws IOException {
        String[] phrases = NLPToolsControllerOpenNLP.getInstance().getPhraseChunker().chunk(tokens, pos);
       /* for(int i=0;i<phrases.length;i++){
        	System.out.println(tokens[i]+ " "+ phrases[i]);
        }
        */
        List<String> candidates = new ArrayList<String>();
        String phrase = "";
        for (int n = 0; n < tokens.length; n++) {
            if (phrases[n].equals("B-NP")) {
                phrase = tokens[n];
                for (int m = n + 1; m < tokens.length; m++) {
                    if (phrases[m].equals("I-NP")) {
                        phrase = phrase + " " + tokens[m];
                    } else {
                        n = m;
                        break;
                    }
                }
                phrase = phrase.replaceAll("\\s+", " ").trim();
                if (phrase.length() > 0)
                    candidates.add(phrase);

            }
        }
        return candidates.toArray(new String[0]);
    }

   

}