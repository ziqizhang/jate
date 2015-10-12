package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class ComplexShingleFilterFactory extends TokenFilterFactory {
    private final int minShingleSize;
    private final int maxShingleSize;
    private final int minCharLength;
    private final int maxCharLength;
    private Set<String> stopWords=null;
    private final boolean removeLeadingStopwords;
    private final boolean removeTrailingStopwords;
    private final boolean sentenceBoundaryAware;
    private final boolean removeLeadingSymbolicTokens;
    private final boolean removeTrailingSymbolicTokens;
    private final boolean outputUnigrams;
    private final boolean outputUnigramsIfNoShingles;
    private final String tokenSeparator;
    private final String fillerToken;
    private boolean stopWordsIgnoreCase;

    /** Creates a new ShingleFilterFactory */
    public ComplexShingleFilterFactory(Map<String, String> args) {
        super(args);
        maxShingleSize = getInt(args, "maxShingleSize", ComplexShingleFilter.DEFAULT_MAX_SHINGLE_SIZE);
        if (maxShingleSize < 2) {
            throw new IllegalArgumentException("Invalid maxShingleSize (" + maxShingleSize + ") - must be at least 2");
        }
        minShingleSize = getInt(args, "minShingleSize", ComplexShingleFilter.DEFAULT_MIN_SHINGLE_SIZE);
        if (minShingleSize < 2) {
            throw new IllegalArgumentException("Invalid minShingleSize (" + minShingleSize + ") - must be at least 2");
        }
        if (minShingleSize > maxShingleSize) {
            throw new IllegalArgumentException
                    ("Invalid minShingleSize (" + minShingleSize + ") - must be no greater than maxShingleSize (" + maxShingleSize + ")");
        }
        maxCharLength = getInt(args, "maxCharLength", ComplexShingleFilter.DEFAULT_MAX_CHAR_LENGTH);
        if (maxCharLength < 2) {
            throw new IllegalArgumentException("Invalid maxCharLength (" + maxCharLength + ") - must be at least 2");
        }
        minCharLength = getInt(args, "minCharLength", ComplexShingleFilter.DEFAULT_MIN_CHAR_LENGTH);
        if (minCharLength < 1) {
            throw new IllegalArgumentException("Invalid minCharLength (" + minCharLength + ") - must be at least 1");
        }
        if (minShingleSize > maxShingleSize) {
            throw new IllegalArgumentException
                    ("Invalid minShingleSize (" + minShingleSize + ") - must be no greater than maxShingleSize (" + maxShingleSize + ")");
        }

        String stopWordsFile=get(args,"stopWords","");
        if(!stopWordsFile.equals("")){
            try {
                Set<String> sw= new HashSet<>(FileUtils.readLines(new File(stopWordsFile)));
                String lowercase = get(args,"stopWordsIgnoreCase","");
                if(lowercase.equalsIgnoreCase("true")){
                    stopWordsIgnoreCase=true;
                    for(String s: sw)
                        stopWords.add(s.toLowerCase());
                }
                else {
                    stopWords.addAll(sw);
                    stopWordsIgnoreCase=false;
                }
            } catch (IOException e) {
                throw new IllegalArgumentException(e);
            }
        }

        outputUnigrams = getBoolean(args, "outputUnigrams", true);
        outputUnigramsIfNoShingles = getBoolean(args, "outputUnigramsIfNoShingles", false);
        tokenSeparator = get(args, "tokenSeparator", ComplexShingleFilter.DEFAULT_TOKEN_SEPARATOR);
        fillerToken = get(args, "fillerToken", ComplexShingleFilter.DEFAULT_FILLER_TOKEN);

        removeLeadingStopwords=getBoolean(args, "removeLeadingStopWords", ComplexShingleFilter.DEFAULT_REMOVE_LEADING_STOPWORDS);
        removeTrailingStopwords=getBoolean(args, "removeTrailingStopWords", ComplexShingleFilter.DEFAULT_REMOVE_TRAILING_STOPWORDS);
        removeLeadingSymbolicTokens=getBoolean(args, "removeLeadingSymbolicTokens", ComplexShingleFilter.DEFAULT_REMOVE_LEADING_SYMBOLS);
        removeTrailingSymbolicTokens=getBoolean(args, "removeTrailingSymbolicTokens", ComplexShingleFilter.DEFAULT_REMOVE_LEADING_SYMBOLS);
        sentenceBoundaryAware=getBoolean(args, "sentenceBoundaryAware", ComplexShingleFilter.DEFAULT_SENTENCE_BOUNDARY_AWARE);
        if (!args.isEmpty()) {
            throw new IllegalArgumentException("Unknown parameters: " + args);
        }
    }

    @Override
    public ComplexShingleFilter create(TokenStream input) {
        ComplexShingleFilter r = new ComplexShingleFilter(input, minShingleSize, maxShingleSize,
                minCharLength, maxCharLength,outputUnigrams, outputUnigramsIfNoShingles,
                tokenSeparator, fillerToken,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                sentenceBoundaryAware,stopWords,stopWordsIgnoreCase);
        return r;
    }
}
