package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.CoreContainer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.ATTF;
import uk.ac.shef.dcs.jate.algorithm.Algorithm;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBasedFBMaster;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.JATEUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;


public class AppATTF extends App {
    private static Logger LOG = Logger.getLogger(AppATTF.class.getName());

    static EmbeddedSolrServer server = null;

    /**
     * @param args, command-line params accepting solr home path, solr core name,
     *              jate properties file and more optional run-time parameters
     * @see uk.ac.shef.dcs.jate.app.AppParams
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String solrHomePath = args[args.length - 3];
        String solrCoreName = args[args.length - 2];
        String jatePropertyFile = args[args.length - 1];

        Map<String, String> params = getParams(args);

        List<JATETerm> terms;
        try {
            AppATTF app = new AppATTF(params);
            terms = app.extract(solrHomePath, solrCoreName, jatePropertyFile);
            app.write(terms);
        } catch (IOException e) {
            System.err.println("IO Exception when exporting terms!");
            e.printStackTrace();
        } catch (JATEException e) {
            e.printStackTrace();
        }
    }

    public AppATTF(Map<String, String> initParams) throws JATEException {
        super(initParams);
    }

    @Override
    public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        JATEProperties properties = new JATEProperties(jatePropertyFile);

        return extract(core, properties);
    }

    /**
     * ranking and filtering
     *
     * @param core,       solr core
     * @param properties, jate properties file
     * @return List<JATETerm>
     * @throws JATEException
     */
    public List<JATETerm> extract(SolrCore core, JATEProperties properties) throws JATEException {
        SolrIndexSearcher searcher = core.getSearcher().get();

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
    }

}
