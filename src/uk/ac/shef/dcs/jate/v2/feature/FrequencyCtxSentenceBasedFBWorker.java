package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

/**
 * Created by zqz on 21/09/2015.
 */
public class FrequencyCtxSentenceBasedFBWorker extends JATERecursiveTaskWorker<Integer, FrequencyCtxBased> {

    private static final Logger LOG = Logger.getLogger(FrequencyCtxSentenceBasedFBWorker.class.getName());
    private JATEProperties properties;
    private IndexReader indexReader;
    private String termTargetField;
    private String sentenceTargetField;

    public FrequencyCtxSentenceBasedFBWorker(JATEProperties properties,
                                             List<Integer> docIds,
                                             IndexReader indexReader,
                                             int maxTasksPerWorker,
                                             String termTargetField, String sentenceTargetField) {
        super(docIds, maxTasksPerWorker);
        this.properties = properties;
        this.indexReader = indexReader;
        this.termTargetField = termTargetField;
        this.sentenceTargetField = sentenceTargetField;
    }

    @Override
    protected JATERecursiveTaskWorker<Integer, FrequencyCtxBased> createInstance(List<Integer> docIdSplit) {
        return new FrequencyCtxSentenceBasedFBWorker(properties, docIdSplit,
                indexReader, maxTasksPerThread, termTargetField, sentenceTargetField);
    }

    @Override
    protected FrequencyCtxBased mergeResult(List<JATERecursiveTaskWorker<Integer, FrequencyCtxBased>> jateRecursiveTaskWorkers) {
        //todo
        return null;
    }

    @Override
    protected FrequencyCtxBased computeSingleWorker(List<Integer> docIds) {
        FrequencyCtxBased feature = new FrequencyCtxBased();
        for (int docId : docIds) {
            try {
                List<TextUnitOffsets> terms = collectOffsets(indexReader.getTermVector(docId, termTargetField));
                List<TextUnitOffsets> sentences = collectOffsets(indexReader.getTermVector(docId, sentenceTargetField));
                int termCursor=0;
                for(TextUnitOffsets sent: sentences){
                    int contextId=feature.nextCtxId();
                    for(int t = termCursor; t<terms.size();t++){
                        TextUnitOffsets term = terms.get(t);
                        if(term.end>sent.end){
                            termCursor=t;
                            break;
                        }

                        //term within sentence boundary
                        if(term.start>=sent.start){
                            feature.increment(contextId,1);
                            feature.increment(contextId,term.string,1);
                        }
                    }
                    contextId++;
                }
            } catch (IOException ioe) {
                StringBuilder sb = new StringBuilder("Unable to build feature for document id:");
                sb.append(docId).append("\n");
                sb.append(ExceptionUtils.getFullStackTrace(ioe));
                LOG.severe(sb.toString());
            }
        }
        return feature;
    }

    private List<TextUnitOffsets> collectOffsets(Terms termVector) throws IOException {
        List<TextUnitOffsets> result = new ArrayList<>();
        if (termVector == null)
            return result;

        TermsEnum ti = termVector.iterator();
        BytesRef luceneTerm = ti.next();
        while(luceneTerm!=null){
            String tString =luceneTerm.utf8ToString();
            //todo: null is not acceptable. Fix this
            PostingsEnum postingsEnum=ti.postings(null, PostingsEnum.OFFSETS);

            int totalOccurrence=postingsEnum.freq();
            for(int i=0; i<totalOccurrence; i++) {
                postingsEnum.nextPosition();
                int start=postingsEnum.startOffset();
                int end=postingsEnum.endOffset();
                result.add(new TextUnitOffsets(tString, start, end));
            }

            luceneTerm=ti.next();
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
    }
}
