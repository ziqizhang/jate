package uk.ac.shef.dcs.jate.indexing;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.io.DocumentCreator;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class SolrParallelIndexingMaster {

    private static final Logger LOG = Logger.getLogger(SolrParallelIndexingMaster.class.getName());

    public void index(List<String> tasks,
                      int batchSize, DocumentCreator docCreator,
                      SolrClient solrClient, int cores,
                      JATEProperties properties){
        cores=cores==0?1:cores;
        int maxTaskPerWorker= tasks.size()/cores;

        StringBuilder msg = new StringBuilder("Beginning indexing dataset using cores=");
        msg.append(cores).append(", total docs="+tasks.size());
        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        if(properties.getSolrFieldnameJATEWords()==null||
                properties.getSolrFieldnameJATEWords().equals(""))
            LOG.warning("'fieldname_jate_words_all' undefined. If your algorithms (e.g., GlossEx, TermEx, Weirdness) do not use word-level features this is ok.");
        if(properties.getSolrFieldnameJATESentences()==null||
                properties.getSolrFieldnameJATESentences().equals(""))
            LOG.warning("'fieldname_jate_sentences_all' undefined. If your algorithms (e.g., Chi-Square, NC-value) do not use sentence-level features this is ok.");
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
