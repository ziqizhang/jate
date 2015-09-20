package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Created by zqz on 19/09/2015.
 */
public class TFIDF extends Algorithm {
    @Override
    public List<JATETerm> execute() throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;

        double totalDocs = (double) fFeature.getTotalDocs();
        boolean collectInfo = termInfoCollector != null;
        List<JATETerm> result = new ArrayList<>();

        for (Map.Entry<String, Integer> entry : fFeature.getMapTerm2TTF().entrySet()) {
            String tString = entry.getKey();
            JATETerm term = new JATETerm(tString);
            int ttf = entry.getValue();

            double tf = fFeature.getTTFNorm(tString);
            double df = fFeature.getTermFrequencyInDocument(tString).size();
            double idf = Math.log(totalDocs / df);

            term.setScore(tf * idf);
            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }

        return result;
    }
}
