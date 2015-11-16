package org.apache.lucene.analysis.jate;


/**
 *
 */
public class SentenceContext {

    private int sentenceId;
    private int firstTokenIdx;
    private int lastTokenIdx;
    private String posTag;

    public SentenceContext(String string){
        init(string);
    }

    public int getSentenceId() {
        return sentenceId;
    }


    public int getFirstTokenIdx() {
        return firstTokenIdx;
    }


    public int getLastTokenIdx() {
        return lastTokenIdx;
    }


    private void init(String string){
        String[] values= string.split(",");
        //String[] result = new String[4];

        for(String v: values){
            if(v.startsWith("f="))
                firstTokenIdx=Integer.valueOf(v.substring(2));
            else if(v.startsWith("l="))
                lastTokenIdx=Integer.valueOf(v.substring(2));
            else if(v.startsWith("p="))
                posTag=v.substring(2);
            else if(v.startsWith("s="))
                sentenceId=Integer.valueOf(v.substring(2));
        }

    }


    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }
}
