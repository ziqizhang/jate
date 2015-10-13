package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 */
public class PunctuationRemoverFactory extends TokenFilterFactory {

    private final boolean stripLeadingSymbols;
    private final boolean stripTrailingSymbols;
    private final boolean stripAnySymbols;
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected PunctuationRemoverFactory(Map<String, String> args) {
        super(args);
        stripAnySymbols = getBoolean(args, "stripAnySymbols", PunctuationRemover.DEFAULT_STRIP_ANY_SYMBOLS);
        stripLeadingSymbols = getBoolean(args, "stripLeadingSymbols", PunctuationRemover.DEFAULT_STRIP_LEADING_SYMBOLS);
        stripTrailingSymbols = getBoolean(args, "stripTrailingSymbols", PunctuationRemover.DEFAULT_STRIP_TRAILING_SYMBOLS);
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new PunctuationRemover(input, stripAnySymbols,
                stripLeadingSymbols, stripTrailingSymbols);
    }
}
