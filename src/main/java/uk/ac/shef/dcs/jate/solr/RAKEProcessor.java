package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppRAKE;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * Solr Automatic Term Recognition (ATR) Processor for RAKE algorithm
 * 
 * see {@code uk.ac.shef.dcs.jate.algorithm.RAKE}}
 *
 */
public class RAKEProcessor implements TermRecognitionProcessor {

	private AppRAKE rakeSolr = null;
	
	private void initialise(Map<String, String> params) throws JATEException {
		if (this.rakeSolr == null) {
			this.rakeSolr = new AppRAKE(params);
		}
	}
	
	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params,
			Algorithm algorithm) throws IOException, JATEException {
		if (Algorithm.RAKE.equals(algorithm)) {
			initialise(params);
			return this.rakeSolr.extract(core, jatePropertyFile);
		}
		return null;
	}

}
