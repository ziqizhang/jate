package uk.ac.shef.dcs.jate.v2.model;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by zqz on 15/09/2015.
 */
public class JATEDocument {
    protected String id;
    protected String path;
    protected String content;
    protected Map<String, String> mapField2Content = new HashMap<String, String>();

    public JATEDocument(String id){
        this.id=id;
    }

    public String getId(){return id;}
    public void setPath(String path){
        this.path=path;
    }

    public void setContent(String content){
        this.content=content;
    }

    public String getContent(){
        return this.content;
    }

    public Map<String, String> getMapField2Content(){
        return mapField2Content;
    }

    public String toString(){
        StringBuilder sb = new StringBuilder("id=");
        sb.append(id).append(",path=").append(path);
        return sb.toString();
    }
}
