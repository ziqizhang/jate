package uk.ac.shef.dcs.jate.app;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

// Java Code Examples for EmbeddedSolrServer

// http://stackoverflow.com/questions/936723/testing-solr-via-embedded-server
// https://wiki.searchtechnologies.com/index.php/Unit_Testing_with_Embedded_Solr
// http://www.programcreek.com/java-api-examples/index.php?api=org.apache.solr.client.solrj.embedded.EmbeddedSolrServer
public abstract class BaseEmbeddedSolrTest {
    private static Logger LOG = Logger.getLogger(BaseEmbeddedSolrTest.class.getName());

    String workingDir = System.getProperty("user.dir");
    String solrCoreName = "testCore";
    String solrHome = workingDir + "\\testdata\\solr-testbed\\";

    EmbeddedSolrServer server;

    @Before
    public void setup() throws Exception {
        try {
        cleanIndexDirectory(solrHome, solrCoreName);
        } catch (IOException ioe) {
            throw new JATEException("Unable to delete index data. Please clean index directory [testdata\\solr-testbed\\testCore\\data] manually!");
        }

        CoreContainer testBedContainer = new CoreContainer(solrHome);
        testBedContainer.load();

        server = new EmbeddedSolrServer(testBedContainer, solrCoreName);
    }

    /**
     * create and add new document with necessary fields
     *  see also @code{uk.ac.shef.dcs.jate.indexing.IndexingHandler}
     * @param docId
     * @param docTitle
     * @param text
     * @param jateProperties
     * @param commit
     * @throws IOException
     * @throws SolrServerException
     * @throws JATEException
     */
    protected void addNewDoc(String docId, String docTitle, String text, JATEProperties jateProperties, boolean commit)
            throws IOException, SolrServerException, JATEException {
        SolrInputDocument newDoc = new SolrInputDocument();
        newDoc.addField("id", docId);
        newDoc.addField("title_s", docTitle);
        newDoc.addField("text", text);

        newDoc.addField(jateProperties.getSolrFieldNameJATENGramInfo(), text);
        newDoc.addField(jateProperties.getSolrFieldNameJATECTerms(), text);

        server.add(newDoc);

        if (commit) {
            server.commit();
        }
    }

//    @Test
//    public void testDocInsertion() throws IOException, SolrServerException {
//
//        addNewDoc("doc-1", "Test Document 1", "Hello solr!", true);
//
//        ModifiableSolrParams params = new ModifiableSolrParams();
//        params.set("q", "*:*");
//
//        try {
//            QueryResponse qResp = server.query(params);
//            SolrDocumentList docList = qResp.getResults();
//
//            assert (docList.getNumFound() == 1);
//            /**
//            cleanData();
//            QueryResponse qResp2 = server.query(params);
//            docList = qResp.getResults();
//            System.out.println("Num docs after deletion: " + docList.getNumFound());
//            assert (docList.getNumFound() == 0);
//            **/
//        } catch (SolrServerException e) {
//            e.printStackTrace();
//        } catch (IOException ioe) {
//            ioe.printStackTrace();
//        }
//    }

    public void cleanIndexDirectory(String solrHome, String coreName) throws IOException {
        File indexDir = new File(solrHome + "/" + coreName + "/data/index/");
        try {
            FileUtils.cleanDirectory(indexDir);
        } catch (IOException e) {
            LOG.error("Failed to clean index directory! Please do it manually!");
            throw e;
        }
    }

    public void cleanData() throws IOException, SolrServerException {
        // LOG.log(Level.FINE, );
        LOG.info("clean all test data...");
        try {
            server.deleteByQuery("*:*");
            server.commit();
        } catch (SolrServerException e) {
            LOG.error("Failed to clean test data in index! Please do it manually!");
            throw e;
        }
    }

    @After
    public void tearDown() throws IOException {
        if (server != null && server.getCoreContainer() != null) {
            LOG.info("shutting down core in :" + server.getCoreContainer().getCoreRootDirectory());

//            try {
//                cleanData();
//            } catch (IOException e) {
//                e.printStackTrace();
//                LOG.error("Failed to clean test data. Please do it manually!");
//            } catch (SolrServerException e) {
//                e.printStackTrace();
//                LOG.error("Failed to clean test data. Please do it manually!");
//            }
            server.getCoreContainer().getCore(solrCoreName).close();
            server.getCoreContainer().shutdown();
            server.close();

            try {
                // wait for server shutting down
                Thread.sleep(6000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            try {
                cleanIndexDirectory(solrHome, solrCoreName);
            } catch (IOException ioe) {
                LOG.warn("Unable to delete indice file. Please do it manually! ");
            }
        } else {
            // LOG.log(Level.SEVERE, "embedded server or core is not created and loaded successfully.");
            LOG.error("embedded server or core is not created and loaded successfully.");
        }
    }

}
