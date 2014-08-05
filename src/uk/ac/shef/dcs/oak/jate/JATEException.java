package uk.ac.shef.dcs.oak.jate;

/**
 */

public class JATEException extends Exception {

	public JATEException(String exception){
		super(exception);
	}

	public JATEException(Exception e){
		super(e);
	}

}
