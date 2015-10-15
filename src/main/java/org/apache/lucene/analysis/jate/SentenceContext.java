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

    public static String createString(String firstTokenIdx, String lastTokenIdx, String sentenceId){
        StringBuilder sb = new StringBuilder(firstTokenIdx);
        sb.append(",").append(lastTokenIdx).append(",").append(sentenceId);
        return sb.toString();
    }

    public static String[] parseString(String string){
        String[] values= string.split(",");
        if(values.length!=3){
            String[] padded = new String[3];
            for(int i=0; i<values.length; i++)
                padded[i]=values[i];
            for(int i=values.length; i<padded.length; i++)
                padded[i]="";
            return padded;
        }
        return values;

    }

    public static String parseSentenceId(String s) {
        int start = s.lastIndexOf(",")+1;
        return start>=s.length()?"":s.substring(start);
    }
}
