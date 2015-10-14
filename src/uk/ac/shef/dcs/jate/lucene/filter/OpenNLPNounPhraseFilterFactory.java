package uk.ac.shef.dcs.jate.lucene.filter;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenStream;
import uk.ac.shef.dcs.jate.nlp.Chunker;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.POSTagger;
import uk.ac.shef.dcs.jate.nlp.opennlp.ChunkerOpenNLP;

import java.util.Map;

/**
 * Created by - on 12/10/2015.
 */
public class OpenNLPNounPhraseFilterFactory extends MWEFilterFactory {
    private POSTagger tagger;
    private Chunker chunker;
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected OpenNLPNounPhraseFilterFactory(Map<String, String> args) {
        super(args);
        String taggerClass = args.get("posTaggerClass");
        if (taggerClass == null)
            throw new IllegalArgumentException("Parameter 'class' for POS tagger is missing.");
        String taggerFileIfExist = args.get("posTaggerModel");
        try {
            tagger = InstanceCreator.createPOSTagger(taggerClass, taggerFileIfExist);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Initiating ");
            sb.append(this.getClass().getName()).append(" failed due to:\n");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(sb.toString());
        }


        String chunkerFileIfExist = args.get("chunkerModel");
        try {
            chunker = InstanceCreator.createChunker(ChunkerOpenNLP.class.getName(), chunkerFileIfExist);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Initiating ");
            sb.append(this.getClass().getName()).append(" failed due to:\n");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(sb.toString());
        }
    }

    @Override
    public TokenStream create(TokenStream input) {

        return new OpenNLPNounPhraseFilter(input,tagger, chunker,
                minTokens, maxTokens, minCharLength, maxCharLength,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
    }
}
