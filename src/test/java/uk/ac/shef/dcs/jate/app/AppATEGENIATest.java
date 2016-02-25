package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
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
            "eval", "GENIA", "terms.txt");

    JATEProperties jateProperties = null;

    List<String> gsTerms;
    Map<String, String> initParams = null;


    public static String SOLR_CORE_NAME = "GENIA";

    protected void setSolrCoreName() {
        solrCoreName = "GENIA";
    }

    protected void setReindex() {
        //change this to false if you want to use existing index
        //always set to true for the automatic test
        reindex = false;
    }


    @Before
    public void setup() throws Exception {
        super.setup();
        jateProperties = new JATEProperties();

        if (reindex) {
            try {
                indexCorpus(loadGENIACorpus());
            } catch (IOException ioe) {
                throw new JATEException("Unable to delete index data. Please clean index directory " +
                        "[testdata/solr-testbed/jate/data] manually!");
            }
        }

        gsTerms = GSLoader.loadGenia(GENIA_CORPUS_CONCEPT_FILE.toFile(), true, true);

        if (gsTerms == null) {
            throw new JATEException("GENIA CORPUS CONCEPT FILE CANNOT BE LOADED SUCCESSFULLY!");
        }
        initParams = new HashMap<>();

        initParams.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(), "2");
        initParams.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(), "0.99999");
        LOG.info("<<TEST BEGINS WITH pre-filter.minimum total term frequency=2>>");
    }

    protected List<JATEDocument> loadGENIACorpus() throws JATEException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = null;
        Metadata metadata = new Metadata();

        List<JATEDocument> corpus = new ArrayList<>();

        try {
            ZipFile geniaCorpus = new ZipFile(GENIA_CORPUS_ZIPPED_FILE.toFile());
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
                }
            }
        } catch (IOException e) {
            throw new JATEException(String.format("GENIA Corpus not found from %s", GENIA_CORPUS_ZIPPED_FILE));
        }

        return corpus;
    }

    protected void indexCorpus(List<JATEDocument> corpus) throws IOException, SolrServerException {
        int count = 0;
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
        server.commit();
    }

    //@Test
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

    //@Test
    public void benchmarking_appATTF() throws JATEException, IOException {
        AppATTF appATTF = new AppATTF(initParams);
        List<JATETerm> termList = appATTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 200, 1, 10,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.82 == scores[0];
        assert 0.83 == scores[1];
        assert 0.77 == scores[2];
        assert 0.77 == scores[3];
        assert 0.68 == scores[4];
        assert 0.71 == scores[5];
        assert 0.65 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============ATTF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    //@Test
    public void benchmarking_appChiSquare() throws IOException, JATEException {
        initParams.put(AppParams.CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE.getParamKey(), "0.3");
        AppChiSquare appChiSquare = new AppChiSquare(initParams);
        List<JATETerm> termList = appChiSquare.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());
        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.94 == scores[0];
        assert 0.83 == scores[1];
        assert 0.77 == scores[2];
        assert 0.76 == scores[3];
        assert 0.7 == scores[4];
        assert 0.67 == scores[5];
        assert 0.64 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============CHISQUARE GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appCValue() throws IOException, JATEException {
        AppCValue appCValue = new AppCValue(initParams);
        List<JATETerm> termList = appCValue.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.94 == scores[0];
        assert 0.88 == scores[1];
        assert 0.84 == scores[2];
        assert 0.81 == scores[3];
        assert 0.73 == scores[4];
        assert 0.65 == scores[5];
        assert 0.64 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============CVALUE GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    //@Test
    public void benchmarking_appGlossEx() throws JATEException, IOException {
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), REF_FREQ_FILE.toString());
        AppGlossEx appGlossEx = new AppGlossEx(initParams);

        List<JATETerm> termList = appGlossEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        LOG.info("termList.size():" + termList.size());
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.94 == scores[0];
        assert 0.83 == scores[1];
        assert 0.71 == scores[2];
        assert 0.7 == scores[3];
        assert 0.7 == scores[4];
        assert 0.7 == scores[5];
        assert 0.68 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============GLOSSEX GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    //@Test
    public void benchmarking_appRAKE() throws JATEException, IOException {
        AppRAKE appRAKE = new AppRAKE(initParams);
        List<JATETerm> termList = appRAKE.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);

        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.72 == scores[0];
        assert 0.74 == scores[1];
        assert 0.69 == scores[2];
        assert 0.68 == scores[3];
        assert 0.67 == scores[4];
        assert 0.62 == scores[5];
        assert 0.63 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============RAKE GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    //@Test
    public void benchmarking_appRIDF() throws JATEException, IOException {
        AppRIDF appRIDF = new AppRIDF(initParams);
        List<JATETerm> termList = appRIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.86 == scores[0];
        assert 0.89 == scores[1];
        assert 0.85 == scores[2];
        assert 0.82 == scores[3];
        assert 0.78 == scores[4];
        assert 0.71 == scores[5];
        assert 0.65 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============RIDF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    //@Test
    public void benchmarking_appTermEx() throws JATEException, IOException {
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), REF_FREQ_FILE.toString());
        AppTermEx appTermEx = new AppTermEx(initParams);

        List<JATETerm> termList = appTermEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        //LOG.info("termList.size():"+termList.size());
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.88 == scores[0];
        assert 0.9 == scores[1];
        assert 0.84 == scores[2];
        assert 0.85 == scores[3];
        assert 0.83 == scores[4];
        assert 0.81 == scores[5];
        assert 0.72 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============TERMEX GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + 0);
    }

    //@Test
    public void benchmarking_appTFIDF() throws JATEException, IOException {
        AppTFIDF appTFIDF = new AppTFIDF(initParams);

        List<JATETerm> termList = appTFIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        //LOG.info("termList.size():"+termList.size());
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.92 == scores[0];
        assert 0.84 == scores[1];
        assert 0.81 == scores[2];
        assert 0.81 == scores[3];
        assert 0.76 == scores[4];
        assert 0.72 == scores[5];
        assert 0.65 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============TFIDF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    //@Test
    public void benchmarking_appTTF() throws JATEException, IOException {
        AppTTF appTTF = new AppTTF(initParams);

        List<JATETerm> termList = appTTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("termList.size():" + termList.size());
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.92 == scores[0];
        assert 0.82 == scores[1];
        assert 0.8 == scores[2];
        assert 0.8 == scores[3];
        assert 0.75 == scores[4];
        assert 0.7 == scores[5];
        assert 0.64 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============TTF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);
    }

    //@Test
    public void benchmarking_appWeirdness() throws JATEException, IOException {
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), REF_FREQ_FILE.toString());
        AppWeirdness appWeirdness = new AppWeirdness(initParams);

        List<JATETerm> termList = appWeirdness.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        // the results depends on specified PoS patterns
        // refer to genia.patterns in solr config for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        LOG.info("termList.size():" + termList.size());
        Assert.assertEquals("Candidate size should be 9628.", 9628, termList.size());


        List<String> rankedTerms = ATEResultLoader.load(termList);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms, true, false, true,
                2, 100, 1, 5,
                50, 100, 500, 1000, 3000, 5000, 8000);
        assert 0.86 == scores[0];
        assert 0.89 == scores[1];
        assert 0.89 == scores[2];
        assert 0.88 == scores[3];
        assert 0.81 == scores[4];
        assert 0.74 == scores[5];
        assert 0.68 == scores[6];
        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.16 == recall;

        LOG.info("=============WEIRDNESS GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  overall recall:" + recall);

    }

}
