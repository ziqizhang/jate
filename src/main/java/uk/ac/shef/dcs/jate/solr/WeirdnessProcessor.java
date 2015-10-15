package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.app.AppWeirdness;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

public class WeirdnessProcessor implements TermRecognitionProcessor {

	private AppWeirdness weirdnessSolr = null;
	private void initialise(Map<String, String> params) throws JATEException {
		if (this.weirdnessSolr == null) {
			this.weirdnessSolr = new AppWeirdness(params);
		}
	}
	
	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params,
			Algorithm algorithm) throws IOException, JATEException {
		if (Algorithm.WEIRDNESS.equals(algorithm)) {
			initialise(params);
			return this.weirdnessSolr.extract(core, jatePropertyFile);
		}
		return null;
	}

}
