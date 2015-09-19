package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by zqz on 17/09/2015.
 */
class FrequencyFeatureBuilderWorker extends JATERecursiveTaskWorker<BytesRef, int[]> {

    private static final Logger LOG = Logger.getLogger(FrequencyFeatureBuilderWorker.class.getName());
    private JATEProperties properties;
    private IndexReader index;
    private FrequencyFeature feature;
    private String targetField;

    FrequencyFeatureBuilderWorker(JATEProperties properties, List<BytesRef> luceneTerms, IndexReader index,
                                  FrequencyFeature feature, int maxTasksPerWorker,
                                  String targetField) {
        super(luceneTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.index = index;
        this.targetField = targetField;
    }

    @Override
    protected JATERecursiveTaskWorker<BytesRef, int[]> createInstance(List<BytesRef> termSplit) {
        return new FrequencyFeatureBuilderWorker(properties, termSplit, index, feature, maxTasksPerThread, targetField);
    }

    @Override
    protected int[] mergeResult(List<JATERecursiveTaskWorker<BytesRef, int[]>> jateRecursiveTaskWorkers) {
        int totalSuccess = 0, total = 0;
        for (JATERecursiveTaskWorker<BytesRef, int[]> worker : jateRecursiveTaskWorkers) {
            int[] rs = worker.join();
            totalSuccess += rs[0];
            total += rs[1];
        }
        return new int[]{totalSuccess, total};
    }

    @Override
    protected int[] computeSingleWorker(List<BytesRef> terms) {
        int totalSuccess = 0;
        for (BytesRef luceneTerm : terms) {
            try {
                PostingsEnum docEnum = MultiFields.getTermDocsEnum(index,
                        targetField, luceneTerm);

                int doc = 0;
                while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    int tfid = docEnum.freq();  //tf in document
                    feature.addTermFrequencyInDocument(luceneTerm.utf8ToString(), doc, tfid);
                }
                totalSuccess++;
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for candidate:");
                sb.append(luceneTerm.utf8ToString()).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                LOG.severe(sb.toString());
            }
        }
        return new int[]{totalSuccess, terms.size()};
    }
}
