package uk.ac.shef.dcs.jate.algorithm;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.feature.TermComponentIndex;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * Rose, S., Engel, D., Cramer, N., & Cowley, W. (2010).
 * Automatic Keyword Extraction from Individual Documents. In M. W. Berry & J. Kogan (Eds.),
 * Text Mining: Theory and Applications: John Wiley & Sons.
 */
public class RAKE extends Algorithm {

    private static final Logger LOG = Logger.getLogger(RAKE.class.getName());
    public static final String SUFFIX_WORD = "_WORD";
    public static final String SUFFIX_TERM = "_TERM";

    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName() + SUFFIX_WORD);
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName() + SUFFIX_TERM);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature2;


        AbstractFeature tciFeature = features.get(TermComponentIndex.class.getName());
        validateFeature(tciFeature, TermComponentIndex.class);
        TermComponentIndex fFeatureTermCompIndex = (TermComponentIndex) tciFeature;

        int cores = Runtime.getRuntime().availableProcessors();
        int maxPerWorker=candidates.size()/cores;
        if (maxPerWorker == 0)
            maxPerWorker = 50;

        StringBuilder msg = new StringBuilder("Beginning computing RAKE values, cores=");
        msg.append(cores).append(" total terms=" + candidates.size()).append(",")
        .append(" max terms per worker thread=").append(maxPerWorker);


        LOG.info(msg.toString());
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        RAKEWorker worker = new RAKEWorker(new ArrayList<>(candidates), Integer.MAX_VALUE, fFeatureWords, fFeatureTerms,
                fFeatureTermCompIndex
        );
        List<JATETerm> result = forkJoinPool.invoke(worker);
        Collections.sort(result);

        LOG.info("Complete");
        return result;
    }
}
