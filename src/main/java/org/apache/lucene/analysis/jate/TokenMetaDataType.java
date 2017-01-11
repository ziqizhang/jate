package org.apache.lucene.analysis.jate;

/**
 *
 */
enum TokenMetaDataType {

    SOURCE_PARAGRAPH_ID_IN_DOC("paragraph-id-in-doc"),
    PARAGRAPHS_IN_DOC("paragraphs-in-doc"),
    SOURCE_PARAGRAPH_DIST_FRM_DOC_TITLE("paragraphs-from-doc-title"), //how far away from the title of the document, distance as the number of
    //paragraphs. The first paragraph in a document is considered the doc title
    SOURCE_SENTENCE_ID_IN_DOC("sentence-id-in-doc"),
    SENTENCES_IN_DOC("sentences-in-doc"),
    SOURCE_SENTENCE_ID_IN_PARAGRAPH("sentence-id-in-paragraph"),
    SENTENCES_IN_PARAGRAPH("sentences-in-paragraph"),
    SOURCE_SENTENCE_DIST_FRM_DOC_TITLE("sentences-from-doc-title"), //how far away from the title of the document, distance as the number of
    //sentences. The first paragraph in a document is considered the doc title

    FIRST_COMPOSING_TOKEN_ID_IN_DOC("first-token-index"),
    LAST_COMPOSING_TOKEN_ID_IN_DOC("last-token-index"),
    TOKEN_POS("pos");

    private String prop;

    private TokenMetaDataType(String prop){
        this.prop=prop;
    }

    public String getProp(){
        return prop;
    }


}
