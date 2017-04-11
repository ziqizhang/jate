package org.apache.lucene.analysis.jate;

import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.util.Map;

/**
 *
 */
public class MWEFeatureFilterFactory extends TokenFilterFactory {
    public MWEFeatureFilterFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public TokenStream create(TokenStream tokenStream) {
        return new MWEFeatureFilter(tokenStream);
    }
}
