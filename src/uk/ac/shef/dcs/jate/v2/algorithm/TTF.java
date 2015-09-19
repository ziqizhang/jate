package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyFeature;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Created by zqz on 19/09/2015.
 */
public class TTF extends Algorithm {

    @Override
    public List<JATETerm> execute() throws JATEException{
        AbstractFeature feature = features.get(FrequencyFeature.class.getName());
        if(feature==null || !(feature instanceof FrequencyFeature)) {
            StringBuilder sb = new StringBuilder(TTF.class.getName());
            sb.append(" requires feature type:").append(FrequencyFeature.class.getName()).append(",")
                    .append(" provided:");
            if(feature==null)
                sb.append("null");
            else
                sb.append(feature.getClass().getName());
            throw new JATEException(sb.toString());
        }

        FrequencyFeature fFeature = (FrequencyFeature) feature;
        boolean collectInfo=termInfoCollector!=null;
        List<JATETerm> result = new ArrayList<>();
        for(Map.Entry<String, Integer> entry: fFeature.getMapTerm2TTF().entrySet()){
            String tString = entry.getKey();
            JATETerm term = new JATETerm(tString, (double)entry.getValue());

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
