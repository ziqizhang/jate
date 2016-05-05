package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexableField;
import org.apache.lucene.index.Term;
import org.apache.solr.core.SolrCore;

import org.apache.solr.schema.CopyField;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler.Algorithm;
import uk.ac.shef.dcs.jate.util.SolrUtil;

/**
 * A composite Term Recognition (TR) processor implemented all methods by
 * delegating them to different TR processor
 */
public class CompositeTermRecognitionProcessor implements TermRecognitionProcessor {

    private Collection<TermRecognitionProcessor> processors = new ArrayList<TermRecognitionProcessor>();
    public static final Float DEFAULT_BOOST_VALUE = 1.0F;

    @Override
    public Boolean candidateExtraction(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        SolrIndexSearcher indexSearcher = core.getSearcher().get();

        IndexWriter writerIn = core.getSolrCoreState().getIndexWriter(core).get();
        Map<String,List<CopyField>> copyFields = core.getLatestSchema().getCopyFieldsMap();

        for (int i=0; i<indexSearcher.maxDoc(); i++) {
            Document doc = indexSearcher.doc(i);

            SolrUtil.copyFields(copyFields, DEFAULT_BOOST_VALUE, doc);

            writerIn.updateDocument(new Term("id",doc.get("id")), doc);
        }
        writerIn.commit();

        return true;
    }



    @Override
    public List<JATETerm> rankingAndFiltering(SolrCore core, String jatePropertyFile, Map<String, String> params,
                                              Algorithm algorithm) throws IOException, JATEException {
        for (TermRecognitionProcessor termRecognitionProcessor : processors) {
            List<JATETerm> terms = termRecognitionProcessor.rankingAndFiltering(core, jatePropertyFile, params, algorithm);
            if (terms != null) {
                return terms;
            }
        }
        return null;
    }

    @Override
    public void initialise(Map<String, String> params) throws JATEException {
        return;
    }

    @Override
    public Boolean export(List<JATETerm> termsResults) throws IOException {
        for (TermRecognitionProcessor termRecognitionProcessor : processors) {
            Boolean isSuccess = termRecognitionProcessor.export(termsResults);
            if (isSuccess != null) {
                return isSuccess;
            }
        }
        return null;
    }

    /**
     * Provides access to the term recognition "processors" list
     *
     * @return a mutable ordered collection of processors
     */
    public Collection<TermRecognitionProcessor> getProcessors() {
        return processors;
    }

}
