package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.v2.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyCtxBased;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.*;

/**
 */
class ChiSquareWorker extends JATERecursiveTaskWorker<String, List<JATETerm>>{
    protected FrequencyCtxBased fFeatureCtxBased;
    protected Cooccurrence fFeatureCoocurr;
    protected TermInfoCollector termInfoCollector;
    protected FrequencyTermBased fFeatureTerms;

    public ChiSquareWorker(List<String> terms, int maxTasksPerWorker,
                           FrequencyTermBased frequencyTermBased,
                           FrequencyCtxBased fFeatureCtxBased, Cooccurrence fFeatureCoocurr,
                           TermInfoCollector termInfoCollector) {
        super(terms, maxTasksPerWorker);
        this.fFeatureTerms=frequencyTermBased;
        this.fFeatureCoocurr=fFeatureCoocurr;
        this.fFeatureCtxBased=fFeatureCtxBased;
        this.termInfoCollector=termInfoCollector;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> terms) {
        return new ChiSquareWorker(terms, maxTasksPerThread,
                fFeatureTerms, fFeatureCtxBased, fFeatureCoocurr,
                termInfoCollector);
    }

    @Override
    protected List<JATETerm> mergeResult(List<JATERecursiveTaskWorker<String, List<JATETerm>>> jateRecursiveTaskWorkers) {
        List<JATETerm> result = new ArrayList<>();
        for(JATERecursiveTaskWorker<String, List<JATETerm>> worker: jateRecursiveTaskWorkers){
            result.addAll(worker.join());
        }

        return result;
    }

    @Override
    protected List<JATETerm> computeSingleWorker(List<String> candidates) {
        List<JATETerm> result = new ArrayList<>();
        Map<String, Integer> ctxTTFLookup = new HashMap<>();//X lookup: the sum of the total number of terms in sentences where X appears
        int count=0;
        int totalTermsInCorpus = fFeatureTerms.getCorpusTotal();
        for (String tString : candidates) {
            Integer n_w = ctxTTFLookup.get(tString);//"the total number of terms in contexts (original paper: sentences)
            // where w appears".
            if (n_w == null) {
                n_w=0;
                Set<String> ctx_w = fFeatureCtxBased.getContextIds(tString);
                if(ctx_w==null){
                    continue;//this is possible if during co-occurrence computing this term is skipped
                    //because it did not satisfy minimum thresholds
                }
                for (String ctxid : ctx_w) {
                    for (Integer f : fFeatureCtxBased.getTFIC(ctxid).values())
                        n_w += f;
                }
                ctxTTFLookup.put(tString, n_w);
            }

            double sumChiSquare_w = 0.0, maxChiSquare = 0.0;
            Map<Integer, Integer> coocurTermIdx2Freq = fFeatureCoocurr.getCoocurrence(tString);
            for (Map.Entry<Integer, Integer> entry : coocurTermIdx2Freq.entrySet()) {
                int g_id = entry.getKey();
                String g_term = fFeatureCoocurr.lookup(g_id);
                int freq_wg = entry.getValue();

                Integer g_w = ctxTTFLookup.get(g_id);
                if (g_w == null) {
                    g_w=0;
                    Set<String> ctx_g = fFeatureCtxBased.getContextIds(g_term);
                    for (String ctxid : ctx_g) {
                        for (Integer f : fFeatureCtxBased.getTFIC(ctxid).values())
                            g_w += f;
                    }
                    ctxTTFLookup.put(g_term, g_w);
                }
                double p_g = (double) g_w / totalTermsInCorpus;

                double prod = n_w * p_g;
                double diff = freq_wg - prod;
                double chiSquare_w = diff * diff / prod;
                sumChiSquare_w += chiSquare_w;
                if (chiSquare_w > maxChiSquare)
                    maxChiSquare = chiSquare_w;
            }

            double score = sumChiSquare_w - maxChiSquare;
            JATETerm term = new JATETerm(tString, score);
            if (termInfoCollector!=null) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }
        return result;
    }
}
