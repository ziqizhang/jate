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
public class FrequencyCtxDocBasedFBWorker extends JATERecursiveTaskWorker<BytesRef, Integer> {

	private static final long serialVersionUID = 8978235926472578074L;
	private static final Logger LOG = Logger.getLogger(FrequencyCtxDocBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private String targetField;
    private Terms ngramInfo;
    private FrequencyCtxBased feature;

    FrequencyCtxDocBasedFBWorker(FrequencyCtxBased feature,
                                 JATEProperties properties, List<BytesRef> luceneTerms, SolrIndexSearcher solrIndexSearcher,
                                 int maxTasksPerWorker,
                                 String targetField,
                                 Terms ngramInfo) {
        super(luceneTerms, maxTasksPerWorker);
        this.feature=feature;
        this.properties = properties;
        this.solrIndexSearcher = solrIndexSearcher;
        this.targetField = targetField;
        this.ngramInfo =ngramInfo;
    }

    @Override
    protected JATERecursiveTaskWorker<BytesRef, Integer> createInstance(List<BytesRef> termSplits) {
        return new FrequencyCtxDocBasedFBWorker(feature,
                properties, termSplits, solrIndexSearcher, maxTasksPerThread,
                targetField, ngramInfo);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<BytesRef, Integer>> jateRecursiveTaskWorkers) {
        Integer total=0;
        for (JATERecursiveTaskWorker<BytesRef, Integer> worker : jateRecursiveTaskWorkers) {
            total+=  worker.join();
        }
        return total;
    }

    @Override
    protected Integer computeSingleWorker(List<BytesRef> terms) {
        int total=0;
        TermsEnum ngramInfoIterator;
        try {
            ngramInfoIterator = ngramInfo.iterator();
            for (BytesRef luceneTerm : terms) {
                String termStr=luceneTerm.utf8ToString();
                try {
                    if (ngramInfoIterator.seekExact(luceneTerm)) {
                        PostingsEnum docEnum = ngramInfoIterator.postings(null);
                        int doc = 0;
                        while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                            int tfid = docEnum.freq();  //tf in document
                            String docId = String.valueOf(doc);
                            feature.increment(docId, tfid);
                            feature.increment(docId, termStr, tfid);
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
        return total;
    }
}
