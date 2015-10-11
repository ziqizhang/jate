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
import java.util.List;
import java.util.Map;

public class AppChiSquare extends App {
	private final Logger log = LoggerFactory.getLogger(getClass());
	private double frequentTermFT=0.3; //top 30% of the terms are considered to be 'frequent'
	
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

	public AppChiSquare(Map<String, String> initParams) throws JATEException {		
		super(initParams);
		log.info("initialise ChiSquare algorithm...");
		initializeFTParam(initParams);
	}

	private void initializeFTParam(Map<String, String> initParams) throws JATEException {
		//This param is Chi-Square only
			String sFT = initParams.get("-ft");
		if(sFT!=null) {
			try {
				frequentTermFT = Double.parseDouble(sFT);
				if (frequentTermFT > 1.0 || frequentTermFT <= 0.0)
					throw new JATEException("Frequent Term cutoff percentage ('-ff') is not set correctly! Value must be within (0,1.0]");
			} catch (NumberFormatException nfe) {
				StringBuilder msg =
						new StringBuilder("Frequent Term cutoff percentage ('-ff') is not set correctly! A decimal value is expected!");
				throw new JATEException(msg.toString());
			}
		}

	}

	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
			throws IOException, JATEException {
		SolrIndexSearcher searcher = core.getSearcher().get();
		try {
			JATEProperties properties = new JATEProperties(jatePropertyFile);
			FrequencyTermBasedFBMaster ftbb = new FrequencyTermBasedFBMaster(searcher, properties, 0);
			FrequencyTermBased ftb = (FrequencyTermBased) ftbb.build();

			FrequencyCtxSentenceBasedFBMaster fcsbb = new FrequencyCtxSentenceBasedFBMaster(searcher, properties,
					properties.getSolrFieldnameJATECTerms(), properties.getSolrFieldnameJATESentences());
			FrequencyCtxBased fcsb = (FrequencyCtxBased) fcsbb.build();

			FrequencyCtxBased ref_fcsb=(FrequencyCtxBased) (new FrequencyCtxBasedCopier(searcher, properties,fcsb,ftb,frequentTermFT).build());

			CooccurrenceFBMaster cb = new CooccurrenceFBMaster(searcher, properties, ftb, this.prefilterMinTTF, fcsb, ref_fcsb,this.prefilterMinTCF);
			Cooccurrence co = (Cooccurrence) cb.build();

			ChiSquare chi = new ChiSquare();
			chi.registerFeature(FrequencyTermBased.class.getName(), ftb);
			chi.registerFeature(FrequencyCtxBased.class.getName() + ChiSquare.SUFFIX_TERM, fcsb);
            chi.registerFeature(FrequencyCtxBased.class.getName() + ChiSquare.SUFFIX_REF_TERM, ref_fcsb);
			chi.registerFeature(Cooccurrence.class.getName(), co);

			List<JATETerm> terms = chi.execute(co.getTerms());
			terms = cutoff(terms);

			addAdditionalTermInfo(terms, searcher, properties.getSolrFieldnameJATENGramInfo(),
					properties.getSolrFieldnameID());
			return terms;
		} finally {
			searcher.close();
		}

	}

	protected static void printHelp() {
		StringBuilder sb = new StringBuilder("Chi-Square, usage:\n");
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
