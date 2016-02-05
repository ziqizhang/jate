package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.eval.ATEResultLoader;
import uk.ac.shef.dcs.jate.eval.Scorer;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * To run on external/remote Solr server, it needs jate-2.0Alpha-SNAPSHOT-jar-with-dependencies.jar
 */
public class AppATTFACLRDTECTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppATTFACLRDTECTest.class.getName());

    public AppATTFACLRDTECTest(String solrHomeDir, String solrCoreName) throws JATEException {
        super(solrHomeDir, solrCoreName);
    }

    public List<JATETerm> rankAndFilter(String solrCoreName) throws JATEException {
        List<JATETerm> terms = new ArrayList<>();

        AppATTF appATTF = new AppATTF();

        terms = appATTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        return terms;
    }

    /**
     * To run the test class via
     *
     *  mvn exec:java -Dexec.mainClass="uk.ac.shef.dcs.jate.app.AppATTFACLRDTECTest" -Dexec.classpathScope="test"
     *
     * @param args
     * @throws JATEException
     */
    public static void main(String[] args) throws JATEException {
        try {
            Path corpusDir = Paths.get(workingDir, "src", "test", "resource", "eval", "acl_rd_tec", "cleansed_text", "xml");

            String workingDir = System.getProperty("user.dir");
            String solrCoreName = "aclRdTecCore";
            Path solrHome = Paths.get(workingDir, "testdata", "solr-testbed");

            AppATTFACLRDTECTest appATTFTest = new AppATTFACLRDTECTest(solrHome.toString(), solrCoreName);
            appATTFTest.indexAndExtract(corpusDir);

            List<JATETerm> terms = appATTFTest.rankAndFilter(solrCoreName);
            appATTFTest.evaluate(terms);

            validate_indexing();

            server.getCoreContainer().shutdown();
        } finally {
            try {
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
