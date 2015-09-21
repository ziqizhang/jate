package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.lucene.index.IndexReader;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;

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

    public CooccurrenceFBMaster(IndexReader index, JATEProperties properties,
                                FrequencyCtxBased contextFeature) {
        super(index, properties);
        this.frequencyCtxBased = contextFeature;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        List<String> contextIds = new ArrayList<>(frequencyCtxBased.getMapCtx2TTF().keySet());
        //start workers
        int cores = Runtime.getRuntime().availableProcessors();
        cores = (int) (cores * properties.getFeatureBuilderMaxCPUsage());
        cores = cores == 0 ? 1 : cores;
        CooccurrenceFBWorker worker = new
                CooccurrenceFBWorker(contextIds, frequencyCtxBased, properties.getFeatureBuilderMaxTermsPerWorker());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        Cooccurrence feature = forkJoinPool.invoke(worker);
        StringBuilder sb = new StringBuilder("Complete building features.");
        LOG.info(sb.toString());

        return feature;
    }
}
