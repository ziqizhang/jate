package uk.ac.shef.dcs.jate.app;

import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.ChiSquare;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;

import java.io.IOException;
import java.util.*;

public class AppChiSquare extends App {
    private final Logger log = LoggerFactory.getLogger(getClass());
    private double frequentTermFT = 0.3; //top 30% of the terms are considered to be 'frequent'

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
            App app = new AppChiSquare(params);
            terms = app.extract(solrHomePath, solrCoreName, jatePropertyFile);

            app.write(terms);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JATEException e) {
            e.printStackTrace();
        }
    }

    //TODO: should allow to initialse with default setting
    public AppChiSquare(Map<String, String> initParams) throws JATEException {
        super(initParams);
        initializeFTParam(initParams);
    }

    /**
     * TODO: provide more guidance and explanation about how to setup initial parameters
     *
     * @param initParams, TODO
     * @throws JATEException
     */
    private void initializeFTParam(Map<String, String> initParams) throws JATEException {
        //This param is Chi-Square only
        String sFT = initParams.get(AppParams.CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE.getParamKey());
        if (sFT != null) {
            try {
                frequentTermFT = Double.parseDouble(sFT);
                if (frequentTermFT > 1.0 || frequentTermFT <= 0.0)
                    throw new JATEException("Frequent Term cutoff percentage ('-ft') is not set correctly! " +
                            "Value must be within (0,1.0]");
            } catch (NumberFormatException nfe) {
                throw new JATEException("Frequent Term cutoff percentage ('-ft') is not set correctly! " +
                        "A decimal value is expected!");
            }
        }

    }

    @Override
    public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
            throws IOException, JATEException {
        JATEProperties properties = new JATEProperties(jatePropertyFile);

        return extract(core, properties);

    }

    public List<JATETerm> extract(SolrCore core, JATEProperties properties) throws JATEException {
        SolrIndexSearcher searcher = core.getSearcher().get();
//        try {

        FrequencyTermBasedFBMaster ftbb = new FrequencyTermBasedFBMaster(searcher, properties, 0);
        FrequencyTermBased ftb = (FrequencyTermBased) ftbb.build();

        //sentence is a context
        FrequencyCtxSentenceBasedFBMaster fcsbb = new FrequencyCtxSentenceBasedFBMaster(searcher, properties, 0);
        FrequencyCtxBased fcsb = (FrequencyCtxBased) fcsbb.build();
        FrequencyCtxBased ref_fcsb = (FrequencyCtxBased) (new FrequencyCtxBasedCopier(searcher, properties, fcsb, ftb, frequentTermFT).build());
        //window is a context
            /*FrequencyCtxWindowBasedFBMaster fcsbb = new FrequencyCtxWindowBasedFBMaster(searcher, properties, null, 5, 0);
            FrequencyCtxBased fcsb = (FrequencyCtxBased) fcsbb.build();
            FrequencyCtxBased ref_fcsb = (FrequencyCtxBased)
                    (new FrequencyCtxWindowBasedFBMaster(searcher, properties, fcsb.getMapCtx2TTF().keySet(), 5, 0).build());*/

        List<String> inter = new ArrayList<>(fcsb.getCtxOverlapZones().keySet());
        inter.removeAll(ref_fcsb.getCtxOverlapZones().keySet());
        Collections.sort(inter);
            /*for (String t : inter)
				System.out.println(t);*/

        CooccurrenceFBMaster cb = new CooccurrenceFBMaster(searcher, properties, ftb, this.prefilterMinTTF, fcsb, ref_fcsb, this.prefilterMinTCF);
        Cooccurrence co = (Cooccurrence) cb.build();

        //feature expected probability for frequent terms
        ChiSquareFrequentTermsFBMaster cf = new ChiSquareFrequentTermsFBMaster(
                ref_fcsb.getMapCtx2TTF(), ref_fcsb.getTerm2Ctx(),ftb.getCorpusTotal(),properties);
        ChiSquareFrequentTerms cff = (ChiSquareFrequentTerms) cf.build();

        ChiSquare chi = new ChiSquare();
        chi.registerFeature(FrequencyCtxBased.class.getName() + ChiSquare.SUFFIX_TERM, fcsb);
        chi.registerFeature(Cooccurrence.class.getName(), co);
        chi.registerFeature(ChiSquareFrequentTerms.class.getName(), cff);

        List<JATETerm> terms = chi.execute(co.getTerms());
        terms = cutoff(terms);

        addAdditionalTermInfo(terms, searcher, properties.getSolrFieldNameJATENGramInfo(),
                properties.getSolrFieldNameID());
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

}
