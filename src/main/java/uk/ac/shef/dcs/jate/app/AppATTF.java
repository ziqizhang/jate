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

//    /**
//     * Index, extract, rank and filter term candidate from the given corpus
//     *
//     *
//     * @param corpusDir, a collection of documents in the corpus directory
//     * @param solrHomeDir, Solr home directory where a given corpus will be indexed to
//     * @param solrCoreName, solr core name
//     * @param jateProperties, jate properties file
//     * @return List<JATETerm>
//     */
//    public List<JATETerm> extract(Path corpusDir, String solrHomeDir, String solrCoreName, String jateProperties)
//            throws JATEException {
//        if (server == null) {
//            CoreContainer solrContainer = new CoreContainer(solrHomeDir);
//            solrContainer.load();
//
//            server = new EmbeddedSolrServer(solrContainer, solrCoreName);
//        }
//        List<JATETerm> terms = new ArrayList<>();
//
//        List<Path> files = loadFiles(corpusDir);
//        indexJATEDocuments(files);
//
//        return terms;
//    }
//
//    private void indexJATEDocuments(List<Path> files) {
//        List<JATEDocument> corpus = new ArrayList<>();
//
//
//
//        files.parallelStream().forEach(file -> {
//
//        });
//    }

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
//        try {
        this.freqFeatureBuilder = new FrequencyTermBasedFBMaster(searcher, properties, FrequencyTermBasedFBMaster.FEATURE_TYPE_TERM);
        this.freqFeature = (FrequencyTermBased) freqFeatureBuilder.build();

        Algorithm attf = new ATTF();
        attf.registerFeature(FrequencyTermBased.class.getName(), freqFeature);

        List<String> candidates = new ArrayList<>(freqFeature.getMapTerm2TTF().keySet());

        filterByTTF(candidates);

        List<JATETerm> terms = attf.execute(candidates);

        terms = cutoff(terms);

        addAdditionalTermInfo(terms, searcher, properties.getSolrFieldNameJATENGramInfo(),
                properties.getSolrFieldNameID());

        return terms;
//        } finally {
//            try {
//                searcher.close();
//            } catch (IOException ioe) {
//                LOG.error(ioe.toString());
//                throw new JATEException("Failed to close Solr Index Searcher.");
//            }
//        }
    }

//	public List<JATETerm> extract(SolrCore core, JATEProperties jateProperties) {
//		SolrIndexSearcher searcher = core.getSearcher().get();
//
//	}

}
