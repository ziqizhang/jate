package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyDocBased;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 *
 */
public class TermEx extends Algorithm{

    private final double _alpha;
    private final double _beta;
    private final double _zeta;

    public static final String PREFIX_REF ="REF_";
    public static final String PREFIX_WORD="WORD_";

    public TermEx() {
        this(0.33,0.33,0.34);
    }

    public TermEx(double alpha, double beta, double zeta) {
        _alpha = alpha;
        _beta = beta;
        _zeta = zeta;
    }

    @Override
    public List<JATETerm> execute() throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeatureTerms = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName()+"_"+PREFIX_WORD);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature2;

        AbstractFeature feature3 = features.get(FrequencyTermBased.class.getName()+"_"+PREFIX_REF);
        validateFeature(feature3, FrequencyTermBased.class);
        FrequencyTermBased fFeatureRef = (FrequencyTermBased) feature3;

        AbstractFeature feature4 = features.get(FrequencyDocBased.class.getName());
        validateFeature(feature4, FrequencyDocBased.class);
        FrequencyDocBased fFeatureDocs = (FrequencyDocBased) feature4;

        List<JATETerm> result = new ArrayList<>();
        boolean collectInfo = termInfoCollector != null;
        double totalTermsInCorpus = fFeatureTerms.getCorpusTotal();
        for(Map.Entry<String, Integer> entry: fFeatureTerms.getMapTerm2TTF().entrySet()) {
            String tString = entry.getKey();

            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;
            double SUMfwi = 0.0;

            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                SUMwi += (double) fFeatureWords.getTTF(wi) / totalTermsInCorpus /
                        (fFeatureRef.getTTFNorm(wi) + ((double) fFeatureWords.getTTF(wi) / totalTermsInCorpus));
                SUMfwi += (double) fFeatureWords.getTTF(wi);
            }

            //calc DC
            Set<Integer> docs = fFeatureTerms.getTermFrequencyInDocument(tString).keySet();
            double sum = 0;
            for (int i : docs) {
                int tfid = fFeatureDocs.getTFID(i).get(tString);
                int ttfid = fFeatureDocs.getMapDoc2TTF().get(i);
                double norm = tfid==0?0: (double)tfid/ttfid;
                if (norm == 0) sum += 0;
                else {
                    sum += norm * Math.log(norm + 0.1);
                }
            }

            double DR = SUMwi;
            double DC = sum;
            double LC = (T * Math.log(fFeatureTerms.getTTF(tString) + 1) * fFeatureTerms.getTTF(tString)) / SUMfwi;

            //System.out.println(DR+"------"+DC+"------"+LC);
            double score = _alpha * DR + _beta * DC + _zeta * LC;
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
