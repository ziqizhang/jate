package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.ContextWindow;
import uk.ac.shef.dcs.jate.feature.FrequencyCtxBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;
import org.apache.log4j.Logger;

/**
 * An implementation of the TermEx term recognition algorithm. See Sclano e. al 2007, <i>
 * TermExtractor: a Web application to learn the shared terminology of emergent web communities</i>
 * <p>
 * In the formula w(t,Di ) =a* DR + B* DC + Y* LC, default values of a, B, and Y are 0.33.
 * </p>
 * <p>
 * This is the implementation of the scoring formula <b>only</b> and does not include the analysis of document structure
 * as discussed in the paper.
 */
public class TermEx extends ReferenceBased {
    private static final Logger LOG = Logger.getLogger(TermEx.class.getName());
    private final double alpha;
    private final double beta;
    private final double zeta;

    public static final String SUFFIX_REF = "_REF";
    public static final String SUFFIX_WORD = "_WORD";
    public static final String SUFFIX_DOC = "_DOC";

    public TermEx() {
        this(0.33, 0.33, 0.34, true);
    }

    public TermEx(double alpha, double beta, double zeta, boolean matchOOM) {
        super(matchOOM);
        this.alpha = alpha;
        this.beta = beta;
        this.zeta = zeta;
    }

    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName() + SUFFIX_WORD);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature2;

        AbstractFeature feature4 = features.get(FrequencyCtxBased.class.getName() + SUFFIX_DOC);
        validateFeature(feature4, FrequencyCtxBased.class);
        FrequencyCtxBased fFeatureDocs = (FrequencyCtxBased) feature4;

        List<FrequencyTermBased> referenceFeatures = new ArrayList<>();
        Map<FrequencyTermBased, Double> mapNullWordProbInReference = new HashMap<>();
        Map<FrequencyTermBased, Double> mapRefScalars = new HashMap<>();
        for (Map.Entry<String, AbstractFeature> en : features.entrySet()) {
            if (en.getKey().startsWith(FrequencyTermBased.class.getName() + SUFFIX_REF)) {
                validateFeature(en.getValue(), FrequencyTermBased.class);
                FrequencyTermBased fFeatureRef = (FrequencyTermBased) en.getValue();
                referenceFeatures.add(fFeatureRef);
                mapNullWordProbInReference.put(fFeatureRef, setNullWordProbInReference(fFeatureRef));
                mapRefScalars.put(fFeatureRef, matchOrdersOfMagnitude(fFeatureWords, fFeatureRef));
            }
        }


        List<JATETerm> result = new ArrayList<>();
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();
        StringBuilder msg = new StringBuilder("Beginning computing TermEx values,");
        msg.append(", total terms=" + candidates.size());
        LOG.info(msg.toString());
        for (String tString : candidates) {
            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;
            double SUMfwi = 0.0;

            //the original paper looks up the term directly (tString). But in many case, technical terms
            //are unlikely to be found in reference corpus. So we break term into component words and look up words
            //then combine the scores
            for (int i = 0; i < T; i++) {
                String wi = elements[i];

                double max_freq_t_dj_norm = 0;
                FrequencyTermBased selectedRefFeature = referenceFeatures.get(0);
                for (FrequencyTermBased refFeature : referenceFeatures) {
                    double freqNorm = refFeature.getTTFNorm(wi);
                    if (freqNorm > max_freq_t_dj_norm) {
                        max_freq_t_dj_norm = freqNorm;
                        selectedRefFeature = refFeature;
                    }
                }
                if (max_freq_t_dj_norm == 0)
                    max_freq_t_dj_norm = mapNullWordProbInReference.get(selectedRefFeature);
                double refScalar = mapRefScalars.get(selectedRefFeature);
                max_freq_t_dj_norm *= refScalar;

                SUMwi += (double) fFeatureWords.getTTF(wi) / totalWordsInCorpus /
                        max_freq_t_dj_norm;
                SUMfwi += (double) fFeatureWords.getTTF(wi);
            }

            //calc DC
            Set<Integer> docs = fFeatureTerms.getTermFrequencyInDocument(tString).keySet();
            double sum = 0;
            for (int i : docs) {
                //do not query for features using this context window. but query to get the
                //real context window object that matches this id
                ContextWindow c = new ContextWindow();
                c.setDocId(i);
                c = fFeatureDocs.getContextWindow(c.toString());
                if (c == null)
                    LOG.error(String.format("TermEx error: expected context window does not exist in doc [%s]", i));

                int tfid = fFeatureDocs.getTFIC(c).get(tString);
                int ttfid = fFeatureDocs.getMapCtx2TTF().get(c);
                double norm = tfid == 0 ? 0 : (double) tfid / ttfid;
                if (norm == 0) sum += 0;
                else {
                    sum += norm * Math.log(norm + 0.1);
                }
            }

            double DP = SUMwi; //this term has been changed to ensure they are in the range of 0 and 1
            double DC = sum;
            double LC = SUMfwi == 0 ? 0 : (T * Math.log(fFeatureTerms.getTTF(tString) + 1) * fFeatureTerms.getTTF(tString)) / SUMfwi;

            double score = alpha * DP + beta * DC + zeta * LC;
            JATETerm term = new JATETerm(tString, score);
            result.add(term);
        }

        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
