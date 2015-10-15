package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.jate.SentenceContext;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxSentenceBasedFBWorker extends JATERecursiveTaskWorker<Integer, Integer> {

	private static final long serialVersionUID = -9172128488678036098L;
	private static final Logger LOG = Logger.getLogger(FrequencyCtxSentenceBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private String sentenceTargetField;
    private Set<String> allCandidates;
    private FrequencyCtxBased feature;

    public FrequencyCtxSentenceBasedFBWorker(FrequencyCtxBased feature, JATEProperties properties,
                                             List<Integer> docIds,
                                             Set<String> allCandidates,
                                             SolrIndexSearcher solrIndexSearcher,
                                             int maxTasksPerWorker,
                                             String sentenceTargetField) {
        super(docIds, maxTasksPerWorker);
        this.properties = properties;
        this.solrIndexSearcher = solrIndexSearcher;
        this.sentenceTargetField = sentenceTargetField;
        this.allCandidates=allCandidates;
        this.feature=feature;
    }

    @Override
    protected JATERecursiveTaskWorker<Integer, Integer> createInstance(List<Integer> docIdSplit) {
        return new FrequencyCtxSentenceBasedFBWorker(feature,properties, docIdSplit,
                allCandidates,
                solrIndexSearcher, maxTasksPerThread, sentenceTargetField);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<Integer, Integer>> jateRecursiveTaskWorkers) {
        Integer total=0;
        for (JATERecursiveTaskWorker<Integer, Integer> worker : jateRecursiveTaskWorkers) {
            total+= worker.join();
        }
        return total;
    }

    @Override
    protected Integer computeSingleWorker(List<Integer> docIds) {
        LOG.info("Total docs to process=" + docIds.size());
        int count = 0;
        for (int docId : docIds) {
            count++;
            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldnameJATENGramInfo(), solrIndexSearcher);
                List<MWESentenceContext> terms = collectTermOffsets(
                        lookupVector);

                for(MWESentenceContext term: terms){
                    String contextId = docId + "." + term.sentenceId;
                    feature.increment(contextId,1);
                    feature.increment(contextId, term.string, 1);
                }
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                LOG.severe(sb.toString());
            } catch (JATEException je) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(je));
                LOG.severe(sb.toString());
            }
        }
        //LOG.info("debug---finished");
        return count;
    }

    private List<int[]> collectSentenceOffsets(SolrIndexSearcher solrIndexSearcher, String fieldname, int docId) throws IOException {
        Document doc = solrIndexSearcher.doc(docId);
        String[] values = doc.getValues(fieldname);
        List<int[]> rs = new ArrayList<>();
        for (String v : values) {
            String[] offsets = v.split(",");
            rs.add(new int[]{Integer.valueOf(offsets[0]), Integer.valueOf(offsets[1])});
        }
        Collections.sort(rs, (o1, o2) -> {
            int compare = Integer.valueOf(o1[0]).compareTo(o2[0]);
            if (compare == 0) {
                return Integer.valueOf(o1[1]).compareTo(o2[1]);
            }
            return compare;
        });
        return rs;
    }

    private List<MWESentenceContext> collectTermOffsets(Terms termVectorLookup) throws IOException {
        List<MWESentenceContext> result = new ArrayList<>();

        TermsEnum tiRef= termVectorLookup.iterator();
        BytesRef luceneTerm = tiRef.next();
        while (luceneTerm != null) {
            if (luceneTerm.length == 0) {
                luceneTerm = tiRef.next();
                continue;
            }
            String tString = luceneTerm.utf8ToString();
            if(!allCandidates.contains(tString)) {
                luceneTerm=tiRef.next();
                continue;
            }


            PostingsEnum postingsEnum = tiRef.postings(null, PostingsEnum.ALL);
            //PostingsEnum postingsEnum = ti.postings(null, PostingsEnum.OFFSETS);

            int doc = postingsEnum.nextDoc(); //this should be just 1 doc, i.e., the constraint for getting this TV
            if (doc != PostingsEnum.NO_MORE_DOCS) {
                int totalOccurrence = postingsEnum.freq();
                for (int i = 0; i < totalOccurrence; i++) {
                    postingsEnum.nextPosition();
                    int start = postingsEnum.startOffset();
                    int end = postingsEnum.endOffset();
                    BytesRef payload=postingsEnum.getPayload();
                    String sentenceId="";
                    if(payload!=null){
                        sentenceId=SentenceContext.parseSentenceId(payload.utf8ToString());
                    }
                    result.add(new MWESentenceContext(tString,sentenceId, start, end));
                }
            }
            luceneTerm = tiRef.next();
        }
        Collections.sort(result);
        return result;
    }

    private class MWESentenceContext implements Comparable<MWESentenceContext> {
        public String string;
        public String sentenceId;
        public int start;
        public int end;

        public MWESentenceContext(String string, String sentenceId, int start, int end) {
            this.string=string;
            this.sentenceId = sentenceId;
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(MWESentenceContext o) {
            int compare = Integer.valueOf(start).compareTo(o.start);
            if (compare == 0) {
                return Integer.valueOf(end).compareTo(o.end);
            }
            return compare;
        }

        public String toString() {
            return sentenceId + "," + start+","+end;
        }
    }
}
