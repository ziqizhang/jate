package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
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
public class FrequencyCtxSentenceBasedFBWorker extends JATERecursiveTaskWorker<Integer, FrequencyCtxBased> {

	private static final long serialVersionUID = -9172128488678036098L;
	private static final Logger LOG = Logger.getLogger(FrequencyCtxSentenceBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private String sentenceTargetField;
    private Set<String> allCandidates;

    public FrequencyCtxSentenceBasedFBWorker(JATEProperties properties,
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
    }

    @Override
    protected JATERecursiveTaskWorker<Integer, FrequencyCtxBased> createInstance(List<Integer> docIdSplit) {
        return new FrequencyCtxSentenceBasedFBWorker(properties, docIdSplit,
                allCandidates,
                solrIndexSearcher, maxTasksPerThread, sentenceTargetField);
    }

    @Override
    protected FrequencyCtxBased mergeResult(List<JATERecursiveTaskWorker<Integer, FrequencyCtxBased>> jateRecursiveTaskWorkers) {
        FrequencyCtxBased joined = new FrequencyCtxBased();
        for (JATERecursiveTaskWorker<Integer, FrequencyCtxBased> worker : jateRecursiveTaskWorkers) {
            FrequencyCtxBased feature = worker.join();
            for (Map.Entry<String, Integer> mapCtx2TTF : feature.getMapCtx2TTF().entrySet()) {
                String ctxId = mapCtx2TTF.getKey();
                int ttf = mapCtx2TTF.getValue();
                joined.increment(ctxId, ttf);
            }

            for (Map.Entry<String, Map<String, Integer>> mapCtx2TFIC : feature.getMapCtx2TFIC().entrySet()) {
                String ctxId = mapCtx2TFIC.getKey();
                Map<String, Integer> mapT2FIC = mapCtx2TFIC.getValue();
                for (Map.Entry<String, Integer> e : mapT2FIC.entrySet()) {
                    joined.increment(ctxId, e.getKey(), e.getValue());
                }
            }

            for (Map.Entry<String, Set<String>> entry : feature.getMapTerm2Ctx().entrySet()) {
                String term = entry.getKey();
                Set<String> addContexts = entry.getValue();
                Set<String> contexts = joined.getMapTerm2Ctx().get(term);
                if (contexts == null)
                    contexts = new HashSet<>();
                contexts.addAll(addContexts);
                joined.getMapTerm2Ctx().put(term, contexts);
            }
        }
        return joined;
    }

    @Override
    protected FrequencyCtxBased computeSingleWorker(List<Integer> docIds) {
        LOG.info("Total docs to process=" + docIds.size());
        FrequencyCtxBased feature = new FrequencyCtxBased();
        int count = 0;
        for (int docId : docIds) {
            count++;
            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldnameJATENGramInfo(), solrIndexSearcher);
                List<TextUnitOffsets> terms = collectTermOffsets(
                        lookupVector);
                List<int[]> sentences = collectSentenceOffsets(solrIndexSearcher, sentenceTargetField, docId);
                /*StringBuilder sb = new StringBuilder("#");
                sb.append(count).append(", docId=").append(docId).append(", total terms=").append(terms.size())
                        .append(", total sentences=").append(sentences);*/
                //LOG.info(sb.toString());
                int termCursor = 0;
                for (int[] sent : sentences) {
                    String contextId = docId + "." + feature.nextCtxId();
                    for (int t = termCursor; t < terms.size(); t++) {
                        TextUnitOffsets term = terms.get(t);
                        if (term.end > sent[1]) {
                            if (term.start > sent[1]) {
                                termCursor = t;
                                break;
                            } else
                                continue;
                        }

                        if (term.start >= sent[0]) { //term within sentence boundary
                            feature.increment(contextId, 1);
                            feature.increment(contextId, term.string, 1);
                        } else {//a term is not within sentence boundary. this is likely for n-gram which are created
                            //across sentence boundaries, or phrases extracted based on pos patterns due to incorrect pos
                            //tagging
                        }
                    }
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
        return feature;
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

    private List<TextUnitOffsets> collectTermOffsets(Terms termVectorLookup) throws IOException {
        List<TextUnitOffsets> result = new ArrayList<>();

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


            PostingsEnum postingsEnum = tiRef.postings(null, PostingsEnum.OFFSETS);
            //PostingsEnum postingsEnum = ti.postings(null, PostingsEnum.OFFSETS);

            int doc = postingsEnum.nextDoc(); //this should be just 1 doc, i.e., the constraint for getting this TV
            if (doc != PostingsEnum.NO_MORE_DOCS) {
                int totalOccurrence = postingsEnum.freq();
                for (int i = 0; i < totalOccurrence; i++) {
                    postingsEnum.nextPosition();
                    int start = postingsEnum.startOffset();
                    int end = postingsEnum.endOffset();
                    result.add(new TextUnitOffsets(tString, start, end));
                }
            }
            luceneTerm = tiRef.next();
        }
        Collections.sort(result);
        return result;
    }

    private class TextUnitOffsets implements Comparable<TextUnitOffsets> {
        public String string;
        public int start;
        public int end;

        public TextUnitOffsets(String string, int start, int end) {
            this.string = string;
            this.start = start;
            this.end = end;
        }

        @Override
        public int compareTo(TextUnitOffsets o) {
            int compare = Integer.valueOf(start).compareTo(o.start);
            if (compare == 0) {
                return Integer.valueOf(end).compareTo(o.end);
            }
            return compare;
        }

        public String toString() {
            return string + "," + start;
        }
    }
}
