package org.apache.lucene.analysis.jate;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.KeywordAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by - on 19/02/2016.
 */
public final class EnglishLemmatisationFilter extends TokenFilter {
    private final EngLemmatiser lemmatiser;
    private Map<String, Integer> tagLookUp;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PayloadAttribute exitingPayload = addAttribute(PayloadAttribute.class);

    public EnglishLemmatisationFilter(EngLemmatiser lemmatiser, TokenStream input) {
        super(input);
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

    @Override
    public boolean incrementToken() throws IOException {
        if (input.incrementToken()) {
            String tok = new String(termAtt.buffer(),0, termAtt.length());
            BytesRef payload = exitingPayload.getPayload();
            String pos="";
            if(payload!=null){
                String attachedInfo = payload.utf8ToString();
                int index = attachedInfo.lastIndexOf("p=");
                if(index!=-1) {
                    pos = attachedInfo.substring(index + 2);
                    int end =pos.indexOf(",");
                    if(end!=-1)
                        pos=pos.substring(0, end);
                }
            }
            //String original=tok;
            tok = normalize(tok, pos);

            termAtt.setEmpty().append(tok);
            return true;
        } else {
            return false;
        }
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
