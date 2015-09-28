package uk.ac.shef.dcs.jate.v2.nlp.opennlp;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import uk.ac.shef.dcs.jate.v2.nlp.POSTagger;

import java.io.File;
import java.io.IOException;

/**
 * Created by zqz on 28/09/2015.
 */
public class POSTaggerOpenNLP implements POSTagger {
    private opennlp.tools.postag.POSTagger tagger;

    public POSTaggerOpenNLP(String modelFile) throws IOException {
        tagger = new POSTaggerME(new POSModel(new File(modelFile)));
    }

    @Override
    public String[] tag(String[] tokens) {
        return tagger.tag(tokens);
    }
}
