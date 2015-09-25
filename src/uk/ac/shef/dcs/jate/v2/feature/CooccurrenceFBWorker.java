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
    protected FrequencyTermBased frequencyTermBased;
    protected int minTTF;
    protected int minTCF;

    public CooccurrenceFBWorker(List<String> contextIds,
                                FrequencyTermBased frequencyTermBased,
                                int minTTF,
                                FrequencyCtxBased frequencyCtxBased,
                                int minTCF,
                                int maxTasksPerWorker) {
        super(contextIds, maxTasksPerWorker);
        this.frequencyCtxBased = frequencyCtxBased;
        this.frequencyTermBased = frequencyTermBased;
        this.minTTF = minTTF;
        this.minTCF = minTCF;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Cooccurrence> createInstance(List<String> contextIdSplit) {
        return new CooccurrenceFBWorker(contextIdSplit, frequencyTermBased,
                minTTF, frequencyCtxBased, minTCF, maxTasksPerThread);
    }

    @Override
    protected Cooccurrence mergeResult(List<JATERecursiveTaskWorker<String, Cooccurrence>> jateRecursiveTaskWorkers) {
        List<Cooccurrence> workerOutput = new ArrayList<>();
        Set<String> allTerms = new HashSet<>();
        for (JATERecursiveTaskWorker<String, Cooccurrence> worker : jateRecursiveTaskWorkers) {
            Cooccurrence output = worker.join();
            allTerms.addAll(output.getTerms());
            workerOutput.add(output);
        }

        LOG.info("Joining output from multiple workers, #="+jateRecursiveTaskWorkers.size());
        Cooccurrence joined = new Cooccurrence(allTerms.size());
        for (Cooccurrence output : workerOutput) {
            for (int term1Id = 0; term1Id < output.getNumTerms(); term1Id++) {
                Map<Integer, Integer> cooccurrence = output.getCooccurrence(term1Id);
                String term1 = output.lookup(term1Id);
                int newTerm1Id = joined.lookupAndIndex(term1);
                for (Map.Entry<Integer, Integer> ent : cooccurrence.entrySet()) {
                    int term2Id = ent.getKey();
                    String term2 = output.lookup(term2Id);
                    int freq = ent.getValue();

                    int newTerm2Id = joined.lookupAndIndex(term2);
                    joined.increment(newTerm1Id, newTerm2Id, freq);
                }
            }
        }
        return joined;
    }

    @Override
    protected Cooccurrence computeSingleWorker(List<String> contextIds) {
        //first pass determine matrix dimension
        Set<String> unique = new HashSet<>();
        for (String ctxId : contextIds) {
            Map<String, Integer> termsInContext = frequencyCtxBased.getTFIC(ctxId);
            if (minTTF == 0 && minTCF == 0)
                unique.addAll(termsInContext.keySet());
            else {
                for (String term : termsInContext.keySet()) {
                    if (frequencyTermBased.getTTF(term) >= minTTF && frequencyCtxBased.getContextIds(term).size() >= minTCF)
                        unique.add(term);
                }
            }
        }

        StringBuilder sb = new StringBuilder("Total ctx to process=");
        sb.append(contextIds.size()).append(", total unique terms=").append(unique.size());
        LOG.info(sb.toString());

        Cooccurrence feature = new Cooccurrence(unique.size());
        for (String ctxId : contextIds) {
            Map<String, Integer> term2TFIC = frequencyCtxBased.getTFIC(ctxId);
            List<String> terms = new ArrayList<>(term2TFIC.keySet());
            for(int i=0; i<terms.size(); i++){
                String targetTerm = terms.get(i);
                if ((minTTF > 0 && frequencyTermBased.getTTF(targetTerm) < minTTF)
                        || (minTCF>0&& frequencyCtxBased.getContextIds(targetTerm).size() < minTCF))
                    continue;

                int targetFIC = term2TFIC.get(targetTerm); //frequency of term in this context
                int targetIdx = feature.lookupAndIndex(targetTerm);
                for(int j=i+1; j<terms.size(); j++){
                    String refTerm = terms.get(j);
                    if (refTerm.equals(targetTerm))
                        continue;
                    if ((minTTF > 0 && frequencyTermBased.getTTF(refTerm) < minTTF)
                            || (minTCF>0&& frequencyCtxBased.getContextIds(refTerm).size() < minTCF))
                        continue;

                    int refTermFIC = term2TFIC.get(refTerm);
                    int refIdx = feature.lookupAndIndex(refTerm);

                    int coocurringFreq = targetFIC < refTermFIC ? targetFIC : refTermFIC;
                    feature.increment(targetIdx, refIdx, coocurringFreq);
                }
            }
        }

        return feature;
    }
}
