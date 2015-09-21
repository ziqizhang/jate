package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyCtxBased;
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

    public static final String SUFFIX_SENTENCE="_SENTENCE";

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;
        int totalTermsInCorpus = fFeatureTerms.getCorpusTotal();
        AbstractFeature feature2 = features.get(Cooccurrence.class.getName());
        validateFeature(feature2, Cooccurrence.class);
        Cooccurrence fFeatureCoocurr = (Cooccurrence) feature2;

        AbstractFeature feature3 = features.get(FrequencyCtxBased.class.getName()+SUFFIX_SENTENCE);
        validateFeature(feature3, FrequencyCtxBased.class);
        FrequencyCtxBased fFeatureCtxBased = (FrequencyCtxBased) feature3;

        List<JATETerm> result = new ArrayList<>();
        boolean collectInfo = termInfoCollector != null;

        Map<String, Integer> ctxTTFLookup = new HashMap<>();//X lookup: the sum of the total number of terms in sentences where X appears

        for (String tString : candidates) {
            Integer n_w = ctxTTFLookup.get(tString);//"the total number of terms in contexts (original paper: sentences)
            // where w appears".
            if (n_w == null) {
                Set<Integer> ctx_w = fFeatureCtxBased.getContextIds(tString);
                for (int ctxid : ctx_w) {
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
                    Set<Integer> ctx_g = fFeatureCtxBased.getContextIds(g_term);
                    for (int ctxid : ctx_g) {
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
            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }

        return result;
    }
}
