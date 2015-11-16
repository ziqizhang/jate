package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
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
import java.util.logging.Logger;

/**
 *
 */
class FrequencyCtxWindowBasedFBWorker extends JATERecursiveTaskWorker<Integer, Integer> {
    private static final long serialVersionUID = -9172128488678036089L;
    private static final Logger LOG = Logger.getLogger(FrequencyCtxWindowBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private Set<String> allCandidates;
    private FrequencyCtxBased feature;
    private int window;
    private Map<Integer, List<ContextWindow>> contextLookup;//set of contexts in which we should count term frequencies

    /**
     * @param feature
     * @param properties
     * @param docIds
     * @param allCandidates
     * @param solrIndexSearcher
     * @param contextLookup     set of contexts in which we should count term frequencies. key:docid+","+sentenceid;
     *                          value: Context objects found in that doc and sentence pair. If the contexts
     *                          should be generated, used null or an empty map
     * @param window
     * @param maxTasksPerWorker
     */
    public FrequencyCtxWindowBasedFBWorker(FrequencyCtxBased feature, JATEProperties properties,
                                           List<Integer> docIds,
                                           Set<String> allCandidates,
                                           SolrIndexSearcher solrIndexSearcher,
                                           Map<Integer, List<ContextWindow>> contextLookup,
                                           int window,
                                           int maxTasksPerWorker) {
        super(docIds, maxTasksPerWorker);
        this.properties = properties;
        this.solrIndexSearcher = solrIndexSearcher;
        this.allCandidates = allCandidates;
        this.feature = feature;
        this.window = window;
        this.contextLookup = contextLookup;
    }

    @Override
    protected JATERecursiveTaskWorker<Integer, Integer> createInstance(List<Integer> docIdSplit) {
        return new FrequencyCtxWindowBasedFBWorker(feature, properties, docIdSplit,
                allCandidates,
                solrIndexSearcher,
                contextLookup,
                window, maxTasksPerThread);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<Integer, Integer>> jateRecursiveTaskWorkers) {
        Integer total = 0;
        for (JATERecursiveTaskWorker<Integer, Integer> worker : jateRecursiveTaskWorkers) {
            total += worker.join();
        }
        return total;
    }

    @Override
    protected Integer computeSingleWorker(List<Integer> docIds) {
        LOG.info("Total docs to process=" + docIds.size());
        if (contextLookup == null || contextLookup.size() == 0)
            return generateContext(docIds);
        else {
            return useContext(docIds);
        }
    }

    //todo: what about overlapping context?
    private int useContext(List<Integer> docIds) {
        int count = 0;
        Set<Integer> firstTokenIndexes = new HashSet<>();
        for (int docId : docIds) {
            count++;
            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldnameJATENGramInfo(), solrIndexSearcher);
                List<MWESentenceContext> terms = collectTermSentenceContext(
                        lookupVector);
                List<ContextWindow> contexts_in_doc = contextLookup.get(docId);
                if (contexts_in_doc == null || contexts_in_doc.size() == 0)
                    continue;

                Collections.sort(contexts_in_doc);
                //contexts now should be sorted by sentence id, then start tok index, then end tok index
                //mwecontext should have been sorted by sentence id too
                int cursor_for_termsList = 0;
                ContextWindow prevCtx = null;
                for (ContextWindow ctx : contexts_in_doc) {
                    ContextOverlap co = null;
                    if (prevCtx != null) {
                        //calculate context overlap
                        if(prevCtx.getTokEnd()<ctx.getTokStart()){
                            co  =new ContextOverlap(prevCtx, ctx, new ArrayList<>());
                        }
                    }
                    for (int i = cursor_for_termsList; i < terms.size(); i++) {
                        MWESentenceContext t = terms.get(i);

                        if (ctx.getSentenceId() != t.sentenceId) {
                            cursor_for_termsList = i;
                            break;//if term not in the s
                        }

                        //is t within this context?
                        if (t.start >= ctx.getTokStart() && t.end <= ctx.getTokEnd()) {
                            feature.increment(ctx, 1);
                            feature.increment(ctx, t.string, 1);
                        }

                        //is t within a context overlap?
                        if(co!=null){
                            if(co.getPrevContext().getTokEnd()>=t.end && co.getNextContext().getTokStart()<=t.start){
                                co.getTerms().add(t.string);
                            }
                        }
                    }

                    prevCtx = ctx;
                }
            } catch (IOException | JATEException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                LOG.severe(sb.toString());
            }
        }
        if (firstTokenIndexes.size() / docIds.size() <= 1)
            try {
                LOG.warning("Check your analyzer chain for your Solr field "
                        + properties.getSolrFieldnameJATENGramInfo() + " if each token's position in a sentence has been produced.");
            } catch (JATEException e) {
            }
        //LOG.info("debug---finished");
        return count;
    }

    private int generateContext(List<Integer> docIds) {
        int count = 0;
        Set<Integer> firstTokenIndexes = new HashSet<>();
        for (int docId : docIds) {
            count++;
            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldnameJATENGramInfo(), solrIndexSearcher);
                List<MWESentenceContext> terms = collectTermSentenceContext(
                        lookupVector);

                int lastToken = terms.get(terms.size()-1).lastTokenIndex;

                int currSentenceId = -1, currWindowStart = -1, currWindowEnd = -1;
                ContextWindow prevCtx = null;
                List<Integer> prevWindowRight = new ArrayList<>();

                for (int i = 0; i < terms.size(); i++) {
                    MWESentenceContext term = terms.get(i);
                    firstTokenIndexes.add(term.firstTokenIndex);

                    //init for a sentence
                    if (currSentenceId == -1 || (currSentenceId != -1 && term.sentenceId != currSentenceId)) {//if new sentence, reset window parameters
                        currSentenceId = term.sentenceId;
                        currWindowStart = -1;
                        currWindowEnd = -1;
                    }

                    if (term.firstTokenIndex >= currWindowStart && term.firstTokenIndex <= currWindowEnd)
                        continue;//the term is included in the current window, it should have been counted

                    //create window based on this term, and check its context
                    currWindowStart = term.firstTokenIndex - window;
                    if (currWindowStart < 0)
                        currWindowStart = 0;
                    currWindowEnd = term.lastTokenIndex + window;
                    if (currWindowEnd >= lastToken)
                        currWindowEnd = lastToken;

                    ContextWindow ctx = new ContextWindow();
                    ctx.setDocId(docId);
                    ctx.setSentenceId(currSentenceId);
                    ctx.setTokStart(currWindowStart);
                    ctx.setTokEnd(currWindowEnd);

                    feature.increment(ctx, 1);
                    feature.increment(ctx, term.string, 1);

                    //previous j tokens
                    List<String> termsInOverlap = new ArrayList<>();
                    for (int j = i - 1; j > -1; j--) {
                        MWESentenceContext nextTerm = terms.get(j);
                        if (nextTerm.lastTokenIndex < currWindowStart)
                            break;
                        feature.increment(ctx, 1);
                        feature.increment(ctx, nextTerm.string, 1);

                        if (prevWindowRight.contains(j)) {
                            termsInOverlap.add(nextTerm.string);
                        }
                    }
                    if (termsInOverlap.size() > 0) {
                        ContextOverlap co = new ContextOverlap(prevCtx, ctx, termsInOverlap);
                        feature.addCtxOverlapZone(co);
                    }
                    prevWindowRight.clear();

                    //following j tokens
                    for (int j = i + 1; j < terms.size(); j++) {
                        MWESentenceContext nextTerm = terms.get(j);
                        if (nextTerm.firstTokenIndex > currWindowEnd)
                            break;
                        feature.increment(ctx, 1);
                        feature.increment(ctx, nextTerm.string, 1);
                        prevWindowRight.add(j);//keep track of the list of terms in the right context of the window
                    }

                    prevCtx = ctx;
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
        if (firstTokenIndexes.size() / docIds.size() <= 1)
            try {
                LOG.warning("Check your analyzer chain for your Solr field "
                        + properties.getSolrFieldnameJATENGramInfo() + " if each token's position in a sentence has been produced.");
            } catch (JATEException e) {
            }
        //LOG.info("debug---finished");
        return count;
    }

    private List<MWESentenceContext> collectTermSentenceContext(Terms termVectorLookup) throws IOException {
        List<MWESentenceContext> result = new ArrayList<>();

        TermsEnum tiRef = termVectorLookup.iterator();
        BytesRef luceneTerm = tiRef.next();
        while (luceneTerm != null) {
            if (luceneTerm.length == 0) {
                luceneTerm = tiRef.next();
                continue;
            }
            String tString = luceneTerm.utf8ToString();
            if (!allCandidates.contains(tString)) {
                luceneTerm = tiRef.next();
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
                    BytesRef payload = postingsEnum.getPayload();
                    SentenceContext sentenceContextInfo = null;
                    if (payload != null) {
                        sentenceContextInfo = new SentenceContext(payload.utf8ToString());
                    }
                    if (sentenceContextInfo == null)
                        result.add(new MWESentenceContext(tString, start, end, 0, 0, 0));
                    else
                        result.add(new MWESentenceContext(tString, start, end,
                                sentenceContextInfo.getFirstTokenIdx(),
                                sentenceContextInfo.getLastTokenIdx(),
                                sentenceContextInfo.getSentenceId()));
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
        public int firstTokenIndex;
        public int lastTokenIndex;
        public int start;
        public int end;

        public MWESentenceContext(String string, int start, int end,
                                  int firstTokenIndex, int lastTokenIndex, int sentenceId) {
            this.string = string;
            this.sentenceId = sentenceId;
            this.start = start;
            this.end = end;
            this.firstTokenIndex = firstTokenIndex;
            this.lastTokenIndex = lastTokenIndex;
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

            return "s=" + sentenceId + ",f=" + firstTokenIndex + ",l=" + lastTokenIndex + ",so=" + start + ",se=" + end;
        }
    }
}
