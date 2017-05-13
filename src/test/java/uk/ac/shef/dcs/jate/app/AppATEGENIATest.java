package uk.ac.shef.dcs.jate.app;

import dragon.nlp.tool.lemmatiser.EngLemmatiser;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.spi.RootLogger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.common.util.Pair;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.junit.*;
import org.xml.sax.SAXException;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.eval.ATEResultLoader;
import uk.ac.shef.dcs.jate.eval.GSLoader;
import uk.ac.shef.dcs.jate.eval.Scorer;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.nlp.Lemmatiser;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Unit tests for App* of a set of ATE algorithms & GENIA based benchmarking test based on Embedded Solr
 * <p>
 * Default Solr config is retrieved through testdata/solr-testbed
 * Default jate.properties is loaded from classpath
 * Enable PoS pattern based candidate extraction in Solr (see field type "jate_text_2_terms" for reference)
 */
public class AppATEGENIATest extends BaseEmbeddedSolrTest {
    private static Logger LOG = Logger.getLogger(AppATEGENIATest.class.getName());

    public static final Path GENIA_CORPUS_ZIPPED_FILE = Paths.get(workingDir, "src", "test", "resource",
            "eval", "GENIA", "corpus.zip");

    public static final Path GENIA_CORPUS_CONCEPT_FILE = Paths.get(workingDir, "src", "test", "resource",
            "eval", "GENIA", "concept.txt");

    public static final int EXPECTED_CANDIDATE_SIZE=12834;
    static Lemmatiser lemmatiser = new Lemmatiser(new EngLemmatiser(
            Paths.get(workingDir, "src", "test", "resource", "lemmatiser").toString(), false, false
    ));

    JATEProperties jateProperties = null;

    List<String> gsTerms;
    Map<String, String> initParams = null;
    private static boolean isIndexed = false;


    public static String SOLR_CORE_NAME = "GENIA";

    protected void setSolrCoreName() {
        solrCoreName = "GENIA";
    }

    protected void setReindex() {
        //change this to false if you want to use existing index
        //always set to true for the automatic test
        reindex = false;
    }

    @BeforeClass
    public static void cleanData() {
        try {
            cleanIndexDirectory(solrHome.toString(), SOLR_CORE_NAME);
            cleanIndexDirectory(solrHome.toString(), "ACLRDTEC");
            cleanIndexDirectory(solrHome.toString(), "jateCore");
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Before
    public void setup() throws Exception {
        super.setup();
        jateProperties = new JATEProperties();
        System.out.println("======================================== is Indexed ? : "+ isIndexed);
        if (!isIndexed || reindex) {
            try {
                indexCorpus(loadGENIACorpus());
            } catch (IOException ioe) {
                throw new JATEException("Unable to delete index data. Please clean index directory " +
                        "[testdata/solr-testbed/jate/data] manually!");
            }
        }

        gsTerms = GSLoader.loadGenia(GENIA_CORPUS_CONCEPT_FILE.toString());

        if (gsTerms == null) {
            throw new JATEException("GENIA CORPUS_DIR CONCEPT FILE CANNOT BE LOADED SUCCESSFULLY!");
        }
        initParams = new HashMap<>();

        initParams.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParams.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");
        LOG.info("<<TEST BEGINS WITH pre-filter.minimum total term frequency=2>>");
    }

    @AfterClass
    public static void tearDownAll() {
        try {
            cleanIndexDirectory(solrHome.toString(), SOLR_CORE_NAME);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    protected List<JATEDocument> loadGENIACorpus() throws JATEException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = null;
        Metadata metadata = new Metadata();

        List<JATEDocument> corpus = new ArrayList<>();
        ZipFile geniaCorpus = null;
        try {
        	geniaCorpus = new ZipFile(GENIA_CORPUS_ZIPPED_FILE.toFile());
            Enumeration<? extends ZipEntry> entries = geniaCorpus.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = entry.getName();
                //skip file in MAC OS
                if (entry.isDirectory() || fileName.startsWith("__MACOSX/") || fileName.contains(".DS_Store"))
                    continue;

                InputStream stream = geniaCorpus.getInputStream(entry);
                handler = new BodyContentHandler(-1);
                try {
                    parser.parse(stream, handler, metadata);
                    String content = handler.toString();
                    JATEDocument doc = new JATEDocument(fileName);
                    doc.setContent(content);
                    corpus.add(doc);
                } catch (SAXException e) {
                    e.printStackTrace();
                } catch (TikaException e) {
                    e.printStackTrace();
                } finally {
                	stream.close();
                }
            }
        } catch (IOException e) {
            throw new JATEException(String.format("GENIA Corpus not found from %s", GENIA_CORPUS_ZIPPED_FILE));
        } finally {
        	if (geniaCorpus != null) {
        		try {
					geniaCorpus.close();
				} catch (IOException e) {
					LOG.error(e.toString());
				}
        	}
        }

        return corpus;
    }

    protected void indexCorpus(List<JATEDocument> corpus) throws IOException, SolrServerException {
        int count = 0;
        long startTime = System.currentTimeMillis();
        for (JATEDocument doc : corpus) {
            try {
                count++;
                super.addNewDoc(doc.getId(), doc.getId(), doc.getContent(), jateProperties, false);
                if (count % 500 == 0) {
                    LOG.info(String.format("%s documents indexed.", count));
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SolrServerException e) {
                e.printStackTrace();
            } catch (JATEException jateEx) {
                jateEx.printStackTrace();
                LOG.warn(String.format("failed to index document. Please check JATE properties " +
                                "for current setting for [%s] and [%s]", JATEProperties.PROPERTY_SOLR_FIELD_CONTENT_NGRAMS,
                        JATEProperties.PROPERTY_SOLR_FIELD_CONTENT_TERMS));
            }
        }
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("Indexing and candidate extraction took [%s] milliseconds", (endTime - startTime)));
        server.commit();
        isIndexed = true;
    }

    @Test
    public void validate_indexing() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", "*:*");

        try {
            QueryResponse qResp = server.query(params);
            SolrDocumentList docList = qResp.getResults();

            assert (docList.getNumFound() == 2000);
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
    }

    @Test
    public void benchmarking_appATTF() throws JATEException, IOException {
        AppATTF appATTF = new AppATTF(initParams);
        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appATTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appATTF ranking took [%s] milliseconds", (endTime - startTime)));

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 200, 1, 10,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);

        LOG.info("=============ATTF GENIA Benchmarking Results==================");
        double recall = Scorer.recall(gsTerms, rankedTerms);
        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.84 == scores[0];
        assert 0.83 == scores[1];
        assert 0.78 == scores[2];
        assert 0.74 == scores[3];
        assert 0.73 == scores[4];
        assert 0.71 == scores[5];
        assert 0.7 == scores[6];
        assert 0.72 == scores[7];
        assert 0.64 == scores[8];
        assert 0.58 == scores[9];
        assert 0.55 == scores[10];
        assert 0.54 == scores[11];
        assert 0.56 == scores[12];
        assert 0.57 == scores[13];
        assert 0.58 == scores[14];
        assert 0.58 == scores[15];
        assert 0.12 == recall;
    }

    @Test
    public void benchmarking_appChiSquare() throws IOException, JATEException {
        initParams.put(AppParams.PREFILTER_MIN_TERM_CONTEXT_FREQUENCY.getParamKey(),"2");
        initParams.put(AppParams.CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE.getParamKey(), "0.1");
        AppChiSquare appChiSquare = new AppChiSquare(initParams);

        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appChiSquare.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appChiSquare ranking took [%s] milliseconds", (endTime - startTime)));

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+12544, 12544, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);

        LOG.info("=============CHISQUARE GENIA Benchmarking Results==================");
        double recall = Scorer.recall(gsTerms, rankedTerms);
        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.66 == scores[0];
        assert 0.67 == scores[1];
        assert 0.71 == scores[2];
        assert 0.7 == scores[3];
        assert 0.69 == scores[4];
        assert 0.69 == scores[5];
        assert 0.68 == scores[6];
        assert 0.67 == scores[7];
        assert 0.66 == scores[8];
        assert 0.65 == scores[9];
        assert 0.64 == scores[10];
        assert 0.63== scores[11];
        assert 0.62 == scores[12];
        assert 0.61 == scores[13];
        assert 0.6 == scores[14];
        assert 0.59 == scores[15];
        assert 0.12 == recall;
    }

    @Test
    public void benchmarking_appCValue() throws IOException, JATEException {
        AppCValue appCValue = new AppCValue(initParams);
        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appCValue.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appCValue ranking took [%s] milliseconds", (endTime - startTime)));

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);
        double[] scores=result.getKey();

        LOG.info("=============CVALUE GENIA Benchmarking Results==================");
        double recall = Scorer.recall(gsTerms, rankedTerms);
        printResults(scores, recall);

        assert 0.8 == scores[0];
        assert 0.81 == scores[1];
        assert 0.81 == scores[2];
        assert 0.79 == scores[3];
        assert 0.78 == scores[4];
        assert 0.77 == scores[5];
        assert 0.76 == scores[6];
        assert 0.75 == scores[7];
        assert 0.71 == scores[8];
        assert 0.68 == scores[9];
        assert 0.63 == scores[10];
        assert 0.56 == scores[11];
        assert 0.56 == scores[12];
        assert 0.56 == scores[13];
        assert 0.57 == scores[14];
        assert 0.57 == scores[15];
        assert 0.12 == recall;
    }

    @Test
    public void benchmarking_appGlossEx() throws JATEException, IOException {
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), REF_FREQ_FILE.toString());
        AppGlossEx appGlossEx = new AppGlossEx(initParams);

        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appGlossEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appGlossEx ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);

        LOG.info("=============GLOSSEX GENIA Benchmarking Results==================");
        double recall = Scorer.recall(gsTerms, rankedTerms);
        double[]scores=result.getKey();
        printResults(scores, recall);

        assert 0.9 == scores[0];
        assert 0.82 == scores[1];
        assert 0.74 == scores[2];
        assert 0.69 == scores[3];
        assert 0.66 == scores[4];
        assert 0.65 == scores[5];
        assert 0.61 == scores[6];
        assert 0.61 == scores[7];
        assert 0.63 == scores[8];
        assert 0.63 == scores[9];
        assert 0.63 == scores[10];
        assert 0.63 == scores[11];
        assert 0.63 == scores[12];
        assert 0.63 == scores[13];
        assert 0.62 == scores[14];
        assert 0.61 == scores[15];

        assert 0.12 == recall;

    }

    private void printResults(double[] scores, double recall) {
        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 300 Precision:" + scores[2]);
        LOG.info("  top 500 Precision:" + scores[3]);
        LOG.info("  top 800 Precision:" + scores[4]);
        LOG.info("  top 1000 Precision:" + scores[5]);
        LOG.info("  top 1500 Precision:" + scores[6]);
        LOG.info("  top 2000 Precision:" + scores[7]);
        LOG.info("  top 3000 Precision:" + scores[8]);
        LOG.info("  top 4000 Precision:" + scores[9]);
        LOG.info("  top 5000 Precision:" + scores[10]);
        LOG.info("  top 6000 Precision:" + scores[11]);
        LOG.info("  top 7000 Precision:" + scores[12]);
        LOG.info("  top 8000 Precision:" + scores[13]);
        LOG.info("  top 9000 Precision:" + scores[14]);
        LOG.info("  top 10000 Precision:" + scores[15]);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appRAKE() throws JATEException, IOException {
        AppRAKE appRAKE = new AppRAKE(initParams);
        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appRAKE.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appRAKE ranking took [%s] milliseconds", (endTime - startTime)));

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);

        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);

        LOG.info("=============RAKE GENIA Benchmarking Results==================");
        double recall = Scorer.recall(gsTerms, rankedTerms);
        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.8 == scores[0];
        assert 0.75 == scores[1];
        assert 0.67 == scores[2];
        assert 0.6 == scores[3];
        assert 0.56 == scores[4];
        assert 0.58== scores[5];
        assert 0.59 == scores[6];
        assert 0.58 == scores[7];
        assert 0.57 == scores[8];
        assert 0.56 == scores[9];
        assert 0.57 == scores[10];
        assert 0.59 == scores[11];
        assert 0.6 == scores[12];
        assert 0.6 == scores[13];
        assert 0.6 == scores[14];
        assert 0.6 == scores[15];
        assert 0.12 == recall;
    }

    @Test
    public void benchmarking_appRIDF() throws JATEException, IOException {
        AppRIDF appRIDF = new AppRIDF(initParams);
        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appRIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appRIDF ranking took [%s] milliseconds", (endTime - startTime)));

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result= Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);
        double recall = Scorer.recall(gsTerms, rankedTerms);
        LOG.info("=============RIDF GENIA Benchmarking Results==================");
        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.8 == scores[0];
        assert 0.78 == scores[1];
        assert 0.79 == scores[2];
        assert 0.79 == scores[3];
        assert 0.79 == scores[4];
        assert 0.79 == scores[5];
        assert 0.78 == scores[6];
        assert 0.78 == scores[7];
        assert 0.74 == scores[8];
        assert 0.73 == scores[9];
        assert 0.69 == scores[10];
        assert 0.65 == scores[11];
        assert 0.61 == scores[12];
        assert 0.6 == scores[13];
        assert 0.6 == scores[14];
        assert 0.59 == scores[15];
        assert 0.12 == recall;


    }

    @Test
    public void benchmarking_appTermEx() throws JATEException, IOException {
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), REF_FREQ_FILE.toString());
        AppTermEx appTermEx = new AppTermEx(initParams);

        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appTermEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appTermEx ranking took [%s] milliseconds", (endTime - startTime)));

        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+19338, 19338, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);
        double recall = Scorer.recall(gsTerms, rankedTerms);

        LOG.info("=============TERMEX GENIA Benchmarking Results==================");
        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.88 == scores[0];
        assert 0.9 == scores[1];
        assert 0.86 == scores[2];
        assert 0.84 == scores[3];
        assert 0.81 == scores[4];
        assert 0.82 == scores[5];
        assert 0.84 == scores[6];
        assert 0.83 == scores[7];
        assert 0.81 == scores[8];
        assert 0.8 == scores[9];
        assert 0.8 == scores[10];
        assert 0.79 == scores[11];
        assert 0.78 == scores[12];
        assert 0.76 == scores[13];
        assert 0.73 == scores[14];
        assert 0.69 == scores[15];

        assert 0.12 == recall;

    }

    @Test
    public void benchmarking_appTFIDF() throws JATEException, IOException {
        AppTFIDF appTFIDF = new AppTFIDF(initParams);

        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appTFIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appTFIDF ranking took [%s] milliseconds", (endTime - startTime)));

        LOG.info("candidate size:" + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);
        double recall = Scorer.recall(gsTerms, rankedTerms);
        LOG.info("=============TFIDF GENIA Benchmarking Results==================");

        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.7 == scores[0];
        assert 0.65 == scores[1];
        assert 0.71 == scores[2];
        assert 0.73 == scores[3];
        assert 0.75 == scores[4];
        assert 0.77 == scores[5];
        assert 0.77 == scores[6];
        assert 0.76 == scores[7];
        assert 0.75 == scores[8];
        assert 0.73 == scores[9];
        assert 0.71 == scores[10];
        assert 0.7 == scores[11];
        assert 0.67 == scores[12];
        assert 0.64 == scores[13];
        assert 0.61 == scores[14];
        assert 0.59 == scores[15];
        assert 0.12 == recall;

    }

    @Test
    public void benchmarking_appTTF() throws JATEException, IOException {
        AppTTF appTTF = new AppTTF(initParams);
        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appTTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appTTF ranking took [%s] milliseconds", (endTime - startTime)));

        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("Candidate size: " + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);
        double recall = Scorer.recall(gsTerms, rankedTerms);
        LOG.info("=============TTF GENIA Benchmarking Results==================");
        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.64 == scores[0];
        assert 0.64 == scores[1];
        assert 0.71 == scores[2];
        assert 0.73 == scores[3];
        assert 0.74 == scores[4];
        assert 0.76 == scores[5];
        assert 0.76 == scores[6];
        assert 0.76 == scores[7];
        assert 0.74 == scores[8];
        assert 0.73 == scores[9];
        assert 0.71 == scores[10];
        assert 0.69 == scores[11];
        assert 0.67 == scores[12];
        assert 0.64 == scores[13];
        assert 0.62 == scores[14];
        assert 0.6 == scores[15];
        assert 0.12 == recall;


    }

    @Test
    public void benchmarking_appWeirdness() throws JATEException, IOException {
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), REF_FREQ_FILE.toString());
        AppWeirdness appWeirdness = new AppWeirdness(initParams);
        long startTime = System.currentTimeMillis();
        List<JATETerm> termList = appWeirdness.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        long endTime = System.currentTimeMillis();
        LOG.info(String.format("appWeirdness ranking took [%s] milliseconds", (endTime - startTime)));

        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("Candidate size: " + termList.size());
        //Assert.assertEquals("Candidate size should be "+EXPECTED_CANDIDATE_SIZE, EXPECTED_CANDIDATE_SIZE, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        Pair<double[], Double> result = Scorer.computePrecisionAtRank(9450,lemmatiser,gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 300, 500, 800, 1000, 1500, 2000, 3000, 4000, 5000, 6000, 7000, 8000,9000,10000);
        double recall = Scorer.recall(gsTerms, rankedTerms);
        LOG.info("=============WEIRDNESS GENIA Benchmarking Results==================");
        double[] scores=result.getKey();
        printResults(scores, recall);

        assert 0.8 == scores[0];
        assert 0.88 == scores[1];
        assert 0.89 == scores[2];
        assert 0.88 == scores[3];
        assert 0.87 == scores[4];
        assert 0.87 == scores[5];
        assert 0.84 == scores[6];
        assert 0.82 == scores[7];
        assert 0.79 == scores[8];
        assert 0.76 == scores[9];
        assert 0.73 == scores[10];
        assert 0.7 == scores[11];
        assert 0.68 == scores[12];
        assert 0.66 == scores[13];
        assert 0.64 == scores[14];
        assert 0.62 == scores[15];
        assert 0.12 == recall;

    }

}
