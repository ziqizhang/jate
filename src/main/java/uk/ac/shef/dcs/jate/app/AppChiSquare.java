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
import java.nio.file.Paths;
import java.util.*;

public class AppChiSquare extends App {
    private final Logger log = LoggerFactory.getLogger(getClass());
    //top 30% of the terms are considered to be 'frequent'
    private double frequentTermFT = 0.3;

    /**
     * @param args, command-line params accepting solr home path, solr core name
     *              <p>
     *              and more optional run-time parameters
     * @see uk.ac.shef.dcs.jate.app.AppParams
     * <p>
     * Chisquare specific setting: frequent term cutoff percentage
     * @see uk.ac.shef.dcs.jate.app.AppParams#CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE
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
            App app = new AppChiSquare(params);
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
     * @param initParams, initial parameters including pre-filtering and post-filtering parameters
     *                    and chisquare specific parameter
     * @throws JATEException
     * @see uk.ac.shef.dcs.jate.app.AppParams
     */
    public AppChiSquare(Map<String, String> initParams) throws JATEException {
        super(initParams);
        initializeFTParam(initParams);
    }

    /**
     * @param initParams, chisquare specific initial parameter
     * @throws JATEException
     * @see uk.ac.shef.dcs.jate.app.AppParams#CHISQUERE_FREQ_TERM_CUTOFF_PERCENTAGE
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
        JATEProperties properties = getJateProperties(jatePropertyFile);

        return extract(core, properties);

    }

    public List<JATETerm> extract(SolrCore core, JATEProperties properties) throws JATEException {
        SolrIndexSearcher searcher = core.getSearcher().get();

        FrequencyTermBasedFBMaster ftbb = new FrequencyTermBasedFBMaster(searcher, properties, 0);
        FrequencyTermBased ft = (FrequencyTermBased) ftbb.build();

        //sentence is a context
        FrequencyCtxSentenceBasedFBMaster fcsbb = new FrequencyCtxSentenceBasedFBMaster(searcher, properties, 0);
        FrequencyCtxBased fcs = (FrequencyCtxBased) fcsbb.build();
        FrequencyCtxBased ref_fcs = (FrequencyCtxBased)
                (new FrequencyCtxBasedCopier(searcher, properties, fcs, ft, frequentTermFT).build());
        //window is a context
            /*FrequencyCtxWindowBasedFBMaster fcsbb = new FrequencyCtxWindowBasedFBMaster(searcher, properties, null, 5, 0);
            FrequencyCtxBased fcsb = (FrequencyCtxBased) fcsbb.build();
            FrequencyCtxBased ref_fcsb = (FrequencyCtxBased)
                    (new FrequencyCtxWindowBasedFBMaster(searcher, properties, fcsb.getMapCtx2TTF().keySet(), 5, 0).build());*/

        CooccurrenceFBMaster cob = new CooccurrenceFBMaster(searcher, properties, ft,
                this.prefilterMinTTF, fcs, ref_fcs, this.prefilterMinTCF);
        Cooccurrence co = (Cooccurrence) cob.build();

        //feature expected probability for frequent terms
        ChiSquareFrequentTermsFBMaster cf = new ChiSquareFrequentTermsFBMaster(
                ref_fcs.getMapCtx2TTF(), ref_fcs.getTerm2Ctx(), ft.getCorpusTotal(), properties);
        ChiSquareFrequentTerms cff = (ChiSquareFrequentTerms) cf.build();

        ChiSquare chi = new ChiSquare();
        chi.registerFeature(FrequencyCtxBased.class.getName() + ChiSquare.SUFFIX_TERM, fcs);
        chi.registerFeature(Cooccurrence.class.getName(), co);
        chi.registerFeature(ChiSquareFrequentTerms.class.getName(), cff);

        List<JATETerm> terms = chi.execute(co.getTerms());
        terms = cutoff(terms);

        addAdditionalTermInfo(terms, searcher, properties.getSolrFieldNameJATENGramInfo(),
                properties.getSolrFieldNameID());
        return terms;
    }

}
