package uk.ac.shef.dcs.jate.util.control;

import net.didion.jwnl.JWNLException;
import opennlp.tools.coref.mention.JWNLDictionary;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.io.IOException;
import java.util.StringTokenizer;

/**
 * Lemmatizer is a specific type of Normaliser and returns a string to its dictionary root.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */
public class Lemmatizer extends Normalizer {

    private JWNLDictionary dict;

	public Lemmatizer() throws IOException, JWNLException {
		init();
	}

	/**
	 * @param word a single word
	 * @param pos the part of speech of the word
	 * @return the lemma of original word
	 */
	public String getLemma(String word, String pos) {
        String[] norms=dict.getLemmas(word.toLowerCase(),pos);
        if(norms.length==0)
            return word;
        return norms[0];

	}

	/**
	 * Lemmatise a phrase or word. If a phrase, only lemmatise the most RHS word.
	 * @param value
	 * @return
	 */
	public String normalize(String value) {
		if(value.indexOf(" ")==-1||value.endsWith(" s")||value.endsWith("'s")) //if string is a single word, or it is in "XYZ's" form where the ' char has been removed
			return getLemma(value,"NNP");

		String part1 = value.substring(0,value.lastIndexOf(" "));
		String part2 = getLemma(value.substring(value.lastIndexOf(" ")+1),"NNP");
		return part1+" "+part2.trim();

	}

	/**
	 * Lemmatise every word in the input string
	 * @param in
	 * @return the lemmatised string
	 */
	public String normalizeContent(String in){
		StringBuilder sb = new StringBuilder();
		StringTokenizer tokenizer = new StringTokenizer(in.toLowerCase());
		while(tokenizer.hasMoreTokens()){
			String tok=tokenizer.nextToken();
			sb.append(normalize(tok)).append(" ");
		}
		return sb.toString().trim();
	}


	private void init() throws IOException, JWNLException {
		dict = new JWNLDictionary(JATEProperties.getInstance().getNLPPath()+"/wordnet_dict");
	}

}
