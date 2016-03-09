package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.common.SolrException;
import org.junit.Assert;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * To run on external/remote Solr server, it needs jate-2.0Alpha-SNAPSHOT-jar-with-dependencies.jar
 * and lib\dragontool.jar
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
     * @param args, true or false for indexing
     * @throws JATEException
     */
    public static void main(String[] args) throws JATEException {
        try {

            AppATEACLRDTECTest appATETest = new AppATEACLRDTECTest(solrHome.toString(), solrCoreName);

            boolean reindex = true;
            if (args.length > 0) {
                try {
                    reindex = Boolean.valueOf(args[0]);
                } catch (Exception e) {
                    throw new JATEException(e);
                }
            }

            long numOfDocs = validate_indexing();
            LOG.info("start to indexing and candidate extraction...");
            if (numOfDocs == 0 || reindex) {
                long startTime = System.currentTimeMillis();
                appATETest.indexAndExtract(corpusDir);
                long endTime = System.currentTimeMillis();
                LOG.info(String.format("Indexing and Candidate Extraction took [%s] milliseconds", (endTime - startTime)));
                /*try {
                    server.getCoreContainer().getCore(solrCoreName).close();
                    server.getCoreContainer().shutdown();
                    server.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
                System.exit(0);*/
            }
            LOG.info("complete indexing and candidate extraction.");
//            System.exit(0);

            List<JATETerm> terms = null;

            AppATTFTest appATTFTest = new AppATTFTest();
            terms = appATTFTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appATTFTest.evaluate(terms, AppATTF.class.getSimpleName());

            AppChiSquareTest appChiSquareTest = new AppChiSquareTest();
            terms = appChiSquareTest.rankAndFilter(server, solrCoreName, appATETest.jateProp);
            appChiSquareTest.evaluate(terms, AppChiSquare.class.getSimpleName());

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
            } catch (SolrException solrEx) {
                solrEx.printStackTrace();
            } finally {
                System.exit(0);
            }
        }

        unlock();

        System.exit(0);
    }

    private static void unlock() {
        File lock = Paths.get(solrHome.toString(), solrCoreName, "data", "index", "write.lock").toFile();
        if (lock.exists()) {
            System.err.println("Previous solr did not shut down cleanly. Unlock it ...");
            Assert.assertTrue(lock.delete());
        }
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

        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppATTF appATTF = new AppATTF(initParam);
        long startTime = System.currentTimeMillis();
        terms = appATTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppATTF ranking took [%s] milliseconds", (endTime - startTime)));
        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appATTF.outputFile = "attf_acltdtec.json";
            appATTF.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
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
        initParam.put(AppParams.PREFILTER_MIN_TERM_CONTEXT_FREQUENCY.getParamKey(), "2");

        initParam.put(AppParams.CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE.getParamKey(), "0.1");

        AppChiSquare appChiSquare = new AppChiSquare(initParam);
        long startTime = System.currentTimeMillis();
        terms = appChiSquare.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppChiSquare ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appChiSquare.outputFile = "chi_square_acltdtec.json";
            appChiSquare.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
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
        long startTime = System.currentTimeMillis();
        terms = appCValue.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppCValue ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appCValue.outputFile = "cvalue_acltdtec.json";
            appCValue.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
        return terms;
    }
}

class AppGlossExTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppGlossExTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppGlossEx ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();

        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        initParam.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), FREQ_GENIC_FILE.toString());
        AppGlossEx appGlossEx = new AppGlossEx(initParam);

        long startTime = System.currentTimeMillis();
        terms = appGlossEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appGlossEx ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appGlossEx.outputFile = "glossEx_acltdtec.json";
            appGlossEx.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }

        return terms;
    }
}

class AppRAKETest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppRAKETest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppRAKE ranking and filtering ... ");
        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppRAKE appRAKE = new AppRAKE(initParam);
        long startTime = System.currentTimeMillis();
        terms = appRAKE.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appRAKE ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appRAKE.outputFile = "rake_acltdtec.json";
            appRAKE.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
        return terms;
    }
}

class AppRIDFTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppRIDFTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppRIDF ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppRIDF appRIDF = new AppRIDF(initParam);
        long startTime = System.currentTimeMillis();
        terms = appRIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppRIDF ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appRIDF.outputFile = "ridf_acltdtec.json";
            appRIDF.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }

        return terms;
    }
}

class AppTermExTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppTermExTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppTermEx ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        initParam.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), FREQ_GENIC_FILE.toString());

        AppTermEx appTermEx = new AppTermEx(initParam);
        long startTime = System.currentTimeMillis();
        terms = appTermEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppTermEx ranking took [%s] milliseconds", (endTime - startTime)));
        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appTermEx.outputFile = "termEx_acltdtec.json";
            appTermEx.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
        return terms;
    }
}

class AppTFIDFTest extends ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(AppTFIDFTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppTFIDF ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppTFIDF appTFIDF = new AppTFIDF(initParam);
        long startTime = System.currentTimeMillis();
        terms = appTFIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppTFIDF ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appTFIDF.outputFile = "tfidf_acltdtec.json";
            appTFIDF.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
        return terms;
    }
}

class AppTTFTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppTTFTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppTTF ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        AppTTF appTTF = new AppTTF(initParam);
        long startTime = System.currentTimeMillis();
        terms = appTTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppTTF ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appTTF.outputFile = "ttf_acltdtec.json";
            appTTF.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
        return terms;
    }
}

class AppWeirdnessTest extends ACLRDTECTest {
    private static Logger LOG = Logger.getLogger(AppWeirdnessTest.class.getName());

    @Override
    List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp)
            throws JATEException {
        LOG.info("AppWeirdness ranking and filtering ... ");

        List<JATETerm> terms = new ArrayList<>();
        Map<String, String> initParam = new HashMap<>();
        initParam.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParam.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");

        initParam.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), FREQ_GENIC_FILE.toString());

        AppWeirdness appWeirdness = new AppWeirdness(initParam);
        long startTime = System.currentTimeMillis();
        terms = appWeirdness.extract(server.getCoreContainer().getCore(solrCoreName), jateProp);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("AppWeirdness ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("complete ranking and filtering.");

        LOG.info("Export results for evaluation ...");
        try {
            appWeirdness.outputFile = "weirdness_acltdtec.json";
            appWeirdness.write(terms);
        } catch (IOException e) {
            throw new JATEException("Fail to export results.");
        }
        return terms;
    }
}