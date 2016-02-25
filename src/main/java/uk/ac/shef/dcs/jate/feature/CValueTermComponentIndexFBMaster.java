package uk.ac.shef.dcs.jate.feature;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 * Created by - on 25/02/2016.
 */
public class CValueTermComponentIndexFBMaster extends AbstractFeatureBuilder {
    private static final Logger LOG = Logger.getLogger(CValueTermComponentIndexFBMaster.class.getName());
    private List<String> candidates;
    public CValueTermComponentIndexFBMaster(JATEProperties properties, List<String> candidates) {
        super(null, properties);
        this.candidates=candidates;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        CValueTermComponentIndex feature = new CValueTermComponentIndex();

        int cores = properties.getMaxCPUCores();
        cores = cores == 0 ? 1 : cores;
        int maxPerThread = candidates.size() / cores;
        if (maxPerThread == 0)
            maxPerThread = 50;

        LOG.info("Beginning building features (CValueTermComponentIndex). Total terms=" + candidates.size() + ", cpu cores=" +
                cores + ", max per core=" + maxPerThread);
        CValueTermComponentIndexFBWorker worker = new
                CValueTermComponentIndexFBWorker(candidates, maxPerThread,
                feature);
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        int total = forkJoinPool.invoke(worker);
        StringBuilder sb = new StringBuilder("Complete building features. Total processed terms = " + total);
        LOG.info(sb.toString());

        return feature;
    }
}
