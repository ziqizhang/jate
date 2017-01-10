package uk.ac.shef.dcs.jate.feature;

import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.util.*;
import java.util.concurrent.ForkJoinPool;
import org.apache.log4j.Logger;

/**
 * This class counts pair-wise co-occurrence between two list of candidates, one called <b>target</b> candidate terms
 * (which are the terms we want to consider as real domain terms); the other called <b>reference</b> candidate terms
 * (lexical items of which we are interested in their co-occurring behaviour with the target candidates). The two
 * lists can be identical.
 *
 * </p> The candidates are provided in the form of FrequencyCtxBased objects. A FrequencyCtxBased object stores a set of
 * candidate terms, and the contexts where they appear. An example scenario is described below.
 *
 * </p>In Chi-Square, we need to calculate co-ocurrence between every target candidate term, and the most frequent n candidate
 * terms (reference term). To do so, we create two FrequencyCtxBased objects. The first stores all candidate terms which
 * contexts they appear in. The second is a subset of the first, and only stores most frequent n candidates and the
 * contexts they appear in.
 *
 * </p><b>NOTE 1</b>:We must ensure that the first and second FrequencyCtxBased objects use the same context windows.
 *
 * </p>Next to calculate co-occurrence, we take a target candidate term (t) from the first FrequencyCtxBased object, pair it with a
 * reference term (rt) from the second FrequencyCtxBased object. We then find the context windows that both t and rt appear in, denoted
 * by X. Given each x (from X), we look up the frequency of t in x, and frequency of rt in x. The co-occurrence frequency is
 * therefore the smaller of the two frequencies.
 *
 * </p><b>NOTE 2</b>:This method works if contexts are mutually exclusive, i.e., they do not have overlap (e.g., a document, or
 * a sentence as a context). When contexts are likely to have overlap (e.g., context window of size n around candidate terms),
 * co-occurrences of terms appearing in overlap are double-counted. This is corrected by deducting the frequency of the pair
 * in the overlap from their total co-occurrence frequency calculated using the above method.
 *
 *
 * @see FrequencyCtxBased
 * @see FrequencyCtxDocBasedFBMaster
 * @see FrequencyCtxSentenceBasedFBMaster
 * @see FrequencyCtxWindowBasedFBMaster
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
        this.frequencyTermBased = termFeature;
        this.ref_frequencyCtxBased = ref_frequencyCtxBased;
        this.minTTF = minTTF;
        this.minTCF = minTCF;//only applies to target terms, not reference terms
    }

    @Override
    public AbstractFeature build() throws JATEException {
        //TokenMetaData windows where target candidate terms appear. It is possible that many reference terms
        //do not appear in these context windows, because reference terms are not identical set to target terms
        List<ContextWindow> contextWindows = new ArrayList<>(frequencyCtxBased.getMapCtx2TTF().keySet());
        //List<ContextWindow> contextWindows = new ArrayList<>(ref_frequencyCtxBased.getMapCtx2TTF().keySet());
        Collections.sort(contextWindows);

        //start workers
        int cores = properties.getMaxCPUCores();
        cores = cores == 0 ? 1 : cores;
        int maxPerThread = contextWindows.size() / cores;
        if (maxPerThread == 0)
            maxPerThread = 50;

        StringBuilder sb = new StringBuilder("Building features using cpu cores=");
        sb.append(cores).append(", total ctx where reference terms appear =").append(contextWindows.size()).append(", max per worker=")
                .append(maxPerThread);
        LOG.info(sb.toString());

        LOG.info("Filtering candidates with min.ttf=" + minTTF + " min.tcf=" + minTCF);
        Set<String> termsPassingPrefilter = new HashSet<>();
        for (ContextWindow ctx : contextWindows) {//now go thru the selected context windows, select target terms that satisfy selection thresholds
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

        //It is possible that many reference terms
        //do not appear in these context windows, because reference terms are not identical set to target terms
        Cooccurrence feature = new Cooccurrence(termsPassingPrefilter.size(),
                ref_frequencyCtxBased.getMapTerm2Ctx().size());
        LOG.info("Beginning building features. Total terms=" + termsPassingPrefilter.size() + ", total contexts=" + contextWindows.size());

        CooccurrenceFBWorker worker = new
                CooccurrenceFBWorker(feature, contextWindows,
                frequencyTermBased, minTTF, frequencyCtxBased, ref_frequencyCtxBased,
                minTCF, Integer.MAX_VALUE);

        ForkJoinPool forkJoinPool = new ForkJoinPool(cores);
        int total = forkJoinPool.invoke(worker);

        /*List<String> col=new ArrayList<>(frequencyCtxBased.getCtxOverlapZones().keySet());
        col.removeAll(ref_frequencyCtxBased.getCtxOverlapZones().keySet());
        System.out.println(col.size());
*/
        //post-process to correct double counting in overlapping context
        //both target candidate terms and reference candidate terms use the same context objects. So they should also
        //have the same context overlaps
        Map<String, ContextOverlap> overlaps = frequencyCtxBased.getCtxOverlapZones();
        if (overlaps.size() > 0) {
            LOG.info("Correcting double counted co-occurrences in context overlapping zones, total zones=" + overlaps.size());
            for (Map.Entry<String, ContextOverlap> en : overlaps.entrySet()) {
                String key = en.getKey();
                ContextOverlap co = en.getValue();

                //a map of unique target terms found in this overlap zone and their frequencies
                Map<String, Integer> freq = new HashMap<>();
                for (String t : co.getTerms()) {
                    Integer f = freq.get(t);
                    f = f == null ? 0 : f;
                    f++;
                    freq.put(t, f);
                }

                if (freq.size() <= 1)
                    continue;

                //get the corresponding context overlap object created for the reference terms
                ContextOverlap ref_co = ref_frequencyCtxBased.getCtxOverlapZones().get(key);
                //a map of unique reference terms and their frequency within the overlap zone for ref terms
                Map<String, Integer> ref_freq = new HashMap<>();
                if (ref_co != null) {
                    for (String t : ref_co.getTerms()) {
                        Integer f = ref_freq.get(t);
                        f = f == null ? 0 : f;
                        f++;
                        ref_freq.put(t, f);
                    }
                }
                if (ref_freq.size() <= 1)
                    continue;

                //now revise co-occurrence stats
                for (Map.Entry<String, Integer> term_in_co : freq.entrySet()) {
                    int f = term_in_co.getValue(); //target term
                    for (Map.Entry<String, Integer> term_in_ref_co : ref_freq.entrySet()) {
                        int rf = term_in_ref_co.getValue(); //reference term
                        if(term_in_co.getKey().equals(term_in_ref_co.getKey()))
                            continue;

                        int deduce = f < rf ? f : rf;
                        int tid = feature.lookupTerm(term_in_co.getKey()); //get index of target term
                        int tid_f = feature.lookupRefTerm(term_in_ref_co.getKey()); //get index of reference term
                        if(tid==-1||tid_f==-1)
                            continue;// after tracking terms in overlapping zones coocurrence stat workers
                                    // may have filtered some terms so they do not exist in the index
                        feature.deduce(tid, tid_f, deduce);
                    }
                }
            }
        }


        sb = new StringBuilder("Complete building features, total contexts processed=" + total);
        sb.append("; total indexed candidate terms=").append(feature.termCounter).append(";")
                .append(" total indexed reference terms=").append(feature.ctxTermCounter);
        LOG.info(sb.toString());

        return feature;
    }
}
