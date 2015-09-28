package org.apache.lucene.analysis.opennlp;

import opennlp.tools.chunker.Chunker;
import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTagger;
import opennlp.tools.postag.POSTaggerME;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.TokenizerME;
import opennlp.tools.tokenize.TokenizerModel;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.analysis.util.TokenFilterFactory;

import java.io.File;
import java.io.IOException;
import java.util.Map;

/**
 * Created by zqz on 28/09/2015.
 */
public class OpenNLPFilterFactory extends TokenFilterFactory {
    private POSTagger posTaggerOp;
    private Chunker chunkerOp;

    /**
     * Initialize this factory via a set of key-value pairs.
     *
     * @param args
     */
    protected OpenNLPFilterFactory(Map<String, String> args) {
        super(args);
        String posModel = args.get("posTaggerModel");
        String chunkModel =args.get("chunkerModel");
        try{
            posTaggerOp = new POSTaggerME(new POSModel(new File(posModel)));
        }catch (Exception e){
            StringBuilder msg = new StringBuilder("Required parameter invalid:");
            msg.append("posTaggerModel=").append(posModel).append("\n");
            msg.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(msg.toString());
        }
        try{
            chunkerOp = new ChunkerME(new ChunkerModel(new File(chunkModel)));
        }catch (Exception e){
            StringBuilder msg = new StringBuilder("Required parameter invalid:");
            msg.append("chunkerModel=").append(posModel).append("\n");
            msg.append(ExceptionUtils.getFullStackTrace(e));
            throw new IllegalArgumentException(msg.toString());
        }
    }


    @Override
    public TokenStream create(TokenStream input) {
        return new OpenNLPFilter(input, posTaggerOp,chunkerOp);
    }
}
