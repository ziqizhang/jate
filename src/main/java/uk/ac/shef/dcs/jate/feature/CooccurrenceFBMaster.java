package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 * Created by zqz on 20/09/2015.
 */
public class CooccurrenceFBMaster extends AbstractFeatureBuilder {
    private static final Logger LOG = Logger.getLogger(CooccurrenceFBMaster.class.getName());
    protected FrequencyCtxBased frequencyCtxBased; //frequency-in-context of target terms
    protected FrequencyCtxBased ref_frequencyCtxBased; //frequency-in-context of ref terms, i.e. which co-occur with target terms
    protected FrequencyTermBased frequencyTermBased; //frequency info of target terms
    protected int minTTF;
    protected int minTCF;


    public CooccurrenceFBMaster(SolrIndexSearcher solrIndexSearcher, JATEProperties properties,
                                FrequencyTermBased termFeature,
                                Integer minTTF,
                                FrequencyCtxBased contextFeature,
                                FrequencyCtxBased ref_frequencyCtxBased,
                                Integer minTCF) {
        super(solrIndexSearcher, properties);
        this.frequencyCtxBased = contextFeature;
        this.frequencyTermBased=termFeature;
        this.ref_frequencyCtxBased=ref_frequencyCtxBased;
        this.minTTF=minTTF;
        this.minTCF=minTCF;//only applies to target terms, not reference terms
    }

    @Override
    public AbstractFeature build() throws JATEException {
        List<String> contextIds = new ArrayList<>(ref_frequencyCtxBased.getMapCtx2TTF().keySet());
        //start workers
        int cores =  properties.getMaxCPUCores();
        cores = cores == 0 ? 1 : cores;
        int maxPerThread = contextIds.size()/cores;

        StringBuilder sb = new StringBuilder("Building features using cpu cores=");
        sb.append(cores).append(", total ctx where reference terms appear =").append(contextIds.size()).append(", max per worker=")
                .append(maxPerThread);
        LOG.info(sb.toString());

        LOG.info("Filtering candidates with min.ttf="+minTTF+" min.tcf="+minTCF);
        Set<String> termsPassingPrefilter = new HashSet<>();
        for (String ctxId : contextIds) {
            Map<String, Integer> termsInContext = frequencyCtxBased.getTFIC(ctxId);
            if (minTTF == 0 && minTCF == 0)
                termsPassingPrefilter.addAll(termsInContext.keySet());
            else {
                for (String term : termsInContext.keySet()) {
                    if (frequencyTermBased.getTTF(term) >= minTTF && frequencyCtxBased.getContextIds(term).size() >= minTCF)
                        termsPassingPrefilter.add(term);
                }
            }
        }
        Cooccurrence feature = new Cooccurrence(termsPassingPrefilter.size(),
                ref_frequencyCtxBased.getMapTerm2Ctx().size());
        LOG.info("Beginning building features. Total terms="+termsPassingPrefilter.size()+", total contexts="+contextIds.size());

        CooccurrenceFBWorker worker = new
                CooccurrenceFBWorker(feature, contextIds,
                frequencyTermBased, minTTF, frequencyCtxBased, ref_frequencyCtxBased,
                minTCF,maxPerThread);

        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        int total = forkJoinPool.invoke(worker);
        sb = new StringBuilder("Complete building features, total contexts processed="+total);
        LOG.info(sb.toString());

        return feature;
    }
}
