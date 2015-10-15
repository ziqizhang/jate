package uk.ac.shef.dcs.jate.solr;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.LeafReader;
import org.apache.solr.common.params.MapSolrParams;
import org.apache.solr.common.params.SolrParams;
import org.apache.solr.common.util.NamedList;
import org.apache.solr.handler.RequestHandlerBase;
import org.apache.solr.request.SolrQueryRequest;
import org.apache.solr.response.SolrQueryResponse;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.app.App;
import uk.ac.shef.dcs.jate.app.AppParams;
import uk.ac.shef.dcs.jate.model.JATETerm;

/**
 * Scans solr indexed and TR aware content field and perform terminology
 * recognition (ranking + filtering + indexing) for whole index.
 * 
 * Current version requires term candidates are preprocessed in index-time. TR
 * AWARE SOLR FIELDS MUST ALSO BE CONFIGURED AND INDEXED FOR TERN RANKING
 * ALGORITHMS.
 * 
 * TODO: An API document needs to be prepared to specify how to define automatic
 * term candidate extraction in index-time.
 * 
 * TODO: future version may need to support term candidate extraction as an
 * option
 * 
 * Example configuration in solrconfig.xml
 * 
 * 1. configure JATE library (jar file) to solr classpath
 * 
 * <lib path=
 * "${solr.install.dir:../../..}/contrib/jate/lib/jate-2.0Alpha-SNAPSHOT-jar-with-dependencies.jar"/>
 * 
 * 2. configure request handler for term recognition and indexing
 * 
 * <pre>
 * {@code
 * 	<requestHandler name="/termRecognise" class=
"uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler">
 * 		<lst name="defaults">
 * 			<str name="jate_property_file">../resource/jate.properties</str>
 * 			<str name="field_domain_terms">industryTerm</str>
 * 			<int name="cut_off_threshold">0</int>
 * 			<!-- <int name="top_n_threshold">0</int> -->
 * 			<!-- <double name="top_percentage_threshold">0</double> -->
 * 		</lst>
 * 	</requestHandler>
 * }
 */
public class TermRecognitionRequestHandler extends RequestHandlerBase {
	private final Logger log = LoggerFactory.getLogger(getClass());

	/** Request parameter. */
	public static enum Algorithm {
		C_VALUE("CValue"), NC_VALUE("NCValue"), ATTF("ATTF"), CHI_SQUARE("ChiSquare"), GLOSSEX("GlossEx"), RAKE(
				"RAKE"), RIDF("RIDF"), TERM_EX("TermEx"), TF_IDF("tfidf"), TTF("ttf"), WEIRDNESS("Weirdness");

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
	 * 
	 * Recommended setting for this field in solr schema.xml:
	 * 
	 * <pre>
	 * 	{@code
	 * 	
	 * 
	 * 	}
	 * </pre>
	 */
	public static final String FIELD_CONTENT_NGRAM = "field_content_ngram";

	/**
	 * Solr field where final filtered term will be indexed and stored This
	 * field should be a multi-valued field.
	 * 
	 * Recommended configuration in solr schema.xml:
	 * 
	 * <pre>
	 *  {@code
	 *  <field name="industryTerm" type="industry_term_type" indexed="true" 
	 *  		stored="true" multiValued="true" omitNorms="true" termVectors="true"/>
	 *  
	 *  <fieldType name="industry_term_type" class="solr.TextField" positionIncrementGap="100">
	 *		<analyzer>
	 *			<tokenizer class="solr.KeywordTokenizerFactory"/>		
	 *			<charFilter class="solr.PatternReplaceCharFilterFactory" pattern="(\-)" replacement=" " />				
	 *			<filter class="solr.LowerCaseFilterFactory"/>
	 *			<filter class="solr.ASCIIFoldingFilterFactory"/>
	 *			<filter class="solr.EnglishMinimalStemFilterFactory"/>
	 *		 </analyzer>
	 *	</fieldType>
	 *  }
	 * </pre>
	 */
	public static final String FIELD_DOMAIN_TERMS = "field_domain_terms";

	/**
	 * JATE property file is a required run-time setting file.
	 * 
	 * The property file must provide the configuration of pre-processed data
	 * (e.g., solr content field, solr content ngram field for term vector
	 * statistics) in index-time when term candidates is extracted.
	 */
	public static final String JATE_PROPERTY_FILE = "jate_property_file";

	/**
	 * Minimum frequency allowed for term candidates. Increase for better
	 * precision
	 */
	public static final String PREFILTER_MIN_TERM_TOTAL_FREQUENCY = AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamName();

	/**
	 * Optional
	 * 
	 * Min frequency of a term appearing in different context
	 *
	 * @see also {@code uk.ac.shef.dcs.jate.app.AppChiSquare}
	 * @see also {@code uk.ac.shef.dcs.jate.app.AppNCValue}
	 */
	public static final String PREFILTER_MIN_TERM_CONTEXT_FREQUENCY = AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY
			.getParamName();
	/**
	 * 
	 * cut-off threshold (exclusive) for filtering and indexing ranked term
	 * candidates by term weight. Any term with weight less or equal to this
	 * value will be filtered. The value is default as 0
	 */
	public static final String CUTOFF_THRESHOLD = AppParams.CUTOFF_THRESHOLD.getParamName();

	/**
	 * Top N (inclusive) threshold for choose top N ranked term candidates
	 * 
	 * This threshold is an alternative to the default
	 * {@code TermRecognitionRequestHandler.CUT_OFF_THRESHOLD}
	 */
	public static final String CUTOFF_TOP_K = AppParams.CUTOFF_TOP_K.getParamName();

	/**
	 * Top percentage of total ranked term candidates.
	 * 
	 * This threshold is an alternative to the default
	 * {@code TermRecognitionRequestHandler.CUT_OFF_THRESHOLD}
	 */
	public static final String CUTOFF_TOP_K_PERCENT = AppParams.CUTOFF_TOP_K_PERCENT.getParamName();

	/**
	 * Term ranking (unithood/termhood) algorithm.
	 * 
	 * @see {@code uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler} for
	 *      all the supported ATR algorithms
	 */
	public static final String TERM_RANKING_ALGORITHM = "term_ranking_algorithm";

	/**
	 * Unigram frequency distribution file required by few termhood calculation
	 * 
	 * @see {@code uk.ac.shef.dcs.jate.app.AppTermEx}
	 * @see {@code uk.ac.shef.dcs.jate.app.AppGlossEx}
	 */
	public static final String REFERENCE_FREQUENCY_FILE = AppParams.REFERENCE_FREQUENCY_FILE.getParamName();

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
		final String solrFieldDomainTerms = req.getParams().get(FIELD_DOMAIN_TERMS);

		JATEProperties properties = new JATEProperties(jatePropertyFile);
		String termCandidateField = properties.getSolrFieldnameJATECTerms();

		final Algorithm algorithm = getAlgorithm(algorithmName);

		final SolrIndexSearcher searcher = req.getSearcher();

		Map<String, String> trRunTimeParams = initialiseTRRunTimeParams(req);

		List<JATETerm> termList = generalTRProcessor.extract(searcher.getCore(), jatePropertyFile, trRunTimeParams,
				algorithm);

		log.info(String.format("complete term recognition extraction! Finalized Term size [%s]", termList));

		index(termList, termCandidateField, solrFieldDomainTerms, searcher.getLeafReader());

	}

	/**
	 * initialise Term Recognition (TR) runtime parameters
	 * 
	 * The method is to make it compatible with TR command line tool
	 * 
	 * TODO: need to have a naming convention to make it consistent for both
	 * ends
	 * 
	 * see also {@code uk.ac.shef.dcs.jate.app.App} see also
	 * {@code uk.ac.shef.dcs.jate.JATEProperties}
	 * 
	 * @param req
	 * @return
	 */
	private Map<String, String> initialiseTRRunTimeParams(SolrQueryRequest req) {
		Map<String, String> trRunTimeParams = new HashMap<String, String>();
		Double cut_off_threshold = req.getParams().getDouble(CUTOFF_THRESHOLD);
		if (cut_off_threshold != null) {
			trRunTimeParams.put(AppParams.CUTOFF_THRESHOLD.getParamKey(), cut_off_threshold.toString());
		}

		Integer topNThreshold = req.getParams().getInt(CUTOFF_TOP_K);
		if (topNThreshold != null) {
			trRunTimeParams.put(AppParams.CUTOFF_TOP_K.getParamKey(), topNThreshold.toString());
		}

		Double topPercentageThreshold = req.getParams().getDouble(CUTOFF_TOP_K_PERCENT);
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

		return trRunTimeParams;
	}

	@Override
	public String getDescription() {
		return "Automatic term recognition and indexing by whole corpus/index analysis.";
	}

	public static void main(String[] args) {
		// org.apache.solr.SolrTestCaseJ4
	}

	public void index(List<JATETerm> terms, String termCandidateField, String domainTermField, LeafReader indexReader)
			throws JATEException {

		int numDocs = indexReader.numDocs();

		log.info(String.format("indexing [%s] terms into field [%s] for total [%s] documents", terms.size(),
				domainTermField, numDocs));

		for (int docID = 0; docID < numDocs; docID++) {
			try {
				Document doc = indexReader.document(docID);
				// TODO: read doc, compare term candidates in "term
				// candidateField" and update fitered term into "domainTermField"
				// update doc
			} catch (IOException ioe) {
				throw new JATEException(
						String.format("I/O exception when processing document. Current document id: [%s]", docID));
			}
		}
		log.info(String.format("finalised terms have been indexed into [%s] field", domainTermField));
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
		// By putting putting the top level into the 1st arg, it overrides
		// request params in 2nd arg.
		req.setParams(SolrParams.wrapDefaults(topInvariants, req.getParams()));
	}

	private Algorithm getAlgorithm(String algName) throws JATEException {
		if (StringUtils.isEmpty(algName)) {
			throw new JATEException(String.format(
					"Current algorithm is not set correctly. Please check API documentation for all the supported ATR algorithms.",
					algName));
		}

		if (algName.equalsIgnoreCase(Algorithm.C_VALUE.getAlgorithmName())) {
			log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
					Algorithm.C_VALUE.getAlgorithmName()));
			return Algorithm.C_VALUE;
		} else if (algName.equalsIgnoreCase(Algorithm.NC_VALUE.getAlgorithmName())) {
			log.debug(String.format("[%s] algorithm is set to rank term candidates. ",
					Algorithm.NC_VALUE.getAlgorithmName()));
			return Algorithm.NC_VALUE;
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