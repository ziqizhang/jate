package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.model.TermInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 * An implementation of the GlossEx term recognition algorithm. See Park, et. al 2002, <i>
 * Automatic Glossary Extraction: beyond terminology identification</i>
 *. This is the implementation of the scoring formula <b>only</b>, and does not include the filtering algorithm as mentioned
 * in the paper.
 * <p>
 * In the equation C(T) = a* TD(T) + B*TC(T), default a=0.2, B = 0.8.
 * </p>
 *
 * You might need to modify the value of B by increasing it substaintially when the reference corpus is relatively
 * much bigger than the target corpus, such as the BNC corpus. For details, please refer to the paper.
 *
 */
public class GlossEx extends ReferenceBased {
    protected final double alpha;
    protected final double beta;

    private static final Logger LOG = Logger.getLogger(GlossEx.class.getName());
    public static final String SUFFIX_REF ="_REF";
    public static final String SUFFIX_WORD ="_WORD";

    public GlossEx(){
        this(0.2,0.8,true);
    }

    public GlossEx(double alpha, double beta, boolean matchOOM){
        super(matchOOM);
        this.alpha = alpha;
        this.beta = beta;
    }

    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName() + SUFFIX_WORD);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature2;

        AbstractFeature feature3 = features.get(FrequencyTermBased.class.getName() + SUFFIX_REF);
        validateFeature(feature3, FrequencyTermBased.class);
        FrequencyTermBased fFeatureRef = (FrequencyTermBased) feature3;
        nullWordProbInReference = setNullWordProbInReference(fFeatureRef);
        double refScalar = matchOrdersOfMagnitude(fFeatureWords, fFeatureRef);

        List<JATETerm> result = new ArrayList<>();
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();
        LOG.info("Calculating GlossEx for "+candidates.size()+" candidate terms.");
        for (String tString : candidates) {
            int ttf = fFeatureTerms.getTTF(tString);
            double score;
            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;
            double SUMfwi = 0.0;
            //some terms are artificially incremented by 1 to avoid division by 0 and also to
            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                double pc_wi = fFeatureRef.getTTFNorm(wi);
                if (pc_wi == 0)
                    pc_wi = nullWordProbInReference; //
                pc_wi*=refScalar;
                SUMwi += /*Math.log(*/(double) fFeatureWords.getTTF(wi) / totalWordsInCorpus / pc_wi/*)*/;
                SUMfwi += (double) fFeatureWords.getTTF(wi);
            }

            double TD = SUMwi / T;
            double TC = (T * Math.log10(ttf + 1) * ttf) / (SUMfwi + 1);

            if (T == 1) score = 0.9 * TD + 0.1 * TC;
            else score = alpha * TD + beta * TC;

            JATETerm term = new JATETerm(tString, score);

            result.add(term);
        }

        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }



}
