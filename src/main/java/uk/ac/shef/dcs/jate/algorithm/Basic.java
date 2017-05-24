package uk.ac.shef.dcs.jate.algorithm;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.Containment;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

/**
 *
 */
public class Basic extends Algorithm {
    private static final Logger LOG = Logger.getLogger(Basic.class.getName());
    private double alpha=0.72;

    public Basic(){}
    public Basic(double alpha) {
        this.alpha=alpha;
    }

    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(Containment.class.getName());
        validateFeature(feature2, Containment.class);
        Containment cFeature = (Containment) feature2;

        int cores = Runtime.getRuntime().availableProcessors();
        int maxPerWorker = candidates.size() / cores;
        StringBuilder msg = new StringBuilder("Beginning computing Basic, cores=");
        msg.append(cores).append(", total terms=" + candidates.size()).append(",").
                append(" max terms per worker thread=").append(maxPerWorker);
        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        BasicWorker worker = new BasicWorker(new ArrayList<>(candidates), maxPerWorker, fFeature,
                cFeature, alpha
        );
        List<JATETerm> result = forkJoinPool.invoke(worker);
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}