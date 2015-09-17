package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.LeafReader;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.util.CharsRefBuilder;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Created by zqz on 17/09/2015.
 */
class ContainmentFeatureBuilderWorker extends JATERecursiveTaskWorker<String, int[]> {
    private static final Logger LOG = Logger.getLogger(FrequencyFeatureBuilderWorker.class.getName());
    private JATEProperties properties;
    private Terms termsInJATEField;
    private ContainmentFeature feature;

    ContainmentFeatureBuilderWorker(JATEProperties properties, List<String> taskTerms,
                                    Terms termsInJATEField,
                                    ContainmentFeature feature, int maxTasksPerWorker) {
        super(taskTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.termsInJATEField=termsInJATEField;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new ContainmentFeatureBuilderWorker(properties, termSplit, termsInJATEField,
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
            StringBuilder pStr = new StringBuilder("\\b(");
            pStr.append(termString).append(")\\b");
            Pattern pattern = Pattern.compile(pStr.toString());

            try {
                TermsEnum termsEnum = termsInJATEField.iterator();
                while (termsEnum.next() != null) {
                    BytesRef term = termsEnum.term();
                    String termStr = term.utf8ToString();
                    if (pattern != null) {
                        if (!pattern.matcher(termStr).find()) {
                            termsEnum.next();
                            continue;
                        }
                        //todo: good term, add
                    }
                }
                count++;

            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for candidate:");
                sb.append(termString).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                LOG.severe(sb.toString());
            }
        }
        return new int[]{count, taskTerms.size()};
    }
}
