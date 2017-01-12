package org.apache.lucene.analysis.jate;

import com.google.gson.Gson;
import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MWEMetadata implements Serializable {
    private static final long serialVersionUID = -3117653988678036089L;
    private static final Logger log = Logger.getLogger(MWEMetadata.class.getName());
    Map<MWEMetadataType, String> metadata = new HashMap<>();

    public void addMetaData(MWEMetadataType prop, String value) {
        metadata.put(prop, value);
    }

    public String getMetaData(MWEMetadataType prop) {
        return metadata.get(prop);
    }

    public static final String serialize(MWEMetadata data) {
        Gson gson = new Gson();
        String json=gson.toJson(data);
        /*try {
            return json.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            return json.getBytes();
        }*/
        return json;
    }

    public static final MWEMetadata deserialize(String json) {
       // String json = new String(data, Charset.forName("UTF-8")).trim();
        Gson gson = new Gson();
        MWEMetadata obj=gson.fromJson(json, MWEMetadata.class);
        return obj;
    }
}
