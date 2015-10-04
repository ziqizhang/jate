package uk.ac.shef.dcs.jate;

public class JATEException extends Exception {

	private static final long serialVersionUID = 7625049137946095304L;

	public JATEException(String exception){
		super(exception);
	}

	public JATEException(Exception e){
		super(e);
	}

}
