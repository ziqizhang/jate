package org.apache.lucene.analysis.jate;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import org.apache.lucene.analysis.TokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.PayloadAttribute;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.nlp.Lemmatiser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by - on 19/02/2016.
 */
public final class EnglishLemmatisationFilter extends TokenFilter {
    private final Lemmatiser lemmatiser;
    private final CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private final PayloadAttribute exitingPayload = addAttribute(PayloadAttribute.class);

    public EnglishLemmatisationFilter(EngLemmatiser dragontoolLemmatiser, TokenStream input) {
        super(input);
        lemmatiser = new Lemmatiser(dragontoolLemmatiser);
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
            if(tok.length()>2) { //words with only 2 chars are unlikely to be inflectional
                //theoretically this is the right way. But in practice, pos is expected to be noun, so using NN is better
                //tok = normalize(tok, pos);
                tok=lemmatiser.normalize(tok, "NN");
            }

            termAtt.setEmpty().append(tok);
            return true;
        } else {
            return false;
        }
    }
}
