package uk.ac.shef.dcs.jate.v2.feature;

import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class CooccurrenceFBWorker extends JATERecursiveTaskWorker<String, Cooccurrence> {
    private static final Logger LOG = Logger.getLogger(CooccurrenceFBWorker.class.getName());
    protected FrequencyCtxBased frequencyCtxBased;

    public CooccurrenceFBWorker(List<String> contextIds, FrequencyCtxBased frequencyCtxBased, int maxTasksPerWorker) {
        super(contextIds, maxTasksPerWorker);
        this.frequencyCtxBased = frequencyCtxBased;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Cooccurrence> createInstance(List<String> contextIdSplit) {
        return new CooccurrenceFBWorker(contextIdSplit, frequencyCtxBased, maxTasksPerThread);
    }

    @Override
    protected Cooccurrence mergeResult(List<JATERecursiveTaskWorker<String, Cooccurrence>> jateRecursiveTaskWorkers) {
        List<Cooccurrence> workerOutput = new ArrayList<>();
        Set<String> allTerms = new HashSet<>();
        for (JATERecursiveTaskWorker<String, Cooccurrence> worker : jateRecursiveTaskWorkers) {
            Cooccurrence output = worker.join();
            allTerms.addAll(output.getTerms());
            workerOutput.add(worker.join());
        }

        Cooccurrence joined = new Cooccurrence(allTerms.size());
        for(Cooccurrence output: workerOutput){
            for(int term1Id=0; term1Id< output.getNumTerms(); term1Id++){
                Map<Integer, Integer> cooccurrence=output.getCooccurrence(term1Id);
                String term1 = output.lookup(term1Id);
                int newTerm1Id=joined.lookupAndIndex(term1);
                for(Map.Entry<Integer, Integer> ent: cooccurrence.entrySet()){
                    int term2Id=ent.getKey();
                    String term2=output.lookup(term2Id);
                    int freq=ent.getValue();

                    int newTerm2Id=joined.lookupAndIndex(term2);
                    joined.increment(newTerm1Id, newTerm2Id, freq);
                }
            }
        }
        return joined;
    }

    @Override
    protected Cooccurrence computeSingleWorker(List<String> contextIds) {
        Cooccurrence feature = new Cooccurrence(contextIds.size());
        for (String ctxId : contextIds) {
            Map<String, Integer> termsInContext = frequencyCtxBased.getTFIC(ctxId);

            for (Map.Entry<String, Integer> entry : termsInContext.entrySet()) {
                String targetTerm = entry.getKey();
                int targetFIC = entry.getValue(); //frequency of term in this context
                int targetIdx = feature.lookupAndIndex(targetTerm);
                for (Map.Entry<String, Integer> en : termsInContext.entrySet()) {
                    String refTerm = en.getKey();
                    if (refTerm.equals(targetTerm))
                        continue;

                    int refTermFIC = en.getValue();
                    int refIdx = feature.lookupAndIndex(refTerm);

                    int coocurringFreq = targetFIC < refTermFIC ? targetFIC : refTermFIC;
                    feature.increment(targetIdx, refIdx, coocurringFreq);
                }
            }
        }

        return feature;
    }
}
