package uk.ac.shef.dcs.jate.lucene.filter;

import opennlp.tools.chunker.Chunker;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenStream;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.util.Map;

/**
 * Created by - on 12/10/2015.
 */
public class NounPhraseFilterFactory extends MWEFilterFactory {
    private POSTagger tagger;
    private Chunker chunker;
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected NounPhraseFilterFactory(Map<String, String> args) {
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

        String chunkerClass = args.get("chunkerClass");
        if (chunkerClass == null)
            throw new IllegalArgumentException("Parameter 'class' for Chunker is missing.");
        String chunkerFileIfExist = args.get("chunkerModel");
        try {
            chunker = InstanceCreator.createChunker(taggerClass, chunkerFileIfExist);
        } catch (Exception e) {
            StringBuilder sb = new StringBuilder("Initiating ");
            sb.append(this.getClass().getName()).append(" failed due to:\n");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(sb.toString());
        }
    }

    @Override
    public TokenStream create(TokenStream input) {

        return new NounPhraseFilter(input,tagger, chunker,
                minTokens, maxTokens, minCharLength, maxCharLength,
                removeLeadingStopwords, removeTrailingStopwords,
                removeLeadingSymbolicTokens, removeTrailingSymbolicTokens,
                stopWords, stopWordsIgnoreCase);
    }
}
