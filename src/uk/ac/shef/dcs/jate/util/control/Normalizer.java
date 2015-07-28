package uk.ac.shef.dcs.jate.util.control;

/**
 * <p>
 * Normalizer returns text units to its canonical forms
 * </p>
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */
public abstract class Normalizer {

	/**
	 * Normalise only the RHS head word of the input text unit
	 *
	 * @param unit the variant form of a single text unit, e.g., word, phrase
	 * @return the normalised canonical form of input
	 */
	public abstract String normalize(String unit);

	/**
	 * Normalise every token found in the input content, assuming tokens are delimited by a whitespace character.
	 * @param content
	 * @return
	 */
	public abstract String normalizeContent(String content);
	
	public static String basicNormalize(String value)
	{
	    if (value.length() == 0) {
	        return value;
	    }
	    //to lower case
	    value = value.toLowerCase();
	    //FR cleaning, remove begin : d', l'
	    value = value.replaceAll("^[l|d]{1}[ ]{0,1}[']{1}", "");
	    //EN cleaning, remove end : 's, 't, 'd,
	    value = value.replaceAll("[']{1}[s|t|d]{1}$", "");
	    //COMMON cleaning, remove punctuation at end of a word
	    value = value.replaceAll("[.'%-]+$", "");
	    //remove unnecessary spaces
	    value = value.replace("- ", "");
	    value = value.replace(" -", "");
	    //trim to remove unnecessary spaces
	    value = value.trim();
	    return value;
	}
}
