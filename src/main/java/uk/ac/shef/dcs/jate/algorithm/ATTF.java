package uk.ac.shef.dcs.jate.algorithm;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;

/**
 * Average Total Term Frequency (ATTF). Compute the total frequency of a term in a corpus (ttf), and total document frequency (tdf).
 * Then divide ttf by tdf
 */
public class ATTF extends Algorithm{
    private static Logger LOG = Logger.getLogger(ATTF.class.getName());
    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException {
        LOG.info("Calculating ATTF for "+candidates.size()+" candidate terms.");
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);

        FrequencyTermBased fFeature = (FrequencyTermBased) feature;
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

            result.add(term);
        }
        Collections.sort(result);
        LOG.info("Complete calculating ATTF");
        return result;
    }
}
