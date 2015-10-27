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
 * Created by - on 18/10/2015.
 */
public class FrequencyCtxWindowBasedFBWorker extends JATERecursiveTaskWorker<Integer, Integer> {
    private static final long serialVersionUID = -9172128488678036089L;
    private static final Logger LOG = Logger.getLogger(FrequencyCtxWindowBasedFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private Set<String> allCandidates;
    private FrequencyCtxBased feature;
    private int window;

    public FrequencyCtxWindowBasedFBWorker(FrequencyCtxBased feature, JATEProperties properties,
                                             List<Integer> docIds,
                                             Set<String> allCandidates,
                                             SolrIndexSearcher solrIndexSearcher,
                                           int window,
                                             int maxTasksPerWorker) {
        super(docIds, maxTasksPerWorker);
        this.properties = properties;
        this.solrIndexSearcher = solrIndexSearcher;
        this.allCandidates=allCandidates;
        this.feature=feature;
        this.window=window;
    }

    @Override
    protected JATERecursiveTaskWorker<Integer, Integer> createInstance(List<Integer> docIdSplit) {
        return new FrequencyCtxWindowBasedFBWorker(feature,properties, docIdSplit,
                allCandidates,
                solrIndexSearcher, window,maxTasksPerThread);
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
        Set<Integer> firstTokenIndexes=new HashSet<>();
        for (int docId : docIds) {
            count++;
            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldnameJATENGramInfo(), solrIndexSearcher);
                List<MWESentenceContext> terms = collectTermSentenceContext(
                        lookupVector);

                int currSentenceId=-1, currWindowStart=-1, currWindowEnd=-1;

                for(int i=0; i< terms.size(); i++){
                    MWESentenceContext term = terms.get(i);
                    firstTokenIndexes.add(term.firstTokenIndex);

                    //init for a sentence
                    if(currSentenceId==-1||(currSentenceId!=-1 && term.sentenceId!=currSentenceId)){//if new sentence, reset window parameters
                        currSentenceId=term.sentenceId;
                        currWindowStart=-1;
                        currWindowEnd=-1;
                    }


                    if(term.firstTokenIndex>=currWindowStart && term.firstTokenIndex<=currWindowEnd)
                        continue;//the term is included in the current window, it should have been counted

                    //create window based on this term, and check its context
                    currWindowStart=term.firstTokenIndex-window;
                    if(currWindowStart<0)
                        currWindowStart=0;
                    currWindowEnd = term.lastTokenIndex+window;
                    if(currWindowEnd>=terms.size())
                        currWindowEnd=terms.size()-1;

                    String windowId = docId+","+currWindowStart+"-"+currWindowEnd;
                    feature.increment(windowId,1);
                    feature.increment(windowId, term.string,1);
                    //previous context
                    //todo-2: check against previous window, to identify overlap. record following info
                    //create an overlap zone; the affected context ids; for each term in the zone, the string, and freq.
                    for(int j=i-1; j>-1; j--){
                        MWESentenceContext nextTerm = terms.get(j);
                        if(nextTerm.lastTokenIndex<currWindowStart)
                            break;
                        feature.increment(windowId, 1);
                        feature.increment(windowId, nextTerm.string, 1);
                    }
                    //following context
                    //todo-1: record following information:
                    //a list of term firsttokenidx maps to its string
                    //
                    for(int j=i+1; j<terms.size(); j++){
                        MWESentenceContext nextTerm = terms.get(j);
                        if(nextTerm.firstTokenIndex>currWindowEnd)
                            break;
                        feature.increment(windowId, 1);
                        feature.increment(windowId, nextTerm.string, 1);
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
        if(firstTokenIndexes.size()/docIds.size()<=1)
            try {
                LOG.warning("Check your analyzer chain for your Solr field "
                        +properties.getSolrFieldnameJATENGramInfo()+" if each token's position in a sentence has been produced.");
            } catch (JATEException e) {
            }
        //LOG.info("debug---finished");
        return count;
    }

    private List<MWESentenceContext> collectTermSentenceContext(Terms termVectorLookup) throws IOException {
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
                    SentenceContext sentenceContextInfo=null;
                    if(payload!=null){
                        sentenceContextInfo=new SentenceContext(payload.utf8ToString());
                    }
                    if(sentenceContextInfo==null)
                        result.add(new MWESentenceContext(tString, start, end,0,0,0));
                    else
                        result.add(new MWESentenceContext(tString, start, end,
                                Integer.parseInt(sentenceContextInfo.getFirstTokenIdx()),
                                Integer.parseInt(sentenceContextInfo.getLastTokenIdx()),
                                Integer.parseInt(sentenceContextInfo.getSentenceId())));
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
            this.string=string;
            this.sentenceId = sentenceId;
            this.start = start;
            this.end = end;
            this.firstTokenIndex=firstTokenIndex;
            this.lastTokenIndex=lastTokenIndex;
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

            return "s="+sentenceId + ",f=" + firstTokenIndex+",l="+lastTokenIndex+",so="+start+",se="+end;
        }
    }
}
