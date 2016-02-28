package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To run on external/remote Solr server, it needs jate-2.0Alpha-SNAPSHOT-jar-with-dependencies.jar
 */
public class AppATEACLRDTECTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppATEACLRDTECTest.class.getName());

    public AppATEACLRDTECTest(String solrHomeDir, String solrCoreName) throws JATEException, IOException {
        initialise(solrHomeDir, solrCoreName);
    }

    public List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        List<JATETerm> terms = new ArrayList<>();
        return terms;
    }

    /**
     * To run the test class via
     * <p>
     * mvn exec:java -Dexec.mainClass="uk.ac.shef.dcs.jate.app.AppATEACLRDTECTest" -Dexec.classpathScope="test"
     *
     * @param args
     * @throws JATEException
     */
    public static void main(String[] args) throws JATEException {
        try {
            AppATEACLRDTECTest appATETest = new AppATEACLRDTECTest(solrHome.toString(), solrCoreName);

            boolean reindex = false;
            if (args.length > 0) {
                try {
                    reindex = Boolean.valueOf(args[0]);
                } catch (Exception e) {
                }
            }

            long numOfDocs = validate_indexing();
            if (numOfDocs == 0 || reindex) {
                appATETest.indexAndExtract(corpusDir);
                /*try {
                    server.getCoreContainer().getCore(solrCoreName).close();
                    server.getCoreContainer().shutdown();
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);*/
            }


            List<JATETerm> terms = null;

            /*AppATTFTest appATTFTest = new AppATTFTest();
            terms = appATTFTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appATTFTest.evaluate(terms, AppATTF.class.getSimpleName());*/
            //System.exit(0);

            AppChiSquareTest appChiSquareTest = new AppChiSquareTest();
            terms = appChiSquareTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appChiSquareTest.evaluate(terms, AppChiSquare.class.getSimpleName());
            System.exit(0);


            AppCValueTest appCValueTest = new AppCValueTest();
            terms = appCValueTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appCValueTest.evaluate(terms, AppCValue.class.getSimpleName());

            AppGlossExTest appGlossExTest = new AppGlossExTest();
            terms = appGlossExTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appGlossExTest.evaluate(terms, AppGlossEx.class.getSimpleName());
            AppRAKETest appRAKETest = new AppRAKETest();
            terms = appRAKETest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appRAKETest.evaluate(terms, AppRAKE.class.getSimpleName());

            AppRIDFTest appRIDFTest = new AppRIDFTest();
            terms = appRIDFTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appRIDFTest.evaluate(terms, AppRIDF.class.getSimpleName());

            AppTermExTest appTermExTest = new AppTermExTest();
            terms = appTermExTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appTermExTest.evaluate(terms, AppTermEx.class.getSimpleName());

            AppTFIDFTest appTFIDFTest = new AppTFIDFTest();
            terms = appTFIDFTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appTFIDFTest.evaluate(terms, AppTFIDF.class.getSimpleName());

            AppTTFTest appTTFTest = new AppTTFTest();
            terms = appTTFTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appTTFTest.evaluate(terms, AppTTF.class.getSimpleName());

            AppWeirdnessTest appWeirdnessTest = new AppWeirdnessTest();
            terms = appWeirdnessTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appWeirdnessTest.evaluate(terms, AppWeirdness.class.getSimpleName());

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                server.getCoreContainer().getCore(solrCoreName).close();
                server.getCoreContainer().shutdown();
                server.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        System.exit(0);
    }
}

class AppATTFTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppATTFTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppATTF ranking and filtering ... ");
        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();

        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "0");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppATTF appATTF = new AppATTF(initParam);

        terms = appATTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppChiSquareTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppChiSquareTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppChiSquare ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");
        initParam.put(AppParams.PREFILTER_MIN_TERM_CONTEXT_FREQUENCY.getParamKey(), "0");

        initParam.put(AppParams.CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE.getParamKey(), "0.3");

        AppChiSquare appChiSquare = new AppChiSquare(initParam);
        terms = appChiSquare.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppCValueTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppCValueTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppCValue ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppCValue appCValue = new AppCValue(initParam);

        terms = appCValue.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppGlossExTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppGlossExTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        LOG.info("AppGlossEx ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();

        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        initParam.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), FREQ_GENIC_FILE.toString());
        AppGlossEx appGlossEx = new AppGlossEx(initParam);

        terms = appGlossEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppRAKETest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppRAKETest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        LOG.info("AppRAKE ranking and filtering ... ");
        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppRAKE appRAKE = new AppRAKE(initParam);
        terms = appRAKE.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppRIDFTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppRIDFTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        LOG.info("AppRIDF ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppRIDF appRIDF = new AppRIDF(initParam);
        terms = appRIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppTermExTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppTermExTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        LOG.info("AppTermEx ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        initParam.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), FREQ_GENIC_FILE.toString());

        AppTermEx appTermEx = new AppTermEx(initParam);
        terms = appTermEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppTFIDFTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppTFIDFTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        LOG.info("AppTFIDF ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppTFIDF appTFIDF = new AppTFIDF(initParam);
        terms = appTFIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppTTFTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppTTFTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        LOG.info("AppTTF ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppTTF appTTF = new AppTTF(initParam);
        terms = appTTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");
        return terms;
    }
}

class AppWeirdnessTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppWeirdnessTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException {
        LOG.info("AppWeirdness ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        initParam.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), FREQ_GENIC_FILE.toString());

        AppWeirdness appWeirdness = new AppWeirdness(initParam);
        terms = appWeirdness.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);

        LOG.info("complete ranking and filtering.");

        return terms;
    }
}