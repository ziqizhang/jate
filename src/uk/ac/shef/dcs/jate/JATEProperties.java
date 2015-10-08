package uk.ac.shef.dcs.jate;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.FileInputStream;
import java.io.IOException;
import java.util.Properties;

/**
 * Created by zqz on 17/09/2015.
 */
public class JATEProperties {

    //private static final Logger LOG = Logger.getLogger(JATEProperties.class.getName());
    private final Logger log = LoggerFactory.getLogger(getClass());
    
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
        String solrHome = getString("solrhome");
        if (solrHome == null)
            throw new JATEException("'solrhome' not defined in jate.properties");
        return solrHome;
    }

    public String getSolrFieldnameID() throws JATEException {
        String idField = getString("solr_field_id");
        if (idField == null)
            throw new JATEException("'solr_field_id' not defined in jate.properties");
        return idField;
    }

    public String getSolrFieldnameJATENGramInfo() throws JATEException {
        String ngramField = getString("solr_field_content_ngrams");
        if (ngramField == null)
            throw new JATEException("'solr_field_content_ngrams' not defined in jate.properties");
        return ngramField;
    }

    public String getSolrFieldnameJATECTerms() throws JATEException {
        String content2terms = getString("solr_field_content_terms");
        if (content2terms == null)
            throw new JATEException("term candidate field 'solr_field_content_terms' is not defined in jate.properties");
        return content2terms;
    }

    public String getSolrFieldnameJATEWords() {
        String content2words = getString("solr_field_content_words");
        if (content2words == null)
        	log.debug("'solr_field_content_words' not defined in jate.properties");
        return content2words;
    }

    public String getSolrFieldnameJATESentences() {
        String sentences = getString("solr_field_sentences");
        if (sentences == null) {
        	log.debug("'solr_field_sentences' not defined in jate.properties");
        }
        return sentences;
    }

    public String getSolrFieldnameJATECTermsF(){
        String docparts2terms = getString("solr_field_map_doc_parts");
        if (docparts2terms == null) {
        	log.debug("Dynamic field 'solr_field_map_doc_parts' is not defined in jate.properties");
        }
        return docparts2terms;
    }
    
    public String getMinTotalTermFreq() {
    	String min_total_term_freq = getString("min_total_term_freq");
    	if (min_total_term_freq == null) {
    		log.debug("Frequency threshold 'min_total_term_freq' for term candidate filtering is not defined in jate.property");
    	}
    	return min_total_term_freq;
    }
    
    public String getMinTermContextFreq() {
    	String min_term_context_freq = getString("min_term_context_freq");
    	if (min_term_context_freq == null) {
    		log.debug("Context frequency threshold 'min_term_context_freq' (optional) for term candidate filtering is not defined in jate.property");
    	}
    	return min_term_context_freq;
    }
    
    private String getString(String propertyName) {
        String string = prop.getProperty(propertyName);
        return string;
    }


    public int getIndexerMaxUnitsToCommit(){
        int defaultMax=500;
        try{
            int v= getInt("indexer_max_units_to_commit");
            if(v<1) {
                log.warn("'indexer_max_units_to_commit' illegal value:"+v+". Default=500 is used.");
                v = defaultMax;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'indexer_max_units_to_commit' illegal value. Default=500 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            log.warn(sb.toString());
            return defaultMax;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'indexer_max_units_to_commit' illegal value. Default=500 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            log.warn(sb.toString());
            return defaultMax;
        }
    }

    private int getInt(String propertyName) {
        String string = prop.getProperty(propertyName);
        return Integer.valueOf(string);
    }

    public int getCandidateExtractionMaxCPUCores(){
        int defaultV=1;
        try{
            int v= getInt("max_cores_candidate_extraction");
            if(v<=0) {
                log.warn(String.format("'max_cores_candidate_extraction' illegal value: %s. Default=1.0 is used.", v));
                v = defaultV;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'max_cores_candidate_extraction' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            log.warn(sb.toString());
            return defaultV;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'max_cores_candidate_extraction' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            log.warn(sb.toString());
            return defaultV;
        }
    }

    public int getCandidateScoringRankingMaxCPUCores(){
        int defaultV=1;
        try{
            int v= getInt("max_cores_candidate_scoring_ranking");
            if(v<=0) {
                log.warn(String.format("'max_cores_candidate_scoring_ranking' illegal value: %s. Default=1.0 is used.", v));
                v = defaultV;
            }
            return v;
        }catch (NumberFormatException nfe){
            StringBuilder sb = new StringBuilder("'max_cores_candidate_scoring_ranking' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(nfe));
            log.warn(sb.toString());
            return defaultV;
        }catch(NullPointerException ne){
            StringBuilder sb = new StringBuilder("'max_cores_candidate_scoring_ranking' illegal value. Default=100 is used.");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(ne));
            log.warn(sb.toString());
            return defaultV;
        }
    }

    private double getDouble(String propertyName) {
        String string = prop.getProperty(propertyName);
        return Double.valueOf(string);
    }

    public String getSentenceSplitterClass() {
        return getString("nlp_sentence_splitter_class");
    }
    public String getSentenceSplitterParams() {
        return getString("nlp_sentence_splitter_params");
    }
}
