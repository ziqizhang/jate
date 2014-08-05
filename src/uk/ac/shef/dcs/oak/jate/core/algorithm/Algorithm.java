package uk.ac.shef.dcs.oak.jate.core.algorithm;

import uk.ac.shef.dcs.oak.jate.JATEException;
import uk.ac.shef.dcs.oak.jate.model.Term;

/**
 * Interface of term recognition algorithm. This class represents the core <b>scoring and ranking</b> algorithm which given a
 * candidate term, produces a confidence score indicating the strength of relatedness of a candidate term to the corpus from
 * which it has been extracted. Note that an instance of algorithm does not do pre- or post-processing to filter candidate
 * terms. 
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public interface Algorithm {

	/**
	 * Execute the algorithm by analysing the features stored in the AbstractFeatureWrapper and return terms extracted and
	 * sorted by their relevance
	 * @param store
	 * @return
	 * @throws uk.ac.shef.dcs.oak.jate.JATEException
	 */
	Term[] execute(AbstractFeatureWrapper store) throws JATEException;

}
