package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.apache.solr.core.SolrCore;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;

/**
 * The interface for all Term Recognition processors
 * 
 * @author jieg
 *
 */
public interface TermRecognitionProcessor {
	/**
	 * 
	 * @param core, solr core
	 * @param jatePropertyFile, jate property file path
	 * @param params, run-time parameters for different TR algorithms, see {@code uk.ac.shef.dcs.jate.app.App.CommandLineParams} for details}
	 * @param algorithm, TR algorithm
	 * @return List<JATETerm>, a list of terms extracted from current solr core
	 * @throws IOException
	 * @throws JATEException
	 */
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params, Algorithm algorithm)
			throws IOException, JATEException;
}
