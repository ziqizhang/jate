package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * NC-Value, see Frantzi et al., Automatic Recognition of Multi-Word Terms: the C-value/NC-value Method
 *
 * In this implementation, the notion of "context words" is just candidate terms that co-occur with a target term within
 * the same sentence. No filter is applied to the selection of context words. All candidate terms are considered.
 */
public class NCValue extends Algorithm {
    private static final Logger LOG = Logger.getLogger(NCValue.class.getName());

    protected double weightCValue =0.8;
    protected double weightContext =0.2;

    protected int maxPerWorker = 1000;

    public NCValue() {
    }
    public NCValue(double weightCValue, double weightContext) {
        this.weightCValue =weightCValue;
        this.weightContext =weightContext;
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
                weightCValue, weightContext);
        List<JATETerm> result = forkJoinPool.invoke(worker);
        Collections.sort(result);
        LOG.info("Complete");
        return result;

    }
}
