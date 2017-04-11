package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.analysis.jate.MWEMetadata;
import org.apache.lucene.analysis.jate.SentenceContext;
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
import org.apache.log4j.Logger;

/**
 *
 */
public class FrequencyCtxSentenceBasedFBWorker extends JATERecursiveTaskWorker<Integer, Integer> {

	private static final long serialVersionUID = -9172128488678036098L;
	private static final Logger LOG = Logger.getLogger(FrequencyCtxSentenceBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private Set<String> allCandidates;
    private FrequencyCtxBased feature;

    public FrequencyCtxSentenceBasedFBWorker(FrequencyCtxBased feature, JATEProperties properties,
                                             List<Integer> docIds,
                                             Set<String> allCandidates,
                                             SolrIndexSearcher solrIndexSearcher,
                                             int maxTasksPerWorker) {
        super(docIds, maxTasksPerWorker);
        this.properties = properties;
        this.solrIndexSearcher = solrIndexSearcher;
        this.allCandidates=allCandidates;
        this.feature=feature;
    }

    @Override
    protected JATERecursiveTaskWorker<Integer, Integer> createInstance(List<Integer> docIdSplit) {
        return new FrequencyCtxSentenceBasedFBWorker(feature,properties, docIdSplit,
                allCandidates,
                solrIndexSearcher, maxTasksPerThread);
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
        Set<Integer> sentenceIds=new HashSet<>();
        for (int docId : docIds) {
            count++;
            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldNameJATENGramInfo(), solrIndexSearcher);
                if(lookupVector==null){
                    LOG.error("Term vector for document id="+count+" is null. The document may be empty");
                    System.err.println("Term vector for document id="+count+" is null. The document may be empty");
                    continue;
                }

                List<MWESentenceContext> terms = collectTermOffsets(
                        lookupVector);

                for(MWESentenceContext term: terms){
                    ContextWindow ctx = new ContextWindow();
                    ctx.setDocId(docId);
                    ctx.setSentenceId(term.sentenceId);

                    feature.increment(ctx,1);
                    feature.increment(ctx, term.string, 1);
                    sentenceIds.add(term.sentenceId);
                }
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                LOG.error(sb.toString());
            } catch (JATEException je) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(je));
                LOG.error(sb.toString());
            }
        }
        if(sentenceIds.size()==1)
            try {
                LOG.error("Among "+docIds.size()+" on average each document has only 1 sentence. If this is not expected, check your analyzer chain for your Solr field "
                +properties.getSolrFieldNameJATENGramInfo()+" (OpenNLPTokenizerFactory) if SentenceContext has been produced corrected.");
            } catch (JATEException e) {
            }
        return count;
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
                    int sentenceId=-1;
                    if(payload!=null){
                        sentenceId=new SentenceContext(MWEMetadata.deserialize(payload.utf8ToString())).getSentenceId();
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
        public int sentenceId;
        public int start;
        public int end;

        public MWESentenceContext(String string, int sentenceId, int start, int end) {
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
