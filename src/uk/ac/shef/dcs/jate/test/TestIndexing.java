package uk.ac.shef.dcs.jate.test;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.io.DocumentCreator;
import uk.ac.shef.dcs.jate.io.TikaSimpleDocumentCreator;
import uk.ac.shef.dcs.jate.model.JATEDocument;

import java.io.*;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by zqz on 04/10/2015.
 */
public class TestIndexing {
    public static void main(String[] args) throws IOException, SolrServerException, JATEException {

        AutoDetectParser parser = new AutoDetectParser();
        for (File f : new File("/Users/-/work/jate/experiment/bugged_corpus").listFiles()) {
            InputStream in = new BufferedInputStream(new FileInputStream(f.toString()));
            BodyContentHandler handler = new BodyContentHandler(-1);
            Metadata metadata = new Metadata();
            try {

                parser.parse(in, handler, metadata);
                String content = handler.toString();
                System.out.println(metadata);
            }catch (Exception e){
                e.printStackTrace();
            }
        }
        System.exit(0);



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
