package uk.ac.shef.dcs.jate.app;

import org.apache.solr.client.solrj.embedded.EmbeddedSolrServer;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.CValue;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
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
        String solrHomePath = args[args.length - 3];
        String solrCoreName=args[args.length-2];
        String jatePropertyFile=args[args.length - 1];
        Map<String, String> params = getParams(args);

        List<JATETerm> terms = new AppCValue().extract(solrHomePath, solrCoreName, jatePropertyFile, params);
        String paramValue=params.get("-o");
        write(terms,paramValue);
    }

    @Override
    public List<JATETerm> extract(SolrCore core, String jatePropertyFile, Map<String, String> params) throws IOException, JATEException {
        SolrIndexSearcher searcher = core.getSearcher().get();

        JATEProperties properties = new JATEProperties(jatePropertyFile);
        FrequencyTermBasedFBMaster ftbb = new
                FrequencyTermBasedFBMaster(searcher, properties, 0);
        FrequencyTermBased ftb = (FrequencyTermBased)ftbb.build();

        ContainmentFBMaster cb = new
                ContainmentFBMaster(searcher, properties);
        Containment cf = (Containment)cb.build();

        CValue cvalue = new CValue();
        cvalue.registerFeature(FrequencyTermBased.class.getName(), ftb);
        cvalue.registerFeature(Containment.class.getName(), cf);

        List<String> candidates = new ArrayList<>(ftb.getMapTerm2TTF().keySet());
        int cutoffFreq = getParamCutoffFreq(params);
        filter(candidates, ftb, cutoffFreq);

        List<JATETerm> terms=cvalue.execute(candidates);
        terms=applyThresholds(terms, params.get("-t"), params.get("-n"));
        String paramValue=params.get("-c");
        if(paramValue!=null &&paramValue.equalsIgnoreCase("true")) {
            collectTermInfo(searcher.getLeafReader(), terms, properties.getSolrFieldnameJATENGramInfo(), properties.getSolrFieldnameID());
        }
        searcher.close();
        core.close();
        return terms;
    }

}
