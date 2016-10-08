package uk.ac.shef.dcs.jate.nlp.opennlp;


import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import uk.ac.shef.dcs.jate.nlp.Chunker;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 */
public class ChunkerOpenNLP implements Chunker {
    private opennlp.tools.chunker.Chunker chunker;

    public ChunkerOpenNLP(InputStream model) throws IOException {
        chunker=new ChunkerME(new ChunkerModel(model));
    }

    public String[] chunk(String[] tokens, String[] posTags){
        String[] chunkerTags=chunker.chunk(tokens, posTags);
        return chunkerTags;
    }

    public String getStartTag(){
        return "B-NP";
    }
    public String getEndTag(){
        return "";
    }
    public String getContinueTag(){
        return "I-NP";
    }

}
