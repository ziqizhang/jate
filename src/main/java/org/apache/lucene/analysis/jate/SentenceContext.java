package org.apache.lucene.analysis.jate;

import java.util.ArrayList;
import java.util.List;

/**
 *
 */
public class SentenceContext {

    private String sentenceId;
    private String firstTokenIdx;
    private String lastTokenIdx;
    private String posTag;

    public SentenceContext(String string){
        init(string);
    }

    public String getSentenceId() {
        return sentenceId;
    }

    public void setSentenceIds(String id) {
        this.sentenceId=id;
    }

    public String getFirstTokenIdx() {
        return firstTokenIdx;
    }

    public void setFirstTokenIdx(String firstTokenIdx) {
        this.firstTokenIdx = firstTokenIdx;
    }

    public String getLastTokenIdx() {
        return lastTokenIdx;
    }

    public void setLastTokenIdx(String lastTokenIdx) {
        this.lastTokenIdx = lastTokenIdx;
    }

    private void init(String string){
        String[] values= string.split(",");
        String[] result = new String[4];

        for(String v: values){
            if(v.startsWith("f="))
                firstTokenIdx=v.substring(2);
            else if(v.startsWith("l="))
                lastTokenIdx=v.substring(2);
            else if(v.startsWith("p="))
                posTag=v.substring(2);
            else if(v.startsWith("s="))
                sentenceId=v.substring(2);
        }

    }


    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }
}
