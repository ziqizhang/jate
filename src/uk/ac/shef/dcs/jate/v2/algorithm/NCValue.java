package uk.ac.shef.dcs.jate.v2.algorithm;

import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.feature.*;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;
import uk.ac.shef.dcs.jate.v2.model.TermInfo;

import java.util.*;

/**
 * Created by zqz on 21/09/2015.
 */
public class NCValue extends Algorithm {
    protected static final double WEIGHT_CVALUE=0.8;
    protected static final double WEIGHT_CONTEXT=0.2;

    @Override
    public List<JATETerm> execute(Set<String> candidates) throws JATEException {
        AbstractFeature feature = features.get(FrequencyTermBased.class.getName());
        validateFeature(feature, FrequencyTermBased.class);
        FrequencyTermBased fFeature = (FrequencyTermBased) feature;

        AbstractFeature feature2 = features.get(Containment.class.getName());
        validateFeature(feature2, Containment.class);
        Containment cFeature = (Containment) feature2;

        AbstractFeature feature3 = features.get(Cooccurrence.class.getName());
        validateFeature(feature3, Cooccurrence.class);
        Cooccurrence fFeatureCoocurr = (Cooccurrence) feature3;

        Map<String, Double> cvalues = new HashMap<>();
        boolean collectInfo = termInfoCollector != null;

        for (String tString : candidates) {
            int ttf = fFeature.getTTF(tString);

            double log2a = Math.log((double) tString.split(" ").length + 0.1) / Math.log(2.0); //Anurag mods for log (a), log(a + 0.1)
            double freqa = (double) ttf;

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

            double cvalue = pTa == 0 ? log2a * freqa : log2a * (freqa - (sumFreqb / pTa));
            cvalues.put(tString, cvalue);
        }
        List<JATETerm> result = new ArrayList<>();
        for(Map.Entry<String, Double> entry: cvalues.entrySet()){
            String term = entry.getKey();
            double cvalue = entry.getValue();
            Map<Integer, Integer> cooccur=fFeatureCoocurr.getCoocurrence(term);
            double ctxScore=0.0;
            for(Map.Entry<Integer,Integer> e: cooccur.entrySet()){
                int ctxTermIdx=e.getKey();
                String ctxTerm = fFeatureCoocurr.lookup(ctxTermIdx);
                Double ctxTermCValue = cvalues.get(ctxTerm);
                if(ctxTermCValue==null)
                    continue;
                int freq = e.getValue();
                ctxScore+=freq*ctxTermCValue;
            }

            JATETerm jTerm = new JATETerm(term, cvalue*WEIGHT_CVALUE+ctxScore*WEIGHT_CONTEXT);
            if (collectInfo) {
                TermInfo termInfo = termInfoCollector.collect(term);
                jTerm.setTermInfo(termInfo);
            }
            result.add(jTerm);
        }
        Collections.sort(result);
        return result;
    }
}
