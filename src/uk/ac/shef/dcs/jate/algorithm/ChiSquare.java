package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.feature.FrequencyCtxBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * An implementation of the Chi Square algorithm. See Matsuo, Y., Ishizuka, M. </i>
 * Keyword Extraction from a Single Document Using Word Co-Occurrence Statistical Information. </i>
 * Proc. 16th Intl. Florida AI Research Society, 2003, 392-396.
 */
public class ChiSquare extends Algorithm {
    private static final Logger LOG = Logger.getLogger(ChiSquare.class.getName());
    public static final String SUFFIX_SENTENCE = "_SENTENCE";

    protected int maxPerWorker = 1000;

    public ChiSquare() {
    }

    public ChiSquare(int maxTermsPerWorker) {
        this.maxPerWorker = maxTermsPerWorker;
    }

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;
        AbstractFeature feature2 = features.get(Cooccurrence.class.getName());
        validateFeature(feature2, Cooccurrence.class);
        Cooccurrence fFeatureCoocurr = (Cooccurrence) feature2;

        AbstractFeature feature3 = features.get(FrequencyCtxBased.class.getName() + SUFFIX_SENTENCE);
        validateFeature(feature3, FrequencyCtxBased.class);
        FrequencyCtxBased fFeatureCtxBased = (FrequencyCtxBased) feature3;

        int cores = Runtime.getRuntime().availableProcessors();
        StringBuilder msg = new StringBuilder("Beginning computing ChiSquare, cores=");
        msg.append(cores).append(", total terms=" + candidates.size()).append(",").
                append(" max terms per worker thread=").append(maxPerWorker);
        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        ChiSquareWorker worker = new ChiSquareWorker(new ArrayList<>(candidates), maxPerWorker, fFeatureTerms, fFeatureCtxBased, fFeatureCoocurr,
                termInfoCollector);
        List<JATETerm> result = forkJoinPool.invoke(worker);
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
