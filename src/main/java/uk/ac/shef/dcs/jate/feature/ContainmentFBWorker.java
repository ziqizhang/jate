package uk.ac.shef.dcs.jate.feature;

import org.apache.log4j.Logger;
import org.apache.solr.common.util.Pair;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.util.*;
import java.util.regex.Pattern;

/**
 * Created by zqz on 17/09/2015.
 */
class ContainmentFBWorker extends JATERecursiveTaskWorker<String, int[]> {

    private static final long serialVersionUID = -1208424489000405913L;
    private static final Logger LOG = Logger.getLogger(ContainmentFBWorker.class.getName());
    private Containment feature;
    private TermComponentIndex featureTermCompIndex;

    ContainmentFBWorker(List<String> taskTerms, int maxTasksPerWorker,
                        Containment feature,
                        TermComponentIndex featureTermCompIndex) {
        super(taskTerms, maxTasksPerWorker);
        this.feature = feature;
        this.featureTermCompIndex = featureTermCompIndex;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new ContainmentFBWorker(termSplit, maxTasksPerThread,
                feature,
                featureTermCompIndex);
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
        LOG.info("Total terms to process=" + taskTerms.size());
        for (String termString : taskTerms) {
            String[] tokens = termString.split(" ");

            Set<String> compareCandidates = new HashSet<>();
            for (String tok : tokens) {
                List<Pair<String, Integer>> candidates = featureTermCompIndex.getSorted(tok);
                Iterator<Pair<String, Integer>> it = candidates.iterator();
                while (it.hasNext()) {
                    Pair<String, Integer> c = it.next();
                    if (c.getValue() <= tokens.length)
                        break;
                    compareCandidates.add(c.getKey());
                }

            }

            StringBuilder pStr = new StringBuilder("(?<!\\w)");
            pStr.append(Pattern.quote(termString)).append("(?!\\w)");
            Pattern pattern = Pattern.compile(pStr.toString());

            for (String pterm : compareCandidates) {
                if (pattern.matcher(pterm).find()) {  //ref term contains term
                    feature.add(termString, pterm);
                }
            }

            count++;
            if (count % 2000 == 0)
                LOG.debug(count + "/" + taskTerms.size());
        }
        return new int[]{count, taskTerms.size()};
    }
}
