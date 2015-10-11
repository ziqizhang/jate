package uk.ac.shef.dcs.jate.indexing;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.utils.ExceptionUtils;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.io.DocumentCreator;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.nlp.InstanceCreator;
import uk.ac.shef.dcs.jate.nlp.SentenceSplitter;
import uk.ac.shef.dcs.jate.util.SolrUtil;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;

/**
 * Created by zqz on 15/09/2015.
 */
public class IndexingHandler {

    private static final Logger LOG = Logger.getLogger(IndexingHandler.class.getName());

    public void index(List<String> tasks,
                      int batchSize, DocumentCreator docCreator,
                      SolrClient solrClient,
                      JATEProperties properties){
        SentenceSplitter sentenceSplitter=null;
        boolean indexJATESentences = properties.getSolrFieldnameJATESentences()!=null &&
                !properties.getSolrFieldnameJATESentences().equals("");

        if(indexJATESentences &&sentenceSplitter==null) {
            try {
                sentenceSplitter = InstanceCreator.createSentenceSplitter(properties.getSentenceSplitterClass(),
                        properties.getSentenceSplitterParams());
            }catch (Exception e){
                StringBuilder msg = new StringBuilder("Cannot instantiate NLP sentence splitter, which will be null. Error trace:\n");
                msg.append(ExceptionUtils.getStackTrace(e));
                LOG.severe(msg.toString());
            }
        }

        StringBuilder msg = new StringBuilder("Beginning indexing dataset").append(", total docs="+tasks.size());
        if(indexJATESentences)
            msg.append(". Sentence boundaries required. ");

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
                solrDoc.addField(properties.getSolrFieldnameID(), doc.getId());
                solrDoc.addField(properties.getSolrFieldnameJATENGramInfo(), doc.getContent());
                solrDoc.addField(properties.getSolrFieldnameJATECTerms(), doc.getContent());

                if(sentenceSplitter!=null) {
                    indexSentenceOffsets(solrDoc, properties.getSolrFieldnameJATESentences(), doc.getContent(),
                            sentenceSplitter);
                }
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


        msg=new StringBuilder("Complete indexing dataset. Total processed items = ");
        msg.append(total);
        if(skipped.length()!=0)
            msg.append("\n").append("Some items are skipped because of empty content. If you are not expecting this, check ")
            .append(DocumentCreator.class.getName()).append(" you have used for indexing, or try a different one.\n");
        msg.append(skipped);
        if(skipped.length()==0)
            LOG.info(msg.toString());
        else
            LOG.warning(msg.toString());
        try {
            solrClient.close();
        } catch (IOException e) {
            String message = "CANNOT CLOSE SOLR: \n";
            LOG.severe(message+ExceptionUtils.getStackTrace(e));
        }
    }

    protected void indexSentenceOffsets(SolrInputDocument solrDoc, String solrFieldnameJATESentencesAll, String content,
                                        SentenceSplitter sentenceSplitter) {
        content=content.replaceAll("\\r","");
        List<int[]> offsets=sentenceSplitter.split(content);
        String[] values= new String[offsets.size()];
        for(int i=0; i<offsets.size(); i++){
            int[] offset = offsets.get(i);
            values[i]=offset[0]+","+offset[1];
        }
        solrDoc.addField(solrFieldnameJATESentencesAll, values);
    }
}
