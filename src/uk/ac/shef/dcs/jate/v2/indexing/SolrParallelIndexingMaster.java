package uk.ac.shef.dcs.jate.v2.indexing;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.io.DocumentCreator;
import uk.ac.shef.dcs.jate.v2.io.TikaSimpleDocumentCreator;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class SolrParallelIndexingMaster {

    private static final Logger LOG = Logger.getLogger(SolrParallelIndexingMaster.class.getName());

    public void index(List<String> tasks, int maxTaskPerWorker,
                      int batchSize, DocumentCreator docCreator,
                      SolrClient solrClient, double cpuUsage,
                      JATEProperties properties){
        int cores = Runtime.getRuntime().availableProcessors();
        cores = (int) (cores*cpuUsage);
        cores=cores==0?1:cores;

        StringBuilder msg = new StringBuilder("Beginning indexing dataset using cores=");
        msg.append(cores).append(", total docs="+tasks.size());
        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        if(properties.getSolrFieldnameJATEWordsAll()==null)
            LOG.info("'fieldname_jate_words_all' undefined. If you do not use GlossEx or TermEx this is ok.");
        SolrParallelIndexingWorker idxWorker = new SolrParallelIndexingWorker(tasks,
                maxTaskPerWorker, batchSize, docCreator, solrClient,properties);
        int total= forkJoinPool.invoke(idxWorker);
        LOG.info("Complete indexing dataset. Total data items = "+total);
        try {
            solrClient.close();
        } catch (IOException e) {
            String message = "CANNOT CLOSE SOLR: \n";
            LOG.severe(message+ExceptionUtils.getStackTrace(e));
        }
    }
}
