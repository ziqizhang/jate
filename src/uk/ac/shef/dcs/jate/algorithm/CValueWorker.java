package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.feature.Containment;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.model.TermInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by zqz on 24/09/2015.
 */
class CValueWorker extends JATERecursiveTaskWorker<String, List<JATETerm>>{
    protected FrequencyTermBased fFeature;
    protected Containment cFeature;

    public CValueWorker(List<String> tasks, int maxTasksPerWorker,
                        FrequencyTermBased fFeature, Containment cFeature
                        ) {
        super(tasks, maxTasksPerWorker);
        this.fFeature=fFeature;
        this.cFeature=cFeature;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> candidates) {
        return new CValueWorker(candidates, maxTasksPerThread, fFeature, cFeature
                );
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
        List<JATETerm> result = new ArrayList<>();
        for (String tString: candidates) {
            int ttf = fFeature.getTTF(tString);
            JATETerm term = new JATETerm(tString);

            double score;
            double log2a = Math.log((double) tString.split(" ").length + 0.1) / Math.log(2.0); //Anurag mods for log (a), log(a + 0.1)
            double freqa = (double) ttf;

            Set<String> parentTerms = cFeature.getTermParents(tString);
            double pTa = (double) parentTerms.size();
            double sumFreqb = 0.0;

            for (String parentTerm : parentTerms) {
                sumFreqb += (double) fFeature.getTTF(parentTerm);
            }

            score = pTa == 0 ? log2a * freqa : log2a * (freqa - (sumFreqb / pTa));
            term.setScore(score);

            result.add(term);
        }
        return result;
    }
}
