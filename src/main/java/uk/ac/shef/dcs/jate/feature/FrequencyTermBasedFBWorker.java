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

class FrequencyTermBasedFBWorker extends JATERecursiveTaskWorker<String, int[]> {

	private static final long serialVersionUID = -5304721004951728503L;
	private static final Logger LOG = Logger.getLogger(FrequencyTermBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private FrequencyTermBased feature;
    private Terms ngramInfo;

    FrequencyTermBasedFBWorker(JATEProperties properties, List<String> luceneTerms, SolrIndexSearcher solrIndexSearcher,
                               FrequencyTermBased feature, int maxTasksPerWorker,
                               Terms ngramInfo) {
        super(luceneTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.solrIndexSearcher = solrIndexSearcher;
        this.ngramInfo = ngramInfo;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new FrequencyTermBasedFBWorker(properties, termSplit, solrIndexSearcher, feature, maxTasksPerThread,
                ngramInfo);
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
    protected int[] computeSingleWorker(List<String> terms) {
        int totalSuccess = 0;
        TermsEnum ngramInfoIterator;
        try {
            ngramInfoIterator = ngramInfo.iterator();

            for (String term : terms) {
                try {
                    if (ngramInfoIterator.seekExact(new BytesRef(term.getBytes("UTF-8")))) {
                        PostingsEnum docEnum = ngramInfoIterator.postings(null);
                        int doc = 0;
                        while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                            //tf in document
                            int tfid = docEnum.freq();
                            feature.increment(term, tfid);
                            feature.incrementTermFrequencyInDocument(term, doc, tfid);
                        }
                        totalSuccess++;
                    } else {
                        String warning = String.format("'%s'  is a candidate term, but not indexed in the n-gram " +
                                "information field. It's score may be mis-computed. You may have used different text " +
                                "analysis process (e.g., different tokenizers, different analysis order, limited " +
                                "n-gram range) for the text-2-candidate-term and text-2-ngram fields.) ", term);
                        LOG.warn(warning);
                    }

                } catch (IOException ioe) {
                    String error = String.format("Unable to build feature for candidate: '%s'. \\n Exception: %s",
                            term, ExceptionUtils.getFullStackTrace(ioe));
                    LOG.error(error.toString());
                }
            }
        } catch (IOException ioe) {
            String error = String.format("Unable to read ngram information field:. \\n Exception: %s",
                    ExceptionUtils.getFullStackTrace(ioe));
            LOG.error(error);
        }
        LOG.debug("progress : " + totalSuccess + "/" + terms.size());
        return new int[]{totalSuccess, terms.size()};
    }
}
