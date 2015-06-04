package uk.ac.shef.dcs.jate.core.extractor;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.Corpus;
import uk.ac.shef.dcs.jate.util.control.Normalizer;
import uk.ac.shef.dcs.jate.util.control.StopList;
import uk.ac.shef.dcs.jate.model.Document;

import java.util.Map;
import java.util.Set;

/**
 * Extract lexical units from texts.  Also defines a number of utility methods for normalizing extracted candidate terms.
 * A candidate lexical unit will be stored in its canonical form in lowercase, depending on the Normalizer provided. Each
 * canonical form maps to several variants found in the document/corpus. When the frequency of terms is counted, each variant
 * will be searched in the document/corpus and the frequency adds up to the total frequency for the canonical term form.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public abstract class CandidateTermExtractor {
    protected StopList _stoplist;
    protected Normalizer _normaliser;

    /**
     * @param c corpus
     * @return a map containing mappings from term canonical form to its variants found in the corpus
     * @throws uk.ac.shef.dcs.jate.JATEException
     *
     */
    public abstract Map<String, Set<String>> extract(Corpus c) throws JATEException;

    /**
     * @param d document
     * @return a map containing mappings from term canonical form to its variants found in the document
     * @throws uk.ac.shef.dcs.jate.JATEException
     *
     */
    public abstract Map<String, Set<String>> extract(Document d) throws JATEException;

    /**
     * @param content a string
     * @return a map containing mappings from term canonical form to its variants found in the string
     * @throws uk.ac.shef.dcs.jate.JATEException
     *
     */
    public abstract Map<String, Set<String>> extract(String content) throws JATEException;

    /**
     * @param string a string
     * @return true if the string contains letter; false otherwise
     */
    public static boolean containsLetter(String string) {
        char[] chars = string.toCharArray();
        for (char c : chars) {
            if (Character.isLetter(c)) return true;
        }
        return false;
    }

    /**
     * @param string
     * @return true if the string contains digit; false otherwise
     */
    public static boolean containsDigit(String string) {
        for (char c : string.toCharArray()) {
            if (Character.isDigit(c)) return true;
        }
        return false;
    }

    /**
     * Replaces [^a-zA-Z\-] characters with " "(white space). Used to normalize texts and candidate terms before counting
     * frequencies
     *
     * @param string  input string to be processed
     * @param pattern regular expression pattern defining characters to be replaced by " " (white space)
     * @return
     */
    public static String applyCharacterReplacement(String string, String pattern) {
        return string.replaceAll(pattern, " ").replaceAll("\\s+", " ").trim();
    }

    /**
     * If the input string contains "and", "or"  and "," , it is assumed to contain multiple candidate terms, and is split.
     * This method is used to process extracted candidate terms. Due to imperfections of NLP tools, some times candidate
     * terms can be noisy and need to be further processed/normalized.
     *
     * @param string input string
     * @return individual strings seperated by "and", "or" and ","
     */

    //Code modification begins : the second and third 'if' changed to 'else if'
    public static String[] applySplitList(String string) {
        StringBuilder sb = new StringBuilder();
        if (string.indexOf(" and ") != -1) {
            String[] parts = string.split("\\band\\b");
            for (String s : parts) sb.append(s.trim() + "|");
        } else if (string.indexOf(" or ") != -1) {
            String[] parts = string.split("\\bor\\b");
            for (String s : parts) sb.append(s.trim() + "|");
        } else if (string.indexOf(",") != -1) {
            if (!containsDigit(string)) {
                String[] parts = string.split("\\,");
                for (String s : parts) sb.append(s.trim() + "|");
            }
        } else {
            sb.append(string);
        }
        String v = sb.toString();
        if (v.endsWith("|")) v = v.substring(0, v.lastIndexOf("|"));
        return v.toString().split("\\|");
    }

    //code modification ends


    /**
     * If a string beings or ends with a stop word (e.g., "the"), the stop word is removed.
     * <br>
     * This method is used to further process/normalize extracted candidate terms. Due to imperfections of NLP tools,
     * sometimes candidate terms can be noisy and needs further normalization.
     *
     * @param string
     * @param stop
     * @param fromLeft   if true trim stopwords from the left
     * @param fromRight  if true trim stopwords from the right
     * @return null if the string is a stopword; otherwise the normalized string
     */
    public static String applyTrimStopwords(String string, StopList stop, Normalizer normalizer, boolean fromLeft, boolean fromRight) {
        if (string.length() == 0)
            return null;
        //check the entire string first (e.g., "e. g. " and "i. e. " which will fail the following checks
        // String s = normalizer.normalize(string);

        if (stop.isStopWord(normalizer.normalize(string).replaceAll("\\s+", "").trim()))
            return null;

        String[] e = string.split("\\s+");
        if (e == null || e.length < 1) return string;

        int head = e.length;
        int end = -1;
        if (fromLeft) {
            for (int i = 0; i < e.length; i++) {
                if (!stop.isStopWord(e[i])) {
                    head = i;
                    break;
                }
            }
        } else {
            head = 0;
        }

        if (fromRight) {
            for (int i = e.length - 1; i > -1; i--) {
                if (!stop.isStopWord(e[i])) {
                    end = i;
                    break;
                }
            }
        } else {
            end = e.length - 1;
        }

        if (head <= end) {
            String trimmed = "";
            for (int i = head; i <= end; i++) {
                trimmed += e[i] + " ";
            }
            return trimmed.trim();
        }
        return null;
    }

    /**
     * This method is used to check if candidate term is a possible noisy symbol or non-term.
     *
     * @param string
     * @return true if the string contains at least 2 letters or digits; false otherwise
     */
    public static boolean hasReasonableNumChars(String string) {
        int len = string.length();
        if (len < 2) return false;
        if (len < 5) {
            char[] chars = string.toCharArray();
            int num = 0;
            for (int i = 0; i < chars.length; i++) {
                if (Character.isLetter(chars[i]) || Character.isDigit(chars[i]))
                    num++;
                if (num > 2) break;
            }
            if (num > 2) return true;
            return false;
        }
        return true;
    }
}
