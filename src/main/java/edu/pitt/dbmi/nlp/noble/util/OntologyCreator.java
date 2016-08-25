package edu.pitt.dbmi.nlp.noble.util;

import java.io.*;
import java.net.URI;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.ontology.*;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Definition;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;


/**
 * Reads in tab-indented seed terminology and outputs an owl file
 * @author tseytlin
 *
 */
public class OntologyCreator {
	private static final String DEFAULT_UMLS = "NCI_Metathesaurus";
	private static final String DEFAULT_URI_BASE = "http://edda.dbmi.pitt.edu/ontologies/";
	public static final String TYPE_FEATURE = "feature";
	public static final String TYPE_ATTRIBUTE = "attribute";
	public static final String TYPE_MODIFIER = "modifier";
	public static final String TYPE_VALUE = "value";
	public static final String TYPE_NUMERIC_VALUE = "numeric-value";
	public static final String TYPE_FINDING = "finding";
	public static final String PROP_HAS_MENTION = "hasMentionOf";
	public static final String PROP_HAS_FEATURE = "hasFeature";
	public static final String PROP_HAS_ATTRIBUTE = "hasAttribute";
	public static final String PROP_HAS_NUMERIC_VALUE = "hasNumericValue";
	public static final String PROP_HAS_MODIFIER = "hasModifier";
	public static final String PROP_HAS_DOC_RANGE = "hasDocumentRange";
	public static final String PROP_HAS_SLOT = "hasSlot";
	
	private final String NUMBER = "NUMBER";
	private Map<String,Terminology> terminologies;
	private IProperty code, semType,synonym,definition,conceptType;
	private boolean importChildren = true, lookupCodes = true;
	
	/**
	 * setup global processing options
	 * @param line
	 */
	private void processOptions(String line){
		//includ children
		Matcher m = Pattern.compile("#\\s*import.children\\s*=(.*)").matcher(line);
		if(m.matches())
			importChildren = Boolean.parseBoolean(m.group(1).trim());
		m = Pattern.compile("#\\s*lookup.codes\\s*=(.*)").matcher(line);
		if(m.matches())
			lookupCodes = Boolean.parseBoolean(m.group(1).trim());
	}
	
	/**
	 * get some terminology in a map
	 * @param type
	 * @return
	 */
	private Terminology getTerminology(String name){
		if(terminologies == null)
			terminologies = new HashMap<String, Terminology>();
		Terminology term = terminologies.get(name);
		if(term == null){
			try {
				term = new NobleCoderTerminology(name);
				terminologies.put(name,term);
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return term;
	}
	
	/**
	 * get number of tabs that are prefixed in a string
	 * @param s
	 * @return
	 */
	public static int getTabOffset(String str){
		int count = 0;
		for(int i = 0;i<str.length();i++){
			if(str.charAt(i) == '\t')
				count ++;
			else
				break;
		}
		return count;
	}
	
	/**
	 * create ontology from seed terminology represented as a text file
	 * @param seedTerminology
	 * @return
	 * @throws IOntologyException 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws TerminologyException 
	 */
	public IOntology createOntology(File seedTerminology) throws IOntologyException, FileNotFoundException, IOException, TerminologyException {
		URI uri = URI.create(DEFAULT_URI_BASE+changeSuffix(seedTerminology.getName(),".owl"));
		IOntology ontology = OOntology.createOntology(uri);
		
		ontology.addLabel(ontology.getName()+" Template");
		ontology.addComment("Template for extracting data elements for "+ontology.getName()+" domain");
		
		code = ontology.createProperty("Code",IProperty.ANNOTATION);
		semType = ontology.createProperty("SemanticType",IProperty.ANNOTATION);
		synonym = ontology.createProperty("Synonym",IProperty.ANNOTATION);
		definition = ontology.createProperty("Definition",IProperty.ANNOTATION);
		conceptType = ontology.createProperty("ConceptType",IProperty.ANNOTATION);
		
		// create two root classes
		Stack<IClass> parentStack = new Stack<IClass>();
		parentStack.push(ontology.getRoot());
		String lastLine = null;
		IClass lastClass = null;
		BufferedReader r = new BufferedReader(new FileReader(seedTerminology));
		for(String line = r.readLine(); line != null; line = r.readLine()){
			if(line.trim().startsWith("#") || line.trim().length() == 0){
				//includ children
				processOptions(line);
				continue;
			}
			
			// figure out hierarchy
			if(lastLine != null){
				int l=  getTabOffset(lastLine);
				int n = getTabOffset(line);
				// this means that this entry is a child of new entry
				if(n > l){
					parentStack.push(lastClass);
				}else if(n < l){
					// else we move back to previous parent
					// depending on the depth
					for(int i=0;!parentStack.isEmpty() && i< (l-n);i++)
						parentStack.pop();
				}
			}
			
			IClass parent = parentStack.peek();
			IClass cls = createClass(line,parent); 
			
			// save for next iteration
			lastClass = cls;
			lastLine = line;
		}
		r.close();
		
		return ontology;
	}
	
	
	
	/**
	 * create class from a line of text in a seed terminology
	 * @param line
	 * @param parent
	 * @return
	 * @throws TerminologyException 
	 */
	private IClass createClass(String line, IClass parent) throws TerminologyException {
		System.out.println("\tprocessing "+line+" ..");
		String name = line.trim();
		String source = null;
		String cui = null;
		boolean useUMLS  = true;
		Pattern pt = Pattern.compile("([^\\(\\)\\[\\]]+)\\s*(\\(.+\\))?\\s*(?:\\[(.+)\\])?");
		Matcher mt = pt.matcher(name);
		if(mt.matches()){
			name = mt.group(1).trim();
			cui = mt.group(2);
			cui = cui != null?cui.trim().substring(1,cui.trim().length()-1).trim():null;
			if(mt.groupCount() > 3)
				source = mt.group(3).trim();
		}
		
		
		// create class
		IClass cls = parent.createSubClass(OntologyUtils.toResourceName(name));
		cls.addLabel(name);
		
		// if has source then get a branch
		if(source != null){
			// do we want UMLS?
			if(source.endsWith("*")){
				useUMLS = false;
				source = source.substring(0,source.length()-1);
			}
			Terminology term = getTerminology(source);
			if(term != null){
				// do we have a concept code?
				if(cui != null && !lookupCodes){
					Concept result = term.lookupConcept(cui);
					if(result != null){
						// use the preferred name from the code instead
						//cls.delete();
						//cls = parent.createSubClass(OntologyUtils.toResourceName(result.getName()));
						//cls.addLabel(result.getName());
						addConceptBranch(cls,new Concept [] {result},new HashSet<String>(),useUMLS);
						cls.addPropertyValue(code,cui);
					}
				}else{
					Concept [] result = filterConcepts(term.search(name));
					if(result.length > 0){
						addConceptBranch(cls,result,new HashSet<String>(),useUMLS);
						if(code != null)
							cls.addPropertyValue(code,cui);
					}else{
						System.err.println("Error: could not find any candidates for "+name);
					}
				}
			}else{
				System.err.println("Error: didn't find "+source+" terminology loaded");
			}
		// if name is a category, don't need to do much	
		}else if(name.endsWith("Category")){ 
			// || parent.getName().equals("Terminology")
			//NOOP
		// otherwise do UMLS lookup for synonyms
		}else{
			Terminology term = getTerminology(DEFAULT_UMLS);
			if(term != null){
				// do we have a concept code?
				if(cui != null && lookupCodes){
					Concept c = term.lookupConcept(cui);
					if(c != null){
						addConceptInfo(c, cls);
					}
				}else{
					for(Concept c: filterConcepts(term.search(name))){
						addConceptInfo(c, cls);
					}
				}
			}else{
				System.err.println("Error: didn't find UMLS terminology loaded");
			}
		}
		
		return cls;
	}

	/**
	 * filter concepts to only the ones that cover the term completly
	 * @param result
	 * @return
	 */
	private Concept [] filterConcepts(Concept [] result){
		List<Concept> list = new ArrayList<Concept>();
		for(Concept c: result){
			if(c.getSearchString().equals(c.getMatchedTerm())){
				list.add(c);
			}
		}
		return list.toArray(new Concept [0]);
	}
	
	
	/**
	 * pull concept branch to ontology from a given terminology
	 * @param cls
	 * @param c
	 * @throws TerminologyException 
	 */
	private void addConceptBranch(IClass cls, Concept [] cc, Set<String> parents,boolean useUMLS) throws TerminologyException {
		for(Concept c: cc){
			if(parents.contains(c.getCode()))
				return;
			addConceptInfo(c,cls);
			if(useUMLS)
				addConeptInfoFromUMLS(cls);
			parents.add(c.getCode());
			
			if(importChildren){
				for(Concept child: c.getChildrenConcepts()){
					if(!parents.contains(child.getCode())){
						IClass childCls = cls.createSubClass(OntologyUtils.toResourceName(child.getName()));
						addConceptBranch(childCls,new Concept [] {child},parents,useUMLS);
					}
				}
			}
		}
	}

	private void addConeptInfoFromUMLS(IClass cls) throws TerminologyException{
		String name = cls.getLabels()[0];
		Terminology term = getTerminology(DEFAULT_UMLS);
		if(term != null){
			for(Concept c: term.search(name)){
				if(c.getMatchedTerm().equals(name)){
					addConceptInfo(c, cls);
				}
			}
		}
	}
	
	
	/**
	 * add concept infor to class
	 * @param c
	 * @param cls
	 */
	private void addConceptInfo(Concept c, IClass cls ){
		cls.setPropertyValue(code,c.getCode());
		if(cls.getLabels().length == 0)
			cls.addLabel(c.getName());
		for(String s: c.getSynonyms()){
			if(!cls.hasPropetyValue(synonym,s))
				cls.addPropertyValue(synonym,s);
		}
		for(SemanticType st: c.getSemanticTypes()){
			String s = st.getName();
			if(!cls.hasPropetyValue(semType,s))
				cls.addPropertyValue(semType, s);
		}
		for(Definition df: c.getDefinitions()){
			cls.addPropertyValue(definition,df.getDefinition());
		}
	}
	
	
	private String changeSuffix(String name, String suffix) {
		int x = name.lastIndexOf('.');
		if(x > -1)
			return name.substring(0,x)+suffix;
		return name+suffix;
	}
	
	
	/**
	 * @param args
	 * @throws Exception 
	 */
	public static void main(String[] args) throws Exception {
		File dataDirectory = new File("/home/tseytlin/Data/Terminologies");
		File inputFile = new File(dataDirectory,"Neoplasm_Core_Hierarchy_2016-05-02.txt");
		File outputFile = new File(dataDirectory,"Neoplasm_Core_Hierarchy.owl");
		
		// create domain
		OntologyCreator termCreator = new OntologyCreator();
		System.out.println("initializing ..");
		IOntology ontology = termCreator.createOntology(inputFile);
		System.out.println("saving ontology "+outputFile.getAbsolutePath()+" ...");
		ontology.write(new FileOutputStream(outputFile),IOntology.OWL_FORMAT);
		
	}
}
