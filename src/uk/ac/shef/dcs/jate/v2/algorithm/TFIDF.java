package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.*;
import java.util.logging.Logger;

/**
 * tfidf modified to work at corpus level. Namely, "tf" is now "ttf"=total term frequency in the corpus
 */
public class TFIDF extends Algorithm {
    private static final Logger LOG = Logger.getLogger(TFIDF.class.getName());
    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;

        double totalDocs = (double) fFeature.getTotalDocs();
        boolean collectInfo = termInfoCollector != null;
        List<JATETerm> result = new ArrayList<>();

        StringBuilder msg = new StringBuilder("Beginning computing TermEx values,");
        msg.append(", total terms=" + candidates.size());
        LOG.info(msg.toString());
        for (String tString: candidates) {
            JATETerm term = new JATETerm(tString);
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
        Collections.sort(result);
        LOG.info("Complete");
        return result;
    }
}
