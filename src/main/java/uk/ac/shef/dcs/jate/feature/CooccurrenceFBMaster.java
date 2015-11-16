package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import java.util.logging.Logger;

/**
 *
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
        List<ContextWindow> contextWindows = new ArrayList<>(ref_frequencyCtxBased.getMapCtx2TTF().keySet());
        //start workers
        int cores =  properties.getMaxCPUCores();
        cores = cores == 0 ? 1 : cores;
        int maxPerThread = contextWindows.size()/cores;
        if(maxPerThread==0)
            maxPerThread=50;

        StringBuilder sb = new StringBuilder("Building features using cpu cores=");
        sb.append(cores).append(", total ctx where reference terms appear =").append(contextWindows.size()).append(", max per worker=")
                .append(maxPerThread);
        LOG.info(sb.toString());

        LOG.info("Filtering candidates with min.ttf="+minTTF+" min.tcf="+minTCF);
        Set<String> termsPassingPrefilter = new HashSet<>();
        for (ContextWindow ctx : contextWindows) {
            Map<String, Integer> termsInContext = frequencyCtxBased.getTFIC(ctx);
            if (minTTF == 0 && minTCF == 0)
                termsPassingPrefilter.addAll(termsInContext.keySet());
            else {
                for (String term : termsInContext.keySet()) {
                    if (frequencyTermBased.getTTF(term) >= minTTF && frequencyCtxBased.getContexts(term).size() >= minTCF)
                        termsPassingPrefilter.add(term);
                }
            }
        }
        Cooccurrence feature = new Cooccurrence(termsPassingPrefilter.size(),
                ref_frequencyCtxBased.getMapTerm2Ctx().size());
        LOG.info("Beginning building features. Total terms="+termsPassingPrefilter.size()+", total contexts="+ contextWindows.size());

        CooccurrenceFBWorker worker = new
                CooccurrenceFBWorker(feature, contextWindows,
                frequencyTermBased, minTTF, frequencyCtxBased, ref_frequencyCtxBased,
                minTCF,maxPerThread);

        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        int total = forkJoinPool.invoke(worker);

        //go through each overlap zone, deduce from co-ocurrence matrix the co-occurrence of pairs of terms within
        //each overlap zone
        Map<String,ContextOverlap> overlaps= frequencyCtxBased.getCtxOverlapZones();
        if(overlaps.size()>0){
            LOG.info("Correcting double counted co-occurrences in context overlapping zones, total zones="+overlaps.size());
            for(Map.Entry<String, ContextOverlap> en: overlaps.entrySet()){
                String key =en.getKey();
                ContextOverlap co = en.getValue();

                //build a map of unique terms and their frequency within the overlap zone
                Map<String,Integer> freq =new HashMap<>();
                for(String t: co.getTerms()){
                    Integer f = freq.get(t);
                    f = f==null? 0: f;
                    f++;
                    freq.put(t, f);
                }

                if(freq.size()==1)
                    continue;

                ContextOverlap ref_co=ref_frequencyCtxBased.getCtxOverlapZones().get(key);
                //build a map of unique terms and their frequency within the overlap zone for ref terms
                Map<String,Integer> ref_freq =new HashMap<>();
                for(String t: ref_co.getTerms()){
                    Integer f = ref_freq.get(t);
                    f = f==null? 0: f;
                    f++;
                    ref_freq.put(t, f);
                }
                if(ref_freq.size()==1)
                    continue;

                //now revise co-occurrence stats
                for(Map.Entry<String, Integer> term_in_co : freq.entrySet()){
                    int f=term_in_co.getValue();
                    for(Map.Entry<String, Integer> term_in_ref_co: ref_freq.entrySet()){
                        int rf =term_in_ref_co.getValue();

                        int deduce=f<rf?f:rf;
                        int tid=feature.lookupTerm(term_in_co.getKey());
                        int tid_f=feature.lookupRefTerm(term_in_ref_co.getKey());
                        feature.deduce(tid,tid_f,deduce);
                    }
                }
            }
        }


        sb = new StringBuilder("Complete building features, total contexts processed="+total);
        LOG.info(sb.toString());

        return feature;
    }
}
