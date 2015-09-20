package uk.ac.shef.dcs.jate.v2.feature;

import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by zqz on 17/09/2015.
 */
class ContainmentFBWorker extends JATERecursiveTaskWorker<String, int[]> {
    private static final Logger LOG = Logger.getLogger(FrequencyTermBasedFBWorker.class.getName());
    private JATEProperties properties;
    private Map<String, Integer> term2NumTokens;
    private Containment feature;

    ContainmentFBWorker(JATEProperties properties, List<String> taskTerms,
                        Map<String, Integer> term2NumTokens,
                        Containment feature, int maxTasksPerWorker) {
        super(taskTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.term2NumTokens = term2NumTokens;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new ContainmentFBWorker(properties, termSplit, term2NumTokens,
                feature, maxTasksPerThread);
    }

    @Override
    protected int[] mergeResult(List<JATERecursiveTaskWorker<String, int[]>> jateRecursiveTaskWorkers) {
        int totalSuccess = 0, total = 0;
        for (JATERecursiveTaskWorker<String, int[]> worker : jateRecursiveTaskWorkers) {
            int[] rs = worker.join();
            totalSuccess += rs[0];
            total += rs[1];
        }
        return new int[]{totalSuccess, total};
    }

    @Override
    protected int[] computeSingleWorker(List<String> taskTerms) {
        int count = 0;

        for (String termString : taskTerms) {
            int tokens = term2NumTokens.get(termString);
            StringBuilder pStr = new StringBuilder("\\b(");
            pStr.append(termString).append(")\\b");
            Pattern pattern = Pattern.compile(pStr.toString());

            for (Map.Entry<String, Integer> entry : term2NumTokens.entrySet()){
                if(tokens>=entry.getValue())
                    continue;
                if (pattern.matcher(entry.getKey()).find()) {  //ref term contains term
                    count++;
                    feature.add(termString, entry.getKey());
                }
            }
        }
        return new int[]{count, taskTerms.size()};
    }
}
