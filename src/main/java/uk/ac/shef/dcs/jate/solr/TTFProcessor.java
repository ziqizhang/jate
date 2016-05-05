package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppTTF;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * Solr Automatic Term Recognition (ATR) Processor for TTF algorithm
 * <p>
 * see {@code uk.ac.shef.dcs.jate.algorithm.TTF}}
 */
public class TTFProcessor implements TermRecognitionProcessor {

    private AppTTF ttfSolr = null;

    /**
     * initialise run-time parameters for current algorithm
     *
     * @param params, run-time parameters (e.g.,min term total freq, cutoff scoring threshold)  for current algorithm
     * @throws JATEException
     * @see uk.ac.shef.dcs.jate.app.AppParams
     */
    public void initialise(Map<String, String> params) throws JATEException {
        if (this.ttfSolr == null) {
            this.ttfSolr = new AppTTF(params);
        }
    }

    @Override
    public Boolean candidateExtraction(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        return null;
    }

    @Override
    public List<JATETerm> rankingAndFiltering(SolrCore core, String jatePropertyFile, Map<String, String> params,
                                              Algorithm algorithm) throws IOException, JATEException {
        if (Algorithm.TTF.equals(algorithm)) {
            initialise(params);
            return this.ttfSolr.extract(core, jatePropertyFile);
        }
        return null;
    }

    @Override
    public Boolean export(List<JATETerm> termsResults) throws IOException {
        if (ttfSolr != null) {
            ttfSolr.write(termsResults);
            return true;
        }
        return null;
    }
}
