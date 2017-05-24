package uk.ac.shef.dcs.jate.algorithm;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.feature.Containment;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Created by zqz on 24/05/17.
 */
public class BasicWorker extends JATERecursiveTaskWorker<String, List<JATETerm>> {

    private static final long serialVersionUID = 6487612650560197335L;
    protected FrequencyTermBased fFeature;
    protected Containment cFeature;
    private double alpha;

    public BasicWorker(List<String> tasks, int maxTasksPerWorker,
                        FrequencyTermBased fFeature, Containment cFeature,
                       double alpha
    ) {
        super(tasks, maxTasksPerWorker);
        this.fFeature=fFeature;
        this.cFeature=cFeature;
        this.alpha=alpha;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> candidates) {
        return new BasicWorker(candidates, maxTasksPerThread, fFeature, cFeature,alpha
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
            //Basic(t) = |t| log f (t) + αe t
            double t =(double) tString.split(" ").length;
            double log = Math.log(ttf);

            Set<String> parentTerms = cFeature.getTermParents(tString);
            double et = (double) parentTerms.size();

            for (String parentTerm : parentTerms) {
                et += (double) fFeature.getTTF(parentTerm);
            }

            double score = t*log+alpha*et;
            term.setScore(score);

            result.add(term);
        }
        return result;
    }
}

