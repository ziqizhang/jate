package uk.ac.shef.dcs.jate.test;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;

import java.io.IOException;
import java.nio.file.Paths;

/**
 * Created by zqz on 04/10/2015.
 */
public class TestIndexing {
    public static void main(String[] args) throws IOException, SolrServerException {
        SolrClient solrClient =
                new EmbeddedSolrServer(Paths.get("D:\\Work\\jate_github\\jate\\solr-5.3.0_\\server\\solr"),
                        "core1");
        SolrInputDocument solrDoc = new SolrInputDocument();
        solrDoc.addField("id", "01");
        String realContent= "ok\r\nthis is the text\r\nand...";
        solrDoc.addField("content", realContent);
        solrClient.add(solrDoc);
        solrClient.commit();
        solrClient.close();
    }
}
