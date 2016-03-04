package uk.ac.shef.dcs.jate.app;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrInputDocument;
import org.apache.solr.core.CoreContainer;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstracted class to support embedded solr based unit test
 * <p>
 * for more examples about how to use solr Embedded Solr server,
 *
 * @see <a href="http://www.programcreek.com/java-api-examples/index.php?api=org.apache.solr.client.solrj.embedded.EmbeddedSolrServer">
 * Java Code Examples for org.apache.solr.client.solrj.embedded.EmbeddedSolrServer</a>
 * @see <a href="https://wiki.searchtechnologies.com/index.php/Unit_Testing_with_Embedded_Solr">
 * Unit Testing with Embedded Solr</a>
 */
public abstract class BaseEmbeddedSolrTest {
    private static Logger LOG = Logger.getLogger(BaseEmbeddedSolrTest.class.getName());

    static String workingDir = System.getProperty("user.dir");
    static Path solrHome = Paths.get(workingDir, "testdata", "solr-testbed");

    static Path REF_FREQ_FILE = Paths.get(workingDir, "testdata", "solr-testbed", "GENIA",
            "conf", "bnc_unifrqs.normal");

    EmbeddedSolrServer server;
    protected String solrCoreName;
    protected boolean reindex;

    protected abstract void setSolrCoreName();

    protected abstract void setReindex();

    //@Before
    public void setup() throws Exception {
        setSolrCoreName();
        setReindex();
//        if(reindex)
//            cleanIndexDirectory(solrHome.toString(), solrCoreName);
        CoreContainer testBedContainer = new CoreContainer(solrHome.toString());
        testBedContainer.load();
        server = new EmbeddedSolrServer(testBedContainer, solrCoreName);
    }

    /**
     * create and add new document with necessary fields
     * see also @code{uk.ac.shef.dcs.jate.indexing.IndexingHandler}
     *
     * @param docId,          document id value for default solr field 'id'
     * @param docTitle,       document title for default solr field 'title_s'
     * @param text,           document content for default solr field 'text' where term will be extracted
     * @param jateProperties, JATE properties for various run-time parameters
     * @param commit,         boolean value to determine whether the new document will be committed
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

    public static void cleanIndexDirectory(String solrHome, String coreName) throws IOException {
//       File indexDir = new File(solrHome + File.separator + coreName + File.separator +
//                "data" + File.separator + "index" + File.separator);
        File indexDir = Paths.get(solrHome, coreName, "data", "index").toFile();

        try {
            if (indexDir.exists()) {
                FileUtils.cleanDirectory(indexDir);
            }
        } catch (IOException e) {
            LOG.error("Failed to clean index directory! Please do it manually!");
            throw e;
        }
    }

    @After
    public void tearDown() throws IOException {
        if (server != null && server.getCoreContainer() != null) {
            LOG.info("shutting down core in :" + server.getCoreContainer().getCoreRootDirectory());

            try {
                server.getCoreContainer().getCore(solrCoreName).close();
                server.getCoreContainer().shutdown();
                server.close();

                unlock();
//            cleanIndexDirectory(solrHome.toString(), solrCoreName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            LOG.error("embedded server or core is not created and loaded successfully.");
        }
    }

    protected void unlock() {
        File lock = Paths.get(solrHome.toString(), solrCoreName, "data", "index", "write.lock").toFile();
        if (lock.exists()) {
            System.err.println("solr did not shut down cleanly");
            Assert.assertTrue(lock.delete());
        }
    }

    @AfterClass
    public static void tearDownClass() throws JATEException {
//        try {
//            cleanIndexDirectory(solrHome.toString(), solrCoreName);
//        } catch (IOException ioe) {
//            throw new JATEException("Unable to delete index data. Please clean index directory " +
//                    "[testdata\\solr-testbed\\testCore\\data] manually!");
//        }
    }


}
