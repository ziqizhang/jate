package uk.ac.shef.dcs.jate;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Created by zqz on 17/09/2015.
 */
public class JATEProperties {

    //private static final Logger LOG = Logger.getLogger(JATEProperties.class.getName());
    private final Logger log = LoggerFactory.getLogger(getClass());

    private Properties prop = new Properties();

    public static final String PROPERTIES_FILE = "jate.properties";
    // solr home (core container) property
    public static final String PROPERTY_SOLR_HOME = "solrhome";
    // solr core name property
    public static final String PROPERTY_SOLR_CORE = "solrcore";
    // The Solr uniqueKey field
    public static final String PROPERTY_SOLR_FIELD_ID = "solr_field_id";
    // n-grams field from a corpus
    public static final String PROPERTY_SOLR_FIELD_CONTENT_NGRAMS = "solr_field_content_ngrams";
    // solr content/text field
    public static final String PROPERTY_SOLR_FIELD_CONTENT_TERMS = "solr_field_content_terms";
    // document metadata extracted from Tika where term will be extracted from
    // see also @code{uk.ac.shef.dcs.jate.io.TikaMultiFieldDocumentCreator}
    public static final String PROPERTY_SOLR_FIELD_MAP_DOC_PARTS = "solr_field_map_doc_parts";
    //minimum frequency allowed for term candidates
    public static final String PROPERTY_MIN_TOTAL_TERM_FREQ = "min_total_term_freq";
    //min frequency of a term appearing in different context
    public static final String PROPERTY_MIN_TERM_CONTEXT_FREQ = "min_term_context_freq";
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

//    public static void main(String[] args) {
//        final InputStream stream = JATEProperties.class.getClassLoader().getResourceAsStream("jate.properties");
//        try {
//            Properties prop = new Properties();
//            prop.load(stream);
//
//            System.out.println(prop.getProperty("solrhome"));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//    }

    public JATEProperties(String propFile) throws JATEException {
        try {
            prop.load(new FileInputStream(propFile));
        } catch (IOException e) {
            throw new JATEException(String.format("Specified properties file not found! [%s]", propFile));
        }
    }

    public String getSolrCoreName() throws JATEException {
        String solrCoreName = getString(PROPERTY_SOLR_CORE);
        if (solrCoreName == null)
            throw new JATEException(String.format("'%s' not defined in jate.properties", PROPERTY_SOLR_CORE));
        return solrCoreName;
    }

    public void setSolrCoreName(String coreName) {
        prop.setProperty(PROPERTY_SOLR_CORE, coreName);
    }

    public String getSolrHome() throws JATEException {
        String solrHome = getString(PROPERTY_SOLR_HOME);
        if (solrHome == null)
            throw new JATEException(String.format("'%s' not defined in jate.properties", PROPERTY_SOLR_HOME));
        return solrHome;
    }

    public void setSolrHome(String solrHome) {
        prop.setProperty(PROPERTY_SOLR_HOME, solrHome);
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
            throw new JATEException(String.format("'%s' not defined in jate.properties", PROPERTY_SOLR_FIELD_CONTENT_NGRAMS));
        return ngramField;
    }

    public void setSolrFieldNameJATEGramInfo(String solrFieldNameJATEGramInfo) {
        prop.setProperty(PROPERTY_SOLR_FIELD_CONTENT_NGRAMS, solrFieldNameJATEGramInfo);
    }

    public String getSolrFieldNameJATECTerms() throws JATEException {
        String content2terms = getString(PROPERTY_SOLR_FIELD_CONTENT_TERMS);
        if (content2terms == null)
            throw new JATEException(String.format("term candidate field '%s' is not defined in jate.properties", PROPERTY_SOLR_FIELD_CONTENT_TERMS));
        return content2terms;
    }

    public void setSolrFieldNameJATECTerms(String solrFieldNameJATECTerms) {
        prop.setProperty(PROPERTY_SOLR_FIELD_CONTENT_TERMS, solrFieldNameJATECTerms);
    }

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

    public String getMinTotalTermFreq() {
        String min_total_term_freq = getString(PROPERTY_MIN_TOTAL_TERM_FREQ);
        if (min_total_term_freq == null) {
            log.warn(String.format("Frequency threshold '%s' for term candidate filtering is not defined in jate.property", PROPERTY_MIN_TOTAL_TERM_FREQ));
        }
        return min_total_term_freq;
    }

    public void setMinTotalTermFreq(Integer minTotalTermFreq) {
        prop.setProperty(PROPERTY_MIN_TOTAL_TERM_FREQ, String.valueOf(minTotalTermFreq));
    }

    public String getMinTermContextFreq() {
        String min_term_context_freq = getString(PROPERTY_MIN_TERM_CONTEXT_FREQ);
        if (min_term_context_freq == null) {
            log.debug(String.format("Context frequency threshold '%s' (optional) for term candidate filtering is not defined in jate.property", PROPERTY_MIN_TERM_CONTEXT_FREQ));
        }
        return min_term_context_freq;
    }

    public void setMinTermContextFreq(Integer minTermContextFreq) {
        prop.setProperty(PROPERTY_MIN_TERM_CONTEXT_FREQ, String.valueOf(minTermContextFreq));
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
