package uk.ac.shef.dcs.jate.app;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AppChiSquareACLRDTECTest extends ACLRDTECTest {

    public AppChiSquareACLRDTECTest(String solrHomeDir, String solrCoreName) throws JATEException {
        super(solrHomeDir, solrCoreName);
    }

    @Override
    List<JATETerm> rankAndFilter(String solrCoreName) throws JATEException {
        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> params = new HashMap<>();
        params.put("-pf.mttf","2");
        params.put("-cf.kp","0.99999");
        params.put("-ft","0.3");
        AppChiSquare appChiSquare = new AppChiSquare(params);
        try {
            terms = appChiSquare.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        } catch (IOException e) {
            e.printStackTrace();
        }

        return terms;
    }


    public static void main(String[] args) throws JATEException {
        try {
            Path corpusDir = Paths.get(workingDir, "src", "test", "resource", "eval", "acl_rd_tec", "cleansed_text", "xml");

            String workingDir = System.getProperty("user.dir");
            String solrCoreName = "aclRdTecCore";
            Path solrHome = Paths.get(workingDir, "testdata", "solr-testbed");

            AppChiSquareACLRDTECTest appChiSquareTest = new AppChiSquareACLRDTECTest(solrHome.toString(), solrCoreName);
            appChiSquareTest.indexAndExtract(corpusDir);

            List<JATETerm> terms = appChiSquareTest.rankAndFilter(solrCoreName);
            appChiSquareTest.evaluate(terms);

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
