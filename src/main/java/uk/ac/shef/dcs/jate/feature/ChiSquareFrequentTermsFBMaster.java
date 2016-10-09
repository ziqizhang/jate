package uk.ac.shef.dcs.jate.feature;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.*;
import java.util.concurrent.ForkJoinPool;

/**
 * This class computes expected probability of frequent terms in the ChiSquare algorithm.
 */
public class ChiSquareFrequentTermsFBMaster extends AbstractFeatureBuilder {
    private static final Logger LOG = Logger.getLogger(ChiSquareFrequentTermsFBMaster.class.getName());

    private final Map<ContextWindow, Integer> ctx2TTF;
    private final Map<String, Set<ContextWindow>> term2Ctx;
    private List<String> allFrequentTerms;
    private final int ttfInCorpus;

    public ChiSquareFrequentTermsFBMaster(
            Map<ContextWindow, Integer> ctx2TTF,
            Map<String, Set<ContextWindow>> term2Ctx,
            int ttfInCorpus, JATEProperties properties) {
        super(null, properties);
        this.allFrequentTerms = new ArrayList<>(term2Ctx.keySet());
        this.ctx2TTF = ctx2TTF;
        this.term2Ctx = term2Ctx;
        this.ttfInCorpus = ttfInCorpus;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        ChiSquareFrequentTerms feature = new ChiSquareFrequentTerms();

        int cores = properties.getMaxCPUCores();
        cores = cores == 0 ? 1 : cores;

        int maxPerThread = getMaxPerThread(cores);

        LOG.info("Beginning building features (ChiSquare frequent terms). Total terms=" + allFrequentTerms.size() + ", cpu cores=" +
                cores + ", max per core=" + maxPerThread);
        ChiSquareFrequentTermsFBWorker worker = new
                ChiSquareFrequentTermsFBWorker(allFrequentTerms, maxPerThread, ctx2TTF, term2Ctx,
                feature, ttfInCorpus);
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        int total = forkJoinPool.invoke(worker);
        StringBuilder sb = new StringBuilder("Complete building features. Total processed terms = " + total);
        LOG.info(sb.toString());

        return feature;
    }

    private int getMaxPerThread(int cores) {
        int maxPerThread = allFrequentTerms.size() / cores;
        if (maxPerThread < MIN_SEQUENTIAL_THRESHOLD) {
            maxPerThread = MIN_SEQUENTIAL_THRESHOLD;
        } else if(maxPerThread > MAX_SEQUENTIAL_THRESHOLD) {
            maxPerThread = MAX_SEQUENTIAL_THRESHOLD;
        }
        return maxPerThread;
    }
}
