package edu.pitt.dbmi.nlp.noble.ontology.concept;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Source;


public class FMAConcept extends Concept {
	public FMAConcept(IClass cls) {
		super(cls);
		
		// not lets do NCI Thesaurus specifics
		IOntology ont = cls.getOntology();
		
		// do codes
		IProperty code_p = ont.getProperty("http://purl.org/sig/ont/fma/FMAID");
		if(code_p != null){
			Object [] val = cls.getPropertyValues(code_p);
			if(val.length > 0){
				setCode(val[0].toString());
			}
		}
		// add URI
		addCode(""+cls.getURI(),Source.getSource("FMA.URI"));
	}
}
