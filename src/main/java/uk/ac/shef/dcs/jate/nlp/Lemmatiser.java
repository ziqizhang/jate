package uk.ac.shef.dcs.jate.nlp;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by - on 26/02/2016.
 */
public class Lemmatiser {

    private final EngLemmatiser lemmatiser;
    private Map<String, Integer> tagLookUp;

    public Lemmatiser(EngLemmatiser lemmatiser) {

        this.lemmatiser=lemmatiser;
        tagLookUp = new HashMap<>();
        tagLookUp.put("NN", 1);
        tagLookUp.put("NNS", 1);
        tagLookUp.put("NNP", 1);
        tagLookUp.put("NNPS", 1);
        tagLookUp.put("VB", 2);
        tagLookUp.put("VBG", 2);
        tagLookUp.put("VBD", 2);
        tagLookUp.put("VBN", 2);
        tagLookUp.put("VBP", 2);
        tagLookUp.put("VBZ", 2);
        tagLookUp.put("JJ", 3);
        tagLookUp.put("JJR", 3);
        tagLookUp.put("JJS", 3);
        tagLookUp.put("RB", 4);
        tagLookUp.put("RBR", 4);
        tagLookUp.put("RBS", 4);
    }
    /**
     * Lemmatise a phrase or word. If a phrase, only lemmatise the most RHS word.
     * @param value
     * @return
     */
    public String normalize(String value, String pos) {
        Integer tag = tagLookUp.get(pos);
        tag=tag==null?1:tag;
        int space = value.lastIndexOf(" ");
        if(space==-1||value.endsWith("'s")) //if string is a single word, or it is in "XYZ's" form where the ' char has been removed
            return lemmatiser.lemmatize(value,tag).trim();


        String part1 = value.substring(0,space);
        String part2 = lemmatiser.lemmatize(value.substring(space+1),tag);
        return (part1+" "+part2).trim();

    }
}
