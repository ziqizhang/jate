package org.apache.lucene.analysis.jate;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenFilterFactory;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.IOException;
import java.util.Map;

/**
 *
 */
public class OpenNLPPOSTaggerFactory extends TokenFilterFactory implements ResourceLoaderAware {

    private POSTagger tagger=null;
    private String posTaggerModelFile=null;
    private String posTaggerClass=null;
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected OpenNLPPOSTaggerFactory(Map<String, String> args) {
        super(args);
        posTaggerModelFile = args.get("posTaggerModel");
        posTaggerClass=args.get("posTaggerClass");
        if (posTaggerClass == null)
            throw new IllegalArgumentException("Parameter 'posTaggerClass' for POS tagger is missing.");
        posTaggerModelFile = args.get("posTaggerModel");
        if (posTaggerModelFile == null) {
            throw new IllegalArgumentException("Parameter 'posTaggerModel' for POS tagger is missing.");
        }
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        if (posTaggerModelFile != null && posTaggerClass != null) {
            try {
                tagger = InstanceCreator.createPOSTagger(posTaggerClass, loader.openResource(posTaggerModelFile));
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
        return new OpenNLPPOSTaggerFilter(input, tagger);
    }
}
