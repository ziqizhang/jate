package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyCtxBased;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.logging.Logger;

/**
 *
 */
public class TermEx extends ReferenceBased{
    private static final Logger LOG = Logger.getLogger(TermEx.class.getName());
    private final double alpha;
    private final double beta;
    private final double zeta;

    public static final String SUFFIX_REF ="_REF";
    public static final String SUFFIX_WORD ="_WORD";
    public static final String SUFFIX_DOC="_DOC";

    public TermEx() {
        this(0.33,0.33,0.34,true);
    }

    public TermEx(double alpha, double beta, double zeta, boolean matchOOM) {
        super(matchOOM);
        this.alpha = alpha;
        this.beta = beta;
        this.zeta = zeta;
    }

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName()+ SUFFIX_WORD);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature2;

        AbstractFeature feature3 = features.get(FrequencyTermBased.class.getName()+ SUFFIX_REF);
        validateFeature(feature3, FrequencyTermBased.class);
        FrequencyTermBased fFeatureRef = (FrequencyTermBased) feature3;

        AbstractFeature feature4 = features.get(FrequencyCtxBased.class.getName()+SUFFIX_DOC);
        validateFeature(feature4, FrequencyCtxBased.class);
        FrequencyCtxBased fFeatureDocs = (FrequencyCtxBased) feature4;

        List<JATETerm> result = new ArrayList<>();
        boolean collectInfo = termInfoCollector != null;
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();
        StringBuilder msg = new StringBuilder("Beginning computing TermEx values,");
        msg.append(", total terms=" + candidates.size());
        LOG.info(msg.toString());
        for(String tString: candidates) {
            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;
            double SUMfwi = 0.0;

            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                /*
                This term is modified to ensure DP within the range of 0~1.0
                 */
                SUMwi += (double) fFeatureWords.getTTF(wi) / totalWordsInCorpus /
                        (fFeatureRef.getTTFNorm(wi) + ((double) fFeatureWords.getTTF(wi) / totalWordsInCorpus));
                SUMfwi += (double) fFeatureWords.getTTF(wi);
            }

            //calc DC
            Set<Integer> docs = fFeatureTerms.getTermFrequencyInDocument(tString).keySet();
            double sum = 0;
            for (int i : docs) {
                int tfid = fFeatureDocs.getTFIC(String.valueOf(i)).get(tString);
                int ttfid = fFeatureDocs.getMapCtx2TTF().get(String.valueOf(i));
                double norm = tfid==0?0: (double)tfid/ttfid;
                if (norm == 0) sum += 0;
                else {
                    sum += norm * Math.log(norm + 0.1);
                }
            }

            double DP = SUMwi;
            double DC = sum;
            double LC = (T * Math.log(fFeatureTerms.getTTF(tString) + 1) * fFeatureTerms.getTTF(tString)) / SUMfwi;

            //System.out.println(DR+"------"+DC+"------"+LC);
            double score = alpha * DP + beta * DC + zeta * LC;
            JATETerm term = new JATETerm(tString, score);
            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }

        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
