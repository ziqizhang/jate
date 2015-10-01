package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by zqz on 17/09/2015.
 */
class FrequencyTermBasedFBWorker extends JATERecursiveTaskWorker<BytesRef, int[]> {

    private static final Logger LOG = Logger.getLogger(FrequencyTermBasedFBWorker.class.getName());
    private JATEProperties properties;
    private IndexReader index;
    private FrequencyTermBased feature;
    private String candidateField;
    private Terms ngramInfo;

    FrequencyTermBasedFBWorker(JATEProperties properties, List<BytesRef> luceneTerms, IndexReader index,
                               FrequencyTermBased feature, int maxTasksPerWorker,
                               String candidateField,
                               Terms ngramInfo) {
        super(luceneTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.index = index;
        this.candidateField = candidateField;
        this.ngramInfo = ngramInfo;
    }

    @Override
    protected JATERecursiveTaskWorker<BytesRef, int[]> createInstance(List<BytesRef> termSplit) {
        return new FrequencyTermBasedFBWorker(properties, termSplit, index, feature, maxTasksPerThread, candidateField,
                ngramInfo);
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
        TermsEnum ngramInfoIterator;
        try {
            ngramInfoIterator = ngramInfo.iterator();

            for (BytesRef luceneTerm : terms) {
                String term = luceneTerm.utf8ToString();
                try {
                    if (ngramInfoIterator.seekExact(luceneTerm)) {
                        PostingsEnum docEnum = ngramInfoIterator.postings(null);
                        int doc = 0;
                        while ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                            int tfid = docEnum.freq();  //tf in document
                            feature.increment(term, tfid);
                            feature.incrementTermFrequencyInDocument(term, doc, tfid);
                        }
                        totalSuccess++;
                    } else {
                        StringBuilder msg = new StringBuilder(term);
                        msg.append(" is a candidate term, but not indexed in the n-gram information field. It's score may be mis-computed.");
                        msg.append(" (You may have used different text analysis process (e.g., different tokenizers) for the two fields.) ");
                        LOG.warning(msg.toString());
                    }
                /*if(totalSuccess%2000==0)
                    LOG.info(totalSuccess+"/"+terms.size());*/
                } catch (IOException ioe) {
                    StringBuilder sb = new StringBuilder("Unable to build feature for candidate:");
                    sb.append(luceneTerm.utf8ToString()).append("\n");
                    sb.append(ExceptionUtils.getFullStackTrace(ioe));
                    LOG.severe(sb.toString());
                } catch (ArrayIndexOutOfBoundsException i) {
                    System.out.println();
                }
            }
        } catch (IOException e) {
            StringBuilder sb = new StringBuilder("Unable to read ngram information field:");
            sb.append(ExceptionUtils.getFullStackTrace(e));
            LOG.severe(sb.toString());
        }
        LOG.info(totalSuccess + "/" + terms.size());
        return new int[]{totalSuccess, terms.size()};
    }
}
