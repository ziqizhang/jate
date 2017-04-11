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
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 *
 */
public class PositionFeatureWorker extends JATERecursiveTaskWorker<Integer, int[]> {

    private static final long serialVersionUID = -5304728799852736303L;
    private static final Logger LOG = Logger.getLogger(PositionFeatureWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private PositionFeature feature;
    private Set<String> allCandidates;

    PositionFeatureWorker(JATEProperties properties, List<Integer> docIds, Set<String> allCandidates,
                          SolrIndexSearcher solrIndexSearcher,
                          PositionFeature feature, int maxTasksPerWorker
    ) {
        super(docIds, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.solrIndexSearcher = solrIndexSearcher;
        this.allCandidates = allCandidates;
    }

    @Override
    protected JATERecursiveTaskWorker<Integer, int[]> createInstance(List<Integer> docIds) {
        return new PositionFeatureWorker(properties, docIds, allCandidates, solrIndexSearcher, feature, maxTasksPerThread
        );
    }

    @Override
    protected int[] mergeResult(List<JATERecursiveTaskWorker<Integer, int[]>> jateRecursiveTaskWorkers) {
        int totalSuccess = 0, total = 0;
        for (JATERecursiveTaskWorker<Integer, int[]> worker : jateRecursiveTaskWorkers) {
            int[] rs = worker.join();
            totalSuccess += rs[0];
            total += rs[1];
        }
        return new int[]{totalSuccess, total};
    }

    @Override
    protected int[] computeSingleWorker(List<Integer> docIds) {
        LOG.info("Total docs to process=" + docIds.size());
        int count = 0;
        for (int docId : docIds) {
            try {
                Terms lookupVector = SolrUtil.getTermVector(docId, properties.getSolrFieldNameJATENGramInfo(), solrIndexSearcher);
                TermsEnum ngramEnum = lookupVector.iterator();
                BytesRef luceneTerm = ngramEnum.next();
                while (luceneTerm != null) {
                    if (luceneTerm.length == 0) {
                        luceneTerm = ngramEnum.next();
                        continue;
                    }
                    String tString = luceneTerm.utf8ToString();
                    if (!allCandidates.contains(tString)) {
                        luceneTerm = ngramEnum.next();
                        continue;
                    }
                    PostingsEnum postingsEnum = ngramEnum.postings(null, PostingsEnum.ALL);
                    if (postingsEnum.nextDoc() != PostingsEnum.NO_MORE_DOCS) {
                        //tf in document
                        int totalOccurrence = postingsEnum.freq();
                        for (int i = 0; i < totalOccurrence; i++) {
                            int pos = postingsEnum.nextPosition();
                            BytesRef payload = postingsEnum.getPayload();
                            MWEMetadata metadata = MWEMetadata.deserialize(payload.utf8ToString());
                            populateFeature(metadata, tString, feature);

                            /*if (totalOccurrence > 1 && tString.equals("1,25-dihydroxy vitamin")) {
                                System.out.println("pos=" + pos + ", " +
                                        postingsEnum.startOffset() + "-" + postingsEnum.endOffset()
                                        + ", " + metadata.getMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_DOC));
                            }*/
                        }
                        //postingsEnum.nextDoc();
                    }
                    luceneTerm=ngramEnum.next();
                }
                count++;
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

        LOG.debug("progress : " + count + "/" + docIds.size());
        return new int[]{count, docIds.size()};
    }

    private void populateFeature(MWEMetadata metadata, String term, PositionFeature feature) {
        int sourceParIdInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SOURCE_PARAGRAPH_ID_IN_DOC));
        int sourceSentIdInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_DOC));
        int sourceSentIdInPar = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SOURCE_SENTENCE_ID_IN_PARAGRAPH));

        int totalParsInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.PARAGRAPHS_IN_DOC));
        int totalSentsInDoc = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SENTENCES_IN_DOC));
        int totalSentsInPar = Integer.valueOf(metadata.getMetaData(MWEMetadataType.SENTENCES_IN_PARAGRAPH));


        if (sourceParIdInDoc == 0)
            feature.incrementFoundInDocTitles(term);

        double parDistFromTitle = calculateDistance(sourceParIdInDoc, totalParsInDoc);
        feature.addParDistFromTitle(term, parDistFromTitle);
        double sentDistFromTitle = calculateDistance(sourceSentIdInDoc, totalSentsInDoc);
        feature.addSentDistFromTitle(term, sentDistFromTitle);
        double sentDistFromPar = calculateDistance(sourceSentIdInPar, totalSentsInPar);
        feature.addSentDistFromPar(term, sentDistFromPar);
    }

    private double calculateDistance(int index, int total) {
        return index / (double) total;
    }

    //this method can be implemented to check if the term contains any elements in the gazetteer.
    protected void applyGazetteer(Set<String> gazetteer, String term) {
        //must update with feature.mweHasIndicative...
    }
}
