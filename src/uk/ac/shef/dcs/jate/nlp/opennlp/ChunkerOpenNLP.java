package uk.ac.shef.dcs.jate.nlp.opennlp;


import opennlp.tools.chunker.ChunkerME;
import opennlp.tools.chunker.ChunkerModel;
import uk.ac.shef.dcs.jate.nlp.Chunker;

import java.io.File;
import java.io.IOException;

/**
 *
 */
public class ChunkerOpenNLP implements Chunker {
    private opennlp.tools.chunker.Chunker chunker;

    public ChunkerOpenNLP(String modelFile) throws IOException {
        chunker=new ChunkerME(new ChunkerModel(new File(modelFile)));
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
