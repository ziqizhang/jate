package uk.ac.shef.dcs.jate.feature;

/**
 * Represents a context of a term occurrence.
 * The context contains the document id, the sentence id in the document,
 * the first token index in the sentence, and the last token index in the sentence
 */
public class ContextWindow implements Comparable<ContextWindow>{

    private int docId=-1;
    private int sentenceId=-1;
    private int firstTok =-1;
    private int lastTok =-1;

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

    public int getFirstTok() {
        return firstTok;
    }

    public void setFirstTok(int firstTok) {
        this.firstTok = firstTok;
    }

    public int getLastTok() {
        return lastTok;
    }

    public void setLastTok(int lastTok) {
        this.lastTok = lastTok;
    }

    public boolean equals(Object o) {
        if (o instanceof ContextWindow) {
            ContextWindow ctx = (ContextWindow) o;
            return ctx.getDocId()==getDocId() &&
                    ctx.getSentenceId()==getSentenceId() &&
                    ctx.getFirstTok() == getFirstTok() &&
                    ctx.getLastTok() == getLastTok();
        }
        return false;
    }

    public String getContextId(){
        StringBuilder sb = new StringBuilder();
        sb.append("d=").append(docId).append(",st=").append(sentenceId)
                .append(",f=").append(firstTok).append(",l=").append(lastTok);
        return sb.toString();
    }

    @Override
    public int compareTo(ContextWindow o) {
        if(docId==o.docId){
            if(sentenceId==o.sentenceId){
                if(firstTok ==o.firstTok){
                    return Integer.valueOf(lastTok).compareTo(o.lastTok);
                }
                return Integer.valueOf(firstTok).compareTo(o.firstTok);
            }
            return Integer.valueOf(sentenceId).compareTo(o.sentenceId);
        }
        return Integer.valueOf(docId).compareTo(o.docId);
    }

    public String toString(){
        return getContextId();
    }
}
