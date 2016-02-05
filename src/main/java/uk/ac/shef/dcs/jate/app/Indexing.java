package uk.ac.shef.dcs.jate.app;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.indexing.IndexingHandler;
import uk.ac.shef.dcs.jate.io.TikaSimpleDocumentCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.apache.log4j.Logger;

public class Indexing {
    public static void main(String[] args) throws IOException, JATEException, SolrServerException {
        Logger logger = Logger.getLogger(Indexing.class.getName());
        JATEProperties prop = new JATEProperties(args[0]);
        boolean deletePrevious = Boolean.valueOf(args[2]);
        SolrClient solrClient =
                new EmbeddedSolrServer(Paths.get(prop.getSolrHome()),
                        prop.getSolrCoreName());
        if (deletePrevious) {
            logger.info("DELETING PREVIOUS INDEX");
            solrClient.deleteByQuery("*:*");
            solrClient.commit();
        }
        logger.info("INDEXING BEGINS");
        IndexingHandler m = new IndexingHandler();
        List<String> files = new ArrayList<>();
        for (File f : new File(args[1]).listFiles())
            files.add(f.toString());


        m.index(files,
                prop.getIndexerMaxUnitsToCommit(),
                new TikaSimpleDocumentCreator(), solrClient,
                prop);
        logger.info("INDEXING COMPLETE");
        System.exit(0);
    }
}
