package uk.ac.shef.dcs.jate.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.log4j.Logger;
import org.apache.lucene.analysis.jate.MWEMetadata;
import org.apache.lucene.analysis.jate.MWEMetadataType;
import org.apache.lucene.index.PostingsEnum;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;

import java.io.IOException;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class PositionFeatureWorker extends JATERecursiveTaskWorker<String, int[]> {

    private static final long serialVersionUID = -5304728799852736303L;
    private static final Logger LOG = Logger.getLogger(PositionFeatureWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private PositionFeature feature;
    private Terms ngramInfo;

    PositionFeatureWorker(JATEProperties properties, List<String> luceneTerms, SolrIndexSearcher solrIndexSearcher,
                          PositionFeature feature, int maxTasksPerWorker,
                      Terms ngramInfo) {
        super(luceneTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.solrIndexSearcher = solrIndexSearcher;
        this.ngramInfo = ngramInfo;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new PositionFeatureWorker(properties, termSplit, solrIndexSearcher, feature, maxTasksPerThread,
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
                        PostingsEnum postingsEnum = ngramInfoIterator.postings(null, PostingsEnum.ALL);
                        while ( postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                            //tf in document
                            int totalOccurrence = postingsEnum.freq();
                            for (int i = 0; i < totalOccurrence; i++) {
                                postingsEnum.nextPosition();
                                BytesRef bytes = postingsEnum.getPayload();
                                MWEMetadata metadata = MWEMetadata.deserialize(bytes.bytes);
                                populateFeature(metadata, term, feature);
                            }
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

    private void populateFeature(MWEMetadata metadata, String term, PositionFeature feature) {
        int sourceParIdInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SOURCE_PARAGRAPH_ID_IN_DOC));
        int sourceSentIdInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_DOC));
        int sourceSentIdInPar = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_PARAGRAPH));

        int totalParsInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.PARAGRAPHS_IN_DOC));
        int totalSentsInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SENTENCES_IN_DOC));
        int totalSentsInPar = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SENTENCES_IN_PARAGRAPH));


        if(sourceParIdInDoc==0)
            feature.incrementFoundInDocTitles(term);

        double parDistFromTitle=calculateDistance(sourceParIdInDoc, totalParsInDoc);
        feature.addParDistFromTitle(term, parDistFromTitle);
        double sentDistFromTitle=calculateDistance(sourceSentIdInDoc, totalSentsInDoc);
        feature.addSentDistFromTitle(term, sentDistFromTitle);
        double sentDistFromPar = calculateDistance(sourceSentIdInPar, totalSentsInPar);
        feature.addSentDistFromPar(term, sentDistFromPar);
    }

    private double calculateDistance(int index, int total) {
        return index/(double)total;
    }

    //this method can be implemented to check if the term contains any elements in the gazetteer.
    protected void applyGazetteer(Set<String> gazetteer, String term) {
        //must update with feature.mweHasIndicative...
    }
}
