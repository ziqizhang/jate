package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyFeature;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 */
public class GlossEx extends Algorithm {
    protected final double _alpha;
    protected final double _beta;

    public static final String PREFIX_REF ="REF_";
    public static final String PREFIX_WORD="WORD_";

    public GlossEx(){
        this(0.2,0.8);
    }

    public GlossEx(double alpha, double beta){
        _alpha = alpha;
        _beta = beta;
    }

    public List<JATETerm> execute() throws JATEException {
        AbstractFeature feature = features.get(FrequencyFeature.class.getName());
        validateFeature(feature, FrequencyFeature.class);
        FrequencyFeature fFeatureTerms = (FrequencyFeature) feature;

        AbstractFeature feature2 = features.get(FrequencyFeature.class.getName()+"_"+PREFIX_WORD);
        validateFeature(feature2, FrequencyFeature.class);
        FrequencyFeature fFeatureWords = (FrequencyFeature) feature2;

        AbstractFeature feature3 = features.get(FrequencyFeature.class.getName()+"_"+PREFIX_REF);
        validateFeature(feature3, FrequencyFeature.class);
        FrequencyFeature fFeatureRef = (FrequencyFeature) feature3;

        List<JATETerm> result = new ArrayList<>();
        boolean collectInfo = termInfoCollector != null;
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();
        for(Map.Entry<String, Integer> entry: fFeatureTerms.getMapTerm2TTF().entrySet()){
            String tString = entry.getKey();
            double score;
            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;
            double SUMfwi = 0.0;

            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                SUMwi += /*Math.log(*/(double) fFeatureWords.getTTF(wi) / totalWordsInCorpus / fFeatureRef.getTTFNorm(wi)/*)*/;
                SUMfwi += (double) fFeatureWords.getTTF(wi);
            }

            double TD = SUMwi / T;
            double TC = (T * Math.log10(fFeatureTerms.getTTF(tString) + 1) * fFeatureTerms.getTTF(tString)) / SUMfwi;

            if (T == 1) score= 0.9 * TD + 0.1 * TC;
            else score= _alpha * TD + _beta * TC;

            JATETerm term  =new JATETerm(tString, score);
            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }

        return result;
    }
}
