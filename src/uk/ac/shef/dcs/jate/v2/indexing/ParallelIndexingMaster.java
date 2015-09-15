package uk.ac.shef.dcs.jate.v2.indexing;

import org.apache.solr.client.solrj.SolrClient;
import uk.ac.shef.dcs.jate.v2.io.DocumentCreator;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class ParallelIndexingMaster {

    private static final Logger LOG = Logger.getLogger(ParallelIndexingMaster.class.getName());

    public void index(List<String> tasks, int maxTaskPerWorker,
                      int batchSize, DocumentCreator docCreator,
                      SolrClient solrClient, double cpuUsage){
        int cores = Runtime.getRuntime().availableProcessors();
        cores = (int) (cores*cpuUsage);
        cores=cores==0?1:cores;

        LOG.info("Beginning indexing dataset.");
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        ParallelIndexingWorker idxWorker = new ParallelIndexingWorker(tasks,
                maxTaskPerWorker, batchSize, docCreator, solrClient);
        int total= forkJoinPool.invoke(idxWorker);
        LOG.info("Complete indexing dataset. Total data items = "+total);
    }
}
