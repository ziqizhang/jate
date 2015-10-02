package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class FrequencyCtxDocBasedFBWorker extends JATERecursiveTaskWorker<BytesRef, FrequencyCtxBased> {

    private static final Logger LOG = Logger.getLogger(FrequencyCtxDocBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private String targetField;
    private Terms ngramInfo;

    FrequencyCtxDocBasedFBWorker(JATEProperties properties, List<BytesRef> luceneTerms, SolrIndexSearcher solrIndexSearcher,
                                 int maxTasksPerWorker,
                                 String targetField,
                                 Terms ngramInfo) {
        super(luceneTerms, maxTasksPerWorker);
        this.properties = properties;
        this.solrIndexSearcher = solrIndexSearcher;
        this.targetField = targetField;
        this.ngramInfo =ngramInfo;
    }

    @Override
    protected JATERecursiveTaskWorker<BytesRef, FrequencyCtxBased> createInstance(List<BytesRef> termSplits) {
        return new FrequencyCtxDocBasedFBWorker(properties, termSplits, solrIndexSearcher, maxTasksPerThread,
                targetField, ngramInfo);
    }

    @Override
    protected FrequencyCtxBased mergeResult(List<JATERecursiveTaskWorker<BytesRef, FrequencyCtxBased>> jateRecursiveTaskWorkers) {
        FrequencyCtxBased joined = new FrequencyCtxBased();
        for (JATERecursiveTaskWorker<BytesRef, FrequencyCtxBased> worker : jateRecursiveTaskWorkers) {
            FrequencyCtxBased feature = worker.join();
            for(Map.Entry<String, Integer> mapDoc2TTF: feature.getMapCtx2TTF().entrySet()){
                String docId = mapDoc2TTF.getKey();
                int ttf = mapDoc2TTF.getValue();
                joined.increment(docId, ttf);
            }

            for(Map.Entry<String, Map<String, Integer>> mapDoc2TFID: feature.getMapCtx2TFIC().entrySet()){
                String docId = mapDoc2TFID.getKey();
                Map<String, Integer> mapT2FID=mapDoc2TFID.getValue();
                for(Map.Entry<String, Integer> e: mapT2FID.entrySet()){
                    joined.increment(docId, e.getKey(), e.getValue());
                }
            }

        }
        return joined;
    }

    @Override
    protected FrequencyCtxBased computeSingleWorker(List<BytesRef> terms) {
        FrequencyCtxBased feature = new FrequencyCtxBased();
        TermsEnum ngramInfoIterator;
        try {
            ngramInfoIterator = ngramInfo.iterator();
            for (BytesRef luceneTerm : terms) {
                try {
                    if (ngramInfoIterator.seekExact(luceneTerm)) {
                        PostingsEnum docEnum = ngramInfoIterator.postings(null);
                        int doc = 0;
                        while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                            int tfid = docEnum.freq();  //tf in document
                            String docId = String.valueOf(doc);
                            feature.increment(docId, tfid);
                            feature.increment(docId, luceneTerm.utf8ToString(), tfid);
                        }
                    }else {
                        StringBuilder msg = new StringBuilder(luceneTerm.utf8ToString());
                        msg.append(" is a candidate term, but not indexed in the n-gram information field. It's score may be mis-computed.");
                        msg.append(" (You may have used different text analysis process (e.g., different tokenizers) for the two fields.) ");
                        LOG.warning(msg.toString());
                    }
                } catch (IOException ioe) {
                    StringBuilder sb = new StringBuilder("Unable to build feature for candidate:");
                    sb.append(luceneTerm.utf8ToString()).append("\n");
                    sb.append(ExceptionUtils.getFullStackTrace(ioe));
                    LOG.severe(sb.toString());
                }
            }
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder("Unable to read ngram information field:");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            LOG.severe(sb.toString());
        }
        return feature;
    }
}
