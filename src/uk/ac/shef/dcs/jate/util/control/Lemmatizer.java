package uk.ac.shef.dcs.jate.util.control;

import net.didion.jwnl.JWNLException;
import opennlp.tools.coref.mention.JWNLDictionary;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.io.IOException;
import java.util.StringTokenizer;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Lemmatizer is a specific type of Normaliser and returns a string to its dictionary root.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */
public class Lemmatizer extends Normalizer {

    /**
     * The current OPENNLP implements a dictionary based lemmatizer. This lemmatizer seems to cause problem when the string
     * contains white space, separator characters such as '-'. It simply removes the part before the separator char
     * and only lemmatizes the remaining part and returns the result (e.g., pre-schoolers => schooler).
     * So the pre-part is missing. To work around this problem we firstly check if an input string contains potential
     * token separators. If so, the string is split at the *last* token separator into two parts. The first part
     * is kept as-is, the last part is lemmatized. Then the result is reconstructed by the first part plus the
     * lemmatized last part.
     *
     * This constant is a pattern that matches potential token separator used by the opennlp dictionary based lemmatizer
     */
    protected static final Pattern JWNLDICTIONARY_POSSIBLE_TOKEN_SEPARATOR = Pattern.compile("[\\-\\.\\s+]");
    private JWNLDictionary dict;

	public Lemmatizer() throws IOException, JWNLException {
		init("wordnet_dict");
	}
	
        public Lemmatizer(String wordnetDict) throws IOException, JWNLException {
            init(wordnetDict);
    }

	/**
	 * @param word a single word
	 * @param pos the part of speech of the word
	 * @return the lemma of original word
	 */
	private String getLemma(String word, String pos) {
        String[] norms=dict.getLemmas(word.toLowerCase(),pos);
        if(norms.length==0)
            return word;
        return norms[0];

	}

    /**
     * The current OPENNLP implements a dictionary based lemmatizer. This lemmatizer seems to cause problem when the string
     * contains white space, separator characters such as '-'. It simply removes the part before the separator char
     * and only lemmatizes the remaining part and returns the result (e.g., pre-schoolers => schooler).
     * So the pre-part is missing. To work around this problem we firstly check if an input string contains potential
     * token separators. If so, the string is split at the *last* token separator into two parts. The first part
     * is kept as-is, the last part is lemmatized. Then the result is reconstructed by the first part plus the
     * lemmatized last part.
     *
     *
	 * @param value
	 * @return
	 */
	public String normalize(String value) {
        if(value.length()==0)
            return value;

        value=value.toLowerCase();
        int position = findJWNLDictionaryTokenSeparator(value);
        if(position==0){
            return getLemma(value,"NNP");
        }
        else{
            String part1 = value.substring(0,position);
            String part2 = value.substring(position);
            if(part2.length()>0) //should always be true otherwise somewhere there is a bug
                part2 = getLemma(part2,"NNP");
            return part1+part2.trim();
        }

		/*if(position==-1||value.endsWith(" s")||value.endsWith("'s")) //if string is a single word, or it is in "XYZ's" form where the ' char has been removed
			return getLemma(value,"NNP");

		String part1 = value.substring(0,value.lastIndexOf(" "));
		String part2 = getLemma(value.substring(value.lastIndexOf(" ")+1),"NNP");
		return part1+" "+part2.trim();*/

	}

    /**
     * The current OPENNLP implements a dictionary based lemmatizer. This lemmatizer seems to cause problem when the string
     * contains white space, separator characters such as '-'. It simply removes the part before the separator char
     * and only lemmatizes the remaining part and returns the result (e.g., pre-schoolers => schooler).
     * So the pre-part is missing. To work around this problem we firstly check if an input string contains potential
     * token separators. If so, the string is split at the *last* token separator into two parts. The first part
     * is kept as-is, the last part is lemmatized. Then the result is reconstructed by the first part plus the
     * lemmatized last part.
     *
     * This method returns the offset of the *last* possible separator char in a string
     *
     * @param inputString
     * @return
     */
    protected int findJWNLDictionaryTokenSeparator(String inputString){
        Matcher m = JWNLDICTIONARY_POSSIBLE_TOKEN_SEPARATOR.matcher(inputString);

        int position=0;
        while(m.find()){
            position= m.end();
        }
        return position;
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


	private void init(String wordnetDict) throws IOException, JWNLException {
		dict = new JWNLDictionary(JATEProperties.getInstance().getNLPPath() + "/" + wordnetDict);
	}

}
