package uk.ac.shef.dcs.jate.v2.app;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import uk.ac.shef.dcs.jate.v2.JATEException;
import uk.ac.shef.dcs.jate.v2.JATEProperties;
import uk.ac.shef.dcs.jate.v2.algorithm.ChiSquare;
import uk.ac.shef.dcs.jate.v2.algorithm.TermInfoCollector;
import uk.ac.shef.dcs.jate.v2.feature.*;
import uk.ac.shef.dcs.jate.v2.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by zqz on 23/09/2015.
 */
public class AppChiSquare extends AbstractApp {
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

        FrequencyCtxSentenceBasedFBMaster fcsbb = new
                FrequencyCtxSentenceBasedFBMaster(indexReader, properties,
                properties.getSolrFieldnameJATETermsAll(),
                properties.getSolrFieldnameJATESentencesAll());
        FrequencyCtxBased fcsb = (FrequencyCtxBased)fcsbb.build();

        CooccurrenceFBMaster cb = new CooccurrenceFBMaster(indexReader, properties, fcsb);
        Cooccurrence co = (Cooccurrence)cb.build();

        ChiSquare chi = new ChiSquare();
        chi.registerFeature(FrequencyTermBased.class.getName(), ftb);
        chi.registerFeature(FrequencyCtxBased.class.getName(), fcsb);
        chi.registerFeature(Cooccurrence.class.getName(), co);

        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true"))
            chi.setTermInfoCollector(new TermInfoCollector(indexReader));
        List<JATETerm> terms=chi.execute(ftb.getMapTerm2TTF().keySet());
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        paramValue=params.get("-o");
        write(terms,paramValue);
        indexReader.close();
    }


}
