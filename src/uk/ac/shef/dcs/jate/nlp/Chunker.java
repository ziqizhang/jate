package uk.ac.shef.dcs.jate.nlp;

/**
 * Created by - on 13/10/2015.
 */
public interface Chunker {
    String[] chunk(String[] tokens, String[] posTags);
    String getStartTag();
    String getEndTag();
}
