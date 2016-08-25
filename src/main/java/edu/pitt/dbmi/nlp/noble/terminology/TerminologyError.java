package edu.pitt.dbmi.nlp.noble.terminology;

/**
 * exception caused for whatever reason, unhandled
 * wraps other exceptions
 * @author tseytlin
 */
public class TerminologyError extends RuntimeException {
	public TerminologyError(String reason){
		super(reason);
	}
	public TerminologyError(String reason, Throwable cause){
		super(reason,cause);
	}
}