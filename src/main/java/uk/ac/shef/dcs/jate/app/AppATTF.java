package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.ATTF;
import uk.ac.shef.dcs.jate.algorithm.Algorithm;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBasedFBMaster;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AppATTF extends App {
    private static Logger LOG = Logger.getLogger(AppATTF.class.getName());

    static EmbeddedSolrServer server = null;

    /**
     * @param args  command-line params accepting solr home path, solr core name and more optional run-time parameters
     * @see uk.ac.shef.dcs.jate.app.AppParams
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }

        String solrHomePath = args[args.length - 2];
        String solrCoreName = args[args.length - 1];

        Map<String, String> params = getParams(args);
        String jatePropertyFile = getJATEProperties(params);
        String corpusDir = getCorpusDir(params);

        List<JATETerm> terms;
        try {
            AppATTF app = new AppATTF(params);

            if (isCorpusProvided(corpusDir)) {
                app.index(Paths.get(corpusDir), Paths.get(solrHomePath), solrCoreName, jatePropertyFile);
            }

            terms = app.extract(solrHomePath, solrCoreName, jatePropertyFile);

            if (isExport(params)) {
                app.write(terms);
            }
            System.exit(0);
        } catch (IOException e) {
            System.err.println("IO Exception when exporting terms!");
            e.printStackTrace();
        } catch (JATEException e) {
            e.printStackTrace();
        }
    }

    /**
     * initialise run-time parameters for current algorithm
     * @param initParams  run-time parameters (e.g.,min term total freq, cutoff scoring threshold) for current algorithm
     *      @see uk.ac.shef.dcs.jate.app.AppParams
     * @throws JATEException
     */
    public AppATTF(Map<String, String> initParams) throws JATEException {
        super(initParams);
    }

    @Override
    public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        JATEProperties properties = getJateProperties(jatePropertyFile);

        return extract(core, properties);
    }

    /**
     * ranking and filtering
     *
     * @param core        solr core
     * @param properties  jate properties file
     * @return List<JATETerm> a list of JATETerm
     * @throws JATEException
     */
    public List<JATETerm> extract(SolrCore core, JATEProperties properties) throws JATEException {
        if (core.isClosed()) {
            core.open();
        }
        SolrIndexSearcher searcher = core.getSearcher().get();
//        try {
	        this.freqFeatureBuilder = new FrequencyTermBasedFBMaster(searcher, properties, FrequencyTermBasedFBMaster.FEATURE_TYPE_TERM);
	        this.freqFeature = (FrequencyTermBased) freqFeatureBuilder.build();
	
	        Algorithm attf = new ATTF();
	        attf.registerFeature(FrequencyTermBased.class.getName(), freqFeature);
	
	        List<String> candidates = new ArrayList<>(freqFeature.getMapTerm2TTF().keySet());
	
	        filterByTTF(candidates);
	
	        List<JATETerm> terms = attf.execute(candidates);
	
	        terms = cutoff(terms);
	        LOG.info("Complete ATTF term extraction.");
	
	        addAdditionalTermInfo(terms, searcher, properties.getSolrFieldNameJATENGramInfo(),
	                properties.getSolrFieldNameID());
	
	        return terms;
//        } finally {
//        	try {
//				searcher.close();
//			} catch (IOException e) {
//				LOG.error(e.toString());
//			}
//        }
    }

}
