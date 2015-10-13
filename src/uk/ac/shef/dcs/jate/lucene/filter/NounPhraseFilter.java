package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;
import java.util.Set;

/**
 * Created by - on 12/10/2015.
 */
public class NounPhraseFilter extends MWEFilter {
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected NounPhraseFilter(TokenStream input, int minTokens, int maxTokens,
                               int minCharLength, int maxCharLength,
                               boolean removeLeadingStopWords,
                               boolean removeTrailingStopwords,
                               boolean removeLeadingSymbolicTokens,
                               boolean removeTrailingSymbolicTokens,
                               Set<String> stopWords,
                               boolean stopWordsIgnoreCase) {
        super(input, minTokens, maxTokens,
                minCharLength, maxCharLength,
                removeLeadingStopWords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
    }

    @Override
    public boolean incrementToken() throws IOException {
        return false;
    }
}
