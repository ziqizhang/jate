package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;

class ChiSquareWorker extends JATERecursiveTaskWorker<String, List<JATETerm>> {
	
	private static final long serialVersionUID = -5293190120654351590L;
	protected FrequencyCtxBased termFeatureCtxBased;
    protected Cooccurrence fCoocurr;
    protected ChiSquareFrequentTerms fChiSquareFTExpProb;

    public ChiSquareWorker(List<String> terms, int maxTasksPerWorker,
                           FrequencyCtxBased termFeatureCtxBased,
                           Cooccurrence fCoocurr,
                           ChiSquareFrequentTerms fChiSquareFTExpProb) {
        super(terms, maxTasksPerWorker);
        this.fCoocurr = fCoocurr;
        this.termFeatureCtxBased = termFeatureCtxBased;
        this.fChiSquareFTExpProb=fChiSquareFTExpProb;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> terms) {
        return new ChiSquareWorker(terms, maxTasksPerThread,
                termFeatureCtxBased, fCoocurr
                ,fChiSquareFTExpProb);
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
        List<JATETerm> result = new ArrayList<>();
        Map<String, Integer> ctxTTFLookup = new HashMap<>();//w lookup: the sum of the total number of terms in sentences where w appears
       
        for (String tString : candidates) {
            //System.out.println(candidates.hashCode());

            Integer n_w = ctxTTFLookup.get(tString);//"the total number of terms in contexts (original paper: sentences)
            // where w appears".
            if (n_w == null) {
                n_w = 0;
                Set<ContextWindow> ctx_w = termFeatureCtxBased.getContexts(tString);
                if (ctx_w == null) {
                    continue;//this is possible if during co-occurrence computing this term is skipped
                    //because it did not satisfy minimum thresholds
                }
                Map<ContextWindow, Integer> ctx2ttf=termFeatureCtxBased.getMapCtx2TTF();
                for (ContextWindow ctxid : ctx_w)
                    n_w += ctx2ttf.get(ctxid);

                ctxTTFLookup.put(tString, n_w);
            }

            double maxChiSquare = fChiSquareFTExpProb.getMaxExpProb();

            //calculate the sum of expected probability of w
            double sumChiSquare_w = n_w * fChiSquareFTExpProb.getSumExpProb(); //this is equivalent to the sum of foreach (g in G), n_w*p_g
            //which is then equivalent to formula (1) in the paper, setting freq(w,g) to 0 for all g

            Map<Integer, Integer> coocurRefTermIdx2Freq = fCoocurr.getCoocurrence(tString);


            for (Map.Entry<Integer, Integer> entry : coocurRefTermIdx2Freq.entrySet()) {
                int g_id = entry.getKey();
                String g_term = fCoocurr.lookupRefTerm(g_id);
                int freq_wg = entry.getValue(); //co-occurrence of target term w and reference frequent term g

                double p_g = fChiSquareFTExpProb.get(g_term);//lookup expected prob of this frequent term g that co-occur with target term w
                double nw_mult_pg = n_w*p_g;
                sumChiSquare_w-=nw_mult_pg; //deduce p_g, which is when we assume freq_wg=0;

                double diff = freq_wg - nw_mult_pg;
                double chi_g =diff*diff/nw_mult_pg;
                sumChiSquare_w+= chi_g; //readjust score by adding back the chisquare for g, using formula 1 and the real freq_wg

                if(chi_g>maxChiSquare)
                    maxChiSquare=chi_g;

            }

            //if a term has no co-occurrence info, it has a score of 0
            double score = sumChiSquare_w - maxChiSquare;

            //<<<<debug
            /*if (Double.isNaN(score)) {
                System.err.println("NaN, " + tString);
                System.exit(1);
            }*/
            ///
            JATETerm term = new JATETerm(tString, score);
            result.add(term);
        }
        return result;
    }
}
