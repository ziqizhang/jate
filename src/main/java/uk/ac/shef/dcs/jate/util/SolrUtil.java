package uk.ac.shef.dcs.jate.util;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.*;
import org.apache.lucene.util.BytesRef;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.schema.CopyField;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;

public class SolrUtil {

    /**
     * Get indexed term vectors
     * @param fieldname  field where term vectors will be retrieved
     * @param solrIndexSearcher  solr index searcher
     * @return Terms  term vectors
     * @throws JATEException
     */
    public static Terms getTermVector(String fieldname, SolrIndexSearcher solrIndexSearcher) throws JATEException {
        try {
            Fields fields = MultiFields.getFields(solrIndexSearcher.getSlowAtomicReader());

            Terms vector = fields.terms(fieldname);
            if (vector == null)
                throw new JATEException(String.format("Cannot find expected field: %s", fieldname));
            return vector;
        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder(String.format("Cannot find expected field: %s. Error stacktrack: \n", fieldname));
            sb.append(org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ioe));
            throw new JATEException(sb.toString());
        }
    }

    public static void copyFields(Map<String, List<CopyField>> copyFields, float boost, Document doc) {
        for (String sourceField : copyFields.keySet()) {
            List<CopyField> copyFieldList = copyFields.get(sourceField);
            for (CopyField copyField : copyFieldList) {
                // remove previous one if exist
                doc.removeField(copyField.getDestination().getName());

                IndexableField jateField = copyField.getDestination().
                        createField(doc.get(copyField.getSource().getName()), boost);
                doc.add(jateField);
            }
        }
    }

    /**
     * Get indexed (normalised) term strings
     *
     * @param indexedTermsVector  term vectors indexed
     * @return List<String> utf-8 string of term
     * @throws IOException
     */
    public static List<String> getNormalisedTerms(Terms indexedTermsVector) throws IOException {
        List<String> normTermStrs = new ArrayList<>();
        if (indexedTermsVector == null || indexedTermsVector.size() == 0) {
            return normTermStrs;
        }

        TermsEnum iterTerms = indexedTermsVector.iterator();
        BytesRef text;
        while((text = iterTerms.next()) != null) {
            normTermStrs.add(text.utf8ToString());
        }
        return normTermStrs;
    }

    public static Terms getTermVector(int docId, String fieldname, SolrIndexSearcher solrIndexSearcher) throws JATEException {
        try {
            Terms vector = solrIndexSearcher.getSlowAtomicReader().getTermVector(docId, fieldname);

            return vector;
        } catch (IOException ioe) {
            StringBuilder sb = new StringBuilder(String.format("Cannot find expected field: %s. Error stacktrack:\n", fieldname));
            sb.append(org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ioe));
            throw new JATEException(sb.toString());
        }
    }

    public static void commit(SolrClient solr,
                              Logger logger, String... messages) {
        try {
            solr.commit();
        } catch (SolrServerException e) {
            StringBuilder message = new StringBuilder("FAILED TO COMMIT TO SOLR: ");
            message.append(Arrays.toString(messages)).append("\n")
                    .append(ExceptionUtils.getStackTrace(e)).append("\n");
            logger.error(message.toString());
        } catch (IOException e) {
            StringBuilder message = new StringBuilder("FAILED TO COMMIT TO SOLR: ");
            message.append(Arrays.toString(messages)).append("\n")
                    .append(ExceptionUtils.getStackTrace(e)).append("\n");
            logger.error(message.toString());
        }
    }
}
