package uk.ac.shef.dcs.jate.app;

import org.apache.log4j.Logger;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.client.solrj.response.QueryResponse;
import org.apache.solr.common.SolrDocumentList;
import org.apache.solr.common.params.ModifiableSolrParams;
import org.apache.solr.core.CoreContainer;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.eval.ATEResultLoader;
import uk.ac.shef.dcs.jate.eval.GSLoader;
import uk.ac.shef.dcs.jate.eval.Scorer;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.test.DebugHelper;
import uk.ac.shef.dcs.jate.util.JATEUtil;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

/**
 * Tests for App* of a set of ATE algorithms & The ACL RD-TEC based benchmarking test based on Embedded Solr.
 * <p>
 * ACL RD-TEC contains almost 11,000 scientific publications. Due to the performance and efficiency reasons,
 * the test and benchmarking tests can only be run manually.
 *
 * @see AppATEACLRDTECTest
 * @see <p>
 * The ACL RD-TEC stands for The ACL Reference Dataset for Terminology Extraction and Classification.
 * @see <a href="http://catalog.elra.info/product_info.php?products_id=1236">European Language Resources Association</a>
 * @see <a href="http://acl-arc.comp.nus.edu.sg/">ACL Anthology Reference Corpus (ACL ARC)</a>
 * @see <a href="http://atmykitchen.info/datasets/acl_rd_tec/">The dataset</a>
 * <p>
 * Raw text files can be downloaded via
 * <a href="Resources from ACL ARC">
 * http://atmykitchen.info/datasets/acl_rd_tec/external_resource/index_external_resource.htm</a>
 * <p>
 * For more details about ACL RD-TEC corpus, please refer to the paper:
 * <p>
 * Zadeh, B. Q., & Handschuh, S. (2014).
 * The ACL RD-TEC: a dataset for benchmarking terminology extraction and classification in computational linguistics.
 * In COLING (pp. 52-63).
 */
public abstract class ACLRDTECTest {

    private static Logger LOG = Logger.getLogger(ACLRDTECTest.class.getName());

    static String workingDir = System.getProperty("user.dir");

    static String solrCoreName = "ACLRDTEC";

    static Path corpusDir = Paths.get(workingDir, "src", "test", "resource", "eval", "ACL_RD-TEC", "corpus", "full","xml");

    static Path solrHome = Paths.get(workingDir, "testdata", "solr-testbed");

    static Path FREQ_GENIC_FILE = Paths.get(workingDir, "testdata","solr-testbed", "ACLRDTEC",
            "conf","bnc_unifrqs.normal");

    static Path allAnnCandidTerms = Paths.get(workingDir, "src", "test", "resource", "eval",
            "ACL_RD-TEC", "terms.txt");

    // The corpus can be downloaded via
    // <a href="http://atmykitchen.info/datasets/acl_rd_tec/cleansed_text/index_cleansed_text.htm">
    // Cleansed Text Files in XML Format</a>
    public static final Path ACL_RD_TEC_CORPUS_ZIPPED_FILE =
            Paths.get(workingDir, "src", "test", "resource", "eval", "ACL_RD-TEC", "corpus", "full","xml.zip");

    static EmbeddedSolrServer server = null;
    static List<String> gsTerms = null;
    JATEProperties jateProp = null;

    public void initialise(String solrHomeDir, String solrCoreName) throws JATEException {
        if (server == null) {
            CoreContainer solrContainer = new CoreContainer(solrHomeDir);
            solrContainer.load();

            server = new EmbeddedSolrServer(solrContainer, solrCoreName);
        }

        gsTerms = GSLoader.loadACLRDTECGSTerms(allAnnCandidTerms, true, true);

        if (gsTerms == null) {
            throw new JATEException("ACLRDTEC CORPUS CONCEPT FILE CANNOT BE LOADED SUCCESSFULLY!");
        }

        jateProp = new JATEProperties();

    }

    /**
     * Corpus indexing and candidate term (at index-time)
     *
     * @param corpusDir, ACL RD-TEC cleansed text xml.zip corpus directory
     * @return List<JATETerm>
     * @throws JATEException
     */
    public void indexAndExtract(Path corpusDir) throws JATEException {
        //File dir = new File(corpusDir);
        List<Path> files = JATEUtil.loadFiles(corpusDir);
        Collections.sort(files);

        LOG.info("indexing and extracting candidates...");
        int count = 0;
        for (Path file : files) {
            try {
                LOG.info(file);
                if (!file.toString().contains(".DS_Store")) {
                    indexJATEDocuments(file, jateProp, false);
                    count++;
                    if (count % 100 == 0)
                        LOG.info("indexing done: " + count + "/" + files.size());
                }
            }catch (NullPointerException e){
                e.printStackTrace();
            }

        }

        try {
            server.commit();
            LOG.info("complete indexing and candidate extraction.");
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    static void indexJATEDocuments(Path file, JATEProperties jateProp, boolean commit) throws JATEException {
        try {
            JATEDocument jateDocument = JATEUtil.loadACLRDTECDocument(new FileInputStream(file.toFile()));

            if(jateDocument.getContent().trim().length()!=0)
                JATEUtil.addNewDoc(server, jateDocument.getId(), jateDocument.getId(), jateDocument.getContent(), jateProp, commit);
        } catch (FileNotFoundException ffe) {
            throw new JATEException(ffe.toString());
        } catch (IOException ioe) {
            throw new JATEException(String.format("failed to index [%s]", file.toString()) + ioe.toString());
        } catch (SolrServerException sse) {
            throw new JATEException(String.format("failed to index [%s] ", file.toString()) + sse.toString());
        }
    }

    public static long validate_indexing() {
        ModifiableSolrParams params = new ModifiableSolrParams();
        params.set("q", "*:*");

        try {
            QueryResponse qResp = server.query(params);
            SolrDocumentList docList = qResp.getResults();

            long numDocs = docList.getNumFound();
            LOG.info(String.format("[%s] documents processed!", numDocs));
            return numDocs;
        } catch (SolrServerException e) {
            e.printStackTrace();
        } catch (IOException ioe) {
            ioe.printStackTrace();
        }
        return 0;
    }

    abstract List<JATETerm> rankAndFilter(EmbeddedSolrServer server, String solrCoreName, JATEProperties jateProp) throws JATEException;

    public void evaluate(List<JATETerm> jateTerms, String algorithmName) throws JATEException {
        LOG.info(String.format("evaluating %s ...", algorithmName));
        List<String> rankedTerms = ATEResultLoader.load(jateTerms);
        double[] scores = Scorer.computePrecisionAtRank(gsTerms, rankedTerms,
                true, false, true,
                2, 100, 1, 10,
                50, 100, 500, 1000, 3000, 5000, 8000, 10000);

        double recall = Scorer.recall(gsTerms, rankedTerms);

        //>>>>>>
        /*Set<String> copy = new HashSet<>(gsTerms);
        copy.retainAll(new HashSet<>(rankedTerms));
        Set<String> gsCopy = new HashSet<>(gsTerms);

        gsCopy.removeAll(copy);
        List<String> sorted = new ArrayList<>(gsCopy);
        Collections.sort(sorted);
        DebugHelper.writeList(sorted, "missed.txt");
        System.exit(1);*/
        //>>>>>>
        assert 0.34 == recall;

        LOG.info(String.format("=============%s ACL RD-TEC Benchmarking Results==================", algorithmName));
        validate_indexing();
        LOG.info("  top 50 Precision:" + scores[0]);
        LOG.info("  top 100 Precision:" + scores[1]);
        LOG.info("  top 500 Precision:" + scores[2]);
        LOG.info("  top 1000 Precision:" + scores[3]);
        LOG.info("  top 3000 Precision:" + scores[4]);
        LOG.info("  top 5000 Precision:" + scores[5]);
        LOG.info("  top 8000 Precision:" + scores[6]);
        LOG.info("  top 10000 Precision:" + scores[7]);
        LOG.info("  overall recall:" + recall);
    }

    /**
     * Using a set of heuristics, sections such as ‘bibliography’ and ‘acknowledgements’ are removed from the corpus
     * and are organized in separate files. In addition, text cleaning is performed, e.g. broken words and text
     * segments are joined, footnotes and captions are removed and sections are organised into paragraphs.
     *
     * @return List<JATEDocument>, documents unzipped and loaded from ACL RD TEC xml.zip
     * @throws JATEException
     */
    protected static List<JATEDocument> loadCorpus() throws JATEException {
        List<JATEDocument> jateDocuments = new ArrayList<>();

        try {
            ZipFile aclCorpus = null;

            aclCorpus = new ZipFile(ACL_RD_TEC_CORPUS_ZIPPED_FILE.toFile());

            ZipInputStream zipIn = new ZipInputStream(new FileInputStream(ACL_RD_TEC_CORPUS_ZIPPED_FILE.toFile()));
            ZipEntry entry = zipIn.getNextEntry();

            // iterates over entries in the zip file
            while (entry != null) {
                if (!entry.isDirectory()) {
                    InputStream docInputStream = aclCorpus.getInputStream(entry);
                    JATEDocument jateDocument;
                    jateDocument = JATEUtil.loadACLRDTECDocument(docInputStream);
                    jateDocuments.add(jateDocument);

                    docInputStream.close();

                    zipIn.closeEntry();
                    entry = zipIn.getNextEntry();
                }
                zipIn.close();
            }
        } catch (IOException e) {
            throw new JATEException(ACL_RD_TEC_CORPUS_ZIPPED_FILE.toString() + " not found!");
        }

        LOG.info("number of jate Documents:" + jateDocuments.size());

        return jateDocuments;
    }
}
