package uk.ac.shef.dcs.jate.v2.app;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.indexing.SolrParallelIndexingMaster;
import uk.ac.shef.dcs.jate.v2.io.TikaSimpleDocumentCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by zqz on 23/09/2015.
 */
public class Indexing {
    public static void main(String[] args) throws IOException, JATEException, SolrServerException {
        Logger logger = Logger.getLogger(Indexing.class.getName());
        JATEProperties prop = new JATEProperties(args[0]);
        boolean deletePrevious = Boolean.valueOf(args[2]);
        SolrClient solrClient =
                new EmbeddedSolrServer(Paths.get(prop.getSolrHome()),
                        prop.getSolrCorename());
        if (deletePrevious) {
            logger.info("DELETING PREVIOUS INDEX");
            solrClient.deleteByQuery("*:*");
            solrClient.commit();
        }
        logger.info("INDEXING BEGINS");
        SolrParallelIndexingMaster m = new SolrParallelIndexingMaster();
        List<String> files = new ArrayList<>();
        for (File f : new File(args[1]).listFiles())
            files.add(f.toString());
        m.index(files, prop.getIndexerMaxDocsPerWorker(),
                prop.getIndexerMaxUnitsToCommit(),
                new TikaSimpleDocumentCreator(), solrClient,
                prop.getFeatureBuilderMaxCPUsage(), prop);
        logger.info("INDEXING COMPLETE");
        System.exit(0);
    }
}
