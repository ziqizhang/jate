package uk.ac.shef.dcs.jate.v2.app;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.SimpleFSLockFactory;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.algorithm.ATTF;
import uk.ac.shef.dcs.jate.v2.algorithm.Algorithm;
import uk.ac.shef.dcs.jate.v2.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.v2.feature.AbstractFeature;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBased;
import uk.ac.shef.dcs.jate.v2.feature.FrequencyTermBasedFBMaster;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Created by zqz on 22/09/2015.
 */
public class AppATTF extends AbstractApp {

    public static void main(String[] args) throws JATEException, IOException {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String indexPath = args[args.length - 2];
        Map<String, String> params = getParams(args);

        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        JATEProperties properties = new JATEProperties(args[args.length - 1]);
        FrequencyTermBasedFBMaster featureBuilder = new
                FrequencyTermBasedFBMaster(indexReader, properties, 0);
        FrequencyTermBased feature = (FrequencyTermBased)featureBuilder.build();
        Algorithm attf = new ATTF();
        attf.registerFeature(FrequencyTermBased.class.getName(), feature);

        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true"))
            attf.setTermInfoCollector(new TermInfoCollector(indexReader));
        List<JATETerm> terms=attf.execute(feature.getMapTerm2TTF().keySet());

        paramValue=params.get("-o");
        write(terms,paramValue);
    }

    private static void printHelp() {
        StringBuilder sb = new StringBuilder("Average Total Term Frequency (ATTF), usage:\n");
        sb.append("java -cp '[CLASSPATH]' ").append(AppATTF.class.getName())
                .append(" [OPTIONS] ").append("[LUCENE_INDEX_PATH] [JATE_PROPERTY_FILE]").append("\nE.g.:\n");
        sb.append("java -cp '/libs/*' -t 20 /solr/server/solr/jate/data jate.properties\n\n");
        sb.append("[OPTIONS]:\n")
                .append("\t\t-c\t\t'true' or 'false'. Whether to collect term information, e.g., offsets in documents. Default is false.\n")
                .append("\t\t-t\t\tA number. Score threshold for selecting terms. If not set then default -n is used.").append("\n")
                .append("\t\t-n\t\tA number. If an integer is given, top N candidates are selected as terms. \n")
        .append("\t\t\t\tIf a decimal number is given, top N% of candidates are selected. Default is 0.25.");
        sb.append("\t\t-o\t\tA file path. If provided, the output is written to the file. \n")
                .append("Otherwise, output is written to the console.");
        System.out.println(sb);
    }
}
