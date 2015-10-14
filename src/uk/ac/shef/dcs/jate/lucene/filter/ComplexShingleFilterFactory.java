package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ComplexShingleFilterFactory extends MWEFilterFactory {
    private final boolean sentenceBoundaryAware;
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

        sentenceBoundaryAware=getBoolean(args, "sentenceBoundaryAware", ComplexShingleFilter.DEFAULT_SENTENCE_BOUNDARY_AWARE);
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
                sentenceBoundaryAware,stopWords,stopWordsIgnoreCase);
        return r;
    }
}
