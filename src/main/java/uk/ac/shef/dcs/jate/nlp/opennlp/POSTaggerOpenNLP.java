package uk.ac.shef.dcs.jate.nlp.opennlp;

import opennlp.tools.postag.POSModel;
import opennlp.tools.postag.POSTaggerME;
import uk.ac.shef.dcs.jate.nlp.POSTagger;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;

/**
 * Created by zqz on 28/09/2015.
 */
public class POSTaggerOpenNLP implements POSTagger {
    private opennlp.tools.postag.POSTagger tagger;

    public POSTaggerOpenNLP(InputStream model) throws IOException {
        tagger = new POSTaggerME(new POSModel(model));
    }

    @Override
    public String[] tag(String[] tokens) {
        return tagger.tag(tokens);
    }
}
