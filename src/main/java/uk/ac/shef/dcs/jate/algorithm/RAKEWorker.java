package uk.ac.shef.dcs.jate.algorithm;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.feature.Cooccurrence;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by - on 25/02/2016.
 */
public class RAKEWorker extends JATERecursiveTaskWorker<String, List<JATETerm>> {

    private static final Logger LOG = Logger.getLogger(RAKEWorker.class.getName());
    private static final long serialVersionUID = 6429950650561513335L;
    protected FrequencyTermBased fFeatureWords;
    protected Cooccurrence coFeature;

    public RAKEWorker(List<String> candidates, int maxTasksPerWorker,
                      FrequencyTermBased fFeature, Cooccurrence coFeature) {
        super(candidates, maxTasksPerWorker);
        this.fFeatureWords=fFeature;
        this.coFeature=coFeature;
    }

    @Override
    protected JATERecursiveTaskWorker<String, List<JATETerm>> createInstance(List<String> candidates) {
        return new RAKEWorker(candidates, maxTasksPerThread, fFeatureWords, coFeature
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
        Set<String> cooccurrenceDictionary = coFeature.getTerms();
        int count=0;
        for (String tString : candidates) {
            String[] elements = tString.split(" ");
            double score = 0;
            for (String word : elements) {
                int freq = fFeatureWords.getTTF(word);
                if(freq==0)    //composing word can be stop words that have been filtered
                    continue;
                int degree = freq;
                if (cooccurrenceDictionary.contains(word)) {
                    Map<Integer, Integer> coocurWordIdx2Freq = coFeature.getCoocurrence(word);
                    for (int f : coocurWordIdx2Freq.values())
                        degree += f;
                }

                double wScore = (double) degree / freq;
                score += wScore;
            }

            JATETerm term = new JATETerm(tString, score);
            result.add(term);
            count++;

            if(count%1000==0) {
                LOG.info("done ="+count+"/"+candidates.size());
            }
        }
        return result;
    }
}
