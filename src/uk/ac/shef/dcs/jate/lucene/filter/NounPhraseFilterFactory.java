package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenStream;

import java.util.Map;

/**
 * Created by - on 12/10/2015.
 */
public class NounPhraseFilterFactory extends MWEFilterFactory {
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected NounPhraseFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream input) {

        return new NounPhraseFilter(input,
                minTokens, maxTokens, minCharLength, maxCharLength,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
    }
}
