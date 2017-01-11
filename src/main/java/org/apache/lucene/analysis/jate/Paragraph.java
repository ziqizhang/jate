package org.apache.lucene.analysis.jate;


/**
 *
 */
class Paragraph {
    protected int startOffset;
    protected int endOffset;
    protected int indexInDoc;

    public Paragraph(int start, int end, int indexInDoc){
        this.startOffset=start;
        this.endOffset=end;
        this.indexInDoc=indexInDoc;
    }
}
