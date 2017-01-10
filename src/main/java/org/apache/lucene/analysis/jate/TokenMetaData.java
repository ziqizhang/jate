package org.apache.lucene.analysis.jate;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TokenMetaData implements Serializable{
    Map<TokenMetaDataType, String> metadata = new HashMap<>();
    public void addMetaData(TokenMetaDataType prop, String value){
        metadata.put(prop, value);
    }
    public String getMetaData(TokenMetaDataType prop){
        return metadata.get(prop);
    }
}
