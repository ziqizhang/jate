package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.*;

/**
 * An implementation of the Chi Square algorithm. See Matsuo, Y., Ishizuka, M. </i>
 * Keyword Extraction from a Single Document Using Word Co-Occurrence Statistical Information. </i>
 * Proc. 16th Intl. Florida AI Research Society, 2003, 392-396.
 */
public class ChiSquare extends Algorithm {

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;
        int totalTermsInCorpus = fFeatureTerms.getCorpusTotal();
        AbstractFeature feature2 = features.get(Cooccurrence.class.getName());
        validateFeature(feature, Cooccurrence.class);
        Cooccurrence fFeatureCoocurr = (Cooccurrence) feature2;

        List<JATETerm> result = new ArrayList<>();
        boolean collectInfo = termInfoCollector != null;

        Map<Integer, Integer> ctxTTFLookup = new HashMap<>();//X lookup: the sum of the total number of terms in sentences where X appears

        for (String tString : candidates) {

            int termId = fFeatureCoocurr.lookup(tString);
            Integer n_w = ctxTTFLookup.get(termId);//"the total number of terms in contexts (original paper: sentences)
            // where w appears".
            if (n_w == null) {
                Set<Integer> ctx_w = fFeatureCoocurr.getContexts(termId);
                for (int ctxid : ctx_w)
                    n_w += fFeatureCoocurr.getTTFOfContext(ctxid);
                ctxTTFLookup.put(termId, n_w);
            }

            double sumChiSquare_w = 0.0, maxChiSquare = 0.0;
            Map<Integer, Integer> coocurTermIdx2Freq = fFeatureCoocurr.getCoocurrence(tString);
            for (Map.Entry<Integer, Integer> entry : coocurTermIdx2Freq.entrySet()) {
                int g_id = entry.getKey();
                int freq_wg = entry.getValue();

                Integer g_w = ctxTTFLookup.get(g_id);
                if (g_w == null) {
                    Set<Integer> ctx_g = fFeatureCoocurr.getContexts(g_id);
                    for (int ctxid : ctx_g)
                        g_w += fFeatureCoocurr.getTTFOfContext(ctxid);
                    ctxTTFLookup.put(g_id, g_w);
                }
                double p_g = (double) g_w / totalTermsInCorpus;

                double prod = n_w * p_g;
                double diff = freq_wg - prod;
                double chiSquare_w = diff * diff / prod;
                sumChiSquare_w += chiSquare_w;
                if (chiSquare_w > maxChiSquare)
                    maxChiSquare = chiSquare_w;
            }

            double score = sumChiSquare_w-maxChiSquare;
            JATETerm term = new JATETerm(tString, score);
            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }

        return result;
    }
}
