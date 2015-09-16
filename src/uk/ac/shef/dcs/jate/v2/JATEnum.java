package uk.ac.shef.dcs.jate.v2;

/**
 * Created by zqz on 16/09/2015.
 */
public enum JATEnum {
    SOLR_FIELD_ID("id"),
    SOLR_FIELD_JATE_ALL_TEXT("jate_all_text"),
    SOLR_CORE_NAME("jate");
    private String string;

    private JATEnum(String string){
        this.string=string;
    }

    public String getString() {
        return string;
    }

}
