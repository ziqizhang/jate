package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.deprecated.model.Term;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.ContainmentFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyFeature;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.*;

/**
 * Created by zqz on 19/09/2015.
 */
public class CValue extends Algorithm {

    @Override
    public List<JATETerm> execute() throws JATEException {

        AbstractFeature feature = features.get(FrequencyFeature.class.getName());
        if (feature == null || !(feature instanceof FrequencyFeature)) {
            StringBuilder sb = new StringBuilder(TTF.class.getName());
            sb.append(" requires feature type:").append(FrequencyFeature.class.getName()).append(",")
                    .append(" provided:");
            if (feature == null)
                sb.append("null");
            else
                sb.append(feature.getClass().getName());
            throw new JATEException(sb.toString());
        }

        FrequencyFeature fFeature = (FrequencyFeature) feature;

        AbstractFeature feature2 = features.get(ContainmentFeature.class.getName());
        if (feature2 == null || !(feature2 instanceof ContainmentFeature)) {
            StringBuilder sb = new StringBuilder(TTF.class.getName());
            sb.append(" requires feature type:").append(ContainmentFeature.class.getName()).append(",")
                    .append(" provided:");
            if (feature == null)
                sb.append("null");
            else
                sb.append(feature2.getClass().getName());
            throw new JATEException(sb.toString());
        }

        ContainmentFeature cFeature = (ContainmentFeature) feature2;


        boolean collectInfo = termInfoCollector != null;
        List<JATETerm> result = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : fFeature.getMapTerm2TTF().entrySet()) {
            String tString = entry.getKey();
            JATETerm term = new JATETerm(tString, (double) entry.getValue());

            double score;
            double log2a = Math.log((double) tString.split(" ").length + 0.1) / Math.log(2.0); //Anurag mods for log (a), log(a + 0.1)
            double freqa = (double) entry.getValue();

            Set<String> parentTerms = cFeature.getTermParents(tString);
            double pTa = (double) parentTerms.size();
            double sumFreqb = 0.0;

            for (String parentTerm : parentTerms) {
                sumFreqb += (double) fFeature.getTTF(parentTerm);
                //todo ask jerry to help check the following code
                /*Set<String> tps = cFeature.getTermParents(parentTerm);
                Set<String> tps_already_considered = new HashSet<>();
                for (String tp : tps) {
                    if (!tps_already_considered.contains(tp)) {
                        sumFreqb -= (double) fFeature.getTTF(tp);
                        for (String t : cFeature.getTermParents(tp))
                            tps_already_considered.add(t);
                    }
                }*/
            }

            score = pTa == 0 ? log2a * freqa : log2a * (freqa - (sumFreqb / pTa));
            term.setScore(score);

            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(tString);
                term.setTermInfo(termInfo);
            }
            result.add(term);
        }
        Collections.sort(result);

        return result;
    }
}
