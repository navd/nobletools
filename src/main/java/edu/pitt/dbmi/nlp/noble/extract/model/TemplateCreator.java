package edu.pitt.dbmi.nlp.noble.extract.model;

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
public class TemplateCreator {
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
		IClass term = ontology.createClass("Terminology");
		term.addLabel("Terminology");
		IClass attributes = term.createSubClass("Attributes");
		attributes.addLabel("Attributes");
		Stack<IClass> parentStack = new Stack<IClass>();
		parentStack.push(term);
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
		
		// add template classes to ontology
		createTemplate(ontology);
		
		return ontology;
	}
	
	/**
	 * create template for a given file
	 * @param ontology
	 */
	private void createTemplate(IOntology ontology){
		final String CAT = "_Category";
		System.out.println("Generating Template ..");
		// create top-level classes
		IClass term = ontology.getClass("Terminology");
		IClass tmpl = ontology.createClass("Template");
		IClass slots = ontology.createClass("Slot");
	
		
		// create properties
		IProperty hasDocumentRange = ontology.createProperty(PROP_HAS_DOC_RANGE,IProperty.DATATYPE);
		hasDocumentRange.setDomain(new IClass [] {slots});
		hasDocumentRange.setRange(new String [] {"String"});
		
		IProperty hasMentionOf = ontology.createProperty(PROP_HAS_MENTION,IProperty.OBJECT);
		hasMentionOf.setDomain(new IClass [] {slots});
		hasMentionOf.setRange(new IClass [] {term});
		
		IProperty hasFeature = ontology.createProperty(PROP_HAS_FEATURE,IProperty.OBJECT);
		hasFeature.setDomain(new IClass [] {slots});
		hasFeature.setRange(new IClass [] {term});
	
		IProperty hasAttribute = ontology.createProperty(PROP_HAS_ATTRIBUTE,IProperty.OBJECT);
		hasAttribute.setDomain(new IClass [] {slots});
		hasAttribute.setRange(new IClass [] {term});
		
		IProperty hasNumericValue = ontology.createProperty(PROP_HAS_NUMERIC_VALUE,IProperty.OBJECT);
		hasNumericValue.setDomain(new IClass [] {slots});
		hasNumericValue.setRange(new IClass [] {term});
		
		IProperty hasModifier = ontology.createProperty(PROP_HAS_MODIFIER,IProperty.OBJECT);
		hasModifier.setDomain(new IClass [] {slots});
		hasModifier.setRange(new IClass [] {term});
	
		IProperty hasSlot = ontology.createProperty(PROP_HAS_SLOT,IProperty.OBJECT);
		hasSlot.setDomain(new IClass [] {tmpl});
		hasSlot.setRange(new IClass [] {slots});
		
		// create a template
		IClass template = tmpl.createSubClass(ontology.getName()+"_Template");
		 
		// now create slots
		List<IClass> categories = new ArrayList<IClass>();
		for(IClass cls : term.getDirectSubClasses()){
			// look at direct children, if they are categories then add them instead
			// the more general category
			// skip special classes
			if(Arrays.asList("Attributes").contains(cls.getName()))
				continue;
			
			IClass [] sub = cls.getDirectSubClasses();
			if(sub.length > 0 && sub[0].getName().endsWith(CAT)){
				for(IClass c: sub){
					categories.add(c);
				}
			}else{
				categories.add(cls);
			}
		}
	
		// iterate over cateogies
		for(IClass cls: categories){
			String name = cls.getName();
			String label = cls.getLabels().length> 0?cls.getLabels()[0]:null;
			
			// strip category
			if(name.endsWith(CAT)){
				name = name.substring(0,name.length()-CAT.length());
				if(label != null)
					label = label.substring(0,label.length()-CAT.length());
			}
			
			// add Slot
			IClass slot = slots.createSubClass(name+"_Slot");
			if(label != null)
				slot.addLabel(label+" Slot");
	
			// add some logic to each slot
			if(cls.hasPropetyValue(conceptType, TYPE_FEATURE)){
				IRestriction a = ontology.createRestriction(IRestriction.SOME_VALUES_FROM);
				a.setProperty(hasFeature);
				a.setParameter(cls.getLogicExpression());
				slot.addNecessaryRestriction(a);
			
				// add children as attributes or modifiers
				boolean hadAttributes = false;
				for(IClass child: cls.getDirectSubClasses()){
					if(child.hasPropetyValue(conceptType, TYPE_ATTRIBUTE)){
						IRestriction aa = ontology.createRestriction(IRestriction.SOME_VALUES_FROM);
						aa.setProperty(hasAttribute);
						aa.setParameter(child.getLogicExpression());
						slot.addNecessaryRestriction(aa);
						child.removeSuperClass(cls);
						hadAttributes = true;
					}else if(child.hasPropetyValue(conceptType, TYPE_NUMERIC_VALUE)){
						IRestriction aa = ontology.createRestriction(IRestriction.SOME_VALUES_FROM);
						aa.setProperty(hasNumericValue);
						aa.setParameter(child.getLogicExpression());
						slot.addNecessaryRestriction(aa);
						hadAttributes = true;
					}
				}
				if(!hadAttributes){
					IRestriction aa = ontology.createRestriction(IRestriction.SOME_VALUES_FROM);
					aa.setProperty(hasModifier);
					aa.setParameter(ontology.getClass(cls.getName()+"_Modifiers").getLogicExpression());
					slot.addNecessaryRestriction(aa);
					for(IClass child: cls.getDirectSubClasses()){
						child.removeSuperClass(cls);
					}
				}
			}else{
				IRestriction a = ontology.createRestriction(IRestriction.SOME_VALUES_FROM);
				a.setProperty(hasMentionOf);
				a.setParameter(cls.getLogicExpression());
				slot.addNecessaryRestriction(a);
			}
			// add Slot to template
			IRestriction r = ontology.createRestriction(IRestriction.SOME_VALUES_FROM);
			r.setProperty(hasSlot);
			r.setParameter(slot.getLogicExpression());
			template.addNecessaryRestriction(r);
		}
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
		String type = null;
		boolean useUMLS  = true;
		Pattern pt = Pattern.compile("([^\\(\\)\\[\\]]+)\\s*(\\(.+\\))?\\s*\\[(.+)\\]");
		Matcher mt = pt.matcher(name);
		if(mt.matches()){
			name = mt.group(1).trim();
			cui = mt.group(2);
			cui = cui != null?cui.trim().substring(1,cui.trim().length()-1).trim():null;
			source = mt.group(3).trim();
		}
		
		// check if this is a feature/attribute branch
		if(name.contains("=")){
			type = name.substring(name.indexOf("=")+1).trim();
			name = name.substring(0,name.indexOf("=")).trim();
		}
		// get attributes class
		IOntology ontology = parent.getOntology();
		IClass attributes = ontology.getClass("Attributes");
		
		// create class
		IClass cls = parent.createSubClass(OntologyUtils.toResourceName(name));
		cls.addLabel(name);
		
		// what is a concept type
		if(TYPE_FEATURE.equals(type)){
			cls.setPropertyValue(conceptType, TYPE_FEATURE);
		}else if(TYPE_ATTRIBUTE.equals(type)){
			cls.setPropertyValue(conceptType, TYPE_ATTRIBUTE);
			cls.addSuperClass(attributes);
		}else if(TYPE_NUMERIC_VALUE.equals(type)){
			cls.setPropertyValue(conceptType, TYPE_NUMERIC_VALUE);
		}else if(parent != null && parent.hasPropetyValue(conceptType, TYPE_ATTRIBUTE)){
			cls.setPropertyValue(conceptType, TYPE_VALUE);
		}else if(parent != null && parent.hasPropetyValue(conceptType, TYPE_FEATURE)){
			cls.setPropertyValue(conceptType, TYPE_MODIFIER);
			// create/get modifier class
			IClass mparent = ontology.getClass(parent.getName()+"_Modifiers");
			if(mparent == null){
				mparent = attributes.createSubClass(parent.getName()+"_Modifiers");
				mparent.addLabel(parent.getName()+"_Modifiers");
			}
			cls.addSuperClass(mparent);
		}else if(TYPE_NUMERIC_VALUE.equals(type)){
			cls.setPropertyValue(conceptType, TYPE_FEATURE);
		}else{
			cls.setPropertyValue(conceptType, TYPE_FINDING);
		}
		
		
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
				if(code != null && !lookupCodes){
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
				for(Concept c: filterConcepts(term.search(name))){
					addConceptInfo(c, cls);
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
		TemplateCreator termCreator = new TemplateCreator();
		System.out.println("initializing ..");
		IOntology ontology = termCreator.createOntology(inputFile);
		System.out.println("saving ontology "+outputFile.getAbsolutePath()+" ...");
		ontology.write(new FileOutputStream(outputFile),IOntology.OWL_FORMAT);
		
	}
}
