package org.apache.lucene.analysis.jate;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.ResourceLoader;
import org.apache.lucene.analysis.util.ResourceLoaderAware;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;

import java.io.IOException;
import java.util.Map;

public class OpenNLPTokenizerFactory extends TokenizerFactory implements ResourceLoaderAware {
    private SentenceDetector sentenceOp = null;
    private String sentenceModelFile = null;
    private opennlp.tools.tokenize.Tokenizer tokenizerOp = null;
    private String tokenizerModelFile = null;

    /**
     * Creates a new StandardTokenizerFactory
     */
    public OpenNLPTokenizerFactory(Map<String, String> args) {
        super(args);
        sentenceModelFile = args.get("sentenceModel");
        tokenizerModelFile = args.get("tokenizerModel");
    }

    @Override
    public Tokenizer create(AttributeFactory factory) {
        OpenNLPTokenizer tokenizer = new OpenNLPTokenizer(factory, sentenceOp, tokenizerOp);
        return tokenizer;
    }

    @Override
    public void inform(ResourceLoader loader) throws IOException {
        if(sentenceModelFile!=null) {
            sentenceOp = new SentenceDetectorME(new SentenceModel(
                    loader.openResource(sentenceModelFile)));
        }

        if(tokenizerModelFile==null)
            throw new IOException("Parameter 'tokenizerModle' is required, but is invalid:"+tokenizerModelFile);
        tokenizerOp = new TokenizerME(new TokenizerModel(
                loader.openResource(tokenizerModelFile)
        ));

    }
}
