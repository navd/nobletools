package edu.pitt.dbmi.nlp.noble.terminology;

/**
 * generic exception that wraps other ones
 * @author tseytlin
 *
 */
public class TerminologyException extends Exception {
	/**
	 * exception 
	 * @param message
	 * @param cause
	 */
	
	public TerminologyException(String message){
		super(message);
	}
	
	/**
	 * exception 
	 * @param message
	 * @param cause
	 */
	public TerminologyException(String message, Throwable cause){
		super(message,cause);
	}
}
