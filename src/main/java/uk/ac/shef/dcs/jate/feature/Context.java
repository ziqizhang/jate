package uk.ac.shef.dcs.jate.feature;

/**
 * Represents a context of a term occurrence. The context contains the document id, the sentence id in the document,
 * the first token index in the sentence, and the last token index
 */
public class Context implements Comparable<Context>{

    private int docId=-1;
    private int sentenceId=-1;
    private int tokStart=-1;
    private int tokEnd=-1;

    public int getDocId() {
        return docId;
    }

    public void setDocId(int docId) {
        this.docId = docId;
    }

    public int getSentenceId() {
        return sentenceId;
    }

    public void setSentenceId(int sentenceId) {
        this.sentenceId = sentenceId;
    }

    public int getTokStart() {
        return tokStart;
    }

    public void setTokStart(int tokStart) {
        this.tokStart = tokStart;
    }

    public int getTokEnd() {
        return tokEnd;
    }

    public void setTokEnd(int tokEnd) {
        this.tokEnd = tokEnd;
    }

    public boolean equals(Object o) {
        if (o instanceof Context) {
            Context ctx = (Context) o;
            return ctx.getDocId()==getDocId() &&
                    ctx.getSentenceId()==getSentenceId() &&
                    ctx.getTokStart() == getTokStart() &&
                    ctx.getTokEnd() == getTokEnd();
        }
        return false;
    }

    public String getContextId(){
        StringBuilder sb = new StringBuilder();
        sb.append("d=").append(docId).append(",s=").append(sentenceId)
                .append(",ts=").append(tokStart).append(",te=").append(tokEnd);
        return sb.toString();
    }

    @Override
    public int compareTo(Context o) {
        if(docId==o.docId){
            if(sentenceId==o.sentenceId){
                if(tokStart==o.tokStart){
                    return Integer.valueOf(tokEnd).compareTo(o.tokEnd);
                }
                return Integer.valueOf(tokStart).compareTo(o.tokStart);
            }
            return Integer.valueOf(sentenceId).compareTo(o.sentenceId);
        }
        return Integer.valueOf(docId).compareTo(o.docId);
    }
}
