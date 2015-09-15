package uk.ac.shef.dcs.jate.v2;

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
