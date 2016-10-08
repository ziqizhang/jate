package uk.ac.shef.dcs.jate;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * JATE Properties maps to "jate.properties" properties file
 *
 * Provide configuration of integration between JATE and Solr index engine
 */
public class JATEProperties {

    //private static final Logger LOG = Logger.getLogger(JATEProperties.class.getName());
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Properties prop = new Properties();

    public static final String PROPERTIES_FILE = "jate.properties";

    // The Solr uniqueKey field
    public static final String PROPERTY_SOLR_FIELD_ID = "solr_field_id";
    // n-grams field from a corpus
    public static final String PROPERTY_SOLR_FIELD_CONTENT_NGRAMS = "solr_field_content_ngrams";
    // solr content/text field
    public static final String PROPERTY_SOLR_FIELD_CONTENT_TERMS = "solr_field_content_terms";
    // document metadata extracted from Tika where term will be extracted from
    // see also @code{uk.ac.shef.dcs.jate.io.TikaMultiFieldDocumentCreator}
    public static final String PROPERTY_SOLR_FIELD_MAP_DOC_PARTS = "solr_field_map_doc_parts";

    // SOLR (string) field name where final filtered candidate terms will be indexed and stored to
    public static final String PROPERTY_SOLR_FIELD_DOMAIN_TERMS = "solr_field_domain_terms";

    // Maximum of data units each thread (worker) of a SolrParallelIndexingWorker should commit to solr
    public static final String PROPERTY_INDEXER_MAX_UNITS_TO_COMMIT = "indexer_max_units_to_commit";
    // Maximum % of parallel CPU cores used
    public static final String PROPERTY_MAX_CORES = "max_cores";

    public static final Integer VALUE_DEFAULT_INDEXER_MAX_UNITS_TO_COMMIT = 500;

    /**
     * load JATE Properties from class path
     */
    public JATEProperties() throws JATEException {
        InputStream stream = null;
        try {
            stream = JATEProperties.class.getClassLoader().getResourceAsStream(PROPERTIES_FILE);

            try {
                prop.load(stream);
            } catch (IOException e) {
                throw new JATEException(String.format("Properties file '%s' not found in your class path.", PROPERTIES_FILE));
            }
        } finally {
            try {
                if (stream != null) {
                    stream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

    }

    public JATEProperties(String propFile) throws JATEException {
    	FileInputStream propertyFileStream = null;
        try {
        	propertyFileStream = new FileInputStream(propFile);
            prop.load(propertyFileStream);
        } catch (IOException e) {
            throw new JATEException(String.format("Specified properties file not found! [%s]", propFile));
        } finally {
        	if (propertyFileStream != null) {
        		try {
					propertyFileStream.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
        	}
        }
    }


    public String getSolrFieldNameID() throws JATEException {
        String idField = getString(PROPERTY_SOLR_FIELD_ID);
        if (idField == null)
            throw new JATEException(String.format("'%s' not defined in jate.properties", PROPERTY_SOLR_FIELD_ID));
        return idField;
    }

    public void setSolrFieldNameID(String solrFieldNameID) {
        prop.setProperty(PROPERTY_SOLR_FIELD_ID, solrFieldNameID);
    }

    public String getSolrFieldNameJATENGramInfo() throws JATEException {
        String ngramField = getString(PROPERTY_SOLR_FIELD_CONTENT_NGRAMS);
        if (ngramField == null)
            throw new JATEException(String.format("'%s' not specified in jate.properties", PROPERTY_SOLR_FIELD_CONTENT_NGRAMS));
        return ngramField;
    }

    public void setSolrFieldNameJATEGramInfo(String solrFieldNameJATEGramInfo) {
        prop.setProperty(PROPERTY_SOLR_FIELD_CONTENT_NGRAMS, solrFieldNameJATEGramInfo);
    }

    public String getSolrFieldNameJATEDomainTerms() throws JATEException {
       String domainTermsField = getString(PROPERTY_SOLR_FIELD_DOMAIN_TERMS);
       if (domainTermsField == null)
            throw new JATEException(String.format("'%s' not specified in jate.properties", PROPERTY_SOLR_FIELD_DOMAIN_TERMS));
        return domainTermsField;
    }

    public void setSolrFieldNameJATEDomainTerms(String solrFieldNameJATEDomainTerms) {
        prop.setProperty(PROPERTY_SOLR_FIELD_DOMAIN_TERMS, solrFieldNameJATEDomainTerms);
    }

    /**
     * get solr field for candidate terms
     * @return candidate term field specified
     * @throws JATEException
     */
    public String getSolrFieldNameJATECTerms() throws JATEException {
        String content2terms = getString(PROPERTY_SOLR_FIELD_CONTENT_TERMS);
        if (content2terms == null)
            throw new JATEException(String.format("term candidate field '%s' is not defined in jate.properties", PROPERTY_SOLR_FIELD_CONTENT_TERMS));
        return content2terms;
    }

    public void setSolrFieldNameJATECTerms(String solrFieldNameJATECTerms) {
        prop.setProperty(PROPERTY_SOLR_FIELD_CONTENT_TERMS, solrFieldNameJATECTerms);
    }

    /**
     * get solr field specified for document metadata (usually extracted via Tika plugin)
     *
     * @return field name specified for document metadata
     */
    public String getSolrFieldNameJATECTermsF() {
        String docparts2terms = getString(PROPERTY_SOLR_FIELD_MAP_DOC_PARTS);
        if (docparts2terms == null) {
            log.warn(String.format("Dynamic field '%s' is not defined in jate.properties", PROPERTY_SOLR_FIELD_MAP_DOC_PARTS));
        }
        return docparts2terms;
    }

    public void setSolrFieldNameJATETermsF(String solrFieldNameJATETermsF) {
        prop.setProperty(PROPERTY_SOLR_FIELD_MAP_DOC_PARTS, solrFieldNameJATETermsF);
    }

    private String getString(String propertyName) {
        String string = prop.getProperty(propertyName);
        return string;
    }

    public int getIndexerMaxUnitsToCommit() {
        int defaultMax = VALUE_DEFAULT_INDEXER_MAX_UNITS_TO_COMMIT;
        try {
            int v = getInt(PROPERTY_INDEXER_MAX_UNITS_TO_COMMIT);
            if (v < 1) {
                log.warn("'indexer_max_units_to_commit' illegal value:" + v + ". Default=500 is used.");
                v = defaultMax;
            }
            return v;
        } catch (NumberFormatException nfe) {
            StringBuilder sb = new StringBuilder(String.format("'%s' illegal value. Default=500 is used.", PROPERTY_INDEXER_MAX_UNITS_TO_COMMIT));
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            log.warn(sb.toString());
            return defaultMax;
        } catch (NullPointerException ne) {
            StringBuilder sb = new StringBuilder(String.format("'%s' illegal value. Default=500 is used.", PROPERTY_INDEXER_MAX_UNITS_TO_COMMIT));
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            log.warn(sb.toString());
            return defaultMax;
        }
    }

    public int getMaxCPUCores() {
        int defaultV = 1;
        try {
            int v = getInt(PROPERTY_MAX_CORES);
            if (v <= 0) {
                log.warn(String.format("'%s' illegal value: %s. Default=1 is used.", PROPERTY_MAX_CORES, v));
                v = defaultV;
            }
            return v;
        } catch (NumberFormatException nfe) {
            StringBuilder sb = new StringBuilder(String.format("'%s' illegal value. Default=1 is used.", PROPERTY_MAX_CORES));
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            log.warn(sb.toString());
            return defaultV;
        } catch (NullPointerException ne) {
            StringBuilder sb = new StringBuilder(String.format("'%s' illegal value. Default=1 is used.", PROPERTY_MAX_CORES));
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            log.warn(sb.toString());
            return defaultV;
        }
    }

    public void setMaxCPUCores(Integer maxCPUCores) {
        prop.setProperty(PROPERTY_MAX_CORES, String.valueOf(maxCPUCores));
    }

    private int getInt(String propertyName) {
        String string = prop.getProperty(propertyName);
        return Integer.valueOf(string);
    }

}
