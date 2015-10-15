package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppChiSquare;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * Solr Automatic Term Recognition (ATR) Processor for ChiSquare algorithm
 * 
 * see {@code uk.ac.shef.dcs.jate.algorithm.ChiSquare}}
 *
 */
public class ChiSquareProcessor implements TermRecognitionProcessor {

	private AppChiSquare chiSquareSolr = null;
	
	private void initialise(Map<String, String> params) throws JATEException {
		if (this.chiSquareSolr == null) {
			this.chiSquareSolr = new AppChiSquare(params);
		}
	}
	
	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params,
			Algorithm algorithm) throws IOException, JATEException {
		if (Algorithm.CHI_SQUARE.equals(algorithm)) {
			initialise(params);
			return this.chiSquareSolr.extract(core, jatePropertyFile);
		}
		return null;
	}

}
