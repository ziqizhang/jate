package uk.ac.shef.dcs.jate.v2;

import org.apache.commons.lang.exception.ExceptionUtils;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;
import java.util.logging.Logger;

/**
 * Created by zqz on 17/09/2015.
 */
public class JATEProperties {

    private static final Logger LOG = Logger.getLogger(JATEProperties.class.getName());
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

    public String getSolrHome() throws JATEException{
        String fieldnameID = getString("solrhome");
        if (fieldnameID == null)
            throw new JATEException("'solrhome' not defined in jate.properties");
        return fieldnameID;
    }

    public String getSolrFieldnameID() throws JATEException {
        String fieldnameID = getString("fieldname_id");
        if (fieldnameID == null)
            throw new JATEException("'fieldname_id' not defined in jate.properties");
        return fieldnameID;
    }

    public String getSolrFieldnameJATETermsAll() throws JATEException {
        String fieldnameID = getString("fieldname_jate_terms_all");
        if (fieldnameID == null)
            throw new JATEException("'fieldname_jate_terms_all' not defined in jate.properties");
        return fieldnameID;
    }

    public String getSolrFieldnameJATEWordsAll() {
        String fieldnameID = getString("fieldname_jate_words_all");
        return fieldnameID;
    }

    public String getSolrFieldnameJATESentencesAll() {
        String fieldnameID = getString("fieldname_jate_sentences_all");
        return fieldnameID;
    }

    public String getSolrFieldnameJATETermsF(){
        String fieldnameID = getString("fieldname_jate_terms_f");
        return fieldnameID;
    }

    private String getString(String propertyName) {
        String string = prop.getProperty(propertyName);
        return string;
    }

    public int getIndexerMaxDocsPerWorker(){
        int defaultMax=100;
        try{
            int v= getInt("indexer_max_docs_per_worker");
            if(v<1) {
                LOG.warning("'indexer_max_docs_per_worker' illegal value:"+v+". Default=100 is used.");
                v = defaultMax;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'indexer_max_docs_per_worker' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            LOG.warning(sb.toString());
            return defaultMax;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'indexer_max_docs_per_worker' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            LOG.warning(sb.toString());
            return defaultMax;
        }
    }

    public int getIndexerMaxUnitsToCommit(){
        int defaultMax=500;
        try{
            int v= getInt("indexer_max_units_to_commit");
            if(v<1) {
                LOG.warning("'indexer_max_units_to_commit' illegal value:"+v+". Default=500 is used.");
                v = defaultMax;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'indexer_max_units_to_commit' illegal value. Default=500 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            LOG.warning(sb.toString());
            return defaultMax;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'indexer_max_units_to_commit' illegal value. Default=500 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            LOG.warning(sb.toString());
            return defaultMax;
        }
    }

    public int getFeatureBuilderMaxDocsPerWorker(){
        int defaultMax=50;
        try{
            int v= getInt("featurebuilder_max_docs_per_worker");
            if(v<1) {
                LOG.warning("'featurebuilder_max_docs_per_worker' illegal value:"+v+". Default=50 is used.");
                v = defaultMax;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'featurebuilder_max_docs_per_worker' illegal value. Default=50 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            LOG.warning(sb.toString());
            return defaultMax;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'featurebuilder_max_docs_per_worker' illegal value. Default=50 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            LOG.warning(sb.toString());
            return defaultMax;
        }
    }

    public int getFeatureBuilderMaxTermsPerWorker(){
        int defaultMax=100;
        try{
            int v= getInt("featurebuilder_max_terms_per_worker");
            if(v<1) {
                LOG.warning("'featurebuilder_max_terms_per_worker' illegal value:"+v+". Default=100 is used.");
                v = defaultMax;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'featurebuilder_max_terms_per_worker' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            LOG.warning(sb.toString());
            return defaultMax;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'featurebuilder_max_terms_per_worker' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            LOG.warning(sb.toString());
            return defaultMax;
        }
    }

    private int getInt(String propertyName) {
        String string = prop.getProperty(propertyName);
        return Integer.valueOf(string);
    }

    public double getFeatureBuilderMaxCPUsage(){
        double defaultMax=1.0;
        try{
            double v= getDouble("max_cpu_usage");
            if(v<=0) {
                LOG.warning("'max_cpu_usage' illegal value:"+v+". Default=1.0 is used.");
                v = defaultMax;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'max_cpu_usage' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            LOG.warning(sb.toString());
            return defaultMax;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'max_cpu_usage' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            LOG.warning(sb.toString());
            return defaultMax;
        }
    }

    private double getDouble(String propertyName) {
        String string = prop.getProperty(propertyName);
        return Double.valueOf(string);
    }
}
