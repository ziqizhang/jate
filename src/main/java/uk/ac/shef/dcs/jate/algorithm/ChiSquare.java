package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import org.apache.log4j.Logger;

/**
 * An implementation of the Chi Square algorithm. See Matsuo, Y., Ishizuka, M. </i>
 * Keyword Extraction from a Single Document Using Word Co-Occurrence Statistical Information. </i>
 * Proc. 16th Intl. Florida AI Research Society, 2003, 392-396.
 */
public class ChiSquare extends Algorithm {
    private static final Logger LOG = Logger.getLogger(ChiSquare.class.getName());
    public static final String SUFFIX_TERM = "_TERM";

    public ChiSquare() {
    }


    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException {
        AbstractFeature feature1 = features.get(Cooccurrence.class.getName());
        validateFeature(feature1, Cooccurrence.class);
        Cooccurrence fFeatureCoocurr = (Cooccurrence) feature1;

        AbstractFeature feature2 = features.get(FrequencyCtxBased.class.getName() + SUFFIX_TERM);
        validateFeature(feature2, FrequencyCtxBased.class);
        FrequencyCtxBased termFeatureCtxBased = (FrequencyCtxBased) feature2;


        AbstractFeature feature3 = features.get(ChiSquareFrequentTerms.class.getName());
        validateFeature(feature3, ChiSquareFrequentTerms.class);
        ChiSquareFrequentTerms refTermExpProb = (ChiSquareFrequentTerms) feature3;

        int cores =Runtime.getRuntime().availableProcessors();
        int maxPerWorker = candidates.size()/cores;
        StringBuilder msg = new StringBuilder("Beginning computing ChiSquare, cores=");
        msg.append(cores).append(", total terms=" + candidates.size()).append(",").
                append(" max terms per worker thread=").append(maxPerWorker);
        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);

        ChiSquareWorker worker = new ChiSquareWorker(new ArrayList<>(candidates), maxPerWorker,
                termFeatureCtxBased, fFeatureCoocurr, refTermExpProb
                );
        List<JATETerm> result = forkJoinPool.invoke(worker);
        LOG.info("Complete chisquare calculation.");
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
