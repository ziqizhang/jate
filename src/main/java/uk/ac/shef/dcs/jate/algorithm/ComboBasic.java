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
 * Created by zqz on 24/05/17.
 */
public class ComboBasic extends Algorithm {
    private static final Logger LOG = Logger.getLogger(ComboBasic.class.getName());
    private double alpha = 0.75;
    private double beta = 0.1;

    public ComboBasic() {
    }

    public ComboBasic(double alpha, double beta) {
        this.alpha = alpha;
        this.beta = beta;
    }

    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(Containment.class.getName());
        validateFeature(feature2, Containment.class);
        Containment cFeature = (Containment) feature2;

        AbstractFeature feature3 = features.get(Containment.class.getName());
        validateFeature(feature3, Containment.class);
        Containment crFeature = (Containment) feature3;

        int cores = Runtime.getRuntime().availableProcessors();
        int maxPerWorker = candidates.size() / cores;
        StringBuilder msg = new StringBuilder("Beginning computing ComboBasic, cores=");
        msg.append(cores).append(", total terms=" + candidates.size()).append(",").
                append(" max terms per worker thread=").append(maxPerWorker);
        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        ComboBasicWorker worker = new ComboBasicWorker(new ArrayList<>(candidates), maxPerWorker, fFeature,
                cFeature, crFeature, alpha, beta
        );
        List<JATETerm> result = forkJoinPool.invoke(worker);
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}