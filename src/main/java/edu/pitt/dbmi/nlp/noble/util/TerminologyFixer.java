package edu.pitt.dbmi.nlp.noble.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Map;

import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;

/**
 * this class is a collection of little fixer methods for "fixing" existing terminology footprints
 * @author tseytlin
 *
 */
public class TerminologyFixer {

	/**
	 * Prune Terminology Roots that are empty
	 */
	public static void pruneTerminologyRoots(String name) throws IOException, TerminologyException{
		NobleCoderTerminology term = new NobleCoderTerminology();
		term.load(name, false);
		NobleCoderTerminology.Storage st = term.getStorage();
		int total = term.getRootConcepts().length;
		int n = 0;
		
		for(String root: new ArrayList<String>(st.getRootMap().keySet())){
			Concept r = term.lookupConcept(root);
			
			boolean exclude = false;
			Concept [] c = r.getChildrenConcepts();
			if(c.length == 0)
				exclude = true;
			else if(c.length == 1) {
				Source src = c[0].getSources()[0];
				if("SRC".equals(src.getCode()))
					exclude = true;
			}
			
			if(exclude){
				st.getRootMap().remove(root);
				System.out.println(root+"\t"+r.getName());
				n++;
			}
				//System.out.println("\tEXCLUDE");
		}
		
		System.out.println(n+" out of "+total+" were removed");
		System.out.println("saving ...");
		term.save();
		System.out.println("done");
	}
	
	/**
	 * add other concept codes to the table
	 * @param args
	 * @throws IOException 
	 * @throws TerminologyException 
	 */
	public static void addOtherConceptCodes(String name) throws IOException, TerminologyException{
		NobleCoderTerminology term = new NobleCoderTerminology();
		term.load(name, false);
		NobleCoderTerminology.Storage st = term.getStorage();
		Map<String,Concept.Content> map = st.getConceptMap();
		Map<String,String> codes = st.getCodeMap();
		
		System.out.println("go over "+map.size()+" concepts in: "+name);
		for(String cui: map.keySet()){
			Concept.Content con = map.get(cui);
			if(con != null && con.codeMap != null){
				for(String code: con.codeMap.values()){
					if(!codes.containsKey(code))
						codes.put(code,cui);
				}
			}
		}
		System.out.println("saving ...");
		term.save();
		System.out.println("done");
	}
	
	public static void main(String[] args) throws IOException, TerminologyException {
		//addOtherConceptCodes("NCI_Metathesaurus");
	}

}
