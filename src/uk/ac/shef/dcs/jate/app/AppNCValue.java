package uk.ac.shef.dcs.jate.app;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.NCValue;
import uk.ac.shef.dcs.jate.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Created by zqz on 24/09/2015.
 */
public class AppNCValue extends App {
    public static void main(String[] args) throws JATEException, IOException {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String indexPath = args[args.length - 2];
        Map<String, String> params = getParams(args);

        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        JATEProperties properties = new JATEProperties(args[args.length - 1]);
        FrequencyTermBasedFBMaster ftbb = new
                FrequencyTermBasedFBMaster(indexReader, properties, 0);
        FrequencyTermBased ftb = (FrequencyTermBased)ftbb.build();

        ContainmentFBMaster cb = new
                ContainmentFBMaster(indexReader, properties);
        Containment cf = (Containment)cb.build();

        FrequencyCtxSentenceBasedFBMaster fcsbb = new
                FrequencyCtxSentenceBasedFBMaster(indexReader, properties,
                properties.getSolrFieldnameJATECTerms(),
                properties.getSolrFieldnameJATESentences());
        FrequencyCtxBased fcsb = (FrequencyCtxBased)fcsbb.build();

        int minTTF = 0, minTCF=0;
        String minTTFStr=params.get("-mttf");
        if(minTTFStr!=null){
            try{minTTF=Integer.valueOf(minTTFStr);}
            catch (NumberFormatException n){}}
        String minTCFStr=params.get("-mtcf");
        if(minTCFStr!=null){
            try{minTCF=Integer.valueOf(minTCFStr);}
            catch (NumberFormatException n){}}

        CooccurrenceFBMaster ccb = new CooccurrenceFBMaster(indexReader, properties, ftb, minTTF, fcsb,
                minTCF);
        Cooccurrence co = (Cooccurrence)ccb.build();

        NCValue ncvalue = new NCValue();
        ncvalue.registerFeature(FrequencyTermBased.class.getName(), ftb);
        ncvalue.registerFeature(Containment.class.getName(), cf);
        ncvalue.registerFeature(Cooccurrence.class.getName(), co);

        List<JATETerm> terms=ncvalue.execute(co.getTerms());
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true")) {
            collectTermInfo(indexReader, terms);
        }
        paramValue=params.get("-o");
        write(terms,paramValue);
        indexReader.close();
    }

    protected static void printHelp() {
        StringBuilder sb = new StringBuilder("NCValue, usage:\n");
        sb.append("java -cp '[CLASSPATH]' ").append(AppATTF.class.getName())
                .append(" [OPTIONS] ").append("[LUCENE_INDEX_PATH] [JATE_PROPERTY_FILE]").append("\nE.g.:\n");
        sb.append("java -cp '/libs/*' -t 20 /solr/server/solr/jate/data jate.properties\n\n");
        sb.append("[OPTIONS]:\n")
                .append("\t\t-c\t\t'true' or 'false'. Whether to collect term information, e.g., offsets in documents. Default is false.\n")
                .append("\t\t-t\t\tA number. Score threshold for selecting terms. If not set then default -n is used.").append("\n")
                .append("\t\t-n\t\tA number. If an integer is given, top N candidates are selected as terms. \n")
                .append("\t\t\t\tIf a decimal number is given, top N% of candidates are selected. Default is 0.25.\n");
        sb.append("\t\t-o\t\tA file path. If provided, the output is written to the file. \n")
                .append("\t\t\t\tOtherwise, output is written to the console.\n")
                .append("\t\t-mttf\t\tA number. Min total fequency of a term for it to be considered for co-occurrence computation. \n")
                .append("\t\t-mtcf\t\tA number. Min frequency of a term appearing in different context for it to be considered for co-occurrence computation. \n")
        ;
        System.out.println(sb);
    }
}
