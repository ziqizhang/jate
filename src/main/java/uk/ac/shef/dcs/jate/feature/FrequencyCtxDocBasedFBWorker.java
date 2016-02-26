package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import org.apache.log4j.Logger;

/**
 *
 */
public class FrequencyCtxDocBasedFBWorker extends JATERecursiveTaskWorker<String, Integer> {

	private static final long serialVersionUID = 8978235926472578074L;
	private static final Logger LOG = Logger.getLogger(FrequencyCtxDocBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private Terms ngramInfo;
    private FrequencyCtxBased feature;

    FrequencyCtxDocBasedFBWorker(FrequencyCtxBased feature,
                                 JATEProperties properties, List<String> luceneTerms, SolrIndexSearcher solrIndexSearcher,
                                 int maxTasksPerWorker,
                                 Terms ngramInfo) {
        super(luceneTerms, maxTasksPerWorker);
        this.feature=feature;
        this.properties = properties;
        this.solrIndexSearcher = solrIndexSearcher;
        this.ngramInfo =ngramInfo;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Integer> createInstance(List<String> termSplits) {
        return new FrequencyCtxDocBasedFBWorker(feature,
                properties, termSplits, solrIndexSearcher, maxTasksPerThread,
                ngramInfo);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<String, Integer>> jateRecursiveTaskWorkers) {
        Integer total=0;
        for (JATERecursiveTaskWorker<String, Integer> worker : jateRecursiveTaskWorkers) {
            total+=  worker.join();
        }
        return total;
    }

    @Override
    protected Integer computeSingleWorker(List<String> terms) {
        int total=0;
        TermsEnum ngramInfoIterator;
        try {
            ngramInfoIterator = ngramInfo.iterator();
            for (String termStr : terms) {
                try {
                    if (ngramInfoIterator.seekExact(new BytesRef(termStr.getBytes("UTF-8")))) {
                        PostingsEnum docEnum = ngramInfoIterator.postings(null);
                        int doc = 0;
                        while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                            int tfid = docEnum.freq();  //tf in document
                            ContextWindow ctx = new ContextWindow();
                            ctx.setDocId(doc);
                            feature.increment(ctx, tfid);
                            feature.increment(ctx, termStr, tfid);
                        }
                        total++;
                    }else {
                        StringBuilder msg = new StringBuilder(termStr);
                        msg.append(" is a candidate term, but not indexed in the n-gram information field. It's score may be mis-computed.");
                        msg.append(" Reasons can be: different analysis chains for the two fields; cross-sentence-boundary MWEs");
                        LOG.warn(msg.toString());
                    }
                } catch (IOException ioe) {
                    StringBuilder sb = new StringBuilder("Unable to build feature for candidate:");
                    sb.append(termStr).append("\n");
                    sb.append(ExceptionUtils.getFullStackTrace(ioe));
                    LOG.error(sb.toString());
                }
            }
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder("Unable to read ngram information field:");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            LOG.error(sb.toString());
        }
        return total;
    }
}
