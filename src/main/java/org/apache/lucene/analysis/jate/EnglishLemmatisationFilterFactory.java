package org.apache.lucene.analysis.jate;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import org.apache.solr.core.SolrResourceLoader;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by - on 19/02/2016.
 */
public class EnglishLemmatisationFilterFactory extends TokenFilterFactory implements ResourceLoaderAware {

    private EngLemmatiser lemmatiser;
    private String lemmatiserResourceDir;
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    public EnglishLemmatisationFilterFactory(Map<String, String> args) {
        super(args);
        lemmatiserResourceDir = args.get("lemmaResourceDir");
        if (lemmatiserResourceDir == null)
            throw new IllegalArgumentException("Parameter 'lemmaResourceDir' for lemmatiser is missing.");
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        if (lemmatiserResourceDir != null ) {
            try {
                lemmatiser = new EngLemmatiser(((SolrResourceLoader) loader).getConfigDir()+ File.separator+lemmatiserResourceDir, false, false);
            } catch (Exception e) {
                StringBuilder sb = new StringBuilder("Initiating ");
                sb.append(this.getClass().getName()).append(" failed due to:\n");
                sb.append(ExceptionUtils.getFullStackTrace(e));
                throw new IllegalArgumentException(sb.toString());
            }
        }
    }

    @Override
    public TokenStream create(TokenStream input) {
        return new EnglishLemmatisationFilter(lemmatiser, input);
    }
}
