package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Average Total Term Frequency = TTF/ doc freq
 */
public class ATTF extends Algorithm{
    @Override
    public List<JATETerm> execute() throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);

        FrequencyTermBased fFeature = (FrequencyTermBased) feature;
        boolean collectInfo=termInfoCollector!=null;
        List<JATETerm> result = new ArrayList<>();
        for(Map.Entry<String, Integer> entry: fFeature.getMapTerm2TTF().entrySet()){
            String tString = entry.getKey();
            Integer ttf = entry.getValue();
            Integer docFrequency = fFeature.getTermFrequencyInDocument(tString).size();
            double score;
            if(ttf==0)
                score=0;
            else
                score = (double)ttf/docFrequency;
            JATETerm term = new JATETerm(tString, score);

            if(collectInfo){
                TermInfo termInfo =termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }
        Collections.sort(result);
        return result;
    }
}
