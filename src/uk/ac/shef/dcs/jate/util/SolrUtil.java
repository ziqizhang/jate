package uk.ac.shef.dcs.jate.util;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSLockFactory;
import org.apache.lucene.store.MMapDirectory;
import org.apache.lucene.store.NIOFSDirectory;
import org.apache.lucene.store.SimpleFSDirectory;
import org.apache.lucene.util.Constants;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.tika.utils.ExceptionUtils;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class SolrUtil {

    public static IndexReader getIndexReader(String indexPath) throws IOException {
        /*if (Constants.JRE_IS_64BIT && MMapDirectory.UNMAP_SUPPORTED) {
            return DirectoryReader.open(new MMapDirectory(Paths.get(indexPath), FSLockFactory.getDefault()));
        } else*/ if (Constants.WINDOWS) {
            return DirectoryReader.open(new SimpleFSDirectory(Paths.get(indexPath), FSLockFactory.getDefault()));
        } else {
            return DirectoryReader.open(new NIOFSDirectory(Paths.get(indexPath), FSLockFactory.getDefault()));
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
