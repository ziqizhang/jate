package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.*;
import java.util.logging.Logger;

/**
 * Average Total Term Frequency = TTF/ doc freq
 */
public class ATTF extends Algorithm{
    private static Logger LOG = Logger.getLogger(ATTF.class.getName());
    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        candidates.remove("");

        LOG.info("Calculating ATTF for "+candidates.size()+" candidate terms.");
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);

        FrequencyTermBased fFeature = (FrequencyTermBased) feature;
        boolean collectInfo=termInfoCollector!=null;
        List<JATETerm> result = new ArrayList<>();
        for(String tString: candidates){
            Integer ttf = fFeature.getTTF(tString);
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
        LOG.info("Complete calculating ATTF");
        return result;
    }
}
