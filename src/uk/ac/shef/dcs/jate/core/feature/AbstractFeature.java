package uk.ac.shef.dcs.jate.core.feature;

import uk.ac.shef.dcs.jate.core.feature.indexer.GlobalIndex;

/**
 * Representation of a term recognition feature built on an instance of GlobalIndex
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */



public abstract class AbstractFeature {
	protected GlobalIndex _index;

	public GlobalIndex getGlobalIndex(){
		return _index;
	}

	public String toString(){
		return this.getClass().toString();
	}
}
