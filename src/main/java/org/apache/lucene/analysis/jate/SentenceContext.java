package org.apache.lucene.analysis.jate;


import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;

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

    public SentenceContext(TokenMetaData metaData) {
        init(metaData);
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


    private void init(TokenMetaData metadata) {
        sentenceId = Integer.valueOf(metadata.getMetaData(TokenMetaDataType.SOURCE_SENTENCE_ID_IN_DOC));
        firstTokenIdx = Integer.valueOf(metadata.getMetaData(TokenMetaDataType.FIRST_COMPOSING_TOKEN_ID_IN_DOC));
        lastTokenIdx = Integer.valueOf(metadata.getMetaData(TokenMetaDataType.LAST_COMPOSING_TOKEN_ID_IN_DOC));
        posTag = metadata.getMetaData(TokenMetaDataType.TOKEN_POS);
        //totalSentsInDoc=Integer.valueOf(metadata.getMetaData(TokenMetaDataType.SENTENCES_IN_DOC));
    }

    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }
}
