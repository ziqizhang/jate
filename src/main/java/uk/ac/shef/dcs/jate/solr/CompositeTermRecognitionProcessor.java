package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * A composite Term Recognition (TR) processor implemented all methods by
 * delegating them to different TR processor
 * 
 *
 */
public class CompositeTermRecognitionProcessor implements TermRecognitionProcessor {

	private Collection<TermRecognitionProcessor> processors = new ArrayList<TermRecognitionProcessor>();

	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params,
			Algorithm algorithm) throws IOException, JATEException {
		for (TermRecognitionProcessor termRecognitionProcessor : processors) {
			List<JATETerm> terms = termRecognitionProcessor.extract(core, jatePropertyFile, params, algorithm);
			if (terms != null) {
				return terms;
			}
		}
		return null;
	}

	/**
	 * Provides access to the term recognition "processors" list
	 * 
	 * @return a mutable ordered collection of processors
	 */
	public Collection<TermRecognitionProcessor> getProcessors() {
		return processors;
	}

}
