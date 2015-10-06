package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppNCValue;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * Solr Automatic Term Recognition (ATR) Processor for NCValue algorithm
 * 
 * see {@code uk.ac.shef.dcs.jate.algorithm.NCValue}}
 *
 */
public class NCValueProcessor implements TermRecognitionProcessor {

	private AppNCValue ncValueSolr = null;
	
	private void initialise(Map<String, String> params) throws JATEException {
		if (this.ncValueSolr == null) {
			this.ncValueSolr = new AppNCValue(params);
		}
	}
	
	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params,
			Algorithm algorithm) throws IOException, JATEException {
		if (Algorithm.NC_VALUE.equals(algorithm)) {
			initialise(params);
			return this.ncValueSolr.extract(core, jatePropertyFile);
		}
		return null;
	}

}
