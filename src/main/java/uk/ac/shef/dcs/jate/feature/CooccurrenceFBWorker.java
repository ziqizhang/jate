package uk.ac.shef.dcs.jate.feature;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.*;
import java.util.logging.Logger;

/**
 * AN IMPORTANT ASSUMPTION IS THAT context id disjoint, e.g., sentences. This class will not
 * work for window-based context. coocurrence can be double-counted in that case
 */
public class CooccurrenceFBWorker extends JATERecursiveTaskWorker<String, Integer> {
	
	private static final long serialVersionUID = 2618520228983802927L;
	private static final Logger LOG = Logger.getLogger(CooccurrenceFBWorker.class.getName());
    protected FrequencyCtxBased frequencyCtxBased;
    protected FrequencyTermBased frequencyTermBased;
    protected FrequencyCtxBased ref_frequencyCtxBased;
    protected int minTTF;
    protected int minTCF;
    protected Cooccurrence feature;

    public CooccurrenceFBWorker(Cooccurrence feature, List<String> contextIds,
                                FrequencyTermBased frequencyTermBased,
                                int minTTF,
                                FrequencyCtxBased frequencyCtxBased,
                                FrequencyCtxBased ref_frequencyCtxBased,
                                int minTCF,
                                int maxTasksPerWorker) {
        super(contextIds, maxTasksPerWorker);
        this.feature=feature;
        this.frequencyCtxBased = frequencyCtxBased;
        this.frequencyTermBased = frequencyTermBased;
        this.ref_frequencyCtxBased=ref_frequencyCtxBased;
        this.minTTF = minTTF;
        this.minTCF = minTCF;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Integer> createInstance(List<String> contextIdSplit) {
        return new CooccurrenceFBWorker(feature,contextIdSplit,frequencyTermBased,
                minTTF, frequencyCtxBased, ref_frequencyCtxBased, minTCF, maxTasksPerThread);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<String, Integer>> jateRecursiveTaskWorkers) {
        Integer total=0;
        for (JATERecursiveTaskWorker<String, Integer> worker : jateRecursiveTaskWorkers) {
            total+= worker.join();
        }

        return total;
    }

    @Override
    protected Integer computeSingleWorker(List<String> contextIds) {
        StringBuilder sb = new StringBuilder("Total ctx to process=");
        sb.append(contextIds.size())
        .append(", total ref terms=").append(ref_frequencyCtxBased.getMapTerm2Ctx().size());
        LOG.info(sb.toString());

        int total=0;
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
            total++;
        }

        return total;
    }
}
