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
public class ComboBasicWorker extends JATERecursiveTaskWorker<String, List<JATETerm>> {

    private static final long serialVersionUID = 6487612650560197335L;
    protected FrequencyTermBased fFeature;
    protected Containment cFeature;
    protected Containment crFeature;
    private double alpha;
    private double beta;

    public ComboBasicWorker(List<String> tasks, int maxTasksPerWorker,
                       FrequencyTermBased fFeature, Containment cFeature,
                            Containment crFeature,
                       double alpha, double beta
    ) {
        super(tasks, maxTasksPerWorker);
        this.fFeature=fFeature;
        this.cFeature=cFeature;
        this.crFeature=crFeature;
        this.alpha=alpha;
        this.beta=beta;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> candidates) {
        return new ComboBasicWorker(candidates, maxTasksPerThread, fFeature, cFeature,crFeature, alpha,beta
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
            /*if(tString.equals("language"))
                System.out.println();*/
            int ttf = fFeature.getTTF(tString);
            JATETerm term = new JATETerm(tString);
            //ComboBasic(t) = |t| log f (t) + αe t + βe ′ t ,
            double t =(double) tString.split(" ").length;
            if(t==1.0){
                term.setScore(Math.log(ttf));
            }
            else {
                double log = Math.log(ttf);

                Set<String> parentTerms = cFeature.getTermParents(tString);
                double et = (double) parentTerms.size();

                /*for (String parentTerm : parentTerms) {
                    et += (double) fFeature.getTTF(parentTerm);
                }*/

                Set<String> childTerms = crFeature.getTermParents(tString);
                double etp = (double) childTerms.size();
                /*for (String childTerm : childTerms) {
                    etp += (double) fFeature.getTTF(childTerm);
                }*/

                double score = t * log + alpha * et + beta * etp;
                term.setScore(score);
            }
            result.add(term);
        }
        return result;
    }
}

