package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppCValue;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * Solr Automatic Term Recognition (ATR) Processor for CValue algorithm
 * 
 * see {@code uk.ac.shef.dcs.jate.algorithm.CValue}}
 *
 */
public class CValueProcessor implements TermRecognitionProcessor {

	private AppCValue cValueSolr = null;

	@Override
	public Boolean candidateExtraction(SolrCore core, String jatePropertyFile)
			throws IOException, JATEException {
		return null;
	}

	/**
	 * initialise run-time parameters for current algorithm
	 * @param params, run-time parameters (e.g.,min term total freq, cutoff scoring threshold)  for current algorithm
	 *      @see uk.ac.shef.dcs.jate.app.AppParams
	 * @throws JATEException
	 */
	public void initialise(Map<String, String> params) throws JATEException {
		if (this.cValueSolr == null) {
			this.cValueSolr = new AppCValue(params);
		}
	}
	
	@Override
	public List<JATETerm> rankingAndFiltering(SolrCore core, String jatePropertyFile, Map<String, String> params,
											  Algorithm algorithm) throws IOException, JATEException {
		if (Algorithm.C_VALUE.equals(algorithm)) {
			initialise(params);
			return this.cValueSolr.extract(core, jatePropertyFile);
		}
		return null;
	}

	@Override
	public Boolean export(List<JATETerm> termsResults) throws IOException {
		if (cValueSolr != null) {
			cValueSolr.write(termsResults);
			return true;
		}
		return null;
	}
}
