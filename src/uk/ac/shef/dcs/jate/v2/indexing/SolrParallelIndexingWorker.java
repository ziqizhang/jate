package uk.ac.shef.dcs.jate.v2.indexing;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrDocument;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.JATERecursiveTaskWorker;
import uk.ac.shef.dcs.jate.v2.io.DocumentCreator;
import uk.ac.shef.dcs.jate.v2.model.JATEDocument;
import uk.ac.shef.dcs.jate.v2.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.v2.nlp.SentenceSplitter;
import uk.ac.shef.dcs.jate.v2.util.SolrUtil;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 *
 */
public class SolrParallelIndexingWorker extends JATERecursiveTaskWorker<String, Integer> {

    private static final Logger LOG = Logger.getLogger(SolrParallelIndexingWorker.class.getName());
    protected List<String> tasks;
    protected int batchSize=100;
    protected DocumentCreator docCreator;
    protected SolrClient solrClient;
    protected JATEProperties properties;
    protected SentenceSplitter sentenceSplitter;

    public SolrParallelIndexingWorker(List<String> tasks,
                                      int maxTasksPerThread,
                                      DocumentCreator docCreator,
                                      SolrClient solrClient,
                                      JATEProperties properties){
        super(tasks, maxTasksPerThread);
        this.docCreator = docCreator;
        this.solrClient=solrClient;
        this.properties=properties;
        boolean indexJATESentences = properties.getSolrFieldnameJATESentencesAll()!=null &&
                !properties.getSolrFieldnameJATESentencesAll().equals("");
        if(indexJATESentences) {
            try {
                sentenceSplitter = InstanceCreator.createSentenceSplitter(properties.getSentenceSplitterClass(),
                        properties.getSentenceSplitterParams());
            }catch (Exception e){
                StringBuilder msg = new StringBuilder("Cannot instantiate NLP sentence splitter, which will be null. Error trace:\n");
                msg.append(ExceptionUtils.getStackTrace(e));
                LOG.severe(msg.toString());
            }
        }
    }

    public SolrParallelIndexingWorker(List<String> tasks,
                                      int maxTasksPerThread,
                                      int batchSize,
                                      DocumentCreator docCreator,
                                      SolrClient solrClient,
                                      JATEProperties properties){
        this(tasks, maxTasksPerThread, docCreator, solrClient,properties);
        this.batchSize=batchSize;
    }

    @Override
    protected JATERecursiveTaskWorker<String, Integer> createInstance(List<String> splitTasks) {
        return new SolrParallelIndexingWorker(splitTasks, maxTasksPerThread,
                batchSize, docCreator.copy(), solrClient, properties);
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
        boolean indexJATEWords = properties.getSolrFieldnameJATEWordsAll()!=null &&
                !properties.getSolrFieldnameJATEWordsAll().equals("");

        for(String task: tasks){
            try {
                JATEDocument doc = docCreator.create(task);
                total++;
                SolrInputDocument solrDoc = new SolrInputDocument();
                solrDoc.addField(properties.getSolrFieldnameID(), doc.getId());
                solrDoc.addField(properties.getSolrFieldnameJATETermsAll(), doc.getContent());
                if(indexJATEWords)
                    solrDoc.addField(properties.getSolrFieldnameJATEWordsAll(), doc.getContent());
                if(sentenceSplitter!=null) {
                    indexSentenceOffsets(solrDoc, properties.getSolrFieldnameJATESentencesAll(), doc.getContent());
                }
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

    protected void indexSentenceOffsets(SolrInputDocument solrDoc, String solrFieldnameJATESentencesAll, String content) {
        List<int[]> offsets=sentenceSplitter.split(content);
        String[] values= new String[offsets.size()];
        for(int i=0; i<offsets.size(); i++){
            int[] offset = offsets.get(i);
            values[i]=offset[0]+","+offset[1];
        }
        solrDoc.addField(properties.getSolrFieldnameJATESentencesAll(), values);
    }
}
