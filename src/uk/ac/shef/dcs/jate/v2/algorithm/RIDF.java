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
public class RIDF extends Algorithm{
    @Override
    public List<JATETerm> execute() throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;

        double totalDocs = (double) fFeature.getTotalDocs();
        boolean collectInfo=termInfoCollector!=null;
        List<JATETerm> result = new ArrayList<>();

        for(Map.Entry<String, Integer> entry: fFeature.getMapTerm2TTF().entrySet()){
            String tString = entry.getKey();
            JATETerm term = new JATETerm(tString);
            int ttf = entry.getValue();
            double cf_over_N = (double) ttf / totalDocs;
            double exponential = Math.exp(0 - cf_over_N);
            double nominator = totalDocs * (1 - exponential);
            double denominator = (double) fFeature.getTermFrequencyInDocument(tString).size();

            if (denominator == 0) {
                denominator=1; //this shouldnt occur. a term that is firstly extracted from the corpus must have a source
            }
            double ridf = Math.log(nominator / denominator) / Math.log(2.0);
            term.setScore(ridf);
            if(collectInfo){
                TermInfo termInfo =termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }

        return result;
    }
}
