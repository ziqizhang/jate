package uk.ac.shef.dcs.jate.feature;


import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import java.util.*;
import java.util.concurrent.ForkJoinPool;

import org.apache.log4j.Logger;


public class ContainmentFBMaster extends AbstractFeatureBuilder {

    private static final Logger LOG = Logger.getLogger(ContainmentFBMaster.class.getName());

    private TermComponentIndex termComponentIndex;
    private Set<String> uniqueCandidateTerms;

    public ContainmentFBMaster(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                               TermComponentIndex termComponentIndex,
                               Set<String> uniqueCandidateTerms) {
        super(solrIndexSearcher, properties);
        this.termComponentIndex = termComponentIndex;
        this.uniqueCandidateTerms = uniqueCandidateTerms;
    }

    @Override
    public AbstractFeature build() throws JATEException {
        Containment feature = new Containment();

        //start workers
        int cores = properties.getMaxCPUCores();
        cores = cores == 0 ? 1 : cores;
        int maxPerThread = getMaxPerThread(cores);

        StringBuilder sb = new StringBuilder("Building features using cpu cores=");
        sb.append(cores).append(", total terms=").append(uniqueCandidateTerms.size()).append(", max per worker=")
                .append(maxPerThread);
        LOG.info(sb.toString());
        ContainmentFBWorker worker = new
                ContainmentFBWorker(new ArrayList<>(uniqueCandidateTerms), maxPerThread,
                feature,
                termComponentIndex);
        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        int[] total = forkJoinPool.invoke(worker);
        sb = new StringBuilder("Complete building features. Total=");
        sb.append(total[1]).append(" success=").append(total[0]);
        LOG.info(sb.toString());

        return feature;
    }

    private int getMaxPerThread(int cores) {
        int maxPerThread = uniqueCandidateTerms.size() / cores;
        if (maxPerThread < MIN_SEQUENTIAL_THRESHOLD) {
            maxPerThread = MIN_SEQUENTIAL_THRESHOLD;
        } else if (maxPerThread > MAX_SEQUENTIAL_THRESHOLD) {
            maxPerThread = MAX_SEQUENTIAL_THRESHOLD;
        }
        return maxPerThread;
    }
}
