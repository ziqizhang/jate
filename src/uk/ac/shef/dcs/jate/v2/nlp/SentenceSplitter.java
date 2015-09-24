package uk.ac.shef.dcs.jate.v2.nlp;

import java.util.List;

/**
 * Created by zqz on 24/09/2015.
 */
public interface SentenceSplitter {
    List<int[]> split(String text);
}
