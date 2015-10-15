package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppTFIDF;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * Solr Automatic Term Recognition (ATR) Processor for TFIDF algorithm
 * 
 * see {@code uk.ac.shef.dcs.jate.algorithm.TFIDF}}
 *
 */
public class TFIDFProcessor implements TermRecognitionProcessor {

	private AppTFIDF tfidfSolr = null;
	
	private void initialise(Map<String, String> params) throws JATEException {
		if (this.tfidfSolr == null) {
			this.tfidfSolr = new AppTFIDF(params);
		}
	}
	
	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params,
			Algorithm algorithm) throws IOException, JATEException {
		if (Algorithm.TF_IDF.equals(algorithm)) {
			initialise(params);
			return this.tfidfSolr.extract(core, jatePropertyFile);
		}
		return null;
	}

}
