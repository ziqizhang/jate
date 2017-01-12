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
 * Created by zqz on 11/01/17.
 */
class WordShapeFBWorker extends JATERecursiveTaskWorker<String, int[]> {

    private static final long serialVersionUID = -5304728799851728503L;
    private static final Logger LOG = Logger.getLogger(WordShapeFBWorker.class.getName());
    private JATEProperties properties;
    private SolrIndexSearcher solrIndexSearcher;
    private WordShapeFeature feature;
    private Terms ngramInfo;
    private Set<String> gazetteer;

    WordShapeFBWorker(JATEProperties properties, List<String> luceneTerms, SolrIndexSearcher solrIndexSearcher,
                      WordShapeFeature feature, int maxTasksPerWorker,
                      Terms ngramInfo,
                      Set<String> gazetteer) {
        super(luceneTerms, maxTasksPerWorker);
        this.properties = properties;
        this.feature = feature;
        this.solrIndexSearcher = solrIndexSearcher;
        this.ngramInfo = ngramInfo;
        this.gazetteer=gazetteer;
    }

    @Override
    protected JATERecursiveTaskWorker<String, int[]> createInstance(List<String> termSplit) {
        return new WordShapeFBWorker(properties, termSplit, solrIndexSearcher, feature, maxTasksPerThread,
                ngramInfo, gazetteer);
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
        TermsEnum ngramInfoEnum;
        try {
            ngramInfoEnum = this.ngramInfo.iterator();

            for (String term : terms) {
                try {
                    if (ngramInfoEnum.seekExact(new BytesRef(term.getBytes("UTF-8")))) {
                        PostingsEnum docEnum = ngramInfoEnum.postings(null, PostingsEnum.ALL);
                        int doc = 0;
                        if ((doc = docEnum.nextDoc()) != PostingsEnum.NO_MORE_DOCS) {
                            //tf in document
                            docEnum.nextPosition();
                            BytesRef bytes=docEnum.getPayload();
                            MWEMetadata metadata = MWEMetadata.deserialize(bytes.utf8ToString());

                            applyGazetteer(gazetteer, term);

                            if(metadata.getMetaData(MWEMetadataType.HAS_DIGIT).equalsIgnoreCase("true"))
                                feature.mweHasDigit(term,true);
                            else
                                feature.mweHasDigit(term,false);
                            if(metadata.getMetaData(MWEMetadataType.HAS_UPPERCASE).equalsIgnoreCase("true"))
                                feature.mweHasUppercase(term,true);
                            else
                                feature.mweHasUppercase(term,false);
                            if(metadata.getMetaData(MWEMetadataType.HAS_SYMBOL).equalsIgnoreCase("true"))
                                feature.mweHasSymbol(term,true);
                            else
                                feature.mweHasSymbol(term,false);
                            if(metadata.getMetaData(MWEMetadataType.HAS_NUMERIC_TOKEN).equalsIgnoreCase("true"))
                                feature.mweHasNumber(term,true);
                            else
                                feature.mweHasNumber(term,false);
                            if(metadata.getMetaData(MWEMetadataType.HAS_ACRONYM_TOKEN).equalsIgnoreCase("true"))
                                feature.mweHasAcronym(term,true);
                            else
                                feature.mweHasAcronym(term,false);
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

    //this method can be implemented to check if the term contains any elements in the gazetteer.
    protected void applyGazetteer(Set<String> gazetteer, String term) {
        //must update with feature.mweHasIndicative...
    }
}

