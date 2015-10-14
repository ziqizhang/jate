package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Created by - on 13/10/2015.
 */
public abstract class MWEFilterFactory extends TokenFilterFactory{
    protected int minTokens;
    protected int maxTokens;
    protected int minCharLength;
    protected int maxCharLength;
    protected Set<String> stopWords=new HashSet<>();
    protected boolean removeLeadingStopwords;
    protected boolean removeTrailingStopwords;
    protected boolean removeLeadingSymbolicTokens;
    protected boolean removeTrailingSymbolicTokens;
    protected boolean stopWordsIgnoreCase;

    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    public MWEFilterFactory(Map<String, String> args) {
        super(args);

        maxTokens = getInt(args, "maxTokens", MWEFilter.DEFAULT_MAX_TOKENS);
        if (maxTokens < 1) {
            throw new IllegalArgumentException("Invalid maxTokens (" + maxTokens + ") - must be at least 1");
        }
        minTokens = getInt(args, "minTokens", MWEFilter.DEFAULT_MIN_TOKENS);
        if (minTokens < 1) {
            throw new IllegalArgumentException("Invalid minTokens (" + minTokens + ") - must be at least 1");
        }
        if (minTokens > maxTokens) {
            throw new IllegalArgumentException
                    ("Invalid minTokens (" + minTokens + ") - must be no greater than maxTokens (" + maxTokens + ")");
        }
        maxCharLength = getInt(args, "maxCharLength", MWEFilter.DEFAULT_MAX_CHAR_LENGTH);
        if (maxCharLength < 1) {
            throw new IllegalArgumentException("Invalid maxCharLength (" + maxCharLength + ") - must be at least 1");
        }
        minCharLength = getInt(args, "minCharLength", MWEFilter.DEFAULT_MIN_CHAR_LENGTH);
        if (minCharLength < 1) {
            throw new IllegalArgumentException("Invalid minCharLength (" + minCharLength + ") - must be at least 1");
        }
        if (minCharLength > maxCharLength) {
            throw new IllegalArgumentException
                    ("Invalid minCharLength (" + minCharLength + ") - must be no greater than maxCharLength (" + maxCharLength + ")");
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

        removeLeadingStopwords=getBoolean(args, "removeLeadingStopWords", ComplexShingleFilter.DEFAULT_REMOVE_LEADING_STOPWORDS);
        removeTrailingStopwords=getBoolean(args, "removeTrailingStopWords", ComplexShingleFilter.DEFAULT_REMOVE_TRAILING_STOPWORDS);
        removeLeadingSymbolicTokens=getBoolean(args, "removeLeadingSymbolicTokens", ComplexShingleFilter.DEFAULT_REMOVE_LEADING_SYMBOLS);
        removeTrailingSymbolicTokens=getBoolean(args, "removeTrailingSymbolicTokens", ComplexShingleFilter.DEFAULT_REMOVE_TRAILING_SYMBOLS);
    }
}
