package org.apache.lucene.analysis.jate;

/**
 *
 */
enum TokenMetaDataType {

    SOURCE_PARAGRAPH_ID_IN_DOC("paragraph-id-in-doc"),
    SOURCE_PARAGRAPH_ID_IN_SECTION("paragraph-id-in-section"),
    SOURCE_PARAGRAPH_DIST_FRM_DOC_TITLE("paragraphs-from-doc-title"), //how far away from the title of the document, distance as the number of
    PARAGRAPHS_IN_DOC("paragraphs-in-doc"),
    //paragraphs
    SOURCE_PARAGRAPH_DIST_FRM_SECTION_TITLE("paragraphs-from-section-title"), //how far away from the closest section title
    PRAGRAPHS_IN_SECTION("paragraphs-in-section"),
    //distance as number of paragraphs
    SOURCE_PARAGRAPH_IS_DOC_TITLE("paragraph-is-doc-title"),
    SOURCE_PARAGRAPH_IS_SECTION_TITLE("paragraph-is-section-title"),

    SOURCE_SENTENCE_ID_IN_DOC("sentence-id-in-doc"),
    SOURCE_SENTENCE_ID_IN_PARAGRAPH("sentence-id-in-paragraph"),
    SOURCE_SENTENCE_DIST_FRM_DOC_TITLE("sentences-from-doc-title"), //how far away from the title of the document, distance as the number of
    SENTENCES_IN_DOC("sentences-in-doc"),
    //paragraphs
    SOURCE_SENTENCE_DIST_FRM_SECTION_TITLE("sentences-from-section-title"), //how far away from the closest section title
    SENTENCES_IN_PARAGRAPH("sentences-in-paragraph"),
    //distance as number of paragraphs

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
