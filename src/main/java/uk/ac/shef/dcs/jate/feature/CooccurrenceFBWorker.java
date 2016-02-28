package uk.ac.shef.dcs.jate.feature;

import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.*;
import org.apache.log4j.Logger;

/**
 * AN IMPORTANT ASSUMPTION IS THAT context id disjoint, e.g., sentences. In window-based context, coocurrence can be
 * double-counted. This is corrected by the CooccurrenceFBMaster class as a post-process.
 *
 * @see CooccurrenceFBMaster
 */
public class CooccurrenceFBWorker extends JATERecursiveTaskWorker<ContextWindow, Integer> {
	
	private static final long serialVersionUID = 2618520228983802927L;
	private static final Logger LOG = Logger.getLogger(CooccurrenceFBWorker.class.getName());
    protected FrequencyCtxBased frequencyCtxBased;
    protected FrequencyTermBased frequencyTermBased;
    protected FrequencyCtxBased ref_frequencyCtxBased;
    protected int minTTF;
    protected int minTCF;
    protected Cooccurrence feature;

    public CooccurrenceFBWorker(Cooccurrence feature, List<ContextWindow> contextWindowIds,
                                FrequencyTermBased frequencyTermBased,
                                int minTTF,
                                FrequencyCtxBased frequencyCtxBased,
                                FrequencyCtxBased ref_frequencyCtxBased,
                                int minTCF,
                                int maxTasksPerWorker) {
        super(contextWindowIds, maxTasksPerWorker);
        this.feature=feature;
        this.frequencyCtxBased = frequencyCtxBased;
        this.frequencyTermBased = frequencyTermBased;
        this.ref_frequencyCtxBased=ref_frequencyCtxBased;
        this.minTTF = minTTF;
        this.minTCF = minTCF;
    }

    @Override
    protected JATERecursiveTaskWorker<ContextWindow, Integer> createInstance(List<ContextWindow> contextWindowIdSplit) {
        return new CooccurrenceFBWorker(feature, contextWindowIdSplit,frequencyTermBased,
                minTTF, frequencyCtxBased, ref_frequencyCtxBased, minTCF, maxTasksPerThread);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<ContextWindow, Integer>> jateRecursiveTaskWorkers) {
        Integer total=0;
        for (JATERecursiveTaskWorker<ContextWindow, Integer> worker : jateRecursiveTaskWorkers) {
            total+= worker.join();
        }

        return total;
    }

    @Override
    protected Integer computeSingleWorker(List<ContextWindow> contextWindows) {
        StringBuilder sb = new StringBuilder("Total ctx to process=");
        sb.append(contextWindows.size())
        .append(", total ref terms=").append(ref_frequencyCtxBased.getMapTerm2Ctx().size());
        LOG.info(sb.toString());

        int total=0;
        for (ContextWindow ctx : contextWindows) {
            //get the reference terms appearing in this ctx object and their frequency
            Map<String, Integer> refTerm2TFIC=ref_frequencyCtxBased.getTFIC(ctx);
            /*if(refTerm2TFIC.size()==0) //it is possible because ref-term may not appear in this context
                continue;*/
            //get the target terms appearing in this ctx object and their frequency
            Map<String, Integer> term2TFIC = frequencyCtxBased.getTFIC(ctx);
            //all terms in this ctxid
            //NOTE!!!: it is possible that there are no target terms in this context, due to target term filtering
            //         As a result, the actual indexed reference terms in this co-occurrence feature may not be identical
            //         to ref_frequencyCtxBased.getMapTerm2CtxId().
            List<String> terms = new ArrayList<>(term2TFIC.keySet());
            for(int i=0; i<terms.size(); i++){
                String targetTerm = terms.get(i);
                if ((minTTF > 0 && frequencyTermBased.getTTF(targetTerm) < minTTF)
                        || (minTCF>0&& frequencyCtxBased.getContexts(targetTerm).size() < minTCF))
                    continue;

                int targetFIC = term2TFIC.get(targetTerm); //frequency of term in this context
                int targetIdx = feature.lookupAndIndexTerm(targetTerm);

                //now go through each reference term to be considered and check cooccurrence:
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
