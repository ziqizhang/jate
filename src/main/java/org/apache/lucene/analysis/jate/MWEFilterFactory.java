package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by - on 13/10/2015.
 */
public abstract class MWEFilterFactory extends TokenFilterFactory implements ResourceLoaderAware{



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
    protected boolean stripLeadingSymbolChars;
    protected boolean stripTrailingSymbolChars;
    protected boolean stripAnySymbolChars;

    protected final String stopWordFile;

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

        stopWordFile =get(args,"stopWords","");
        stopWordsIgnoreCase=getBoolean(args, "stopWordsIgnoreCase", MWEFilter.DEFAULT_STOP_WORDS_IGNORE_CASE);


        removeLeadingStopwords=getBoolean(args, "removeLeadingStopWords", MWEFilter.DEFAULT_REMOVE_LEADING_STOPWORDS);
        removeTrailingStopwords=getBoolean(args, "removeTrailingStopWords", MWEFilter.DEFAULT_REMOVE_TRAILING_STOPWORDS);
        removeLeadingSymbolicTokens=getBoolean(args, "removeLeadingSymbolicTokens", MWEFilter.DEFAULT_REMOVE_LEADING_SYMBOLS);
        removeTrailingSymbolicTokens=getBoolean(args, "removeTrailingSymbolicTokens", MWEFilter.DEFAULT_REMOVE_TRAILING_SYMBOLS);

        stripAnySymbolChars = getBoolean(args, "stripAnySymbolChars", MWEFilter.DEFAULT_STRIP_ANY_SYMBOL_CHARS);
        stripLeadingSymbolChars = getBoolean(args, "stripLeadingSymbolChars", MWEFilter.DEFAULT_STRIP_LEADING_SYMBOL_CHARS);
        stripTrailingSymbolChars = getBoolean(args, "stripTrailingSymbolChars", MWEFilter.DEFAULT_STRIP_TRAILING_SYMBOL_CHARS);

    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        if (stopWordFile != null) {
            List<String> wlist = getLines(loader, stopWordFile.trim());
            stopWords.addAll(wlist);
        }
    }
}
