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
}
