package org.apache.lucene.analysis.jate;


/**
 * Represents the context where a candidate term appears in a sentence. The following information is
 * recorded:
 * <br/> sentence id
 * <br/> index of the first token of the candidate as it appears in the sentence
 * <br/> index of the second token of the candidate as it appears in the sentence
 * <br/> pos of the candidate, if makes sense
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
