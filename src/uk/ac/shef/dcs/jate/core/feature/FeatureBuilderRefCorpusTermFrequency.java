package uk.ac.shef.dcs.jate.core.feature;

import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndex;
import uk.ac.shef.dcs.jate.JATEException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.logging.Logger;

/**
 * A specific type of feature builder that builds an instance of FeatureRefCorpusTermFrequency. This is a dummy class
 * which reads the data from a text file which stores information as:
 * <br> [freq_in_corpus]      [term]
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public class FeatureBuilderRefCorpusTermFrequency extends AbstractFeatureBuilder {

	private static Logger _logger = Logger.getLogger(FeatureBuilderRefCorpusTermFrequency.class.getName());

	private final String _refStatsPath;

	/**
	 * Default constructor
	 * @param refStatsPath file path to the reference corpus statistics file. The file should store one term on a line, and in the format of:
	 * <br>[freq_in_ref_corpus]   [term]
	 * <br> Any terms with frequency < 2 will be ignored.
	 */
	public FeatureBuilderRefCorpusTermFrequency(String refStatsPath) {
		super(null, null, null);
		_refStatsPath=refStatsPath;
	}


	/**
	 * Dummy method which does nothing with the GlobalIndexMem instance but load statistics and creates
	 * and instance of FeatureRefCorpusTermFrequency from the file
	 * specified in the constructor
	 * @param nullValue can be either an instance or null value
	 * @return
	 * @throws uk.ac.shef.dcs.jate.JATEException
	 */
	public FeatureRefCorpusTermFrequency build(GlobalIndex nullValue) throws JATEException {
		FeatureRefCorpusTermFrequency _feature = new FeatureRefCorpusTermFrequency();

		try{
			final BufferedReader reader = new BufferedReader(new FileReader(_refStatsPath));
			String line;
			while ((line = reader.readLine()) != null) {
				line = line.trim();
				String[] elements = line.split("\\s+");
				if(Integer.valueOf(elements[0])<2) continue;
				_feature.addToTermFreq(elements[1].trim(), Integer.valueOf(elements[0]));
			}

		}
		catch(Exception e){e.printStackTrace();}

		return _feature;
	}
}
