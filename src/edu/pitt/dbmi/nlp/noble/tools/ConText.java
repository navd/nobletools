package edu.pitt.dbmi.nlp.noble.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Modifier;
import edu.pitt.dbmi.nlp.noble.coder.model.Processor;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyError;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.util.PathHelper;

public class ConText implements Processor<Sentence> {
	public static final String DEFAULT_MODIFIER_ONTOLOGY = "http://blulab.chpc.utah.edu/ontologies/v2/Modifier.owl";
	public static final List<String> CONTEXT_ROOTS =  Arrays.asList("Closure","Pseudo","LinguisticModifier");
	public static final String RELATION_TERMINATION = "hasTermination";
	public static final String RELATION_PSEUDO = "hasPseudo";
	public static final String RELATION_ACTION = "hasActionEn";
	public static final String PROP_WINDOW_SIZE = "windowSize";
	public static final String PROP_IS_DEFAULT_VALUE = "isDefaultValue";
	public static final String PROP_HAS_DEFAULT_VALUE = "hasDefaultValue";
	
	public static final String SEMTYPE_INSTANCE = "Instance";
	public static final String SEMTYPE_CLASS = "Class";
	public static final String LANGUAGE = "en";
	public static final String CONTEXT_OWL = "ConText.owl";
	public static final String ACTION_TERMINATE = "terminate";
	public static final String ACTION_FORWARD = "forward";
	public static final String ACTION_BACKWARD = "backward";
	public static final String ACTION_BIDIRECTIONAL = "bidirectional";
	public static final String ACTION_DISCONTINUOUS = "discontinuous";
	public static final String LINGUISTIC_MODIFIER = "LinguisticModifier";
	public static final String PSEUDO = "Pseudo";
	public static final int DEFAULT_WINDOW_SIZE = 8;
	
	
	public static final String MODIFIER_TYPE_POLARITY = "Polarity";
	public static final String MODIFIER_TYPE_EXPERIENCER = "Experiencer";
	public static final String MODIFIER_TYPE_TEMPORALITY = "Temporality";
	public static final String MODIFIER_TYPE_CERTAINTY = "Certainty";
	public static final String MODIFIER_TYPE_ASPECT = "ContextualAspect";
	public static final String MODIFIER_TYPE_MODALITY = "ContextualModality";
	public static final String MODIFIER_TYPE_DEGREE = "Degree";
	public static final String MODIFIER_TYPE_PERMENENCE = "Permanence";
	
	
	public static final String MODIFIER_VALUE_POSITIVE = "Positive_Polarity";
	public static final String MODIFIER_VALUE_NEGATIVE = "Negative_Polarity";
	public static final String MODIFIER_VALUE_HEDGED = "Hedged_ContextualModality";
	public static final String MODIFIER_VALUE_FAMILY_MEMBER = "FamilyMember_Experiencer";
	public static final String MODIFIER_VALUE_HISTORICAL = "Before_DocTimeRel";

	
	public static final List<String> MODIFIER_TYPES = Arrays.asList(
			MODIFIER_TYPE_CERTAINTY,
			MODIFIER_TYPE_ASPECT,
			MODIFIER_TYPE_MODALITY,
			MODIFIER_TYPE_DEGREE,
			MODIFIER_TYPE_EXPERIENCER,
			MODIFIER_TYPE_PERMENENCE,
			MODIFIER_TYPE_POLARITY,
			MODIFIER_TYPE_TEMPORALITY);
	
	
	private long time;
	private NobleCoderTerminology terminology;
	private PathHelper paths;
	private Map<String,String> defaultValues;
	
	
	/**
	 * initialize ConText with default modifier ontology
	 * first check the cache, if not there load/save from the web
	 */
	public ConText(){
		try{
			// check if pre-existing terminology exists
			if(NobleCoderTerminology.hasTerminology(getClass().getSimpleName())){
				terminology = new NobleCoderTerminology(getClass().getSimpleName());
			}else{
				load(OOntology.loadOntology(DEFAULT_MODIFIER_ONTOLOGY));
				terminology.dispose();
				terminology = new NobleCoderTerminology(getClass().getSimpleName());
			}
		}catch(Exception ex){
			throw new TerminologyError("Unable to load ConText ontology", ex);
		}
		paths = new PathHelper(terminology);
	}
	
	
	public ConText(IOntology ont){
		try {
			load(ont);
		} catch (Exception e) {
			throw new TerminologyError("Unable to load ConText ontology",e);
		}
		paths = new PathHelper(terminology);
	}
	
	/**
	 * load ConText ontology from a given ontology object
	 * @param ontology
	 * @return
	 * @throws TerminologyException
	 * @throws IOException
	 */
	private void load(IOntology ontology) throws TerminologyException, IOException {
		// setup special interest of noble coder
		terminology = new NobleCoderTerminology();
		terminology.load(getClass().getSimpleName(),false);
		terminology.setDefaultSearchMethod(NobleCoderTerminology.CUSTOM_MATCH);
		terminology.setContiguousMode(true);
		terminology.setSubsumptionMode(false);
		terminology.setOverlapMode(true);
		terminology.setPartialMode(false);
		terminology.setOrderedMode(true);
		terminology.setMaximumWordGap(0);
		terminology.setScoreConcepts(false);
		terminology.setHandlePossibleAcronyms(false);
		terminology.setLanguageFilter(new String [] {LANGUAGE});
		terminology.setStemWords(false);
		terminology.setStripStopWords(false);
		terminology.setIgnoreSmallWords(false);
		terminology.setIgnoreDigits(false);
		terminology.setSemanticTypeFilter(SEMTYPE_INSTANCE);
		
		// set language filter to only return English values
		if(ontology instanceof OOntology)
			((OOntology)ontology).setLanguageFilter(Arrays.asList(LANGUAGE));
		
		
		// load classes 
		for(String root: CONTEXT_ROOTS ){
			IClass cls = ontology.getClass(root);
			if(cls != null){
				// add roots to terminology
				terminology.addRoot(addConcept(cls).getCode());
				for(IInstance inst : cls.getDirectInstances()){
					addConcept(inst);
				}
				
				// go over all subclasses
				for(IClass c: cls.getSubClasses()){
					addConcept(c);
					for(IInstance inst : c.getDirectInstances()){
						addConcept(inst);
					}
				}
			}
		}
		
		// save terminology
		terminology.save();
	}

	private Concept addConcept(IInstance inst) throws TerminologyException {
		Concept concept = new Concept(inst);
		concept.setCode(inst.getName());
		if(!isRootInstance(inst))
			concept.addSemanticType(SemanticType.getSemanticType(SEMTYPE_INSTANCE));
		
		
		// add relations to concept
		for(IClass c: inst.getDirectTypes()){
			for(SemanticType st: getSemanticTypes(c)){
				concept.addSemanticType(st);
				concept.addProperty(st.getName(),getModifierValue(st.getName(),c));
			}
			concept.addRelatedConcept(Relation.BROADER,c.getName());
			
			// add default value if parent is default
			if(isDefaultValue(c)){
				concept.addProperty(PROP_IS_DEFAULT_VALUE,"true");
			}
		}
		
		// add other relations to concept
		for(IProperty p:  inst.getProperties()){
			for(Object o: inst.getPropertyValues(p)){
				if(o instanceof IResource){
					//concept.addRelatedConcept(Relation.getRelation(p.getName()),((IResource)o).getName());
					concept.addProperty(p.getName(),((IResource)o).getName());
				}
			}
		}
		
		//add to terminology
		terminology.addConcept(concept);
		
		
		return concept;
	}
	
	/**
	 * get modifier value
	 * @param type
	 * @param c
	 * @return
	 */
	private String getModifierValue(String type, IClass c){
		IOntology o = c.getOntology();
		if(c.hasDirectSuperClass(o.getClass(type))){
			return c.getName();
		}
		for(IClass p: c.getDirectSuperClasses()){
			String v = getModifierValue(type, p);
			if(v != null)
				return v;
		}
		return null;
	}
	
	

	private boolean isRootInstance(IInstance inst) {
		for(IClass c: inst.getDirectTypes()){
			if(CONTEXT_ROOTS.contains(c.getName()))
				return true;
		}
		return false;
	}

	private Concept addConcept(IClass cls) throws TerminologyException{
		Concept concept = cls.getConcept();
		//overwrite URI, with name
		concept.setCode(cls.getName());
		
		// add semantic type
		for(SemanticType st: getSemanticTypes(cls))
			concept.addSemanticType(st);
		
		// add relations to concept
		for(IClass c: cls.getDirectSuperClasses()){
			concept.addRelatedConcept(Relation.BROADER,c.getName());
		}
		
		// add relations to concept
		for(IClass c: cls.getDirectSubClasses()){
			concept.addRelatedConcept(Relation.NARROWER,c.getName());
			
			// get the default value for this type
			if(isModifierType(cls)){
				if(isDefaultValue(c)){
					concept.addProperty(PROP_HAS_DEFAULT_VALUE,c.getName());
				}
			}
		}
		
		// add relations to concept
		for(IInstance c: cls.getDirectInstances()){
			concept.addRelatedConcept(Relation.NARROWER,c.getName());
		}
		
		// add other properties defined in ConText to concept properties
		for(IProperty prop: cls.getProperties()){
			if(prop.getURI().toString().contains(CONTEXT_OWL)){
				Object o = cls.getPropertyValue(prop);
				if(o != null){
					concept.addProperty(prop.getName(),""+o);
				}
			}
		}
		
		// add other properties
		for(Object o: cls.getDirectNecessaryRestrictions()){
			if(o instanceof IRestriction){
				IRestriction r = (IRestriction) o;
				for(Object v: r.getParameter()){
					if(!(v instanceof IClass)){
						concept.addProperty(r.getProperty().getName(),""+v);
					}
				}
			}
		}
		
		
		// add other relations to a concept
		for(Object o: cls.getNecessaryRestrictions()){
			if(o instanceof IRestriction){
				IRestriction r = (IRestriction) o;
				for(Object v: r.getParameter()){
					if(v instanceof IClass){
						concept.addRelatedConcept(Relation.getRelation(r.getProperty().getName()), ((IClass)v).getName());
					}
				}
			}
		}
			
		// add terminology
		terminology.addConcept(concept);
		
		
		return concept;
	}
	
	
	public Terminology getTerminology() {
		return terminology;
	}

	private boolean isModifierType(IClass cls){
		return cls.getURI().toString().contains(CONTEXT_OWL) && !cls.getName().contains("_");
	}
	
	private boolean isDefaultValue(IClass cls){
		for(Object o: cls.getDirectNecessaryRestrictions()){
			if(o instanceof IRestriction){
				IRestriction r = (IRestriction) o;
				if(PROP_IS_DEFAULT_VALUE.equals(r.getProperty().getName())){
					for(Object v: r.getParameter()){
						return Boolean.parseBoolean(v.toString());
					}
				}
			}
		}
		return false;
	}
	
	
	private Set<SemanticType> getSemanticTypes(IClass cls) {
		Set<SemanticType> semTypes = new LinkedHashSet<SemanticType>();
		// if defined in ConText ontology, then class is its own SemType
		if(isModifierType(cls)){
			semTypes.add(SemanticType.getSemanticType(cls.getName()));
		}else{
			// else try the direct parent, the ontology is shallow
			for(IClass c: cls.getDirectSuperClasses()){
				semTypes.addAll(getSemanticTypes(c));
			}
		}
		// this should never happen, but just in case here is the defautl
		//return SemanticType.getSemanticType(SEMTYPE_CLASS);
		return semTypes;
	}

	
	/**
	 * get default values map
	 * @return
	 * @throws TerminologyException
	 */
	private Map<String,String> getDefaultValues() throws TerminologyException{
		if(defaultValues == null){
			defaultValues = new LinkedHashMap<String,String>();
			for(String type: MODIFIER_TYPES){
				Concept context = terminology.lookupConcept(type);
				if(context != null && context.getProperties().containsKey(PROP_HAS_DEFAULT_VALUE)){
					defaultValues.put(type,context.getProperty(PROP_HAS_DEFAULT_VALUE));
				}
			}
		}
		return defaultValues;
	}
	
	
	
	
	
	/**
	 * now actually process sentence and see what we have
	 */
	
	public Sentence process(Sentence sentence) throws TerminologyException {
		time = System.currentTimeMillis();
		
		// get mentions for this sentence, make a copy of since we don't add mentions
		// to the original sentence
		Sentence text = terminology.process(new Sentence(sentence));
		
		
		//add defaults for stuff that was not picked up
		for(Mention m: sentence.getMentions()){
			for(String type: getDefaultValues().keySet()){
				m.addModifier(getModifier(type,getDefaultValues().get(type)));
			}
		}
		
		// go over mentions
		for(Mention m: getLinguisticModifiers(text)){
			for(Mention target: getTargetMentions(m,sentence,getTerminators(m,text))){
				target.addModifiers(getModifiers(m));
			}
		}
		
		time = System.currentTimeMillis() - time;
		return sentence;
	}

	
	private List<Modifier> getModifiers(Mention m) throws TerminologyException{
		List<Modifier> modifiers = Modifier.getModifiers(m);
		for(Modifier mod: modifiers){
			String val = getDefaultValues().get(mod.getType());
			mod.setDefaultValue(mod.getValue().equals(val));
		}
		return modifiers;
	}
	
	private Modifier getModifier(String type, String value){
		Modifier modifier = Modifier.getModifier(type,value);
		modifier.setDefaultValue(true);
		return modifier;
	}
	
	
	private List<Mention> getTargetMentions(Mention modifier, Sentence targetText, List<Mention> terminators) throws TerminologyException {
		List<Mention> list = new ArrayList<Mention>();

		List<String> acts = getAction(modifier.getConcept());
		boolean forward =  acts.contains(ACTION_FORWARD) || acts.contains(ACTION_BIDIRECTIONAL);
		boolean backward = acts.contains(ACTION_BACKWARD) || acts.contains(ACTION_BIDIRECTIONAL);

		// figure out termination offset
		int start = getWordWindowIndex(modifier,targetText,false);
		int end   = getWordWindowIndex(modifier,targetText,true);
		
		//System.out.println(modifier+" st: "+start+"\tend: "+end+"\tsubs: "+targetText.getText().substring(start,end));
		
		// figure out terminator offset
		for(Mention m: terminators){
			// if going forward, make sure that the terminator is after modifier
			if(forward && modifier.before(m) && m.getStartPosition() < end){
				end = m.getStartPosition();
			}
			// if looking backward, make sure that the terminator is before modifier
			if(backward && modifier.after(m) && m.getStartPosition() > start)
				start = m.getStartPosition();
		}
		

		// go over mentions in a sentence
		for(Mention target: targetText.getMentions()){
			boolean add = false;

			// looking forward, if modifier is before target and target is before termination point
			if(forward && modifier.getStartPosition() <= target.getStartPosition() && target.getStartPosition() <= end){
				add = true;
			}
			// looking backward, if modifier is after target and target is after termination point
			if(backward &&  modifier.getStartPosition() >= target.getStartPosition() && start <= target.getStartPosition()){
				add = true;
			}

			if(add)
				list.add(target);
		}

		return list;
	}

	private int getWordWindowIndex(Mention modifier, Sentence targetText, boolean beforeModifier) throws TerminologyException {
		int offs;
		int windowSize = getWindowSize(modifier.getConcept());
		String txt = targetText.getText();
		int offset = targetText.getOffset();
		
		// if windows size after modifier
		if(beforeModifier){
			offs = targetText.getLength();
			for(int i = modifier.getEndPosition()-offset,j=0;i>=0 && i<txt.length();i = txt.indexOf(' ',i+1),j++){
				if(j >= windowSize){
					offs = i;
					break;
				}
			}
		// if windows size before modifier	
		}else{
			offs = 0;
			for(int i = modifier.getStartPosition()-offset,j=0;i>=0;i = txt.lastIndexOf(' ',i-1),j++){
				if(j > windowSize){
					offs = i;
					break;
				}
			}
		}
		return offs+offset;
	}


	private List<Mention> getTerminators(Mention modifier,Sentence text) throws TerminologyException{
		List<Mention> list = new ArrayList<Mention>();
		List<String> terminators = getTermination(modifier.getConcept());

		for(Mention m: text.getMentions()){
			if(getAction(m.getConcept()).contains(ACTION_TERMINATE)) {
				for (Concept parent : m.getConcept().getParentConcepts()) {
					if (terminators.contains(parent.getCode())) {
						list.add(m);
					}
				}
			}
		}
		return list;
	}

	
	/**
	 * get a list of linguistic modifiers that are not pseudo modifiers
	 * @param text
	 * @return
	 * @throws TerminologyException
	 */
	private List<Mention> getLinguisticModifiers(Sentence text) throws TerminologyException{
		List<Mention> list = new ArrayList<Mention>();
		List<Mention> pseudo = getPseudoModifiers(text);
		Concept linguisticModifier = terminology.lookupConcept(LINGUISTIC_MODIFIER);
		for(Mention m: text.getMentions()){
			if(paths.hasAncestor(m.getConcept(),linguisticModifier) && !isPseudo(m,pseudo)){
				list.add(m);
			}
		}
		return list;
	}
	
	
	/**
	 * get a list of pseudo modifier
	 * @param text
	 * @return
	 * @throws TerminologyException
	 */
	private List<Mention> getPseudoModifiers(Sentence text) throws TerminologyException{
		List<Mention> list = new ArrayList<Mention>();
		Concept pseudo = terminology.lookupConcept(PSEUDO);
		for(Mention m: text.getMentions()){
			if(paths.hasAncestor(m.getConcept(),pseudo)){
				list.add(m);
			}
		}
		return list;
	}
	

	/**
	 * is this method interacting with any of the pseudo modifiers?
	 * @param m
	 * @return
	 * @throws TerminologyException 
	 */
	private boolean isPseudo(Mention m, List<Mention> pseudo) throws TerminologyException {
		if(pseudo.isEmpty())
			return false;
		
		// get a list of valid pseudo categories for this modifier
		List<String> actions = getPseudo(m.getConcept());
		
		// if we do have possible pseudo actions
		if(!actions.isEmpty()){
			for(Mention p: pseudo){
				// if this modifier intesects with this pseudo
				if(m.intersects(p)){
					// make sure that this pseudo is a pseudo for this modifier
					for(String a: actions){
						for(Concept pp : p.getConcept().getParentConcepts()){
							// if this is a valid group, then cancel this modifier
							if(a.equals(pp.getCode()))
								return true;
						}
					}
				}
			}
		}
		return false;
	}


	private static List<String> getAction(Concept c) throws TerminologyException {
		List<String> list = new ArrayList<String>();
		list.add(c.getProperty(RELATION_ACTION));
		/*for(Concept a :	c.getRelatedConcepts(Relation.getRelation(RELATION_ACTION))){
			list.add(a.getCode());
		}*/
		return list;
	}
	
	
	/**
	 * get window size
	 * @param c
	 * @return
	 * @throws TerminologyException
	 */
	private static int getWindowSize(Concept c) throws TerminologyException {
		if(c.getProperties().containsKey(PROP_WINDOW_SIZE))
			return Integer.parseInt(""+c.getProperty(PROP_WINDOW_SIZE));
		for(Concept p: c.getParentConcepts()){
			return getWindowSize(p);
		}
		return DEFAULT_WINDOW_SIZE;
	}
	
	/**
	 * get modifier type for a given modifier mention
	 * @param c
	 * @return
	 */
	public static List<String> getModifierTypes(Concept c){
		List<String> types = new ArrayList<String>();
		for(SemanticType st: c.getSemanticTypes()){
			if(!SEMTYPE_INSTANCE.equals(st.getName()))
				types.add(st.getCode());
		}
		return types;
	}
	
	/**
	 * get modifier value for a given mention
	 * @param type
	 * @param c
	 * @return
	 */
	public static String getModifierValue(String type, Concept c) {
		return c.getProperty(type);
	}
	
	
	private List<String> getTermination(Concept c) throws TerminologyException {
		List<String> list = new ArrayList<String>();
		for(Concept p: c.getParentConcepts()){
			for(Concept t: p.getRelatedConcepts(Relation.getRelation(RELATION_TERMINATION))){
				list.add(t.getCode());
			}
		}
		return list;
	}
	
	private List<String> getPseudo(Concept c) throws TerminologyException {
		List<String> list = new ArrayList<String>();
		for(Concept p: c.getParentConcepts()){
			for(Concept t: p.getRelatedConcepts(Relation.getRelation(RELATION_PSEUDO))){
				list.add(t.getCode());
			}
		}
		return list;
	}
	
	
	
	public long getProcessTime() {
		return time;
	}

	
	public static void main(String[] args) throws IOntologyException, IOException, TerminologyException {
		ConText conText = new ConText();
		
		NobleCoderTerminology terminology = new NobleCoderTerminology("nlpBreastCancer");
		terminology.setScoreConcepts(true);
		terminology.setSelectBestCandidate(true);
		terminology.setDefaultSearchMethod(NobleCoderTerminology.PRECISE_MATCH);
		terminology.setSemanticTypeFilter("Neoplastic Process; Sign or Symptom; Finding");

		for(String text: Arrays.asList(
				"There was no evidence of melanoma for this patient, but there was a family history of breast cancer.",
				"The patient presents with a 3 day history of cough.",
				"There is no significant change in lymphacitic infiltrate.",
				"The patient reports mother has had breast cancer in the past.",
				"Images show possible dysplastic nevus vs melanoma.",
				"No lytic or blastic osseous lesions are seen.",
				"Heart Trouble: No High Blood Pressure: No Integumentary Skin Cancer/Skin Condition: No Skin Lesion/Rash: No Respiratory",
				"No definite ultrasonographic correlation of the posterior focus of enhancement at 3 o'clock of the left breast."
			
				)){
			Sentence sentence = new Sentence(text);
			
			// process with regular dictionary
			terminology.process(sentence);
			
			// process with context
			conText.process(sentence);
		
			// print results
			System.out.println("sentence: "+text+" | nc: "+terminology.getProcessTime()+" | context: "+conText.getProcessTime());
			for(Mention m: sentence.getMentions()){
				Concept c = m.getConcept();
				System.out.println("\t"+c.getName()+" ("+c.getCode()+") "+Arrays.toString(c.getSemanticTypes())+" \""+
						m.getText()+"\"");
				for(String context: m.getModifiers().keySet()){
					Modifier modifier = m.getModifier(context);
					String mention = modifier.getMention() != null?"\""+modifier.getMention()+"\"":"(default)";
					System.out.println("\t\t"+modifier.getType()+" : "+modifier+"\t "+mention);
				}

			}
			
		}
		
		
		/*	// display the ConText browser
		TerminologyBrowser tb = new TerminologyBrowser();
		tb.setTerminologies(new Terminology []{terminology,conText.getTerminology()});
		tb.showDialog(null,"ConText");*/
		
	}

}
