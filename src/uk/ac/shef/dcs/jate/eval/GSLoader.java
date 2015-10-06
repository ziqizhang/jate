package uk.ac.shef.dcs.jate.eval;

import java.util.ArrayList;
import java.util.List;

/**
 * load gs from file
 */
public class GSLoader {
    //todo

    /**
     *
     * @param file
     * @param lowercase
     * @param normalize if true, will run ascii unfolding then Lucene simple english stemmer
     * @param stopwords
     * @param stopwordsRemoval
     * @return
     */
    public static List<String> loadGenia(String file, boolean lowercase, boolean normalize, List<String> stopwords, int stopwordsRemoval){
        List<String> terms = new ArrayList<>();

        return terms;
    }

    public static List<String> loadACLRD(String file, boolean lowercase, boolean normalize, List<String> stopwords, int stopwordsRemoval){
        List<String> terms = new ArrayList<>();

        return terms;
    }


}
