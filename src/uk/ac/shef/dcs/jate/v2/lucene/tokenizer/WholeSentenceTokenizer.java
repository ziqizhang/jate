package uk.ac.shef.dcs.jate.v2.lucene.tokenizer;

import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.tokenattributes.OffsetAttribute;
import org.apache.lucene.analysis.util.SegmentingTokenizerBase;
import org.apache.lucene.util.AttributeFactory;

import java.text.BreakIterator;

/**
 *
 */
public class WholeSentenceTokenizer extends SegmentingTokenizerBase {
    protected int sentenceStart, sentenceEnd;
    protected boolean hasSentence;

    private CharTermAttribute termAtt = addAttribute(CharTermAttribute.class);
    private OffsetAttribute offsetAtt = addAttribute(OffsetAttribute.class);

    public WholeSentenceTokenizer() {
        super(BreakIterator.getSentenceInstance());
    }

    public WholeSentenceTokenizer(BreakIterator iterator) {
        super(iterator);
    }

    public WholeSentenceTokenizer(AttributeFactory factory, BreakIterator iterator) {
        super(factory, iterator);
    }

    @Override
    protected void setNextSentence(int sentenceStart, int sentenceEnd) {
        this.sentenceStart = sentenceStart;
        this.sentenceEnd = sentenceEnd;
        hasSentence = true;
    }

    @Override
    protected boolean incrementWord() {
        if (hasSentence) {
            hasSentence = false;
            clearAttributes();
            termAtt.copyBuffer(buffer, sentenceStart, sentenceEnd - sentenceStart);
            offsetAtt.setOffset(correctOffset(offset + sentenceStart), correctOffset(offset + sentenceEnd));
            return true;
        } else {
            return false;
        }
    }

}
