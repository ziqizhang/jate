package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class FrequencyDocBasedFBWorker extends JATERecursiveTaskWorker<BytesRef, FrequencyDocBased> {

    private static final Logger LOG = Logger.getLogger(FrequencyDocBasedFBWorker.class.getName());
    private JATEProperties properties;
    private IndexReader index;
    private String targetField;

    FrequencyDocBasedFBWorker(JATEProperties properties, List<BytesRef> luceneTerms, IndexReader index,
                              int maxTasksPerWorker,
                               String targetField) {
        super(luceneTerms, maxTasksPerWorker);
        this.properties = properties;
        this.index = index;
        this.targetField = targetField;
    }

    @Override
    protected JATERecursiveTaskWorker<BytesRef, FrequencyDocBased> createInstance(List<BytesRef> termSplits) {
        return new FrequencyDocBasedFBWorker(properties, termSplits, index, maxTasksPerThread, targetField);
    }

    @Override
    protected FrequencyDocBased mergeResult(List<JATERecursiveTaskWorker<BytesRef, FrequencyDocBased>> jateRecursiveTaskWorkers) {
        FrequencyDocBased joined = new FrequencyDocBased();
        for (JATERecursiveTaskWorker<BytesRef, FrequencyDocBased> worker : jateRecursiveTaskWorkers) {
            FrequencyDocBased feature = worker.join();
            for(Map.Entry<Integer, Integer> mapDoc2TTF: feature.getMapDoc2TTF().entrySet()){
                int docId = mapDoc2TTF.getKey();
                int ttf = mapDoc2TTF.getValue();
                joined.increment(docId, ttf);
            }

            for(Map.Entry<Integer, Map<String, Integer>> mapDoc2TFID: feature.getMapDoc2TFID().entrySet()){
                int docId = mapDoc2TFID.getKey();
                Map<String, Integer> mapT2FID=mapDoc2TFID.getValue();
                for(Map.Entry<String, Integer> e: mapT2FID.entrySet()){
                    joined.increment(docId, e.getKey(), e.getValue());
                }
            }

        }
        return joined;
    }

    @Override
    protected FrequencyDocBased computeSingleWorker(List<BytesRef> terms) {
        FrequencyDocBased feature = new FrequencyDocBased();
        for (BytesRef luceneTerm : terms) {
            try {
                PostingsEnum docEnum = MultiFields.getTermDocsEnum(index,
                        targetField, luceneTerm);

                int doc = 0;
                while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                    int tfid = docEnum.freq();  //tf in document
                    feature.increment(doc, tfid);
                    feature.increment(doc, luceneTerm.utf8ToString(), tfid);
                }
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for candidate:");
                sb.append(luceneTerm.utf8ToString()).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                LOG.severe(sb.toString());
            }
        }
        return feature;
    }
}
