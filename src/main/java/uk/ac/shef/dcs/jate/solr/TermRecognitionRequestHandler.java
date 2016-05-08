package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.common.util.Pair;
import org.apache.solr.core.SolrCore;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.schema.CopyField;
import org.apache.solr.schema.IndexSchema;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.app.App;
import uk.ac.shef.dcs.jate.app.AppParams;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.SolrUtil;

/**
 * Scans solr indexed and TR aware content field and perform terminology
 * recognition (ranking + filtering + indexing) for whole index.
 * <p>
 * Term candidates are extracted and stored in index-time,
 * which can be triggered by document indexing (e.g., by HTTP POST TOOL)
 * or setting 'extraction' to true (as an option) in this request handler.
 * <p>
 * TR-AWARE SOLR FIELDS MUST ALSO BE CONFIGURED AND INDEXED FOR TERN RANKING ALGORITHMS
 * (SEE EXAMPLES SETTING OF schema.xml IN $JATE_HOME/testdata/solr-testbed).
 * <p>
 * Example configuration in solrconfig.xml
 * <p>
 * 1. configure JATE library (jar file) to solr classpath
 * <p>
 * <lib path=
 * "${solr.install.dir:../../..}/contrib/jate/lib/jate-2.0-*-with-dependencies.jar"/>
 * <p>
 * 2. configure request handler for term recognition and indexing
 * <p>
 * <pre>
 * {@code
 * <requestHandler name="/termRecogniser" class="uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler">
 * <lst name="defaults">
 * <str name="algorithm">CValue</str>
 * <bool name="extraction">false</bool>
 * <bool name="indexTerm">true</bool>
 * <bool name="boosting">false</bool>
 * <str name="-prop"><YOUR_PATH>/resource/jate.properties</str>
 * <float name="-cf.t">0</float>
 * <str name="-o"><YOUR_PATH>/industry_terms.json</str>
 * </lst>
 * </requestHandler>
 * }
 */
public class TermRecognitionRequestHandler extends RequestHandlerBase {
    private final Logger log = LoggerFactory.getLogger(getClass());

    /**
     * Request parameter.
     */
    public static enum Algorithm {
        C_VALUE("CValue"), ATTF("ATTF"), CHI_SQUARE("ChiSquare"), GLOSSEX("GlossEx"), RAKE(
                "RAKE"), RIDF("RIDF"), TERM_EX("TermEx"), TF_IDF("TTF-IDF"), TTF("TTF"), WEIRDNESS("Weirdness");

        private final String algorithmName;

        Algorithm(String algorithmName) {
            this.algorithmName = algorithmName;
        }

        public String getAlgorithmName() {
            return this.algorithmName;
        }
    }

    /**
     * Solr field where content ngram info indexed and stored by means of
     * {@code solr.ShingleFilterFactory}
     * <p>
     * Recommended setting for this field in solr schema.xml:
     * <p>
     * <pre>
     *    {@code
     *
     * 	}
     * </pre>
     */
    public static final String FIELD_CONTENT_NGRAM = "solr_field_content_ngrams";

    /**
     * Solr field where final filtered term will be indexed and stored This
     * field should be a multi-valued field.
     * <p>
     * Recommended configuration in solr schema.xml:
     * <p>
     * <pre>
     *  {@code
     *      <field name="jate_domain_terms" type="string" indexed="true"
     *          stored="true" required="false" omitNorms="false" multiValued="true"/>
     *  }
     * </pre>
     */
    @Deprecated
    public static final String FIELD_DOMAIN_TERMS = "field_domain_terms";

    /**
     * Term ranking (unithood/termhood) algorithm.
     *
     * @see {@code uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler} for
     * all the supported ATR algorithms
     */
    public static final String TERM_RANKING_ALGORITHM = "algorithm";

    /**
     * Boolean flag to indicate whether extract candidate or not
     */
    public static final String CANDIDATE_EXTRACTION = "extraction";

    /**
     * Boolean flag to indicate whether term score will be as boost value for indexed term
     */
    public static final String BOOSTING = "boosting";

    /**
     * Boolean flag to indicate whether filtered candidate terms will be indexed and stored
     * This requires corresponding solr field to be configured in schema if set to true
     */
    public static final String INDEX_TERM = "indexTerm";

    /**
     * JATE property file is a required run-time setting file.
     * <p>
     * The property file must provide the configuration of pre-processed data
     * (e.g., solr content field, solr content ngram field for term vector
     * statistics) in index-time when term candidates is extracted.
     */
    public static final String JATE_PROPERTY_FILE = AppParams.JATE_PROPERTIES_FILE.getParamKey();

    /**
     * Minimum frequency allowed for term candidates. Increase for better
     * precision
     */
    public static final String PREFILTER_MIN_TERM_TOTAL_FREQUENCY = AppParams.
            PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey();

    /**
     * Optional
     * <p>
     * Min frequency of a term appearing in different context
     *
     * @see {@code uk.ac.shef.dcs.jate.app.AppChiSquare}
     * @see {@code uk.ac.shef.dcs.jate.app.AppNCValue}
     */
    public static final String PREFILTER_MIN_TERM_CONTEXT_FREQUENCY = AppParams.
            PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey();
    /**
     * cut-off threshold (exclusive) for filtering and indexing ranked term
     * candidates by term weight. Any term with weight less or equal to this
     * value will be filtered. The value is default as 0
     */
    public static final String CUTOFF_THRESHOLD = AppParams.CUTOFF_THRESHOLD.getParamKey();

    /**
     * Top N (inclusive) threshold for choose top N ranked term candidates
     * <p>
     * This threshold is an alternative to the default
     * {@code TermRecognitionRequestHandler.CUT_OFF_THRESHOLD}
     */
    public static final String CUTOFF_TOP_K = AppParams.CUTOFF_TOP_K.getParamKey();

    /**
     * Top percentage of total ranked term candidates.
     * <p>
     * This threshold is an alternative to the default
     * {@code TermRecognitionRequestHandler.CUT_OFF_THRESHOLD}
     */
    public static final String CUTOFF_TOP_K_PERCENT = AppParams.CUTOFF_TOP_K_PERCENT.getParamKey();

    /**
     * Unigram frequency distribution file required by few termhood calculation
     *
     * @see {@code uk.ac.shef.dcs.jate.app.AppTermEx}
     * @see {@code uk.ac.shef.dcs.jate.app.AppGlossEx}
     */
    public static final String REFERENCE_FREQUENCY_FILE = AppParams.REFERENCE_FREQUENCY_FILE.getParamKey();

    public static final Float DEFAULT_BOOST_VALUE = 1.0F;

    private final TermRecognitionProcessor generalTRProcessor;

    public TermRecognitionRequestHandler() {
        generalTRProcessor = TermRecognitionProcessorFactory.createTermRecognitionProcessor();
    }

    @Override
    public void handleRequestBody(SolrQueryRequest req, SolrQueryResponse rsp) throws Exception {
        log.info("Term recognition request handler...");
        setTopInitArgsAsInvariants(req);

        final String jatePropertyFile = req.getParams().get(JATE_PROPERTY_FILE);
        final String algorithmName = req.getParams().get(TERM_RANKING_ALGORITHM);
        final Boolean isExtraction = req.getParams().getBool(CANDIDATE_EXTRACTION);
        final String outFilePath = req.getParams().get(AppParams.OUTPUT_FILE.getParamKey());
        final Boolean isIndexTerms = req.getParams().getBool(INDEX_TERM);
        final Boolean isBoosted = req.getParams().getBool(BOOSTING);

        final Algorithm algorithm = getAlgorithm(algorithmName);

        JATEProperties properties = App.getJateProperties(jatePropertyFile);

        final SolrIndexSearcher searcher = req.getSearcher();

        if (isExtraction) {
            log.info("start candidate extraction (i.e., re-index of whole corpus) ...");
            generalTRProcessor.candidateExtraction(searcher.getCore(), jatePropertyFile);
            log.info("complete candidate terms indexing.");
        }

        Map<String, String> trRunTimeParams = initialiseTRRunTimeParams(req);
        List<JATETerm> termList = generalTRProcessor.rankingAndFiltering(searcher.getCore(), jatePropertyFile, trRunTimeParams,
                algorithm);

        log.info(String.format("complete term recognition extraction! Finalized Term size [%s]", termList.size()));

        if (isExport(outFilePath)) {
            generalTRProcessor.export(termList);
        }

        if (isIndexTerms) {
            log.info("start to index filtered candidate terms ...");
            indexTerms(termList, properties, searcher, isBoosted, isExtraction);
            log.info("complete the indexing of candidate terms.");
        }

    }

    private boolean isExport(String outFilePath) {
        return outFilePath != null && StringUtils.isNotEmpty(outFilePath);
    }

    /**
     * initialise Term Recognition (TR) runtime parameters
     * <p>
     * The method is to make it compatible with TR command line tool
     * <p>
     * TODO: need to have a naming convention to make it consistent for both
     * ends
     * <p>
     * see also {@code uk.ac.shef.dcs.jate.app.App} see also
     * {@code uk.ac.shef.dcs.jate.JATEProperties}
     *
     * @param req, Container for a request to execute a query
     * @return initialisation parameter map for ATE algorithm
     */
    private Map<String, String> initialiseTRRunTimeParams(SolrQueryRequest req) {
        Map<String, String> trRunTimeParams = new HashMap<String, String>();
        Float cut_off_threshold = req.getParams().getFloat(CUTOFF_THRESHOLD);
        if (cut_off_threshold != null) {
            trRunTimeParams.put(AppParams.CUTOFF_THRESHOLD.getParamKey(), cut_off_threshold.toString());
        }

        Integer topNThreshold = req.getParams().getInt(CUTOFF_TOP_K);
        if (topNThreshold != null) {
            trRunTimeParams.put(AppParams.CUTOFF_TOP_K.getParamKey(), topNThreshold.toString());
        }

        Float topPercentageThreshold = req.getParams().getFloat(CUTOFF_TOP_K_PERCENT);
        if (topPercentageThreshold != null) {
            trRunTimeParams.put(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey(),
                    topPercentageThreshold.toString());
        }

        Integer minTotalTermFreq = req.getParams().getInt(PREFILTER_MIN_TERM_TOTAL_FREQUENCY);
        if (minTotalTermFreq != null) {
            trRunTimeParams.put(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey(),
                    minTotalTermFreq.toString());
        }

        Integer minTermContextFreq = req.getParams().getInt(PREFILTER_MIN_TERM_CONTEXT_FREQUENCY);
        if (minTermContextFreq != null) {
            trRunTimeParams.put(AppParams.PREFILTER_MIN_TERM_CONTEXT_FREQUENCY.getParamKey(),
                    minTermContextFreq.toString());
        }

        String unigramFreqFile = req.getParams().get(REFERENCE_FREQUENCY_FILE);
        if (unigramFreqFile != null) {
            trRunTimeParams.put(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(), unigramFreqFile);
        }

        String outputFile = req.getParams().get(AppParams.OUTPUT_FILE.getParamKey());
        if (outputFile != null) {
            trRunTimeParams.put(AppParams.OUTPUT_FILE.getParamKey(), outputFile);
        }

        Boolean collectTermInfo = req.getParams().getBool(AppParams.COLLECT_TERM_INFO.getParamKey());

        if (collectTermInfo != null) {
            trRunTimeParams.put(AppParams.COLLECT_TERM_INFO.getParamKey(), collectTermInfo.toString());
        }

        return trRunTimeParams;
    }

    @Override
    public String getDescription() {
        return "Automatic term recognition and indexing by whole corpus/index analysis.";
    }

    /**
     * Index weighted & filtered final terms back into Solr
     *
     * @param filteredTerms,  filtered JATE terms
     * @param jateProperties, jate properties for integration config between jate2.0 and solr instance
     * @param indexSearcher,  solr index searcher
     * @param isBoosted,      true or false to indivate whether term will be boosted with ATE score
     * @throws JATEException
     */
    public void indexTerms(List<JATETerm> filteredTerms, JATEProperties jateProperties,
                           SolrIndexSearcher indexSearcher, boolean isBoosted, boolean isExtraction)
            throws JATEException {
        int numDocs = indexSearcher.maxDoc();
        String domainTermsFieldName = jateProperties.getSolrFieldNameJATEDomainTerms();
        String candidateTermFieldName = jateProperties.getSolrFieldNameJATECTerms();
        log.info(String.format("indexing [%s] terms into field [%s] for total [%s] documents ...", filteredTerms.size(),
                domainTermsFieldName, numDocs));
        if (filteredTerms.size() == 0) {
            return;
        }

        SolrCore core = indexSearcher.getCore();
        IndexSchema indexSchema = core.getLatestSchema();
        try {
            IndexWriter writerIn = core.getSolrCoreState().getIndexWriter(core).get();
            Map<String, List<CopyField>> copyFields = indexSchema.getCopyFieldsMap();

            for (int docID = 0; docID < numDocs; docID++) {
                try {
                    Document doc = indexSearcher.doc(docID);
                    if (isExtraction) {
                        //TODO: may consider to avoid to index those intermediate values again
                        SolrUtil.copyFields(copyFields, DEFAULT_BOOST_VALUE, doc);
                    }

                    Terms indexedCandidateTermsVectors = SolrUtil.getTermVector(docID,
                            candidateTermFieldName, indexSearcher);
                    List<String> candidateTerms = SolrUtil.getNormalisedTerms(indexedCandidateTermsVectors);

                    List<Pair<String, Double>> filteredCandidateTerms = getSelectedWeightedCandidates(filteredTerms,
                            candidateTerms);

                    iterateAddDomainTermFields(isBoosted, domainTermsFieldName, indexSchema, doc, filteredCandidateTerms);

                    writerIn.updateDocument(new Term("id", doc.get("id")), doc);

                } catch (IOException e) {
                    throw new JATEException(
                            String.format("Failed to retrieve current document (docId: [%s]) due to " +
                                    "an unexpected I/O exception: %s", docID, e.toString()));
                }
            }
        } catch (IOException ioe) {
            throw new JATEException(String.format("Failed to index filtered domain terms due to I/O exception when " +
                    "loading solr index writer: %s", ioe.toString()));
        }
        log.info(String.format("finalised terms have been indexed into [%s] field for all documents",
                domainTermsFieldName));
    }

    private void iterateAddDomainTermFields(boolean isBoosted, String domainTermsFieldName,
                                            IndexSchema indexSchema, Document doc,
                                            List<Pair<String, Double>> filteredCandidateTerms) {
        for (Pair<String, Double> filteredTerm : filteredCandidateTerms) {
            if (filteredTerm == null) {
                continue;
            }

            if (isBoosted) {
                doc.add(indexSchema.getField(domainTermsFieldName).createField(filteredTerm.getKey(),
                        filteredTerm.getValue().floatValue()));
            } else {
                doc.add(indexSchema.getField(domainTermsFieldName).createField(filteredTerm.getKey(),
                        DEFAULT_BOOST_VALUE));
            }
        }
    }

    private List<Pair<String, Double>> getSelectedWeightedCandidates(List<JATETerm> filteredTerms, List<String> candidateTerms) {
        List<Pair<String, Double>> filteredCandidateTerms = new ArrayList<>();
        candidateTerms.parallelStream().forEach(candidateTerm -> {
            filteredTerms.parallelStream().forEach(filteredTerm -> {
                if (filteredTerm != null && candidateTerm != null
                        && filteredTerm.getString() != null &&
                        filteredTerm.getString().equalsIgnoreCase(candidateTerm)) {
                    Pair<String, Double> selectedTerm =
                            new Pair<String, Double>(filteredTerm.getString(), filteredTerm.getScore());
                    filteredCandidateTerms.add(selectedTerm);
                }
            });
        });
        return filteredCandidateTerms;
    }

    /**
     * This request handler supports configuration options defined at the top
     * level as well as those in typical Solr 'defaults', 'appends', and
     * 'invariants'. The top level ones are treated as invariants.
     */
    private void setTopInitArgsAsInvariants(SolrQueryRequest req) {
        // First convert top level initArgs to SolrParams
        HashMap<String, String> map = new HashMap<String, String>(initArgs.size());
        for (int i = 0; i < initArgs.size(); i++) {
            Object val = initArgs.getVal(i);
            if (val != null && !(val instanceof NamedList))
                map.put(initArgs.getName(i), val.toString());
        }
        if (map.isEmpty())
            return;// short circuit; nothing to do
        SolrParams topInvariants = new MapSolrParams(map);
        // By putting the top level into the 1st arg, it overrides
        // request params in 2nd arg.
        req.setParams(SolrParams.wrapDefaults(topInvariants, req.getParams()));
    }

    private Algorithm getAlgorithm(String algName) throws JATEException {
        if (StringUtils.isEmpty(algName)) {
            throw new JATEException("ATE algorithm is not specified. " +
                    "Please check API documentation for all the supported ATR algorithms.");
        }

        if (algName.equalsIgnoreCase(Algorithm.C_VALUE.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.C_VALUE.getAlgorithmName()));
            return Algorithm.C_VALUE;
        } else if (algName.equalsIgnoreCase(Algorithm.ATTF.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.ATTF.getAlgorithmName()));
            return Algorithm.ATTF;
        } else if (algName.equalsIgnoreCase(Algorithm.CHI_SQUARE.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.CHI_SQUARE.getAlgorithmName()));
            return Algorithm.CHI_SQUARE;
        } else if (algName.equalsIgnoreCase(Algorithm.GLOSSEX.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.GLOSSEX.getAlgorithmName()));
            return Algorithm.GLOSSEX;
        } else if (algName.equalsIgnoreCase(Algorithm.RAKE.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.RAKE.getAlgorithmName()));
            return Algorithm.RAKE;
        } else if (algName.equalsIgnoreCase(Algorithm.RIDF.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.RIDF.getAlgorithmName()));
            return Algorithm.RIDF;
        } else if (algName.equalsIgnoreCase(Algorithm.TERM_EX.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.TERM_EX.getAlgorithmName()));
            return Algorithm.TERM_EX;
        } else if (algName.equalsIgnoreCase(Algorithm.TF_IDF.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.TF_IDF.getAlgorithmName()));
            return Algorithm.TF_IDF;
        } else if (algName.equalsIgnoreCase(Algorithm.TTF.getAlgorithmName())) {
            log.debug(
                    String.format("[%s] algorithm is set to rank term candidates. ", Algorithm.TTF.getAlgorithmName()));
            return Algorithm.TTF;
        } else if (algName.equalsIgnoreCase(Algorithm.WEIRDNESS.getAlgorithmName())) {
            log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
                    Algorithm.WEIRDNESS.getAlgorithmName()));
            return Algorithm.WEIRDNESS;
        } else {
            throw new JATEException(String.format(
                    "Current algorithm [%s] is not supported. Please check API documentation for all the supported ATR algorithms.",
                    algName));
        }
    }
}