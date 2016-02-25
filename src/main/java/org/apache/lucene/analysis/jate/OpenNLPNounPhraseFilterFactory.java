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
 *
 */
public class OpenNLPNounPhraseFilterFactory extends MWEFilterFactory {
    private Chunker chunker;
    private String chunkerModelFile;
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    public OpenNLPNounPhraseFilterFactory(Map<String, String> args) {
        super(args);
        chunkerModelFile = args.get("chunkerModel");
        if (chunkerModelFile == null)
            throw new IllegalArgumentException("Parameter 'chunkerModel' for chunker is missing.");

    }

    @Override
    public TokenStream create(TokenStream input) {

        return new OpenNLPNounPhraseFilter(input, chunker,
                minTokens, maxTokens, minCharLength, maxCharLength,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stripLeadingSymbolChars,
                stripTrailingSymbolChars,
                stripAnySymbolChars,
                stopWords, stopWordsIgnoreCase);
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        super.inform(loader);

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
