package uk.ac.shef.dcs.jate.solr;

import org.apache.solr.core.SolrCore;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppRIDF;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

import java.io.IOException;
import java.util.List;
import java.util.Map;


/**
 * Solr Automatic Term Recognition (ATR) Processor for RIDF algorithm
 *
 * see {@code uk.ac.shef.dcs.jate.algorithm.RIDF}}
 *
 */
public class RIDFProcessor implements TermRecognitionProcessor {

    private AppRIDF ridfSolr = null;

    @Override
    public Boolean candidateExtraction(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        return null;
    }

    /**
     * initialise run-time parameters for current algorithm
     * @param params  run-time parameters (e.g.,min term total freq, cutoff scoring threshold)  for current algorithm
     *      @see uk.ac.shef.dcs.jate.app.AppParams
     * @throws JATEException
     */
    public void initialise(Map<String, String> params) throws JATEException {
        if(this.ridfSolr == null) {
            this.ridfSolr = new AppRIDF(params);
        }
    }

    @Override
    public List<JATETerm> rankingAndFiltering(SolrCore core,
                                              String jatePropertyFile,
                                              Map<String, String> params,
                                              Algorithm algorithm) throws IOException, JATEException {
        if (Algorithm.RIDF.equals(algorithm)) {
            initialise(params);
            return this.ridfSolr.extract(core, jatePropertyFile);
        }
        return null;
    }

    @Override
    public Boolean export(List<JATETerm> termsResults) throws IOException {
        if(ridfSolr != null) {
            ridfSolr.write(termsResults);
            return true;
        }
        return null;
    }
}
