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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.xml.sax.SAXException;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.eval.ATEResultLoader;
import uk.ac.shef.dcs.jate.eval.GSLoader;
import uk.ac.shef.dcs.jate.eval.Scorer;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.*;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * Unit tests for App* of a set of ATE algorithms & GENIA based benchmarking test based on Embedded Solr
 *
 * Default Solr config is retrieved through testdata/solr-testbed
 * Default jate.properties is loaded from classpath
 * Enable PoS pattern based candidate extraction in Solr (see field type "jate_text_2_terms" for reference)
 *
 */
public class AppATETest extends BaseEmbeddedSolrTest {
    private static Logger LOG = Logger.getLogger(AppATETest.class.getName());

    String workingDir = System.getProperty("user.dir");

    public static final String GENIA_CORPUS_ZIPPED_FILE = System.getProperty("user.dir") +
            "\\src\\test\\resource\\eval\\GENIAcorpus-files.zip";
    public static final String GENIA_CORPUS_CONCEPT_FILE = System.getProperty("user.dir") +
            "\\src\\test\\resource\\eval\\GENIAcorpus-concept.txt";

    JATEProperties jateProperties = null;

    List<String> gsTerms;

    @Before
    public void setup() throws Exception {
        super.setup();

        jateProperties = new JATEProperties();
        jateProperties.setSolrHome(this.solrHome);
        jateProperties.setSolrCoreName(this.solrCoreName);

        indexCorpus(loadGENIACorpus());

        gsTerms = GSLoader.loadGenia(GENIA_CORPUS_CONCEPT_FILE, true, true);

        if (gsTerms == null) {
            throw new JATEException("GENIA CORPUS CONCEPT FILE CANNOT BE LOADED SUCCESSFULLY!");
        }
    }

    protected List<JATEDocument> loadGENIACorpus() throws JATEException {
        AutoDetectParser parser = new AutoDetectParser();
        BodyContentHandler handler = null;
        Metadata metadata = new Metadata();

        List<JATEDocument> corpus = new ArrayList<JATEDocument>();

        try {
            ZipFile geniaCorpus = new ZipFile(GENIA_CORPUS_ZIPPED_FILE);
            Enumeration<? extends ZipEntry> entries = geniaCorpus.entries();
            while (entries.hasMoreElements()) {
                ZipEntry entry = entries.nextElement();
                String fileName = entry.getName();

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
        corpus.forEach(doc -> {
            try {
                super.addNewDoc(doc.getId(), doc.getId(), doc.getContent(), jateProperties, false);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (SolrServerException e) {
                e.printStackTrace();
            } catch (JATEException jateEx) {
                jateEx.printStackTrace();
                LOG.error(String.format("failed to index document. Please check JATE properties " +
                                "for current setting for [%s] and [%s]", JATEProperties.PROPERTY_SOLR_FIELD_CONTENT_NGRAMS,
                        JATEProperties.PROPERTY_SOLR_FIELD_CONTENT_TERMS));
            }
        });
        server.commit();
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
        AppATTF appATTF = new AppATTF();
        List<JATETerm> termList = appATTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // the results depends on specified PoS patterns
        // refer to genia.patterns for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.86 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.85 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.78 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.79 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.67 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.7 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.56 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.5 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============ATTF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appChiSquare() throws IOException, JATEException {
        AppChiSquare appChiSquare = new AppChiSquare();
        List<JATETerm> termList = appChiSquare.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // refer to genia.patterns for the default candidate extraction patterns
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());
        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.8 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.71 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.51 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.5 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.49 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.48 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.45 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.44 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============ChiSquare GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appCValue() throws IOException, JATEException {
        AppCValue appCValue = new AppCValue();
        List<JATETerm> termList = appCValue.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // refer to genia.patterns for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.92 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.89 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.83 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.8 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.71 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.62 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.5 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.41 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============AppCValue GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appGlossEx() throws JATEException, IOException {
        Map<String, String> initParams = new HashMap<>();
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), workingDir+"\\src\\main\\resource\\bnc_unifrqs.normal");
        AppGlossEx appGlossEx = new AppGlossEx(initParams);

        List<JATETerm> termList = appGlossEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        LOG.info("termList.size():"+termList.size());
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.6 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.63 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.48 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.43 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.35 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.33 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.35 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.35 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============GlossEx GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appRAKE() throws JATEException, IOException {
        AppRAKE appRAKE = new AppRAKE();
        List<JATETerm> termList = appRAKE.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // refer to genia.patterns for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.04 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.02 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.03 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.03 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.04 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.05 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.5 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.07 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============AppRAKE GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appRIDF() throws JATEException, IOException {
        AppRIDF appRIDF = new AppRIDF();
        List<JATETerm> termList = appRIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        assert termList != null;
        // refer to genia.patterns for the default candidate extraction patterns
        // candidate extraction is performed at index-time
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.92 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.92 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.87 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.83 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.78 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.7 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.64 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.6 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============AppRIDF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appTermEx() throws JATEException, IOException {
        Map<String, String> initParams = new HashMap<>();
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), workingDir+"\\src\\main\\resource\\bnc_unifrqs.normal");
        AppTermEx appTermEx = new AppTermEx(initParams);

        List<JATETerm> termList = appTermEx.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        LOG.info("termList.size():"+termList.size());
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.32 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.41 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.29 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.28 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.29 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.29 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.32 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.32 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============AppTermEx GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appTFIDF()throws JATEException, IOException {
        AppTFIDF appTFIDF = new AppTFIDF();

        List<JATETerm> termList = appTFIDF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);
        LOG.info("termList.size():"+termList.size());
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.96 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.91 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.81 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.81 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.76 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.7 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.64 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.6 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============TFIDF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appTTF() throws JATEException, IOException {
        AppTTF appTTF = new AppTTF();

        List<JATETerm> termList = appTTF.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        LOG.info("termList.size():"+termList.size());
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.94 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.88 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.81 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.8 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.75 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.69 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.63 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.6 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============TTF GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @Test
    public void benchmarking_appWeirdness()throws JATEException, IOException {
        Map<String, String> initParams = new HashMap<>();
        initParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), workingDir+"\\src\\main\\resource\\bnc_unifrqs.normal");
        AppWeirdness appWeirdness = new AppWeirdness(initParams);

        List<JATETerm> termList = appWeirdness.extract(server.getCoreContainer().getCore(solrCoreName), jateProperties);

        LOG.info("termList.size():"+termList.size());
        Assert.assertEquals("Candidate size should be 39367.", 39367, termList.size());

        List<String> rankedTerms = ATEResultLoader.load(termList);
        double top50Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 50);
        assert 0.74 == top50Precision;

        double top100Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 100);
        assert 0.7 == top100Precision;

        double top500Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 500);
        assert 0.66 == top500Precision;

        double top1000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 1000);
        assert 0.65 == top1000Precision;

        double top3000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 3000);
        assert 0.63 == top3000Precision;

        double top5000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 5000);
        assert 0.63 == top5000Precision;

        double top8000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 8000);
        assert 0.61 == top8000Precision;

        double top10000Precision = Scorer.computePrecisionWithNormalisation(gsTerms, rankedTerms, true, false, true, 10000);
        assert 0.59 == top10000Precision;

        double recall = Scorer.recall(gsTerms, rankedTerms);
        assert 0.34 == recall;

        LOG.info("=============appWeirdness GENIA Benchmarking Results==================");

        LOG.info("  top 50 Precision:" + top50Precision);
        LOG.info("  top 100 Precision:" + top100Precision);
        LOG.info("  top 500 Precision:" + top500Precision);
        LOG.info("  top 1000 Precision:" + top1000Precision);
        LOG.info("  top 3000 Precision:" + top3000Precision);
        LOG.info("  top 5000 Precision:" + top5000Precision);
        LOG.info("  top 8000 Precision:" + top8000Precision);
        LOG.info("  top 10000 Precision:" + top10000Precision);
        LOG.info("  overall recall:" + recall);
    }

    @After
    public void tearDown() throws IOException {
        super.tearDown();
    }
}
