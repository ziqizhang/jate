package uk.ac.shef.dcs.jate.indexing;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.io.DocumentCreator;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.*;

public class IndexingHandler {

    private static final Logger LOG = Logger.getLogger(IndexingHandler.class.getName());

    public void index(List<String> tasks,
                      int batchSize, DocumentCreator docCreator,
                      SolrClient solrClient,
                      JATEProperties properties){

        StringBuilder msg = new StringBuilder("Beginning indexing dataset").append(", total docs="+tasks.size());

        LOG.info(msg.toString());
        int total=0, batches=0;

        StringBuilder skipped=new StringBuilder();
        for(String task: tasks){
            try {
                JATEDocument doc = docCreator.create(task);

                String content=doc.getContent();
                if(content.length()==0){
                    skipped.append(doc.getId()).append("\n");
                    continue;
                }
                total++;
                SolrInputDocument solrDoc = new SolrInputDocument();
                solrDoc.addField(properties.getSolrFieldNameID(), doc.getId());
                solrDoc.addField(properties.getSolrFieldNameJATENGramInfo(), doc.getContent());
                solrDoc.addField(properties.getSolrFieldNameJATECTerms(), doc.getContent());

                for(Map.Entry<String, String> field2Value : doc.getMapField2Content().entrySet()){
                    String field = field2Value.getKey();
                    String value = field2Value.getValue();

                    solrDoc.addField(field, value);
                }
                solrClient.add(solrDoc);
                if(total%batchSize==0) {
                    batches++;
                    LOG.info("Done batches: "+batches*batchSize);
                    SolrUtil.commit(solrClient, LOG, String.valueOf(batches), String.valueOf(batchSize));
                }
            } catch (JATEException e) {
                StringBuilder message = new StringBuilder("FAILED TO ADD DOC TO SOLR (no commit): ");
                message.append(task).append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                LOG.error(message.toString());
            } catch (IOException e) {
                StringBuilder message = new StringBuilder("FAILED TO ADD DOC TO SOLR (no commit): ");
                message.append(task).append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                LOG.error(message.toString());
            }  catch (SolrServerException e) {
                StringBuilder message = new StringBuilder("FAILED TO ADD DOC TO SOLR (add): ");
                message.append(task).append("\n")
                        .append(ExceptionUtils.getStackTrace(e)).append("\n");
                LOG.error(message.toString());
            }
        }
        SolrUtil.commit(solrClient,LOG,String.valueOf(batches+1), String.valueOf(batchSize));


        msg=new StringBuilder("Complete indexing dataset. Total processed items = ");
        msg.append(total);
        if(skipped.length()!=0)
            msg.append("\n").append("Some items are skipped because of empty content. If you are not expecting this, check ")
            .append(DocumentCreator.class.getName()).append(" you have used for indexing, or try a different one.\n");
        msg.append(skipped);
        if(skipped.length()==0)
            LOG.info(msg.toString());
        else
            LOG.warn(msg.toString());
        try {
            solrClient.close();
        } catch (IOException e) {
            String message = "CANNOT CLOSE SOLR: \n";
            LOG.error(message + ExceptionUtils.getStackTrace(e));
        }
    }
}
