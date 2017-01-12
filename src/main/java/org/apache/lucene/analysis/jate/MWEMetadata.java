package org.apache.lucene.analysis.jate;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;

import java.io.*;
import java.util.HashMap;
import java.util.Map;

/**
 *
 */
public class MWEMetadata implements Serializable {
    private static final long serialVersionUID = -9117653988678036089L;
    private static final Logger log = Logger.getLogger(MWEMetadata.class.getName());
    Map<MWEMetadataType, String> metadata = new HashMap<>();

    public void addMetaData(MWEMetadataType prop, String value) {
        metadata.put(prop, value);
    }

    public String getMetaData(MWEMetadataType prop) {
        return metadata.get(prop);
    }

    public static final byte[] serialize(MWEMetadata data) {
        try {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ObjectOutputStream os = new ObjectOutputStream(out);
            os.writeObject(data);
            return out.toByteArray();
        } catch (IOException e) {
            log.error("SEVERE: serialization of MWEMetadata failed due to:\n" + ExceptionUtils.getFullStackTrace(e));
            return new byte[0];
        }
    }

    public static final MWEMetadata deserialize(byte[] data) {
        try {
            ByteArrayInputStream in = new ByteArrayInputStream(data);
            ObjectInputStream is = new ObjectInputStream(in);
            return (MWEMetadata) is.readObject();
        } catch (Exception e) {
            log.error("SEVERE: deserialization of MWEMetadata failed due to:\n" + ExceptionUtils.getFullStackTrace(e));
            return new MWEMetadata();
        }
    }
}
