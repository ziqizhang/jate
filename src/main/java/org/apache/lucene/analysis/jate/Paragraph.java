package org.apache.lucene.analysis.jate;

import java.util.HashMap;
import java.util.Map;

/**
 *
 */
class Paragraph {
    int startOffset;
    int endOffset;

    Map<TokenMetaDataType, String> properties = new HashMap<>();

    protected Paragraph(int startOffset, int endOffset){
        this.startOffset=startOffset;
        this.endOffset=endOffset;
    }

    protected void addProperty(TokenMetaDataType prop, String value){
        properties.put(prop, value);
    }

    protected Map<TokenMetaDataType, String> getProperties(){
        return properties;
    }
}
