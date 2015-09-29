package uk.ac.shef.dcs.jate.app;

import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.CValue;
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
public class AppCValue extends App {
    public static void main(String[] args) throws JATEException, IOException {
        if (args.length < 1) {
            printHelp();
            System.exit(1);
        }
        String indexPath = args[args.length - 2];
        String jatePropertyFile=args[args.length - 1];
        Map<String, String> params = getParams(args);

        List<JATETerm> terms = new AppCValue().extract(indexPath, jatePropertyFile, params);
        String paramValue=params.get("-o");
        write(terms,paramValue);
    }

    @Override
    public List<JATETerm> extract(String indexPath, String jatePropertyFile, Map<String, String> params) throws IOException, JATEException {
        IndexReader indexReader = DirectoryReader.open(FSDirectory.open(Paths.get(indexPath)));
        JATEProperties properties = new JATEProperties(jatePropertyFile);
        FrequencyTermBasedFBMaster ftbb = new
                FrequencyTermBasedFBMaster(indexReader, properties, 0);
        FrequencyTermBased ftb = (FrequencyTermBased)ftbb.build();

        ContainmentFBMaster cb = new
                ContainmentFBMaster(indexReader, properties);
        Containment cf = (Containment)cb.build();

        CValue cvalue = new CValue();
        cvalue.registerFeature(FrequencyTermBased.class.getName(), ftb);
        cvalue.registerFeature(Containment.class.getName(), cf);

        List<JATETerm> terms=cvalue.execute(ftb.getMapTerm2TTF().keySet());
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true")) {
            collectTermInfo(indexReader, terms);
        }
        indexReader.close();
        return terms;
    }
}
