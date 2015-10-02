package uk.ac.shef.dcs.jate.util;

import org.apache.lucene.index.*;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Constants;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServer;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class SolrUtil {

    public static Terms getTermVector(String fieldname, SolrIndexSearcher solrIndexSearcher) throws JATEException{
        try {
            Fields fields = MultiFields.getFields(solrIndexSearcher.getLeafReader());

            Terms vector = fields.terms(fieldname);
            if (vector == null)
                throw new JATEException("Cannot find expected field: " + fieldname);
            return vector;
        }catch (IOException ioe){
            StringBuilder sb = new StringBuilder("Cannot find expected field: ");
            sb.append(fieldname).append(". Error stacktrack:\n");
            sb.append(org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ioe));
            throw new JATEException(sb.toString());
        }
    }

    public static Terms getTermVector(int docId, String fieldname, SolrIndexSearcher solrIndexSearcher) throws JATEException{
        try {
            Terms vector=solrIndexSearcher.getLeafReader().getTermVector(docId, fieldname);
            if (vector == null)
                throw new JATEException("Cannot find expected field: " + fieldname);
            return vector;
        }catch (IOException ioe){
            StringBuilder sb = new StringBuilder("Cannot find expected field: ");
            sb.append(fieldname).append(". Error stacktrack:\n");
            sb.append(org.apache.commons.lang.exception.ExceptionUtils.getFullStackTrace(ioe));
            throw new JATEException(sb.toString());
        }
    }

    public static void commit(SolrClient solr,
                              Logger logger, String... messages){
        try {
            solr.commit();
        } catch (SolrServerException e) {
            StringBuilder message = new StringBuilder("FAILED TO COMMIT TO SOLR: ");
            message.append(Arrays.toString(messages)).append("\n")
                    .append(ExceptionUtils.getStackTrace(e)).append("\n");
            logger.severe(message.toString());
        } catch (IOException e) {
            StringBuilder message = new StringBuilder("FAILED TO COMMIT TO SOLR: ");
            message.append(Arrays.toString(messages)).append("\n")
                    .append(ExceptionUtils.getStackTrace(e)).append("\n");
            logger.severe(message.toString());
        }
    }
}
