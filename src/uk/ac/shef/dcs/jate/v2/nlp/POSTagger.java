package uk.ac.shef.dcs.jate.v2.nlp;

/**
 * Created by zqz on 28/09/2015.
 */
public interface POSTagger {
    String[] tag(String[] tokens);
}
