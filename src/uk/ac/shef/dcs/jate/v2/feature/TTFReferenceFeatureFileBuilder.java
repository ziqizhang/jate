package uk.ac.shef.dcs.jate.v2.feature;

import org.apache.commons.lang.exception.ExceptionUtils;
import uk.ac.shef.dcs.jate.v2.JATEException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * A specific type of feature builder that builds an instance of FeatureRefCorpusTermFrequency. This is a dummy class
 * which reads the data from a text file which stores information as:
 * <br> [freq_in_corpus]      [term]
 *
 * @author <a href="mailto:ziqi.zhang@sheffield.ac.uk">Ziqi Zhang</a>
 */


public class TTFReferenceFeatureFileBuilder extends AbstractFeatureBuilder {

    private static final Logger LOG = Logger.getLogger(TTFReferenceFeatureFileBuilder.class.getName());

    private final String _refStatsPath;

    /**
     * Default constructor
     *
     * @param refStatsPath file path to the reference corpus statistics file. The file should store one term on a line, and in the format of:
     *                     <br>[freq_in_ref_corpus]   [term]
     *                     <br> Any terms with frequency < 2 will be ignored.
     */
    public TTFReferenceFeatureFileBuilder(String refStatsPath) {
        super(null, null);
        _refStatsPath = refStatsPath;
    }


    /**
     * Dummy method which does nothing with the GlobalIndexMem instance but load statistics and creates
     * and instance of FeatureRefCorpusTermFrequency from the file
     * specified in the constructor
     *
     * @return
     * @throws uk.ac.shef.dcs.jate.v2.JATEException
     */
    public FrequencyTermBased build() throws JATEException {
        FrequencyTermBased feature = new FrequencyTermBased();

        try {
            final BufferedReader reader = new BufferedReader(new FileReader(_refStatsPath));
            String line;
            while ((line = reader.readLine()) != null) {
                line = line.trim();
                String[] elements = line.split("\\s+");
                if (Integer.valueOf(elements[0]) < 2) continue;
                feature.increment(elements[1].trim(), Integer.valueOf(elements[0]));
            }

        } catch (IOException e) {
            StringBuilder sb = new StringBuilder("Failed to build features!");
            sb.append("\n").append(ExceptionUtils.getFullStackTrace(e));
            throw new JATEException(sb.toString());
        }
        return feature;
    }
}
