package uk.ac.shef.dcs.jate.app;

import org.apache.commons.lang.math.NumberUtils;
import org.apache.solr.core.SolrCore;
import org.apache.solr.search.SolrIndexSearcher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import uk.ac.shef.dcs.jate.JATEException;
import uk.ac.shef.dcs.jate.JATEProperties;
import uk.ac.shef.dcs.jate.algorithm.CValue;
import uk.ac.shef.dcs.jate.app.App.CommandLineParams;
import uk.ac.shef.dcs.jate.feature.*;
import uk.ac.shef.dcs.jate.model.JATETerm;
import uk.ac.shef.dcs.jate.util.JATEUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AppCValue extends App {
	private final Logger log = LoggerFactory.getLogger(getClass());
	
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
			terms = new AppCValue(params).extract(solrHomePath, solrCoreName, jatePropertyFile);

			String paramValue = params.get(CommandLineParams.OUTPUT_FILE.getParamKey());
			write(terms, paramValue);
		} catch (IOException e) {
			e.printStackTrace();
		} catch (JATEException e) {
			e.printStackTrace();
		}

	}

	public AppCValue(Map<String, String> initParams) throws JATEException {
		super(initParams);
		
		if (initParams.containsKey(CommandLineParams.MIN_TOTAL_TERM_FREQUENCY.getParamKey())) {
			String minTTF = initParams.get(CommandLineParams.MIN_TOTAL_TERM_FREQUENCY.getParamKey());
			if (!NumberUtils.isNumber(minTTF)) {
				log.error("Minimum total term frequency ('-mttf') is not set correctly! A string of numeric value is expected!");
				throw new JATEException("A string of numeric value is expected for Minimum total term frequency ('-mttf') !");
			} else if (JATEUtil.isInteger(minTTF)) {
				log.debug(String.format("Mininum total term frequency is set to [%s]", minTTF));
				this.minTTF = Integer.parseInt(minTTF);
			} else {
				log.error("Minimum total term frequency ('-mttf') is not set correctly! A string of numeric value is expected!");
				throw new JATEException("A string of numeric value is expected for Minimum total term frequency ('-mttf') !");
			}
		}
	}

	@Override
	public List<JATETerm> extract(SolrCore core, String jatePropertyFile)
			throws IOException, JATEException {
		SolrIndexSearcher searcher = core.getSearcher().get();
		try {
			JATEProperties properties = new JATEProperties(jatePropertyFile);
			
			this.freqFeatureBuilder = new FrequencyTermBasedFBMaster(searcher, properties, 0);
			this.freqFeature = (FrequencyTermBased) freqFeatureBuilder.build();

			ContainmentFBMaster cb = new ContainmentFBMaster(searcher, properties);
			Containment cf = (Containment) cb.build();

			CValue cvalue = new CValue();
			cvalue.registerFeature(FrequencyTermBased.class.getName(), this.freqFeature);
			cvalue.registerFeature(Containment.class.getName(), cf);

			List<String> candidates = new ArrayList<>(this.freqFeature.getMapTerm2TTF().keySet());
			
			filterByTTF(candidates, this.minTTF);

			List<JATETerm> terms = cvalue.execute(candidates);
			terms = filtering(terms);

			addAdditionalTermInfo(terms, searcher, properties.getSolrFieldnameJATENGramInfo(),
					properties.getSolrFieldnameID());
			return terms;
		} finally {
			searcher.close();
		}

	}

}
