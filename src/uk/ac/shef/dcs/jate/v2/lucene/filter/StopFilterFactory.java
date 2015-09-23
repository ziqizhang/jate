package uk.ac.shef.dcs.jate.v2.lucene.filter;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 * Created by zqz on 23/09/2015.
 *
 * todo: should allow filtering stopwords using dictionary and rules (e.g., 1 character words, numbers etc)
 */
public class StopFilterFactory extends TokenFilterFactory{
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected StopFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream input) {
        return null;
    }
}
