package uk.ac.shef.dcs.jate.lucene.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * Created by - on 12/10/2015.
 */
public class PunctuationRemoverFactory extends TokenFilterFactory {
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected PunctuationRemoverFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream input) {
        return null;
    }
}
