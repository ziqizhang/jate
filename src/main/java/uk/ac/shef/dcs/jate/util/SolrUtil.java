package uk.ac.shef.dcs.jate.util;

import org.apache.lucene.index.*;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.IOException;
import java.util.Arrays;

import org.apache.log4j.Logger;

public class SolrUtil {

    public static Terms getTermVector(String fieldname, SolrIndexSearcher solrIndexSearcher) throws JATEException {
        try {
            Fields fields = MultiFields.getFields(solrIndexSearcher.getLeafReader());

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

    public static Terms getTermVector(int docId, String fieldname, SolrIndexSearcher solrIndexSearcher) throws JATEException {
        try {
            Terms vector = solrIndexSearcher.getLeafReader().getTermVector(docId, fieldname);
            if (vector == null)
                throw new JATEException("Cannot find expected field: " + fieldname);
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
