package uk.ac.shef.dcs.jate.feature;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.*;
import java.util.logging.Logger;

/**
 *
 */
public class CooccurrenceFBWorker extends JATERecursiveTaskWorker<String, Cooccurrence> {
	
	private static final long serialVersionUID = 2618520228983802927L;
	private static final Logger LOG = Logger.getLogger(CooccurrenceFBWorker.class.getName());
    protected FrequencyCtxBased frequencyCtxBased;
    protected FrequencyTermBased frequencyTermBased;
    protected FrequencyCtxBased ref_frequencyCtxBased;
    protected int minTTF;
    protected int minTCF;

    public CooccurrenceFBWorker(List<String> contextIds,
                                FrequencyTermBased frequencyTermBased,
                                int minTTF,
                                FrequencyCtxBased frequencyCtxBased,
                                FrequencyCtxBased ref_frequencyCtxBased,
                                int minTCF,
                                int maxTasksPerWorker) {
        super(contextIds, maxTasksPerWorker);
        this.frequencyCtxBased = frequencyCtxBased;
        this.frequencyTermBased = frequencyTermBased;
        this.ref_frequencyCtxBased=ref_frequencyCtxBased;
        this.minTTF = minTTF;
        this.minTCF = minTCF;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Cooccurrence> createInstance(List<String> contextIdSplit) {
        return new CooccurrenceFBWorker(contextIdSplit,frequencyTermBased,
                minTTF, frequencyCtxBased, ref_frequencyCtxBased, minTCF, maxTasksPerThread);
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

        LOG.info("Joining output from multiple workers, #="+jateRecursiveTaskWorkers.size()+", combined total terms="+allTerms.size());
        Cooccurrence joined = new Cooccurrence(allTerms.size(), ref_frequencyCtxBased.getMapTerm2Ctx().size());
        for (Cooccurrence output : workerOutput) {
            for (int term1Id = 0; term1Id < output.getNumTerms(); term1Id++) {
                //LOG.info("worker has terms="+output.getNumTerms());
                Map<Integer, Integer> cooccurrence = output.getCooccurrence(term1Id);
                String term1 = output.lookupTerm(term1Id);
                int newTerm1Id = joined.lookupAndIndexTerm(term1);
                for (Map.Entry<Integer, Integer> ent : cooccurrence.entrySet()) {
                    int term2Id = ent.getKey();
                    String term2 = output.lookupTerm(term2Id);
                    int freq = ent.getValue();

                    int newTerm2Id = joined.lookupAndIndexTerm(term2);
                    joined.increment(newTerm1Id, newTerm2Id, freq);
                }
            }
        }
        //System.out.println("complete joining, "+joined.getNumTerms()+" terms.");
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
        sb.append(contextIds.size()).append(", total unique terms=").append(unique.size())
        .append(", total ref terms=").append(ref_frequencyCtxBased.getMapTerm2Ctx().size());
        LOG.info(sb.toString());

        Cooccurrence feature = new Cooccurrence(unique.size(), ref_frequencyCtxBased.getMapTerm2Ctx().size());
        for (String ctxId : contextIds) {
            //map containing ref term as key, its frequency in this context (ctxid) as value
            Map<String, Integer> refTerm2TFIC=ref_frequencyCtxBased.getTFIC(ctxId);
            if(refTerm2TFIC.size()==0)
                continue;
            //map containing term as key, its frequency in this context (ctxid) as value
            Map<String, Integer> term2TFIC = frequencyCtxBased.getTFIC(ctxId);
            //all terms in this ctxid
            List<String> terms = new ArrayList<>(term2TFIC.keySet());

            for(int i=0; i<terms.size(); i++){
                String targetTerm = terms.get(i);
                if ((minTTF > 0 && frequencyTermBased.getTTF(targetTerm) < minTTF)
                        || (minTCF>0&& frequencyCtxBased.getContextIds(targetTerm).size() < minTCF))
                    continue;

                int targetFIC = term2TFIC.get(targetTerm); //frequency of term in this context
                int targetIdx = feature.lookupAndIndexTerm(targetTerm);
                //now go through each contextual term to be considered and check cooccurrence:

                for(Map.Entry<String, Integer> en : refTerm2TFIC.entrySet()){
                    String refTerm=en.getKey();
                    if (refTerm.equals(targetTerm))
                        continue;

                    int refTermFIC = en.getValue();
                    int refIdx = feature.lookupAndIndexRefTerm(refTerm);

                    int coocurringFreq = targetFIC < refTermFIC ? targetFIC : refTermFIC;
                    feature.increment(targetIdx, refIdx, coocurringFreq);
                }
            }
        }

        return feature;
    }
}
