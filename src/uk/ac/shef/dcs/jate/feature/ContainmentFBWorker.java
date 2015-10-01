package uk.ac.shef.dcs.jate.feature;

import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by zqz on 17/09/2015.
 */
class ContainmentFBWorker extends JATERecursiveTaskWorker<String, int[]> {
    private static final Logger LOG = Logger.getLogger(FrequencyTermBasedFBWorker.class.getName());
    private JATEProperties properties;
    private Map<Integer, Set<String>> numTokens2Terms;
    private Containment feature;

    ContainmentFBWorker(JATEProperties properties, List<String> taskTerms,
                        Map<Integer, Set<String>> numTokens2Terms,
                        Containment feature, int maxTasksPerWorker) {
        super(taskTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.numTokens2Terms = numTokens2Terms;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new ContainmentFBWorker(properties, termSplit, numTokens2Terms,
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
        LOG.info("Total terms to process="+taskTerms.size());
        for (String termString : taskTerms) {
            int tokens = termString.split(" ").length;

            StringBuilder pStr = new StringBuilder("(?<!\\w)");
            pStr.append(Pattern.quote(termString)).append("(?!\\w)");
            Pattern pattern = Pattern.compile(pStr.toString());

            for (Map.Entry<Integer, Set<String>> entry : numTokens2Terms.entrySet()){
                if(entry.getKey()<=tokens)
                    continue;
                for(String pterm: entry.getValue()) {
                    if (pattern.matcher(pterm).find()) {  //ref term contains term
                        feature.add(termString, pterm);
                    }
                }
            }
            count++;
            if(count%1000==0)
                LOG.info(count+"/"+taskTerms.size());
        }
        return new int[]{count, taskTerms.size()};
    }
}
