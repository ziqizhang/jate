package uk.ac.shef.dcs.jate.v2;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by zqz on 17/09/2015.
 */
public class JATEProperties {

    private Properties prop = new Properties();

    public JATEProperties(String propFile) throws IOException {
        prop.load(new FileInputStream(propFile));
    }

    public String getSolrCorename() throws JATEException{
        String fieldnameID = getString("solrcore");
        if (fieldnameID == null)
            throw new JATEException("'solrcore' not defined in jate.properties");
        return fieldnameID;
    }

    public String getSolrFieldnameID() throws JATEException {
        String fieldnameID = getString("fieldname_id");
        if (fieldnameID == null)
            throw new JATEException("'fieldname_id' not defined in jate.properties");
        return fieldnameID;
    }

    public String getSolrFieldnameJATETextAll() throws JATEException {
        String fieldnameID = getString("fieldname_jate_text_all");
        if (fieldnameID == null)
            throw new JATEException("'fieldname_jate_text_all' not defined in jate.properties");
        return fieldnameID;
    }

    public String getSolrFieldnameJATETextF(){
        String fieldnameID = getString("fieldname_jate_text_f");
        return fieldnameID;
    }

    private String getString(String propertyName) {
        String string = prop.getProperty(propertyName);
        return string;
    }
}
