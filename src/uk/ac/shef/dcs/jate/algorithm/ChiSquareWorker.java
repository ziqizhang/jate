package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.feature.FrequencyCtxBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;

class ChiSquareWorker extends JATERecursiveTaskWorker<String, List<JATETerm>> {
	
	private static final long serialVersionUID = -5293190120654351590L;
	protected FrequencyCtxBased fFeatureCtxBased;
    protected Cooccurrence fFeatureCoocurr;
    protected FrequencyTermBased fFeatureTerms;

    public ChiSquareWorker(List<String> terms, int maxTasksPerWorker,
                           FrequencyTermBased frequencyTermBased,
                           FrequencyCtxBased fFeatureCtxBased, Cooccurrence fFeatureCoocurr) {
        super(terms, maxTasksPerWorker);
        this.fFeatureTerms = frequencyTermBased;
        this.fFeatureCoocurr = fFeatureCoocurr;
        this.fFeatureCtxBased = fFeatureCtxBased;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> terms) {
        return new ChiSquareWorker(terms, maxTasksPerThread,
                fFeatureTerms, fFeatureCtxBased, fFeatureCoocurr
                );
    }

    @Override
    protected List<JATETerm> mergeResult(List<JATERecursiveTaskWorker<String, List<JATETerm>>> jateRecursiveTaskWorkers) {
        List<JATETerm> result = new ArrayList<>();
        for (JATERecursiveTaskWorker<String, List<JATETerm>> worker : jateRecursiveTaskWorkers) {
            result.addAll(worker.join());
        }

        return result;
    }

    @Override
    protected List<JATETerm> computeSingleWorker(List<String> candidates) {
    	//TODO: ziqi, pls make this code more clear by refactoring into multiple meaningful/testable functions
    	
        List<JATETerm> result = new ArrayList<>();
        Map<String, Integer> ctxTTFLookup = new HashMap<>();//X lookup: the sum of the total number of terms in sentences where X appears
       
        int totalTermsInCorpus = fFeatureTerms.getCorpusTotal();
        for (String tString : candidates) {
            Integer n_w = ctxTTFLookup.get(tString);//"the total number of terms in contexts (original paper: sentences)
            // where w appears".
            if (n_w == null) {
                n_w = 0;
                Set<String> ctx_w = fFeatureCtxBased.getContextIds(tString);
                if (ctx_w == null) {
                    continue;//this is possible if during co-occurrence computing this term is skipped
                    //because it did not satisfy minimum thresholds
                }
                for (String ctxid : ctx_w)
                    n_w += fFeatureCtxBased.getMapCtx2TTF().get(ctxid);

                ctxTTFLookup.put(tString, n_w);
            }

            double sumChiSquare_w = 0.0, maxChiSquare = 0.0;
            Map<Integer, Integer> coocurTermIdx2Freq = fFeatureCoocurr.getCoocurrence(tString);
            for (Map.Entry<Integer, Integer> entry : coocurTermIdx2Freq.entrySet()) {
                int g_id = entry.getKey();
                String g_term = fFeatureCoocurr.lookupTerm(g_id);
                int freq_wg = entry.getValue();

                Integer g_w = ctxTTFLookup.get(g_id); //the sum of the total number of terms in sentences where g appears
                if (g_w == null) {
                    g_w = 0;
                    Set<String> ctx_g = fFeatureCtxBased.getContextIds(g_term);
                    if (ctx_g == null) {
                        continue;//this is possible if during co-occurrence computing this term is skipped
                        //because it did not satisfy minimum thresholds
                    }
                    for (String ctxid : ctx_g)
                        g_w += fFeatureCtxBased.getMapCtx2TTF().get(ctxid);
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

            //if a term has no co-occurrence info, it has a score of 0
            double score = sumChiSquare_w - maxChiSquare;
            JATETerm term = new JATETerm(tString, score);
            result.add(term);
        }
        return result;
    }
}
