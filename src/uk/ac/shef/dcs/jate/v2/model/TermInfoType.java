package uk.ac.shef.dcs.jate.v2.model;

/**
 * Created by zqz on 19/09/2015.
 */
public enum TermInfoType {

    PROVENANCE("provenance"),
    PARENT("parent");

    private String name;
    private TermInfoType(String name){
        this.name=name;
    }
    public String toString(){
        return name;
    }
}
