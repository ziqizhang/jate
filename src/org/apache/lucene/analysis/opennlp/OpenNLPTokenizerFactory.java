package org.apache.lucene.analysis.opennlp;

import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.TokenizerFactory;
import org.apache.lucene.util.AttributeFactory;
import uk.ac.shef.dcs.jate.v2.JATEException;

import java.io.File;
import java.util.Map;

/**
 * Created by zqz on 28/09/2015.
 */
public class OpenNLPTokenizerFactory extends TokenizerFactory {
    private final int maxTokenLength;
    private SentenceDetector sentenceOp = null;
    private opennlp.tools.tokenize.Tokenizer tokenizerOp = null;

    /** Creates a new StandardTokenizerFactory */
    public OpenNLPTokenizerFactory(Map<String,String> args) {
        super(args);
        maxTokenLength = getInt(args, "maxTokenLength", StandardAnalyzer.DEFAULT_MAX_TOKEN_LENGTH);
        String sentModel = args.get("sentenceModel");
        String tokenizerModel =args.get("tokenizerModel");
        try{
            sentenceOp = new SentenceDetectorME(new SentenceModel(new File(sentModel)));
        }catch (Exception e){
            StringBuilder msg = new StringBuilder("Required parameter invalid:");
            msg.append("sentenceModel=").append(sentModel).append("\n");
            msg.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(msg.toString());
        }
        try{
            tokenizerOp = new TokenizerME(new TokenizerModel(new File(tokenizerModel)));
        }catch (Exception e){
            StringBuilder msg = new StringBuilder("Required parameter invalid:");
            msg.append("tokenizerModel=").append(sentModel).append("\n");
            msg.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(msg.toString());
        }
    }

    @Override
    public Tokenizer create(AttributeFactory factory) {
        OpenNLPTokenizer tokenizer = new OpenNLPTokenizer(factory, sentenceOp, tokenizerOp);
        return tokenizer;
    }
}
