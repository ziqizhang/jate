package uk.ac.shef.dcs.jate.v2.indexing;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.v2.JATEnum;
import uk.ac.shef.dcs.jate.v2.io.DocumentCreator;
import uk.ac.shef.dcs.jate.v2.model.Document;
import uk.ac.shef.dcs.jate.v2.util.SolrUtil;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class ParallelIndexingWorker extends JATERecursiveTaskWorker<String, Integer> {

    private static final Logger LOG = Logger.getLogger(ParallelIndexingWorker.class.getName());
    protected List<String> tasks;
    protected int batchSize=100;
    protected DocumentCreator docCreator;
    protected SolrClient solrClient;

    public ParallelIndexingWorker(List<String> tasks,
                                  int maxTasksPerThread,
                                  DocumentCreator docCreator,
                                  SolrClient solrClient){
        super(tasks, maxTasksPerThread);
        this.docCreator = docCreator;
        this.solrClient=solrClient;
    }

    public ParallelIndexingWorker(List<String> tasks,
                                  int maxTasksPerThread,
                                  int batchSize,
                                  DocumentCreator docCreator,
                                  SolrClient solrClient){
        this(tasks, maxTasksPerThread, docCreator, solrClient);
        this.batchSize=batchSize;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Integer> createInstance(List<String> splitTasks) {
        return new ParallelIndexingWorker(splitTasks, maxTasksPerThread,
                batchSize, docCreator.copy(), solrClient);
    }

    @Override
    protected Integer mergeResult(List<JATERecursiveTaskWorker<String, Integer>> workers) {
        int total=0;
        for(JATERecursiveTaskWorker<String, Integer> worker: workers)
            total+=worker.join();
        return total;
    }

    @Override
    protected Integer computeSingleWorker(List<String> tasks) {
        int total = 0, batches=0;
        for(String task: tasks){
            try {
                Document doc = docCreator.create(task);
                total++;
                SolrInputDocument solrDoc = new SolrInputDocument();
                solrDoc.addField(JATEnum.SOLR_FIELD_ID.getString(), doc.getId());
                solrDoc.addField(JATEnum.SOLR_FIELD_JATE_ALL_TEXT.getString(), doc.getContent());
                for(Map.Entry<String, String> field2Value : doc.getMapField2Content().entrySet()){
                    String field = field2Value.getKey();
                    String value = field2Value.getValue();

                    solrDoc.addField(field, value);
                }
                solrClient.add(solrDoc);
                if(total%batchSize==0) {
                    batches++;
                    LOG.info("Done batches: "+batches);
                    SolrUtil.commit(solrClient,LOG,String.valueOf(batches), String.valueOf(batchSize));
                }
            } catch (JATEException e) {
                StringBuilder message = new StringBuilder("FAILED TO ADD DOC TO SOLR (no commit): ");
                message.append(task).append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                LOG.severe(message.toString());
            } catch (IOException e) {
                StringBuilder message = new StringBuilder("FAILED TO ADD DOC TO SOLR (no commit): ");
                message.append(task).append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                LOG.severe(message.toString());
            }  catch (SolrServerException e) {
                StringBuilder message = new StringBuilder("FAILED TO ADD DOC TO SOLR (add): ");
                message.append(task).append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                LOG.severe(message.toString());
            }
        }
        SolrUtil.commit(solrClient,LOG,String.valueOf(batches+1), String.valueOf(batchSize));
        return total;
    }
}
