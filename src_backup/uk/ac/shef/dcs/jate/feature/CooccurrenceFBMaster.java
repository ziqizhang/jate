package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 20/09/2015.
 */
public class CooccurrenceFBMaster extends AbstractFeatureBuilder {
    private static final Logger LOG = Logger.getLogger(CooccurrenceFBMaster.class.getName());
    protected FrequencyCtxBased frequencyCtxBased;
    protected FrequencyCtxBased ref_frequencyCtxBased;
    protected FrequencyTermBased frequencyTermBased;
    protected int minTTF;
    protected int minTCF;

    public CooccurrenceFBMaster(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                                FrequencyTermBased termFeature,
                                Integer minTTF,
                                FrequencyCtxBased contextFeature,
                                FrequencyCtxBased ref_frequencyCtxBased,
                                Integer minTCF) {
        super(solrIndexSearcher, properties);
        this.frequencyCtxBased = contextFeature;
        this.frequencyTermBased=termFeature;
        this.ref_frequencyCtxBased=ref_frequencyCtxBased;
        this.minTTF=minTTF;
        this.minTCF=minTCF;//only applies to target terms, not reference terms
    }

    @Override
    public AbstractFeature build() throws JATEException {
        List<String> contextIds = new ArrayList<>(ref_frequencyCtxBased.getMapCtx2TTF().keySet());
        //start workers
        int cores =  properties.getMaxCPUCores();
        cores = cores == 0 ? 1 : cores;
        int maxPerThread = contextIds.size()/cores;

        StringBuilder sb = new StringBuilder("Building features using cpu cores=");
        sb.append(cores).append(", total ctx=").append(contextIds.size()).append(", max per worker=")
                .append(maxPerThread);
        LOG.info(sb.toString());
        CooccurrenceFBWorker worker = new
                CooccurrenceFBWorker(contextIds,
                frequencyTermBased, minTTF, frequencyCtxBased, ref_frequencyCtxBased,
                minTCF,maxPerThread);
        LOG.info("Filtering candidates with min.ttf="+minTTF+" min.tcf="+minTCF);
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        Cooccurrence feature = forkJoinPool.invoke(worker);
        sb = new StringBuilder("Complete building features.");
        LOG.info(sb.toString());

        return feature;
    }
}
