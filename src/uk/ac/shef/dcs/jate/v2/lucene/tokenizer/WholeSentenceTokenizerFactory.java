package uk.ac.shef.dcs.jate.v2.lucene.tokenizer;

import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.text.BreakIterator;
import java.util.Map;

/**
 * Created by zqz on 23/09/2015.
 */
public class WholeSentenceTokenizerFactory extends TokenizerFactory {
    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected WholeSentenceTokenizerFactory(Map<String, String> args) {
        super(args);
    }

    @Override
    public Tokenizer create(AttributeFactory factory) {
        return new WholeSentenceTokenizer(factory, BreakIterator.getSentenceInstance());
    }
}
