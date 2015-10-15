package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.*;
import java.util.logging.Logger;

/**
 * Total Term Frequency in corpus
 */
public class TTF extends Algorithm {
    private static final Logger LOG = Logger.getLogger(TTF.class.getName());
    @Override
    public List<JATETerm> execute(Collection<String> candidates) throws JATEException{
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;
        List<JATETerm> result = new ArrayList<>();

        StringBuilder msg = new StringBuilder("Beginning computing TermEx values,");
        msg.append(", total terms=" + candidates.size());
        LOG.info(msg.toString());
        for(String tString: candidates){
            JATETerm term = new JATETerm(tString, (double)fFeature.getTTF(tString));

            result.add(term);
        }
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
