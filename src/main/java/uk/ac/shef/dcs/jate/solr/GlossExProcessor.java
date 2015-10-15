package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppGlossEx;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * Solr Automatic Term Recognition (ATR) Processor for GlossEx algorithm
 * 
 * see {@code uk.ac.shef.dcs.jate.algorithm.GlossEx}}
 *
 */
public class GlossExProcessor implements TermRecognitionProcessor {

	private AppGlossEx glossExSolr = null;
	
	private void initialise(Map<String, String> params) throws JATEException {
		if (this.glossExSolr == null) {
			this.glossExSolr = new AppGlossEx(params);
		}
	}
	
	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params,
			Algorithm algorithm) throws IOException, JATEException {
		if (Algorithm.GLOSSEX.equals(algorithm)) {
			initialise(params);
			return this.glossExSolr.extract(core, jatePropertyFile);
		}
		return null;
	}

}
