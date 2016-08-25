package edu.pitt.dbmi.nlp.noble.ontology.concept;

import java.util.LinkedHashMap;
import java.util.Map;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalHelper;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;


/**
 * a registry map of all available concept handlers
 * @author tseytlin
 *
 */
public class ConceptRegistry {
	public static final Map<String,String> REGISTRY = new LinkedHashMap<String,String>();
	// initialize resource map
	static {
		REGISTRY.put("http://ncicb.nci.nih.gov/xml/owl/EVS/Thesaurus.owl",ThesaurusConcept.class.getName());
		REGISTRY.put("http://sig.biostr.washington.edu/fma3.0",FMAConcept.class.getName());
		REGISTRY.put("http://purl.org/sig/ont/fma",FMAConcept.class.getName());
		REGISTRY.put("/"+BioPortalHelper.BIOPORTAL_URL.replaceAll("\\.","\\.")+".*/",BioPortalConcept.class.getName());
	}
	
	public static void main(String [] args) throws Exception {
		IOntology ont = OOntology.loadOntology("/home/tseytlin/Data/Ontologies/fma_4.2.0.owl");
		System.out.println(ont.getURI());
		for(IClass c: ont.getRootClasses()){
			c.getConcept().printInfo(System.out);
		}
	}
}
