package org.apache.lucene.analysis.jate;

import java.io.Serializable;

/**
 *
 */
public enum MWEMetadataType implements Serializable{

    SOURCE_PARAGRAPH_ID_IN_DOC("paragraph-id-in-doc"),
    PARAGRAPHS_IN_DOC("paragraphs-in-doc"),
    //paragraphs. The first paragraph in a document is considered the doc title
    SOURCE_SENTENCE_ID_IN_DOC("sentence-id-in-doc"),
    SENTENCES_IN_DOC("sentences-in-doc"),
    SOURCE_SENTENCE_ID_IN_PARAGRAPH("sentence-id-in-paragraph"),
    SENTENCES_IN_PARAGRAPH("sentences-in-paragraph"),
    //sentences. The first paragraph in a document is considered the doc title

    FIRST_COMPOSING_TOKEN_ID_IN_SENT("first-token-index"),
    LAST_COMPOSING_TOKEN_ID_IN_SENT("last-token-index"),
    POS("pos"),

    HAS_UPPERCASE("has_uppercase"),
    HAS_DIGIT("has_digit"), //p28n
    HAS_SYMBOL("has_symbol"),
    HAS_ACRONYM_TOKEN("has_acronym_token"), //the NBC corp, the X26 flight
    HAS_NUMERIC_TOKEN("has_numeric_token"); //the 28 lane,

    private static final long serialVersionUID = -9172128488887341289L;
    private String prop;

    private MWEMetadataType(String prop){
        this.prop=prop;
    }

    public String getProp(){
        return prop;
    }


}
