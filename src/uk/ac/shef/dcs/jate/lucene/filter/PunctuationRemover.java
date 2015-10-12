package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;

import java.io.IOException;

/**
 * Created by - on 12/10/2015.
 */
public class PunctuationRemover extends TokenFilter {

    public static boolean DEFAULT_STRIP_LEADING_PUNCUATIONS=false;
    public static boolean DEFAULT_STRIP_TRAILING_PUNCUATIONS=false;
    public static boolean DEFAULT_STRIP_ANY_PUNCUATIONS=false;

    /**
     * Construct a token stream filtering the given input.
     *
     * @param input
     */
    protected PunctuationRemover(TokenStream input) {
        super(input);
    }

    @Override
    public boolean incrementToken() throws IOException {
        return false;
    }
}
