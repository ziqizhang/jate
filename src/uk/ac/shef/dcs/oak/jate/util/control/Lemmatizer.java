package uk.ac.shef.dcs.oak.jate.util.control;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import uk.ac.shef.dcs.oak.jate.JATEProperties;

import java.util.HashMap;
import java.util.Map;
import java.util.StringTokenizer;

/**
 * Lemmatizer is a specific type of Normaliser and returns a string to its dictionary root.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */
public class Lemmatizer extends Normalizer {

	private EngLemmatiser lemmatizer;
	private Map<String, Integer> tagLookUp = new HashMap<String, Integer>();

	public Lemmatizer() {
		init();
	}

	/**
	 * @param value original word
	 * @param pos the part of speech of the last word
	 * @return the lemma of original word
	 */
	public String getLemma(String value, String pos) {
		int POS = tagLookUp.get(pos);
		if (POS == 0)
			return lemmatizer.lemmatize(value);
		else
			return lemmatizer.lemmatize(value, POS);
	}

	/**
	 * Lemmatise a phrase or word. If a phrase, only lemmatise the most RHS word.
	 * @param value
	 * @return
	 */
	public String normalize(String value) {
		if(value.indexOf(" ")==-1||value.endsWith(" s")||value.endsWith("'s")) //if string is a single word, or it is in "XYZ's" form where the ' char has been removed
			return lemmatizer.lemmatize(value,1).trim();

		String part1 = value.substring(0,value.lastIndexOf(" "));
		String part2 = lemmatizer.lemmatize(value.substring(value.lastIndexOf(" ")+1),1);
		return part1+" "+part2.trim();

	}

	/**
	 * Lemmatise every word in the input string
	 * @param in
	 * @return the lemmatised string
	 */
	public String normalizeContent(String in){
		StringBuilder sb = new StringBuilder();
		StringTokenizer tokenizer = new StringTokenizer(in);
		while(tokenizer.hasMoreTokens()){
			String tok=tokenizer.nextToken();
			sb.append(normalize(tok)).append(" ");
		}
		return sb.toString().trim();
	}


	private void init() {
		lemmatizer = new EngLemmatiser(JATEProperties.getInstance().getNLPPath()+"/lemmatizer", false, true);
		tagLookUp.put("NN", 1);
		tagLookUp.put("NNS", 1);
		tagLookUp.put("NNP", 1);
		tagLookUp.put("NNPS", 1);
		tagLookUp.put("VB", 2);
		tagLookUp.put("VBG", 2);
		tagLookUp.put("VBD", 2);
		tagLookUp.put("VBN", 2);
		tagLookUp.put("VBP", 2);
		tagLookUp.put("VBZ", 2);
		tagLookUp.put("JJ", 3);
		tagLookUp.put("JJR", 3);
		tagLookUp.put("JJS", 3);
		tagLookUp.put("RB", 4);
		tagLookUp.put("RBR", 4);
		tagLookUp.put("RBS", 4);
	}

}
