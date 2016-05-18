package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * The interface for all Term Recognition processors
 *
 * @author jieg
 */
public interface TermRecognitionProcessor {

    /**
     * initialise current ATE algorithm
     * @param params  params, run-time parameters (e.g.,min term total freq, cutoff scoring threshold)  for current algorithm
     *      @see uk.ac.shef.dcs.jate.app.AppParams
     */
    void initialise(Map<String, String> params) throws JATEException;

    Boolean candidateExtraction(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException;
    /**
     * ranking and filtering candidate terms
     *
     * @param core              solr core
     * @param jatePropertyFile  jate property file path, if not provided (i.e., null), the file will be automatically
     *                          loaded from the default one
     * @param params            run-time parameters for different TR algorithms, see {@code uk.ac.shef.dcs.jate.app.App.CommandLineParams} for details}
     * @param algorithm         TR algorithm
     * @return List<JATETerm>   a list of terms extracted from current solr core
     * @throws IOException
     * @throws JATEException
     * @see uk.ac.shef.dcs.jate.app.App#getJateProperties(String)
     */
    List<JATETerm> rankingAndFiltering(SolrCore core, String jatePropertyFile, Map<String, String> params, Algorithm algorithm)
            throws IOException, JATEException;

    /**
     * export final terms
     *
     * @param termsResults  term results
     */
    Boolean export(List<JATETerm> termsResults) throws IOException;
}
