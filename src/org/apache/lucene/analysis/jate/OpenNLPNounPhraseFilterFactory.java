package org.apache.lucene.analysis.jate;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.util.ResourceLoader;
import uk.ac.shef.dcs.jate.nlp.Chunker;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.POSTagger;
import uk.ac.shef.dcs.jate.nlp.opennlp.ChunkerOpenNLP;

import java.io.IOException;
import java.util.Map;

/**
 * Created by - on 12/10/2015.
 */
public class OpenNLPNounPhraseFilterFactory extends MWEFilterFactory {
    private POSTagger tagger;
    private String posTaggerClass;
    private String posTaggerModelFile;
    private Chunker chunker;
    private String chunkerModelFile;
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    public OpenNLPNounPhraseFilterFactory(Map<String, String> args) {
        super(args);
        posTaggerClass = args.get("posTaggerClass");
        if (posTaggerClass == null)
            throw new IllegalArgumentException("Parameter 'posTaggerClass' for POS tagger is missing.");
        posTaggerModelFile = args.get("posTaggerModel");
        if (posTaggerModelFile == null)
            throw new IllegalArgumentException("Parameter 'posTaggerModel' for POS tagger is missing.");
        chunkerModelFile = args.get("chunkerModel");
        if (chunkerModelFile == null)
            throw new IllegalArgumentException("Parameter 'chunkerModel' for chunker is missing.");

    }

    @Override
    public TokenStream create(TokenStream input) {

        return new OpenNLPNounPhraseFilter(input,tagger, chunker,
                minTokens, maxTokens, minCharLength, maxCharLength,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        super.inform(loader);
        try {
            tagger = InstanceCreator.createPOSTagger(posTaggerClass, loader.openResource(posTaggerModelFile));
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Initiating ");
            sb.append(this.getClass().getName()).append(" failed due to:\n");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(sb.toString());
        }

        try {
            chunker = InstanceCreator.createChunker(ChunkerOpenNLP.class.getName(), loader.openResource(chunkerModelFile));
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Initiating ");
            sb.append(this.getClass().getName()).append(" failed due to:\n");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(sb.toString());
        }

    }
}
