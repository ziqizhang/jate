package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;
import java.util.Set;

/**
 * Multi-Word Expression (MWE) filter is used to build multi-word expressions (n-grams, phrases) from a token stream.
 *
 */
public abstract class MWEFilter extends TokenFilter{

    /**
     * default maximum shingle size is 2.
     */
    public static final int DEFAULT_MAX_TOKENS = 2;

    /**
     * default minimum shingle size is 2.
     */
    public static final int DEFAULT_MIN_TOKENS = 2;

    /**
     * default maximum shingle size is 2.
     */
    public static final int DEFAULT_MAX_CHAR_LENGTH = 50;

    /**
     * default minimum shingle size is 2.
     */
    public static final int DEFAULT_MIN_CHAR_LENGTH = 1;

    /**
     * given "the cat sat on the" if this is true, and "the" is a stopword it becomes "cat sat on the"
     */
    public static final boolean DEFAULT_REMOVE_LEADING_STOPWORDS = false;

    /**
     * given "the cat sat on the" if this is true, and "the" is a stopword it becomes "the cat sat on"
     */
    public static final boolean DEFAULT_REMOVE_TRAILING_STOPWORDS = false;

    /**
     * if true, given ", my house +" this becomes "my house +"
     */
    public static final boolean DEFAULT_REMOVE_LEADING_SYMBOLS = false;


    /**
     * if true, given ", my house +" this becomes ", my house"
     */
    public static final boolean DEFAULT_REMOVE_TRAILING_SYMBOLS = false;

    public static final boolean DEFAULT_STOP_WORDS_IGNORE_CASE=false;


    /**
     * maximum shingle size (number of tokens)
     */
    protected int maxTokens;

    /**
     * minimum shingle size (number of tokens)
     */
    protected int minTokens;

    /**
     * maximum shingle size (number of tokens)
     */
    protected int maxCharLength;

    /**
     * minimum shingle size (number of tokens)
     */
    protected int minCharLength;

    protected boolean removeLeadingStopwords;
    protected boolean removeTrailingStopwords;
    protected boolean removeLeadingSymbolicTokens;
    protected boolean removeTrailingSymbolicTokens;

    protected Set<String> stopWords;
    protected boolean stopWordsIgnoreCase;

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected MWEFilter(TokenStream input) {
        super(input);
    }

    public MWEFilter(TokenStream input, int minTokens, int maxTokens,
                                int minCharLength, int maxCharLength,
                                boolean removeLeadingStopWords,
                                boolean removeTrailingStopwords,
                                boolean removeLeadingSymbolicTokens,
                                boolean removeTrailingSymbolicTokens,
                                Set<String> stopWords,
                                boolean stopWordsIgnoreCase) {
        super(input);
        this.minTokens = minTokens;
        this.maxTokens = maxTokens;
        this.minCharLength = minCharLength;
        this.maxCharLength = maxCharLength;
        this.removeLeadingStopwords = removeLeadingStopWords;
        this.removeTrailingStopwords = removeTrailingStopwords;
        this.removeLeadingSymbolicTokens = removeLeadingSymbolicTokens;
        this.removeTrailingSymbolicTokens = removeTrailingSymbolicTokens;
        this.stopWords = stopWords;
        this.stopWordsIgnoreCase=stopWordsIgnoreCase;
    }


}
