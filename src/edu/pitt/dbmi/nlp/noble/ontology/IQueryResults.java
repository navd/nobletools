package edu.pitt.dbmi.nlp.noble.ontology;

import java.util.*;

public interface IQueryResults extends Iterator {

	/**
	 * get list of variables that were returned
	 * @return
	 */
	public String [] getVariables();
	
	/**
	 * get next row
	 * @return
	 */
	public Map next();
}
