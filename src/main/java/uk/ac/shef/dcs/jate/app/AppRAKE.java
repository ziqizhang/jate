package uk.ac.shef.dcs.jate.app;

import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import uk.ac.shef.dcs.jate.algorithm.RAKE;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppRAKE extends App {
    private final Logger log = LoggerFactory.getLogger(AppRAKE.class.getName());

    /**
     *
     * @param args, command-line params accepting solr home path, solr core name and more optional run-time parameters
     *              @see uk.ac.shef.dcs.jate.app.AppParams
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
            App appRake = new AppRAKE(params);
            if (isCorpusProvided(corpusDir)) {
                appRake.index(Paths.get(corpusDir), Paths.get(solrHomePath), solrCoreName, jatePropertyFile);
            }

            terms = appRake.extract(solrHomePath, solrCoreName, jatePropertyFile);

            if (isExport(params)) {
                appRake.write(terms);
            }

            System.exit(0);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JATEException e) {
            e.printStackTrace();
        }

    }

    /**
     * initialise run-time parameters for current algorithm
     * @param initParams, run-time parameters (e.g.,min term total freq, cutoff scoring threshold)  for current algorithm
     *      @see uk.ac.shef.dcs.jate.app.AppParams
     * @throws JATEException
     */
    public AppRAKE(Map<String, String> initParams) throws JATEException {
        super(initParams);
    }

    public AppRAKE() {

    }

    @Override
    public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        JATEProperties properties = getJateProperties(jatePropertyFile);

        return extract(core, properties);
    }

    public List<JATETerm> extract(SolrCore core, JATEProperties properties) throws JATEException {
        SolrIndexSearcher searcher = core.getSearcher().get();

        this.freqFeatureBuilder = new FrequencyTermBasedFBMaster(searcher, properties, 0);
        this.freqFeature = (FrequencyTermBased) freqFeatureBuilder.build();

        FrequencyTermBasedFBMaster fwbb = new FrequencyTermBasedFBMaster(searcher, properties, 1);
        FrequencyTermBased fwb = (FrequencyTermBased) fwbb.build();

        TermComponentIndexFBMaster tcib = new TermComponentIndexFBMaster(properties,
                new ArrayList<>(this.freqFeature.getMapTerm2TTF().keySet()));
        TermComponentIndex termComponentIndex = (TermComponentIndex) tcib.build();

        RAKE rake = new RAKE();
        rake.registerFeature(FrequencyTermBased.class.getName() + RAKE.SUFFIX_TERM, this.freqFeature);
        rake.registerFeature(FrequencyTermBased.class.getName() + RAKE.SUFFIX_WORD, fwb);
        rake.registerFeature(TermComponentIndex.class.getName(), termComponentIndex);

        List<String> candidates = new ArrayList<>(this.freqFeature.getMapTerm2TTF().keySet());

        filterByTTF(candidates);

        List<JATETerm> terms = rake.execute(candidates);
        terms = cutoff(terms);

        addAdditionalTermInfo(terms, searcher, properties.getSolrFieldNameJATENGramInfo(),
                properties.getSolrFieldNameID());
        return terms;
    }

    protected static void printHelp() {
        StringBuilder sb = new StringBuilder("RAKE, usage:\n");
        sb.append("java -cp '[CLASSPATH]' ").append(AppATTF.class.getName()).append(" [OPTIONS] ")
                .append("[LUCENE_INDEX_PATH] [JATE_PROPERTY_FILE]").append("\nE.g.:\n");
        sb.append("java -cp '/libs/*' -t 20 /solr/server/solr/jate/data jate.properties\n\n");
        sb.append("[OPTIONS]:\n")
                .append("\t\t-c\t\t'true' or 'false'. Whether to collect term information, e.g., offsets in documents. Default is false.\n")
                .append("\t\t-t\t\tA number. Score threshold for selecting terms. If not set then default -n is used.")
                .append("\n")
                .append("\t\t-n\t\tA number. If an integer is given, top N candidates are selected as terms. \n")
                .append("\t\t\t\tIf a decimal number is given, top N% of candidates are selected. Default is 0.25.\n");
        sb.append("\t\t-o\t\tA file path. If provided, the output is written to the file. \n")
                .append("\t\t\t\tOtherwise, output is written to the console.\n")
                .append("\t\t-mttf\t\tA number. Min total fequency of a term for it to be considered for co-occurrence computation. \n")
                .append("\t\t-mtcf\t\tA number. Min frequency of a term appearing in different context for it to be considered for co-occurrence computation. \n");
        System.out.println(sb);
    }
}
