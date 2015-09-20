package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by zqz on 19/09/2015.
 */
public class Weirdness extends Algorithm {

    public static final String SUFFIX_REF ="_REF";
    public static final String SUFFIX_WORD ="_WORD";

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature1 = features.get(FrequencyTermBased.class.getName()+"_"+SUFFIX_WORD);
        validateFeature(feature1, FrequencyTermBased.class);
        FrequencyTermBased fFeatureWords = (FrequencyTermBased) feature1;

        AbstractFeature feature2 = features.get(FrequencyTermBased.class.getName()+"_"+ SUFFIX_REF);
        validateFeature(feature2, FrequencyTermBased.class);
        FrequencyTermBased fFeatureRef = (FrequencyTermBased) feature2;
        List<JATETerm> result = new ArrayList<>();
        boolean collectInfo = termInfoCollector != null;
        double totalWordsInCorpus = fFeatureWords.getCorpusTotal();
        for(String tString: candidates) {
            JATETerm term = new JATETerm(tString);

            String[] elements = tString.split(" ");
            double T = (double) elements.length;
            double SUMwi = 0.0;

            for (int i = 0; i < T; i++) {
                String wi = elements[i];
                double v = (double) fFeatureWords.getTTF(wi) / totalWordsInCorpus / fFeatureRef.getTTFNorm(wi);
                //SUMwi += Math.log((double) gFeatureStore.getWordFreq(wi) / (double) gFeatureStore.getTotalCorpusWordFreq() / gFeatureStore.getRefWordFreqNorm(wi));
                SUMwi += Math.log(v);
            }

            double TD = SUMwi / T;
            term.setScore(TD);

            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }
        return result;
    }

}
