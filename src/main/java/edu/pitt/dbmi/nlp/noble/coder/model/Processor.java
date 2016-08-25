package edu.pitt.dbmi.nlp.noble.coder.model;

import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;

/**
 * interface for processing spannable resourcess
 * @author tseytlin
 *
 * @param <T>
 */
public interface Processor<T extends Spannable> {
	/**
	 * process a spannable text resources s.a. Document or Sentence
	 * @param r
	 * @return
	 */
	public T process(T r) throws TerminologyException;
	
	/**
	 * get running time in milis for the last called process() method
	 * @return
	 */
	public long getProcessTime();
}
