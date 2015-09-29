package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.model.TermInfo;

import java.util.*;
import java.util.logging.Logger;

/**
 * Rose, S., Engel, D., Cramer, N., & Cowley, W. (2010).
 * Automatic Keyword Extraction from Individual Documents. In M. W. Berry & J. Kogan (Eds.),
 * Text Mining: Theory and Applications: John Wiley & Sons.
 */
public class RAKE extends Algorithm {

    private static final Logger LOG = Logger.getLogger(RAKE.class.getName());
    public static final String SUFFIX_WORD = "_WORD";

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName() + SUFFIX_WORD);
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature;

        AbstractFeature ccFeature = features.get(Cooccurrence.class.getName() + SUFFIX_WORD);
        validateFeature(ccFeature, Cooccurrence.class);
        Cooccurrence fFeatureCoocurr = (Cooccurrence) ccFeature;

        List<JATETerm> result = new ArrayList<>();
        Set<String> cooccurrenceDictionary = fFeatureCoocurr.getTerms();
        StringBuilder msg = new StringBuilder("Beginning computing RAKE values,");
        msg.append(", total terms=" + candidates.size());
        LOG.info(msg.toString());
        for (String tString : candidates) {
            String[] elements = tString.split(" ");
            double score = 0;
            for (String word : elements) {
                int freq = fFeatureWords.getTTF(word);
                int degree = freq;
                if (cooccurrenceDictionary.contains(word)) {
                    Map<Integer, Integer> coocurWordIdx2Freq = fFeatureCoocurr.getCoocurrence(word);
                    for (int f : coocurWordIdx2Freq.values())
                        degree += f;
                }

                double wScore = (double) degree / freq;
                score += wScore;
            }

            JATETerm term = new JATETerm(tString, score);
            result.add(term);
        }

        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
