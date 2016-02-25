package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 *
 */
public class ComplexShingleFilterFactory extends MWEFilterFactory {
    private final boolean outputUnigrams;
    private final boolean outputUnigramsIfNoShingles;
    private final String tokenSeparator;
    private final String fillerToken;

    /** Creates a new ShingleFilterFactory */
    public ComplexShingleFilterFactory(Map<String, String> args) {
        super(args);
        if (maxTokens < 2) {
            throw new IllegalArgumentException("Invalid maxTokens (" + maxTokens + ") - must be at least 2");
        }
        if (minTokens < 2) {
            throw new IllegalArgumentException("Invalid minTokens (" + minTokens + ") - must be at least 2");
        }
        if (minTokens > maxTokens) {
            throw new IllegalArgumentException
                    ("Invalid minTokens (" + minTokens + ") - must be no greater than maxTokens (" + maxTokens + ")");
        }

        outputUnigrams = getBoolean(args, "outputUnigrams", true);
        outputUnigramsIfNoShingles = getBoolean(args, "outputUnigramsIfNoShingles", false);
        tokenSeparator = get(args, "tokenSeparator", ComplexShingleFilter.DEFAULT_TOKEN_SEPARATOR);
        fillerToken = get(args, "fillerToken", ComplexShingleFilter.DEFAULT_FILLER_TOKEN);

        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public ComplexShingleFilter create(TokenStream input) {
        ComplexShingleFilter r = new ComplexShingleFilter(input, minTokens, maxTokens,
                minCharLength, maxCharLength,outputUnigrams, outputUnigramsIfNoShingles,
                tokenSeparator, fillerToken,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stripLeadingSymbolChars,
                stripTrailingSymbolChars,
                stripAnySymbolChars,
                stopWords,stopWordsIgnoreCase);
        return r;
    }
}
