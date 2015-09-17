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

        LOG.info("Beginning indexing dataset using cores="+cores);
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
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

    public static void main(String[] args) throws IOException, JATEException {
        JATEProperties prop = new JATEProperties("");
        SolrClient solrClient =
                new EmbeddedSolrServer(Paths.get("D:\\Work\\jate_github\\jate\\solr-5.3.0\\server\\solr"),
                       prop.getSolrCorename());
        SolrParallelIndexingMaster m = new SolrParallelIndexingMaster();
        List<String> files = new ArrayList<>();
        for(File f: new File("D:\\Work\\jate_github\\jate\\sample\\input").listFiles())
            files.add(f.toString());
        m.index(files, 10,5,new TikaSimpleDocumentCreator(),solrClient, 0.5,prop);
    }
}
