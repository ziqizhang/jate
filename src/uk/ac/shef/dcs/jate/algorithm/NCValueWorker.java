package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.feature.Containment;
import uk.ac.shef.dcs.jate.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.model.TermInfo;

import java.util.*;


class NCValueWorker extends JATERecursiveTaskWorker<String, List<JATETerm>>{

    protected FrequencyTermBased fFeature;
    protected Containment cFeature;
    protected Cooccurrence ccFeature;
    protected double weightCValue;
    protected double weightContext;
    public NCValueWorker(List<String> tasks, int maxTasksPerWorker,
                         FrequencyTermBased fFeature,
                         Containment cFeature,
                         Cooccurrence ccFeature,
                         double weightCValue, double weightContext) {
        super(tasks, maxTasksPerWorker);
        this.fFeature=fFeature;
        this.cFeature=cFeature;
        this.ccFeature=ccFeature;
        this.weightCValue=weightCValue;
        this.weightContext=weightContext;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> candidates) {
        return new NCValueWorker(candidates,maxTasksPerThread,
                fFeature, cFeature, ccFeature,
                weightCValue, weightContext);
    }

    @Override
    protected List<JATETerm> mergeResult(List<JATERecursiveTaskWorker<String, List<JATETerm>>> jateRecursiveTaskWorkers) {
        List<JATETerm> result = new ArrayList<>();
        for(JATERecursiveTaskWorker<String, List<JATETerm>> worker: jateRecursiveTaskWorkers){
            result.addAll(worker.join());
        }
        return result;
    }

    @Override
    protected List<JATETerm> computeSingleWorker(List<String> candidates) {
        Map<String, Double> cvalues = new HashMap<>();

        for (String tString : candidates) {
            int ttf = fFeature.getTTF(tString);

            double log2a = Math.log((double) tString.split(" ").length + 0.1) / Math.log(2.0); //Anurag mods for log (a), log(a + 0.1)
            double freqa = (double) ttf;

            Set<String> parentTerms = cFeature.getTermParents(tString);
            double pTa = (double) parentTerms.size();
            double sumFreqb = 0.0;

            for (String parentTerm : parentTerms) {
                sumFreqb += (double) fFeature.getTTF(parentTerm);
            }

            double cvalue = pTa == 0 ? log2a * freqa : log2a * (freqa - (sumFreqb / pTa));
            cvalues.put(tString, cvalue);
        }
        List<JATETerm> result = new ArrayList<>();

        /*
        If you want to select just top N terms as context words, you process "cvalues"
        and make a set containing your selection
         */
        for(Map.Entry<String, Double> entry: cvalues.entrySet()){
            String term = entry.getKey();
            double cvalue = entry.getValue();
            Map<Integer, Integer> cooccur=ccFeature.getCoocurrence(term);
            if(cooccur==null)
                continue;
            double ctxScore=0.0;
            /*
            If you want to select just top N terms as context words, here is where you
             check each co-occurring term against your filter selected above
             */
            for(Map.Entry<Integer,Integer> e: cooccur.entrySet()){
                int ctxTermIdx=e.getKey();
                String ctxTerm = ccFeature.lookup(ctxTermIdx);
                Double ctxTermCValue = cvalues.get(ctxTerm);
                if(ctxTermCValue==null)
                    continue;
                int freq = e.getValue();
                ctxScore+=freq*ctxTermCValue;
            }

            JATETerm jTerm = new JATETerm(term, cvalue*weightCValue+ctxScore*weightContext);
            result.add(jTerm);
        }
        return result;
    }
}
