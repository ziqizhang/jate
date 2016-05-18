package uk.ac.shef.dcs.jate.app;

import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.GlossEx;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppGlossEx extends App {
    private final Logger log = LoggerFactory.getLogger(AppGlossEx.class.getName());

    /**
     * @param args  command-line params accepting solr home path, solr core name and more optional run-time parameters
     * @see uk.ac.shef.dcs.jate.app.AppParams
     * <p>
     * and GlossEx specific parameter : reference frequency file
     * @see AppParams#REFERENCE_FREQUENCY_FILE
     */
    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String solrHomePath = args[args.length - 2];
        String solrCoreName = args[args.length - 1];

        Map<String, String> params = getParams(args);
        String jatePropertyFile = getJATEProperties(params);
        String corpusDir = getCorpusDir(params);

        List<JATETerm> terms;
        try {
            App app = new AppGlossEx(params);
            if (isCorpusProvided(corpusDir)) {
                app.index(Paths.get(corpusDir), Paths.get(solrHomePath), solrCoreName, jatePropertyFile);
            }

            terms = app.extract(solrHomePath, solrCoreName, jatePropertyFile);

            if (isExport(params)) {
                app.write(terms);
            }

            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JATEException e) {
            e.printStackTrace();
        }
    }

    /**
     * @param initParams  intial map params accepting pre-filtering, post-filtering params and GlossEx specific parameter
     * @throws JATEException
     * @see AppParams
     * @see AppParams#REFERENCE_FREQUENCY_FILE
     */
    public AppGlossEx(Map<String, String> initParams) throws JATEException {
        super(initParams);
        initalizeRefFreqParam(initParams);
    }


    @Override
    public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        log.info("start GlossEx term extraction for whole index ...");
        JATEProperties properties = getJateProperties(jatePropertyFile);

        return extract(core, properties);
    }

    public List<JATETerm> extract(SolrCore core, JATEProperties properties) throws JATEException {
        SolrIndexSearcher searcher = core.getSearcher().get();
//        try {
        this.freqFeatureBuilder = new FrequencyTermBasedFBMaster(searcher, properties, 0);
        this.freqFeature = (FrequencyTermBased) freqFeatureBuilder.build();

        FrequencyTermBasedFBMaster fwbb = new FrequencyTermBasedFBMaster(searcher, properties, 1);
        FrequencyTermBased fwb = (FrequencyTermBased) fwbb.build();

        TTFReferenceFeatureFileBuilder ftrb = new TTFReferenceFeatureFileBuilder(this.referenceFrequencyFilePath);
        FrequencyTermBased frb = ftrb.build();

        GlossEx glossex = new GlossEx();
        glossex.registerFeature(FrequencyTermBased.class.getName(), this.freqFeature);
        glossex.registerFeature(FrequencyTermBased.class.getName() + GlossEx.SUFFIX_WORD, fwb);
        glossex.registerFeature(FrequencyTermBased.class.getName() + GlossEx.SUFFIX_REF, frb);

        List<String> candidates = new ArrayList<>(this.freqFeature.getMapTerm2TTF().keySet());

        filterByTTF(candidates);

        List<JATETerm> terms = glossex.execute(candidates);
        terms = cutoff(terms);

        addAdditionalTermInfo(terms, searcher, properties.getSolrFieldNameJATENGramInfo(),
                properties.getSolrFieldNameID());

        log.info("complete GlossEx term extraction.");
        return terms;
//        } finally {
//            try {
//                searcher.close();
//            } catch (IOException e) {
//                e.printStackTrace();
//                log.error("Failed to close current SolrIndexSearcher!" + e.getCause().toString());
//            }
//        }
    }

    protected static void printHelp() {
        StringBuilder sb = new StringBuilder("GlossEx Usage:\n");
        sb.append("java -cp '[CLASSPATH]' ").append(AppATTF.class.getName()).append(" [OPTIONS] ")
                .append("-r [REF_TERM_TF_FILE] [LUCENE_INDEX_PATH] [JATE_PROPERTY_FILE]").append("\nE.g.:\n");
        sb.append(
                "java -cp '/libs/*' -t 20 -r /resource/bnc_unifrqs.normal /solr/server/solr/jate/data jate.properties ...\n\n");
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
