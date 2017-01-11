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

    public SentenceContext(MWEMetadata metaData) {
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


    private void init(MWEMetadata metadata) {
        sentenceId = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_DOC));
        firstTokenIdx = Integer.valueOf(metadata.getMetaData(MWEMetadataType.FIRST_COMPOSING_TOKEN_ID_IN_DOC));
        lastTokenIdx = Integer.valueOf(metadata.getMetaData(MWEMetadataType.LAST_COMPOSING_TOKEN_ID_IN_DOC));
        posTag = metadata.getMetaData(MWEMetadataType.POS);
        //totalSentsInDoc=Integer.valueOf(metadata.getMetaData(MWEMetadataType.SENTENCES_IN_DOC));
    }

    public String getPosTag() {
        return posTag;
    }

    public void setPosTag(String posTag) {
        this.posTag = posTag;
    }
}
