package uk.ac.shef.dcs.jate.core.voting;

import uk.ac.shef.dcs.jate.model.Term;

import java.util.List;

/**
 * Represents weigted term recognition result in a voting system.
 *
 * @author <a href="mailto:z.zhang@dcs.shef.ac.uk">Ziqi Zhang</a>
 */


public class WeightedOutput {

	private List<Term> _result;
	private double _weight;

	/**
	 * Creates an instance which holds ranked list of terms extracted by an algorithm, and the voting power (weight) assigned
	 * to that algorithm.
	 * @param result
	 * @param weight
	 */
	public WeightedOutput(List<Term> result, double weight) {
		_result = result;
		_weight = weight;
	}

	public double getWeight() {
		return _weight;
	}

	public List<Term> getOutputList() {
		return _result;
	}

}
