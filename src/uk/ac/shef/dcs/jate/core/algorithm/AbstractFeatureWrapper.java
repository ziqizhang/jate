package uk.ac.shef.dcs.jate.core.algorithm;

import java.util.Set;

/**
 * A feature wrapper wrapps one or multiple instances of FeatureAbstract and provides specific querying and updating
 * services of features tailored to specific term extraction algorithms.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */

public abstract class AbstractFeatureWrapper {

	/**
	 * @return set of candidate term strings
	 */
	public abstract Set<String> getTerms();

	public String toString(){
		return this.getClass().toString();
	}
}
