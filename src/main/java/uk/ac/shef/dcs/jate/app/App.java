package uk.ac.shef.dcs.jate.app;

import com.google.gson.Gson;

import org.apache.commons.lang.exception.ExceptionUtils;
import org.apache.lucene.index.LeafReader;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.feature.FrequencyTermBasedFBMaster;
import uk.ac.shef.dcs.jate.model.JATEDocument;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.IOUtil;
import uk.ac.shef.dcs.jate.util.JATEUtil;

import java.io.*;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;


public abstract class App {

    private final Logger log = LoggerFactory.getLogger(getClass());


    /**
     * corresponding to "-c" in command line
     * <p>
     * 'true' or 'false'. Whether to collect offsets of term occurrences in the corpus
     * and save in the output. Default is false.
     */
    protected Boolean collectTermInfo = false;

    /**
     * Three cutoff options to seperate real terms from non-terms. All values are inclusive
     *
     */
    /**
     * a cut off score
     */
    protected Double cutoffThreshold = null;

    /**
     * select highest ranked K
     */
    protected Integer cutoffTopK = null;

    /**
     * select highst ranked K%
     */
    protected Double cutoffTopKPercent = null;

    /**
     * corresponding to "-o" in command line
     * <p>
     * file path. If provided, the output list of terms is written to the file. Otherwise,
     * output is written to the console.
     */
    protected String outputFile = null;


    // Min total fequency of a term
    protected Integer prefilterMinTTF = 0;

    // Min frequency of a term appearing in different context
    protected Integer prefilterMinTCF = 0;

    //used by algorithms such as weirdness, glossex, termex that compares against a reference corpus
    protected String referenceFrequencyFilePath = null;

    public void setOutputFile(String outputFilePath) {
        outputFile = outputFilePath;
    }

    public String getOutputFile() {
        return outputFile;
    }

    protected FrequencyTermBasedFBMaster freqFeatureBuilder = null;
    // term indexed feature (typically frequency info.)
    // see also {@code AppATTF}
    protected FrequencyTermBased freqFeature = null;

    private static String DEFAULT_OUTPUT_FILE = "terms.txt";

    public App() {
    }

    protected static boolean isExport(Map<String, String> params) {
        return params.containsKey(AppParams.OUTPUT_FILE.getParamKey());
    }

    /**
     * if corpus provided, perform indexing first and then ranking & filtering
     *
     * @param corpusDir
     * @return true if corpus is provided otherwise false
     */
    protected static boolean isCorpusProvided(String corpusDir) {
        return corpusDir != null && !corpusDir.isEmpty();
    }

    private int parseIntParam(String name, String value) throws JATEException {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException nfe) {
            String msg = String.format("%s is not set correctly. An integer value is expected. " +
                    "Actual input is %s", name, value);
            log.error(msg);
            throw new JATEException(msg);
        }
    }

    private double parseDoubleParam(String name, String value) throws JATEException {
        try {
            return Double.parseDouble(value);
        } catch (NumberFormatException nfe) {
            String msg = String.format("%s is not set correctly. An integer value is expected. " +
                    "Actual input is %s", name, value);
            log.error(msg);
            throw new JATEException(msg);
        }
    }


    /**
     * Initialise common run-time parameters
     *
     * @param params, command line run-time parameters (paramKey, value) for term
     *                ranking algorithms
     * @throws JATEException
     * @see AppParams
     * @see AppParams
     */
    App(Map<String, String> params) throws JATEException {
        if (params.containsKey(AppParams.CUTOFF_TOP_K.getParamKey())) {
            String topKSetting = params.get(AppParams.CUTOFF_TOP_K.getParamKey());
            this.cutoffTopK = parseIntParam("Cutoff parameter Top K " + AppParams.CUTOFF_TOP_K.getParamKey()
                    , topKSetting);
            log.debug(String.format("Cutoff parameter: top [%s] term candidates will be selected as final terms", topKSetting));
        }

        if (params.containsKey(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey())) {
            String topPercSetting = params.get(AppParams.CUTOFF_TOP_K_PERCENT.getParamKey());
            this.cutoffTopKPercent = parseDoubleParam("Cutoff parameter Top K% " + AppParams.CUTOFF_TOP_K_PERCENT.getParamKey()
                    , topPercSetting);
            log.debug(String.format("Cutoff parameter: top [%s] percent of term candidates will be selected as final terms", topPercSetting));
        }

        if (params.containsKey(AppParams.CUTOFF_THRESHOLD.getParamKey())) {
            String cutOffThreshold = params.get(AppParams.CUTOFF_THRESHOLD.getParamKey());
            this.cutoffThreshold = parseDoubleParam("Cutoff parameter term score " + AppParams.CUTOFF_THRESHOLD.
                    getParamKey(), cutOffThreshold);
            log.debug(String.format("Cutoff paramter: terms with a minimum score of [%s] will be selected as final terms", cutOffThreshold));
        }

        if (params.containsKey(AppParams.PREFILTER_MIN_TERM_CONTEXT_FREQUENCY.getParamKey())) {
            String minTCF = params.get(AppParams.PREFILTER_MIN_TERM_CONTEXT_FREQUENCY.getParamKey());
            this.prefilterMinTCF = parseIntParam("Pre-filter minimum term context frequency " +
                    AppParams.PREFILTER_MIN_TERM_CONTEXT_FREQUENCY, minTCF);
            log.debug(String.format("Pre-filter mininum term context frequency (used by co-occurrence based methods) is set to [%s]", prefilterMinTCF));
        }


        if (params.containsKey(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey())) {
            String minTTF = params.get(AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY.getParamKey());
            this.prefilterMinTTF = parseIntParam("Pre-filter minimum total term frequency " +
                    AppParams.PREFILTER_MIN_TERM_TOTAL_FREQUENCY, minTTF);
            log.debug(String.format("Pre-filter mininum total term frequency is set to [%s]", prefilterMinTCF));
        }


        if (params.containsKey(AppParams.COLLECT_TERM_INFO.getParamKey())) {
            String collectTermOffsets = params.get(AppParams.COLLECT_TERM_INFO.getParamKey());
            if (collectTermOffsets != null && collectTermOffsets.equalsIgnoreCase("true")) {
                this.collectTermInfo = true;
                log.debug("Term offsets will be collected and written to the output");
            }
        }

        if (params.containsKey(AppParams.OUTPUT_FILE.getParamKey())) {
            String outFile = params.get(AppParams.OUTPUT_FILE.getParamKey());

            String msg = "Output file is missing or its path is invalid (you can ignore this if you are running " +
                    "in the Plugin mode and do not require the list of terms to be exported to a file.) \n" +
                    "Output will be written to a default file 'terms.txt' instead.";
            if (outFile == null) {
                log.warn(msg);
                outputFile = DEFAULT_OUTPUT_FILE;
            } else {
                try {
                    PrintWriter p = new PrintWriter(outFile);
                    p.close();
                    outputFile = outFile;
                } catch (IOException ioe) {
                    log.warn(msg);
                    outputFile = DEFAULT_OUTPUT_FILE;
                }
            }

        }

    }

    /**
     * @param initParams  map param accepting reference frequency file
     * @throws JATEException
     * @see AppParams#REFERENCE_FREQUENCY_FILE
     */
    protected void initalizeRefFreqParam(Map<String, String> initParams) throws JATEException {
        if (initParams.containsKey(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey())) {
            String refFreqFilePath = initParams.get(AppParams.REFERENCE_FREQUENCY_FILE.getParamKey());

            if (refFreqFilePath == null) {
                String msg = String.format("Reference corpus frequency file %s is not set. A file path is expected.",
                        AppParams.REFERENCE_FREQUENCY_FILE.getParamKey());

                log.error(msg);
                throw new JATEException(msg);
            }

            File refFreqFile = new File(refFreqFilePath);
            if (!refFreqFile.exists()) {
                String msg = String.format("Excepted reference corpus frequency file %s does not exist in %s.",
                        AppParams.REFERENCE_FREQUENCY_FILE.getParamKey(),
                        refFreqFilePath);
                log.error(msg);
                throw new JATEException(msg);
            }

            this.referenceFrequencyFilePath = refFreqFilePath;
        } else {
            String msg = String.format("Reference corpus frequency file (-r) %s is not set. A file path is expected.",
                    AppParams.REFERENCE_FREQUENCY_FILE.getParamKey());
            log.error(msg);
            throw new JATEException(msg);
        }
    }


    /**
     * Rank and Filter terms candidates based on a given Solr index
     * <p>
     * This method assume that documents are indexed in the solr container (solrHomePath)
     * and term candidates have already been extracted at index-time.
     * <p>
     * jate properties provides necessary information needed by the ATE algorithm (e.g., text field, ngram info field,
     * term candiate field, cut-off threshold)
     *
     * @param core           solr core
     * @param jatePropertyFile  property file path, use the default one from classpath if not provided
     * @return List<JATETerm>  the list of terms extracted
     * @throws IOException
     * @throws JATEException
     */
    public abstract List<JATETerm> extract(SolrCore core, String jatePropertyFile) throws IOException, JATEException;

    /**
     * Rank and Filter terms candidates based on a given Solr index
     * <p>
     * This method assume that documents are indexed in the solr container (solrHomePath)
     * and term candidates have already been extracted at index-time.
     * <p>
     * jate properties provides necessary information needed by the ATE algorithm (e.g., text field, ngram info field,
     * term candiate field, cut-off threshold)
     *
     * @param solrHomePath     solr core home directory path
     * @param coreName         solr core name from where term recognition is executed
     * @param jatePropertyFile jate property file path
     * @return List<JATETerm> the list of terms extracted
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
            
//            core.close();
//            solrServer.close();
            Iterator<JATETerm> it = result.iterator();
            while(it.hasNext()){
                JATETerm jt = it.next();
                if(jt.getString().replaceAll("[^a-zA-Z0-9]","").length()==0)
                    it.remove();
            }
            return result;
        } finally {
//            try {
                if (solrServer != null) {

//                    try {
//						solrServer.commit();
//					} catch (SolrServerException e) {
//						log.error(e.toString());
//					}

                    if (core != null) {
                        core.close();
                    }

                    solrServer.close();
                    //workaround to avoid ERROR "CachingDirectoryFactory:150"
                    solrServer.getCoreContainer().getAllCoreNames().forEach(currentCoreName -> {
                        File lock = Paths.get(solrHomePath, currentCoreName, "data", "index", "write.lock").toFile();
                        if (lock.exists()) {
                            lock.delete();
                        }
                    });
                }
//                if (solrServer != null) {
//                    solrServer.commit(true, true);
//                    Thread.sleep(5000);
//
//                    solrServer.getCoreContainer().shutdown();
//                    solrServer.close();
//                }
//            } catch (Exception e) {
//                log.error("Unable to close solr index, error cause:");
//                log.error(ExceptionUtils.getFullStackTrace(e));
//            }
        }        
    }

    /**
     * Corpus indexing and candidate extraction
     *
     * @param corpusDir  corpus directory to be indexed, from where term candidate will be extracted
     * @param solrHomePath   solr home path is the solr core container
     * @param coreName   solr core name
     * @param jatePropertyFile   JATE properties file
     */
    public void index(Path corpusDir, Path solrHomePath, String coreName, String jatePropertyFile)
            throws JATEException {
        log.info(String.format("Indexing corpus from [%s] and perform candidate extraction ...", corpusDir));

        List<Path> files = JATEUtil.loadFiles(corpusDir);
        log.info(" [" + files.size() + "] files are scanned and will be indexed and analysed.");

        final EmbeddedSolrServer solrServer = new EmbeddedSolrServer(solrHomePath, coreName);
        
        JATEProperties jateProp = getJateProperties(jatePropertyFile);

        try {
            files.stream().forEach(file -> {
                try {
                    indexJATEDocuments(file, solrServer, jateProp, false);
                } catch (JATEException e) {
                    e.printStackTrace();
                }
            });

            solrServer.commit();
            log.info("all corpus are indexed with term candidates.");
        } catch (SolrServerException | IOException e) {
            throw new JATEException(String.format("Failed to index current corpus. Error:[%s]", e.toString()));
        } finally {
            try {
//                if (core != null) {
//                    core.close();
//                }
//                if (solrServer != null) {
                solrServer.close();
//                }
            } catch (Exception e) {
                log.error("Unable to close solr index, error cause:");
                log.error(ExceptionUtils.getFullStackTrace(e));
            }
        }
    }

    protected void indexJATEDocuments(Path file, EmbeddedSolrServer solrServer, JATEProperties jateProp, boolean commit) throws JATEException {
        if (file == null) {
            return;
        }

        try {
            JATEDocument jateDocument = JATEUtil.loadJATEDocument(file);

            if (isNotEmpty(jateDocument))
                JATEUtil.addNewDoc(solrServer, jateDocument.getId(),
                        jateDocument.getId(), jateDocument.getContent(), jateProp, commit);
        } catch (FileNotFoundException ffe) {
            throw new JATEException(ffe.toString());
        } catch (IOException ioe) {
            throw new JATEException(String.format("failed to index [%s]", file.toString()) + ioe.toString());
        } catch (SolrServerException sse) {
            throw new JATEException(String.format("failed to index [%s] ", file.toString()) + sse.toString());
        }
    }

    private static boolean isNotEmpty(JATEDocument jateDocument) {
        return jateDocument != null &&
                jateDocument.getContent() != null &&
                jateDocument.getContent().trim().length() != 0;
    }

    /**
     * Only effective under the Embedded mode.
     * <p>
     * User can choose to output term offset information. If this is the case, this method will be
     * called upon every final term. Iterating through the solr index can be slow so this method can
     * take some time.
     *
     * @param leafReader         index reader
     * @param terms              term list
     * @param ngramInfoFieldname indexed n-gram field, see 'jate_text_2_ngrams' field in example schema
     * @param idFieldname        doc unique id field
     * @throws IOException
     */
    public void collectTermOffsets(List<JATETerm> terms, LeafReader leafReader, String ngramInfoFieldname,
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
     * @param terms              filtered term candidates
     * @param searcher           solr index searcher
     * @param content2NgramField solr content to ngram TR aware field
     * @param idField            solr unique id
     * @throws JATEException
     */
    public void addAdditionalTermInfo(List<JATETerm> terms, SolrIndexSearcher searcher, String content2NgramField,
                                      String idField) throws JATEException {
        if (this.collectTermInfo) {
            try {
                collectTermOffsets(terms, searcher.getLeafReader(), content2NgramField, idField);
            } catch (IOException e) {
                throw new JATEException("I/O exception when reading Solr index. " + e.toString());
            }
        }
    }

    /**
     * Term candidate filtering by total (whole index/corpus) term frequency
     * (exclusive)
     *
     * @param candidates  term candidates
     * @throws JATEException
     */
    protected void filterByTTF(List<String> candidates) throws JATEException {
        if (this.freqFeature == null) {
            throw new JATEException("FrequencyTermBased is not initialised for TTF term filtering.");
        }

        if (candidates == null || candidates.size() == 0) {
            return;
        }

        if (this.prefilterMinTTF != null) {
            log.debug(String.format("Filter [%s] term candidates by total term frequency [%s] (exclusive)",
                    candidates.size(), this.prefilterMinTTF));
            Iterator<String> it = candidates.iterator();
            while (it.hasNext()) {
                String t = it.next();
                if (this.freqFeature.getTTF(t) < prefilterMinTTF)
                    it.remove();
            }
            log.debug(String.format("filtered term candidate size: [%s]", candidates.size()));
        }
    }

    protected static Map<String, String> getParams(String[] args) {
        Map<String, String> params = new HashMap<>();
        if (args.length < 3) {
            return params;
        }
        for (int i = 0; i < args.length; i++) {
            if (i == args.length - 2 || i == args.length - 1) {
                continue;
            }

            if (i + 1 < args.length) {
                String param = args[i];
                String value = args[i + 1];
                i++;
                params.put(param, value);
            }
        }
        return params;
    }


    public void write(List<JATETerm> terms) throws IOException {
        Gson gson = new Gson();
        if (outputFile == null) {
            throw new IOException("Output file is null");
        } else {
            log.info(String.format("Exporting terms to [%s]", outputFile));
            Writer w = IOUtil.getUTF8Writer(outputFile);
            gson.toJson(terms, w);
            w.close();
            log.info("complete.");
        }
    }

    /**
     * filter term candidates by cut-off threshold, top K or K% where applicable
     *
     * @param terms  candidate terms to be filtered
     * @return List<JATETerm>, filtered terms
     */
    protected List<JATETerm> cutoff(List<JATETerm> terms) {
        if (this.cutoffThreshold != null) {
            return cutoffByTermScoreThreshold(terms, this.cutoffThreshold);
        } else if (this.cutoffTopK != null) {
            return cutoffByTopK(terms, this.cutoffTopK);
        } else if (this.cutoffTopKPercent != null) {
            return cutoffByTopKPercent(terms, this.cutoffTopKPercent);
        }

        return terms;
    }

    /**
     * Filter term candidate list by termhood/unithood based threshold
     * (inclusive)
     *
     * @param terms            a list of term candidates with term weight
     * @param cutOffThreshold  term score measured by ATR algorithms
     * @return List<JATETerm>  filtered terms
     */
    protected List<JATETerm> cutoffByTermScoreThreshold(List<JATETerm> terms, Double cutOffThreshold) {
        List<JATETerm> weightedTerms = new ArrayList<>();
        weightedTerms.addAll(terms);

        if (cutOffThreshold != null & weightedTerms.size() > 0) {
            log.debug(String.format("cutoff [%s] term candidates by termhood/unithood based threshold [%s]",
                    weightedTerms.size(), cutOffThreshold));

            Iterator<JATETerm> iterTerms = weightedTerms.iterator();
            while (iterTerms.hasNext()) {
                if (iterTerms.next().getScore() < cutOffThreshold)
                    iterTerms.remove();
            }
            log.debug(String.format("final filtered term candidate size [%s]", terms.size()));
        }
        return weightedTerms;
    }

    /**
     * Filter term candidate list by top N (inclusive) terms
     *
     * @param terms  terms ranked by term weight
     * @param topK   top N term number
     * @return List<JATETerm>  filtered terms
     */
    protected List<JATETerm> cutoffByTopK(List<JATETerm> terms, Integer topK) {
        if (topK != null & terms != null & terms.size() > 0 & topK < terms.size()) {
            log.debug(String.format("cutoff [%s] term candidates by Top [%s] ...", terms.size(), topK));
            terms = terms.subList(0, (topK + 1));
            log.debug(String.format("final filtered term list size is [%s]", terms.size()));
        }
        return terms;
    }

    /**
     * Filter term candidate list by rounding top percentage of total term size
     *
     * @param terms  weighted term list
     * @param topPercentage  top percentage of weighted terms to be retained
     * @return List<JATETerm>  filtered top K percent terms
     */
    protected List<JATETerm> cutoffByTopKPercent(List<JATETerm> terms, Double topPercentage) {
        if (topPercentage != null & terms != null & terms.size() > 0) {
            log.debug(String.format("filter [%s] term candidates by Top [%s] percent (rounded) ...",
                    terms.size(),
					topPercentage * 100));
            Integer topN = (int) Math.round(topPercentage * terms.size());
            if (topN > 0)
                terms = cutoffByTopK(terms, topN);
            log.debug(String.format("final filtered term list size is [%s]", terms.size()));
        }
        return terms;
    }

    protected static String getJATEProperties(Map<String, String> params) {
        if (params.containsKey(AppParams.JATE_PROPERTIES_FILE.getParamKey())) {
            return params.get(AppParams.JATE_PROPERTIES_FILE.getParamKey());
        }
        return null;
    }

    protected static String getCorpusDir(Map<String, String> params) {
        if (params.containsKey(AppParams.CORPUS_DIR.getParamKey())) {
            return params.get(AppParams.CORPUS_DIR.getParamKey());
        }
        return null;
    }

    /**
     * load JATE property file, if not provided (i.e., null), the file will be loaded from the default one.
     * @param jatePropertyFile  jate property file path where the file will be loaded
     * @return JATEProperties object
     * @throws JATEException
     */
    public static JATEProperties getJateProperties(String jatePropertyFile) throws JATEException {
        JATEProperties properties;
        if (jatePropertyFile != null && !jatePropertyFile.isEmpty()) {
            properties = new JATEProperties(jatePropertyFile);
        } else {
            properties = new JATEProperties();
        }
        return properties;
    }

    protected static void printHelp() {
        StringBuilder sb = new StringBuilder("Usage:\n");
        sb.append("java -cp '[CLASSPATH]' ").append(App.class.getName()).append(" ")
                .append("[OPTIONS] [SOLR_HOME_PATH] [SOLR_CORE_NAME] ").append("\n\n");
        sb.append("Example: java -cp '/libs/*' /corpus/ /solr/server/solr jate  -prop jate.properties -cf.k 20  ...\n\n");
        sb.append("[OPTIONS]:\n")
                .append("\t\t-corpusDir\t\t. The corpus to be indexed, from where term candidate will be extracted, ranked and weighted.")
                .append("\t\t-prop\t\t. jate.properties file for the configuration of Solr schema.")
                .append("\t\t-c\t\t'true' or 'false'. Whether to collect term information for exporting, e.g., offsets in documents. Default is false.\n")
                .append("\t\t-r\t\t. Reference corpus frequency file path (-r) is required by AppGlossEx, AppTermEx and AppWeirdness.\n")
                .append("\t\t-cf.t\t\tA number. Cutoff score threshold for selecting terms. If multiple -cf.* parameters are set the preference order will be cf.t, cf.k, cf.kp.")
                .append("\n")
                .append("\t\t-cf.k\t\tA number. Cutoff top ranked K terms to be selected. If multiple -cf.* parameters are set the preference order will be cf.t, cf.k, cf.kp.")
                .append("\n")
                .append("\t\t-cf.kp\t\tA number. Cutoff top ranked K% terms to be selected. If multiple -cf.* parameters are set the preference order will be cf.t, cf.k, cf.kp.")
                .append("\n")
                .append("\t\t-pf.mttf\t\tA number. Pre-filter minimum total term frequency. \n")
                .append("\t\t-pf.mtcf\t\tA number. Pre-filter minimum context frequency of a term (used by co-occurrence based methods). \n")

                .append("\t\t-o\t\tA file path to save output. \n");
        System.out.println(sb);
    }
}
