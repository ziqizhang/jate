package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.*;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 21/09/2015.
 */
public class NCValue extends Algorithm {
    private static final Logger LOG = Logger.getLogger(NCValue.class.getName());

    protected static final double WEIGHT_CVALUE=0.8;
    protected static final double WEIGHT_CONTEXT=0.2;

    protected int maxPerWorker = 1000;

    public NCValue() {
    }

    public NCValue(int maxTermsPerWorker) {
        this.maxPerWorker = maxTermsPerWorker;
    }

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(Containment.class.getName());
        validateFeature(feature2, Containment.class);
        Containment cFeature = (Containment) feature2;

        AbstractFeature feature3 = features.get(Cooccurrence.class.getName());
        validateFeature(feature3, Cooccurrence.class);
        Cooccurrence fFeatureCoocurr = (Cooccurrence) feature3;

        int cores = Runtime.getRuntime().availableProcessors();
        StringBuilder msg = new StringBuilder("Beginning computing NCValue, cores=");
        msg.append(cores).append(", total terms=" + candidates.size()).append(",").
                append(" max terms per worker thread=").append(maxPerWorker);
        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        NCValueWorker worker = new NCValueWorker(new ArrayList<>(candidates), maxPerWorker, fFeature,
                cFeature,fFeatureCoocurr,
                termInfoCollector, WEIGHT_CVALUE, WEIGHT_CONTEXT);
        List<JATETerm> result = forkJoinPool.invoke(worker);
        Collections.sort(result);
        return result;

    }
}
