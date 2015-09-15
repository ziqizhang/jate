package uk.ac.shef.dcs.jate.deprecated.util.counter;


import java.util.HashSet;
import java.util.Set;

/**
 * Count frequencies of phrases or n-grams in corpus
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */

public class TermFreqCounter {

    /**
     * Count number of occurrences of a string in a context.
     *
     * @param noun    the string to be counted
     * @param context the text in which the string can be found
     * @return number of frequencies
     */
    public int count(String noun, String context) {
        int next;
        int start = 0;
        int freq = 0;
        while (start <= context.length()) {
            next = context.indexOf(noun, start);
            char prefix = next - 1 < 0 ? ' ' : context.charAt(next - 1);
            char suffix = next + noun.length() >= context.length() ? ' ' : context.charAt(next + noun.length());
            if (next != -1 && isValidChar(prefix) && isValidChar(suffix)) {
                freq++;
            }

            if (next == -1 || next == context.length()) break;
            start = next + noun.length();
        }
        return freq;
    }

    /**
     * Count number of occurrences of a string in a context.
     *
     * @param noun    the string to be counted
     * @param context the text in which the string can be found
     * @return int array which contains offsets of occurrences in the text.
     */
    public Set<Integer> countOffsets(String noun, String context) {
        Set<Integer> offsets = new HashSet<Integer>();
        int next;
        int start = 0;
        while (start <= context.length()) {
            next = context.indexOf(noun, start);
            char prefix = next - 1 < 0 ? ' ' : context.charAt(next - 1);
            char suffix = next + noun.length() >= context.length() ? ' ' : context.charAt(next + noun.length());
            if (next != -1 && isValidChar(prefix) && isValidChar(suffix)) {
                offsets.add(next);
            }
            if (next == -1) break;
            start = next + noun.length();
        }


        return offsets;
    }

    /**
     * Count the total frequencies of a set of terms in certain context. Counting is always case-sensitive. For case-insensitive
     *  counting, the input params should be pre-processed accordingly
     *
     * @param terms   a set of terms
     * @param context in which the terms are expected to be found
     * @return total frequencies of the set of terms
     */
    public int count(String context, Set<String> terms) {
        Set<Integer> offsets = new HashSet<Integer>();

        if (terms != null) {
            for (String t : terms) {
                offsets.addAll(countOffsets(t, context));
            }

        }
        return offsets.size();
    }

    /**
     * Check if a character is a valid separater of a word. A valid separater is non digit and non letter.
     *
     * @param c
     * @return
     */
    private boolean isValidChar(char c) {
        return !Character.isLetter(c) && !Character.isDigit(c);
    }
}
