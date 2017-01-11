package org.apache.lucene.analysis.jate;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class TokenMetaData implements Serializable {
    private static final Logger log = Logger.getLogger(TokenMetaData.class.getName());
    Map<TokenMetaDataType, String> metadata = new HashMap<>();

    public void addMetaData(TokenMetaDataType prop, String value) {
        metadata.put(prop, value);
    }

    public String getMetaData(TokenMetaDataType prop) {
        return metadata.get(prop);
    }

    public static final byte[] serialize(TokenMetaData data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(data);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("SEVERE: serialization of TokenMetaData failed due to:\n" + ExceptionUtils.getFullStackTrace(e));
            return new byte[0];
        }
    }

    public static final TokenMetaData deserialize(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return (TokenMetaData) is.readObject();
        } catch (Exception e) {
            log.error("SEVERE: deserialization of TokenMetaData failed due to:\n" + ExceptionUtils.getFullStackTrace(e));
            return new TokenMetaData();
        }
    }
}
