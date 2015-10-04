package uk.ac.shef.dcs.jate.app;

import com.google.gson.Gson;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.lucene.index.LeafReader;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.app.App.CommandLineParams;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBasedFBMaster;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.IOUtil;
import uk.ac.shef.dcs.jate.util.JATEUtil;

import java.io.File;
import java.io.IOException;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.*;

public abstract class App {

	private final Logger log = LoggerFactory.getLogger(getClass());

	// TODO: ziqi, please make parameter clear.
	// if you want to support both top percentage and top N term filtering,
	// please set two different parameters for two different settings.
	protected static final double DEFAULT_THRESHOLD_N = 0.25;

	/**
	 * corresponding to "-c" in command line
	 * 
	 * 'true' or 'false'. Whether to collect term information, e.g., offsets in
	 * documents. Default is false.
	 * 
	 * 
	 * TODO: ziqi, pls add more explains about this property. why it is
	 * necessary for user to set to collect term info ? what is term info ?
	 */
	protected Boolean isCollectTermInfo = false;

	/**
	 * corresponding to "-t" in command line Term weight (termhood/unithood)
	 * threshold for filter terms. If not set then default -n is used.
	 * 
	 */
	protected Double cutOffThreshold = null;

	/**
	 * corresponding to "-n" in command line
	 * 
	 * If an integer is given, top N candidates are selected as terms
	 * 
	 * TODO: ziqi, pls explain why this is needed? why user will select top N
	 * terms and how they would know the "N"?
	 */
	protected Integer topNTerms = null;

	protected Double topPercentageTerms = null;

	/**
	 * corresponding to "-o" in command line
	 * 
	 * file path. If provided, the output is written to the file. Otherwise,
	 * output is written to the console.
	 * 
	 * TODO: ziqi, pls explain what is outputted ? Why it is necessary to output
	 * if even file is not provided ?
	 */
	protected String outputFile = null;

	// Min total fequency of a term
	protected Integer minTTF = null;

	// Min frequency of a term appearing in different context
	protected Integer minTCF = null;

	protected String unigramFreqFilePath = null;

	/**
	 * CommandLineParams provides the mapping of term ranking algorithms runtime
	 * parameter key (for abbv.) and parameter name (for the solr config
	 * setting)
	 * 
	 * see also {@code uk.ac.shef.dcs.jate.solr.TermRecognitionRequestHandler}
	 * 
	 */
	public enum CommandLineParams {
		// TODO: ziqi, pls check/add comments to explain each command line
		// parameter

		IS_COLLECT_TERM_INFO("-c", "is_collect_term_info"),
		// cut off threshold to filter term candidates list by termhood/unithood
		CUT_OFF_THRESHOLD("-t", "cut_off_threshold"),
		// top N terms to filter term candidates
		TOP_N_TERMS("-n", "top_n_terms"),
		// output file to export final filtered term list
		OUTPUT_FILE("-o", "output_file"),
		// Min total fequency of a term for it to be considered for
		// co-occurrence computation
		// (see {@code uk.ac.shef.dcs.jate.app.AppChiSquare})
		MIN_TOTAL_TERM_FREQUENCY("-mttf", "min_total_term_freq"),
		// Min frequency of a term appearing in different context for it
		// to be considered for co-occurrence computation.
		// (see {@code uk.ac.shef.dcs.jate.app.AppChiSquare})
		MIN_TERM_CONTEXT_FREQUENCY("-mtcf", "min_term_context_freq"),
		// file path to the reference corpus statistics (unigram
		// distribution) file.
		// see bnc_unifrqs.normal default file in /resource directory
		// see also {@code uk.ac.shef.dcs.jate.app.AppTermEx}
		// see also {@code uk.ac.shef.dcs.jate.app.AppWeirdness})
		UNIGRAM_FREQUENCY_FILE("-r", "unigram_freq_file");

		private final String paramKey;
		private final String paramName;

		CommandLineParams(String paramKey, String paramName) {
			this.paramKey = paramKey;
			this.paramName = paramName;
		}

		public String getParamKey() {
			return this.paramKey;
		}

		public String getParamName() {
			return this.paramName;
		}

	}

	protected FrequencyTermBasedFBMaster freqFeatureBuilder = null;
	// term indexed feature (typically frequency info.)
	// see also {@code AppATTF}
	protected FrequencyTermBased freqFeature = null;

	/**
	 * Initialise common run-time parameters
	 * 
	 * @param initParams,
	 *            command line run-time parameters (paramKey, value) for term
	 *            ranking algorithms
	 * 
	 *            see also {code CommandLineParams}
	 * @throws JATEException
	 */
	App(Map<String, String> initParams) throws JATEException {
		if (initParams.containsKey(CommandLineParams.TOP_N_TERMS.getParamKey())) {
			String topNSetting = initParams.get(CommandLineParams.TOP_N_TERMS.getParamKey());
			if (!NumberUtils.isNumber(topNSetting)) {
				log.error("Top N terms setting ('-n') is not set correctly! A string of numeric value is expected!");
				// TODO: ziqi, shall we default the value to the
				// DEFAULT_THRESHOLD_N when it is incorrectly set?
				throw new JATEException("A string of numeric value is expected for Top N terms setting ('-n') !");
			} else if (JATEUtil.isInteger(topNSetting)) {
				log.debug(String.format("Term candidate is set to filter by top [%s]", topNSetting));
				this.topNTerms = Integer.parseInt(topNSetting);
			} else {
				log.debug(String.format("Term candidate is set to filter by top [%s](rounded)", topNSetting));
				this.topPercentageTerms = Double.parseDouble(topNSetting);
			}
		}

		if (initParams.containsKey(CommandLineParams.CUT_OFF_THRESHOLD.getParamKey())) {
			String cutOffThreshold = initParams.get(CommandLineParams.CUT_OFF_THRESHOLD.getParamKey());
			if (!NumberUtils.isNumber(cutOffThreshold)) {
				log.error(
						"Term weight cut-off threshold setting ('-t') is not set correctly! A string of numeric value is expected!");
				// TODO: ziqi, shall we default the value to the
				// DEFAULT_THRESHOLD_N when it is incorrectly set?
				throw new JATEException(
						"A string of numeric value is expected for Term weight cut-off threshold setting ('-t') !");
			} else {
				log.debug(String.format("Term weight cut-off threshold is set to [%s]", cutOffThreshold));
				this.cutOffThreshold = Double.parseDouble(cutOffThreshold);
			}
		}

		if (initParams.containsKey(CommandLineParams.IS_COLLECT_TERM_INFO.getParamKey())) {
			String isCollectTermInfo = initParams.get(CommandLineParams.IS_COLLECT_TERM_INFO.getParamKey());
			if (isCollectTermInfo != null && isCollectTermInfo.equalsIgnoreCase("true")) {
				this.isCollectTermInfo = true;
			}
		}

	}

	protected void initaliseNgramFreqParam(Map<String, String> initParams) throws JATEException {
		if (initParams.containsKey(CommandLineParams.UNIGRAM_FREQUENCY_FILE.getParamKey())) {
			String unigramFreqFilePath = initParams.get(CommandLineParams.UNIGRAM_FREQUENCY_FILE.getParamKey());

			if (unigramFreqFilePath == null) {
				log.error(
						"Unigram Frequency distribution file ('-r') is not set correctly! A string of file path is expected!");
				throw new JATEException(
						" A string of file path is expected for Unigram Frequency distribution file ('-r')!");
			}

			File unigramFreqFile = new File(unigramFreqFilePath);
			if (!unigramFreqFile.exists()) {
				log.error(
						"Unigram Frequency distribution file ('-r') is not set correctly! Current file is not accessible!");
				throw new JATEException(" Unigram Frequency distribution file ('-r') is not accessible!");
			}

			this.unigramFreqFilePath = unigramFreqFilePath;
		}
	}
	
	protected void initialiseMTCFParam(Map<String, String> initParams) throws JATEException {
		if (initParams.containsKey(CommandLineParams.MIN_TERM_CONTEXT_FREQUENCY.getParamKey())) {
			String minTCF = initParams.get(CommandLineParams.MIN_TERM_CONTEXT_FREQUENCY.getParamKey());
			if (!NumberUtils.isNumber(minTCF)) {
				log.error(
						"Minimum term context frequency ('-mtcf') is not set correctly! A string of numeric value is expected!");
				throw new JATEException(
						"A string of numeric value is expected for minimum term context frequency ('-mtcf') !");
			} else if (JATEUtil.isInteger(minTCF)) {
				log.debug(String.format("Mininum term context frequency is set to [%s]", minTTF));
				this.minTCF = Integer.parseInt(minTCF);
			} else {
				log.error(
						"Minimum term context frequency ('-mttf') is not set correctly! A string of numeric value is expected!");
				throw new JATEException(
						"A string of numeric value is expected for minimum term context frequency ('-mtcf') !");
			}
		}
	}

	protected void initialiseMTTFParam(Map<String, String> initParams) throws JATEException {
		if (initParams.containsKey(CommandLineParams.MIN_TOTAL_TERM_FREQUENCY.getParamKey())) {
			String minTTF = initParams.get(CommandLineParams.MIN_TOTAL_TERM_FREQUENCY.getParamKey());
			if (!NumberUtils.isNumber(minTTF)) {
				log.error(
						"Minimum total term frequency ('-mttf') is not set correctly! A string of numeric value is expected!");
				throw new JATEException(
						"A string of numeric value is expected for Minimum total term frequency ('-mttf') !");
			} else if (JATEUtil.isInteger(minTTF)) {
				log.debug(String.format("Mininum total term frequency is set to [%s]", minTTF));
				this.minTTF = Integer.parseInt(minTTF);
			} else {
				log.error(
						"Minimum total term frequency ('-mttf') is not set correctly! A string of numeric value is expected!");
				throw new JATEException(
						"A string of numeric value is expected for Minimum total term frequency ('-mttf') !");
			}
		}
	}

	/**
	 * 
	 * @param core,
	 *            solr core
	 * @param jatePropertyFile,
	 *            property file path
	 * @param params,
	 *            key-value map settings for different TR algorithm (see
	 *            {@code uk.ac.shef.dcs.jate.app.App.CommandLineParams} for
	 *            details)
	 * @return List<JATETerm>, the list of terms extracted
	 * @throws IOException
	 * @throws JATEException
	 */
	public abstract List<JATETerm> extract(SolrCore core, String jatePropertyFile) throws IOException, JATEException;

	/**
	 * 
	 * @param solrHomePath,
	 *            solr core home directory path
	 * @param coreName,
	 *            solr core name from where term recognition is executed
	 * @param jatePropertyFile,
	 *            property file path
	 * @param params,
	 *            key-value map settings for different TR algorithm (see
	 *            {@code uk.ac.shef.dcs.jate.app.App.CommandLineParams} for
	 *            details)
	 * @return List<JATETerm>, the list of terms extracted
	 * @throws IOException
	 * @throws JATEException
	 */
	public List<JATETerm> extract(String solrHomePath, String coreName, String jatePropertyFile)
			throws IOException, JATEException {
		EmbeddedSolrServer solrServer = null;
		SolrCore core = null;
		List<JATETerm> result = new ArrayList<JATETerm>();

		try {
			solrServer = new EmbeddedSolrServer(Paths.get(solrHomePath), coreName);
			core = solrServer.getCoreContainer().getCore(coreName);

			result = extract(core, jatePropertyFile);
		} finally {
			if (core != null) {
				core.close();
			}
			if (solrServer != null) {
				solrServer.close();
			}
		}
		return result;
	}

	/**
	 * TODO: ziqi, please add explain about this function(e.g., why it is
	 * needed, what is expected output)
	 * 
	 * @param leafReader
	 * @param terms
	 * @param ngramInfoFieldname
	 * @param idFieldname
	 * @throws IOException
	 */
	public void collectTermInfo(List<JATETerm> terms, LeafReader leafReader, String ngramInfoFieldname,
			String idFieldname) throws IOException {
		TermInfoCollector infoCollector = new TermInfoCollector(leafReader, ngramInfoFieldname, idFieldname);

		log.info("Gathering term information (e.g., provenance and offsets). This may take a while. Total="
				+ terms.size());
		int count = 0;
		for (JATETerm jt : terms) {
			jt.setTermInfo(infoCollector.collect(jt.getString()));
			count++;
			if (count % 500 == 0)
				log.info("done " + count);
		}
	}

	/**
	 * Add additional (indexed) term info into term list
	 * 
	 * @param terms,
	 *            filtered term candidates
	 * @param searcher,
	 *            solr index searcher
	 * @param content2NgramField,
	 *            solr content to ngram TR aware field
	 * @param idField,
	 *            solr unique id
	 * @throws JATEException
	 */
	public void addAdditionalTermInfo(List<JATETerm> terms, SolrIndexSearcher searcher, String content2NgramField,
			String idField) throws JATEException {
		if (this.isCollectTermInfo) {
			try {
				collectTermInfo(terms, searcher.getLeafReader(), content2NgramField, idField);
			} catch (IOException e) {
				throw new JATEException("I/O exception when reading Solr index. " + e.toString());
			}
		}
	}

	/**
	 * 
	 * Term candidate filtering by total (whole index/corpus) term frequency
	 * (exclusive)
	 * 
	 * @param candidates,
	 *            term candidates
	 * @param fFeature
	 * @param cutoff,
	 *            total term frequency cut-off threshold(exclusive)
	 * @throws JATEException
	 */
	protected void filterByTTF(List<String> candidates, int cutoff) throws JATEException {
		if (this.freqFeature == null) {
			throw new JATEException("FrequencyTermBased is not initialised for TTF term filtering.");
		}

		if (this.minTTF != null & candidates != null & candidates.size() > 0) {
			log.debug(String.format("Filter [%s] term candidates by total term frequency [%s] (exclusive)",
					candidates.size(), this.minTTF));
			Iterator<String> it = candidates.iterator();
			while (it.hasNext()) {
				String t = it.next();
				if (this.freqFeature.getTTF(t) < cutoff)
					it.remove();
			}
			log.debug(String.format("filtered term candidate size: [%s]", candidates));
		}
	}

	protected static Map<String, String> getParams(String[] args) {
		Map<String, String> params = new HashMap<>();
		for (int i = 0; i < args.length; i++) {
			if (i + 1 < args.length) {
				String param = args[i];
				String value = args[i + 1];
				i++;
				params.put(param, value);
			}
		}
		return params;
	}

	protected static int getParamCutoffFreq(Map<String, String> params) {
		String cutoff = params.get(CommandLineParams.MIN_TOTAL_TERM_FREQUENCY.getParamKey());
		int minFreq = 0;
		if (cutoff != null) {
			try {
				minFreq = Integer.valueOf(cutoff);
			} catch (NumberFormatException ne) {
			}
		}
		return minFreq;
	}

	protected static void write(List<JATETerm> terms, String path) throws IOException {
		Gson gson = new Gson();
		if (path == null) {
			// TODO: ziqi: why system print here ? should avoid this in the
			// code!
			// if you want to just support to print the result, pls avoid this.
			System.out.println(gson.toJson(terms));
		} else {
			Writer w = IOUtil.getUTF8Writer(path);
			gson.toJson(terms, w);
			w.close();
		}
	}

	/**
	 * filter term candidates by cut-off threshold, top N where applicable
	 * 
	 * @param terms
	 * @return List<JATETerm>, filtered terms
	 */
	protected List<JATETerm> filtering(List<JATETerm> terms) {
		if (this.cutOffThreshold != null) {
			return filterByTermWeightThreshold(terms, this.cutOffThreshold);
		}

		if (this.topNTerms != null) {
			return filterByTopNTerm(terms, this.topNTerms);
		}

		if (this.topPercentageTerms != null) {
			return filterByTopPercentage(terms, this.topPercentageTerms);
		}

		return terms;
	}

	/**
	 * Filter term candidate list by termhood/unithood based threshold
	 * (inclusive)
	 * 
	 * @param terms,
	 *            a list of term candidates with term weight
	 * @param cutOffThreshold,
	 *            term score measured by ATR algorithms
	 * @return List<JATETerm>, filtered terms
	 */
	protected List<JATETerm> filterByTermWeightThreshold(List<JATETerm> terms, Double cutOffThreshold) {
		if (cutOffThreshold != null & terms != null & terms.size() > 0) {
			log.debug(String.format("filter [%s] term candidates by termhood/unithood based threshold [%s]",
					terms.size(), cutOffThreshold));

			for (JATETerm jt : terms) {
				if (jt.getScore() < cutOffThreshold)
					terms.remove(jt);
			}
			log.debug(String.format("final filtered term candidate size [%s]", terms.size()));
		}
		return terms;
	}

	/**
	 * Filter term candidate list by top N (inclusive) terms
	 * 
	 * @param terms,
	 *            terms ranked by term weight
	 * @param topN,
	 *            top N term number
	 * @return List<JATETerm>, filtered terms
	 */
	protected List<JATETerm> filterByTopNTerm(List<JATETerm> terms, Integer topN) {
		if (topN != null & terms != null & terms.size() > 0 & topN < terms.size()) {
			log.debug(String.format("filter [%s] term candidates by Top [%s] ...", terms.size(), topN));
			terms = terms.subList(0, (topN + 1));
			log.debug(String.format("final filtered term list size is [%s]", terms.size()));
		}
		return terms;
	}

	/**
	 * Filter term candidate list by rounding top percentage of total term size
	 * 
	 * @param terms
	 * @param topPercentage
	 * @return
	 */
	protected List<JATETerm> filterByTopPercentage(List<JATETerm> terms, Double topPercentage) {
		if (topPercentage != null & terms != null & terms.size() > 0) {
			log.debug(String.format("filter [%s] term candidates by Top [%s]% (rounded) ...", terms.size(),
					topPercentage * 100));
			Integer topN = (int) Math.round(topPercentage * terms.size());
			if (topN > 0)
				terms = filterByTopNTerm(terms, topN);
			log.debug(String.format("final filtered term list size is [%s]", terms.size()));
		}
		return terms;
	}

	public static void main(String[] args) {
		Long topNInteger = Math.round(0.45 * 1);
		System.out.println(topNInteger);
	}

	protected static void printHelp() {
		StringBuilder sb = new StringBuilder("Usage:\n");
		sb.append("java -cp '[CLASSPATH]' ").append(AppATTF.class.getName()).append(" [OPTIONS] ")
				.append("[SOLR_HOME_PATH] [SOLR_CORE_NAME] [JATE_PROPERTY_FILE]").append("\nE.g.:\n");
		sb.append("java -cp '/libs/*' -t 20 /solr/server/solr jate jate.properties ...\n\n");
		sb.append("[OPTIONS]:\n")
				.append("\t\t-c\t\t'true' or 'false'. Whether to collect term information, e.g., offsets in documents. Default is false.\n")
				.append("\t\t-t\t\tA number. Score threshold for selecting terms. If not set then default -n is used.")
				.append("\n")
				.append("\t\t-n\t\tA number. If an integer is given, top N candidates are selected as terms. \n")
				.append("\t\t\t\tIf a decimal number is given, top N% of candidates are selected. Default is 0.25.\n");
		sb.append("\t\t-o\t\tA file path. If provided, the output is written to the file. \n")
				.append("\t\t\t\tOtherwise, output is written to the console.");
		System.out.println(sb);
	}
}
