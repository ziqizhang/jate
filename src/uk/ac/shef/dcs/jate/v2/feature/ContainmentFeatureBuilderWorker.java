package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by zqz on 17/09/2015.
 */
class ContainmentFeatureBuilderWorker extends JATERecursiveTaskWorker<String, int[]> {
    private static final Logger LOG = Logger.getLogger(FrequencyFeatureBuilderWorker.class.getName());
    private JATEProperties properties;
    private Map<String, Integer> term2NumTokens;
    private ContainmentFeature feature;

    ContainmentFeatureBuilderWorker(JATEProperties properties, List<String> taskTerms,
                                    Map<String, Integer> term2NumTokens,
                                    ContainmentFeature feature, int maxTasksPerWorker) {
        super(taskTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.term2NumTokens = term2NumTokens;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new ContainmentFeatureBuilderWorker(properties, termSplit, term2NumTokens,
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
