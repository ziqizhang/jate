package uk.ac.shef.dcs.jate.util;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.tika.utils.ExceptionUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class SolrUtil {
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
