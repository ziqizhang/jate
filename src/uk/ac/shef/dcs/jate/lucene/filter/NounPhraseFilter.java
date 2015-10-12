package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;

/**
 * Created by - on 12/10/2015.
 */
public class NounPhraseFilter extends TokenFilter {
    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected NounPhraseFilter(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        return false;
    }
}
