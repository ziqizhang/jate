package uk.ac.shef.dcs.jate.app;

import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;

import uk.ac.shef.dcs.jate.algorithm.RAKE;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppRAKE extends App {
    public static void main(String[] args) {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String solrHomePath = args[args.length - 3];
        String solrCoreName = args[args.length - 2];
        String jatePropertyFile = args[args.length - 1];
        Map<String, String> params = getParams(args);

        List<JATETerm> terms;
        try {
            App appRake = new AppRAKE(params);
            terms = appRake.extract(solrHomePath, solrCoreName, jatePropertyFile);

            String paramValue = params.get(AppParams.OUTPUT_FILE.getParamKey());
            appRake.write(terms);

        } catch (IOException e) {
            e.printStackTrace();
        } catch (JATEException e) {
            e.printStackTrace();
        }

    }

    public AppRAKE(Map<String, String> initParams) throws JATEException {
        super(initParams);
    }

    public AppRAKE() {

    }

    @Override
    public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        JATEProperties properties = new JATEProperties(jatePropertyFile);

        return extract(core, properties);
    }

    public List<JATETerm> extract(SolrCore core, JATEProperties properties) throws JATEException, IOException {
        SolrIndexSearcher searcher = core.getSearcher().get();
        try {

            this.freqFeatureBuilder = new FrequencyTermBasedFBMaster(searcher, properties, 0);
            this.freqFeature = (FrequencyTermBased) freqFeatureBuilder.build();

            FrequencyTermBasedFBMaster fwbb = new FrequencyTermBasedFBMaster(searcher, properties, 1);
            FrequencyTermBased fwb = (FrequencyTermBased) fwbb.build();

            FrequencyCtxSentenceBasedFBMaster fcsbb = new FrequencyCtxSentenceBasedFBMaster(searcher, properties,
                    1);
            FrequencyCtxBased fcsb = (FrequencyCtxBased) fcsbb.build();

            CooccurrenceFBMaster cb = new CooccurrenceFBMaster(searcher, properties, fwb, prefilterMinTTF, fcsb, fcsb, prefilterMinTCF);
            Cooccurrence co = (Cooccurrence) cb.build();

            RAKE rake = new RAKE();
            rake.registerFeature(FrequencyTermBased.class.getName() + RAKE.SUFFIX_WORD, fwb);
            rake.registerFeature(Cooccurrence.class.getName() + RAKE.SUFFIX_WORD, co);

            List<String> candidates = new ArrayList<>(this.freqFeature.getMapTerm2TTF().keySet());

            filterByTTF(candidates);

            List<JATETerm> terms = rake.execute(candidates);
            terms = cutoff(terms);

            addAdditionalTermInfo(terms, searcher, properties.getSolrFieldNameJATENGramInfo(),
                    properties.getSolrFieldNameID());
            return terms;

        } finally {
            searcher.close();
        }
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
