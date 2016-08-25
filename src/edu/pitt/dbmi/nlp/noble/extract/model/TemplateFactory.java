package edu.pitt.dbmi.nlp.noble.extract.model;

import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.ANATOMY_ONTOLOGY_URI;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.LOCATIONS;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.MODIFIERS;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.VALUES;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.getFeature;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.getPotentialTemplateAttributes;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isAnatomicLocation;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isAttributeCategory;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isDisease;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isFeature;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isHeader;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isLocation;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isModifier;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isNumber;
import static edu.pitt.dbmi.nlp.noble.extract.model.util.SlideTutorOntologyHelper.isWorksheet;
import static edu.pitt.dbmi.nlp.noble.extract.model.TemplateCreator.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.TransformerException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.concept.ConceptRegistry;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.CompositTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.StringUtils;
import edu.pitt.dbmi.nlp.noble.util.XMLUtils;

/**
 * create Templates for information extraction from various sources
 * @author tseytlin
 */
public class TemplateFactory {
	private Map<String,Template> templates;
	private static TemplateFactory instance;
	private TemplateFactory(){}
	public static TemplateFactory getInstance(){
		if(instance == null)
			instance = new TemplateFactory();
		return instance;
	}
	
	/**
	 * get a map of templates
	 */
	public Map<String,Template> getTemplateMap(){
		if(templates == null)
			templates = new HashMap<String, Template>();
		return templates;
	}
	
	/**
	 * get template based on name
	 */
	public Template getTemplate(String name){
		return getTemplateMap().get(name);
	}
	
	/**
	 * get all template based on name
	 */
	public List<Template> getTemplates(){
		if(getTemplateMap().values() instanceof List)
			return (List)getTemplateMap().values();
		return new ArrayList<Template>(getTemplateMap().values());
	}
	
	/**
	 * import templates from url
	 * looks at URL to figure out how to import it
	 * @param url
	 */
	public void importTemplates(String url) throws Exception {
		// is this a known SlideTutor ontology?
		if(url.matches("http://.*/curriculum/owl/.*\\.owl(#.+)?")){
			addSlideTutorTemplates(url);
		// is this an existing terminology?
		}else if(url.endsWith(NobleCoderTerminology.TERM_SUFFIX) && new File(url).exists()){
			addTerminologyTemplate(url);
		}else if(new File(NobleCoderTerminology.getPersistenceDirectory(),url+NobleCoderTerminology.TERM_SUFFIX).exists()){
			addTerminologyTemplate(url);
		}
	}
	
	/**
	 * create a template from terminology where each root is a template item
	 * @param url
	 */
	private void addTerminologyTemplate(String url) throws Exception {
		Template template = importTerminologyTemplate(url);
		getTemplateMap().put(template.getName(),template);
	}
	
	/**
	 * create a template from terminology where each root is a template item
	 * @param url
	 */
	public static Template importTerminologyTemplate(String url) throws Exception {
		Terminology terminology = new NobleCoderTerminology(url);
		
		// setup template
		Template template = new Template();
		template.setName(terminology.getName()+" Template");
		template.setDescription(terminology.getDescription());
		template.setTerminology(terminology);
		template.getFilters().add(new DocumentFilter("(?s)^BACKGROUND:$.*^$",true));
		
		for(Concept c: terminology.getRootConcepts()){
			TemplateItem item = new TemplateItem();
			/*			
			{
				//TODO: this is a hack for BRAF, let it stay for now?
				public List<ItemInstance> process(TemplateDocument doc) throws TerminologyException {
					// do not include concepts from BACKGROUND section and below
					List<ItemInstance> items =  super.process(doc);
					int s = doc.getText().indexOf("\nBACKGROUND:\n");
					if(s > -1){
						List<ItemInstance> items2 = new ArrayList<ItemInstance>();
						for(ItemInstance i: items){
							if(i.getAnnotations().get(0).getEndPosition() < s)
								items2.add(i);
						}
						return items2;
					}
					return items;
				}
				
			};
			*/
			item.setTemplate(template);
			item.setConcept(c);
			item.setType(TemplateItem.TYPE_FINDING);
			item.setValueDomain(TemplateItem.DOMAIN_SELF);
			template.getTemplateItems().add(item);
		}
		return template;
	}


	public static class SlideTutorConcept extends Concept {
		public SlideTutorConcept(IClass cls) {
			super(cls);
			if(isWorksheet(cls))
				setName(cls.getName());
		}
	}
	
	/**
	 * create a template object from SlideTutor ontology
	 * @param url
	 * @return
	 */
	private void addSlideTutorTemplates(String url) throws Exception {
		for(Template template: importSlideTutorTemplates(url)){
			getTemplateMap().put(template.getName(),template);
		}
	}
	
	/**
	 * create a template object from SlideTutor ontology
	 * @param url
	 * @return
	 */
	public static List<Template> importSlideTutorTemplates(String url) throws Exception {
		List<Template> templates = new ArrayList<Template>();
		IOntology ont = OOntology.loadOntology(url);
		ConceptRegistry.REGISTRY.put(url,SlideTutorConcept.class.getName());
		
		// create in-memory terminology from this ontology
		NobleCoderTerminology term = new NobleCoderTerminology();
		term.loadOntology(ont,null,true,true);
		term.setScoreConcepts(false);
		term.setSelectBestCandidate(false);
		term.setCachingEnabled(false);
		
		NobleCoderTerminology aterm = new NobleCoderTerminology();
		aterm.loadOntology(OOntology.loadOntology(""+ANATOMY_ONTOLOGY_URI),null,true,true);
		aterm.setCachingEnabled(false);
		
		// add a terminology to it
		CompositTerminology terminology = new CompositTerminology();
		terminology.addTerminology(term);
		terminology.addTerminology(aterm);
		
		
		// go over templates
		for(IClass template: ont.getClass("TEMPLATES").getDirectSubClasses()){
			// get orders
			final Map<String,Integer> conceptOrder = new HashMap<String,Integer>();
			for(Object o: template.getPropertyValues(ont.getProperty("order"))){
				String [] p = o.toString().split(":");
				if(p.length == 2){
					conceptOrder.put(p[0].trim(),new Integer(p[1].trim()));
				}
			}
			// get triggers
			//TODO:
			
			// get template contents
			List<IClass> templateContent = new ArrayList<IClass>();
			for(IRestriction r: template.getRestrictions(ont.getProperty("hasPrognostic"))){
				IClass c = (IClass) r.getParameter().getOperand();
				templateContent.add(c);
			}
			
			// sort them in order
			Collections.sort(templateContent,new Comparator<IClass>() {
				public int compare(IClass o1, IClass o2) {
					if(conceptOrder.containsKey(o1.getName()) && conceptOrder.containsKey(o2.getName())){
						return conceptOrder.get(o1.getName()).compareTo(conceptOrder.get(o2.getName()));
					}
					return o1.compareTo(o2);
				}
			});
			
			// setup template
			Template temp = new Template();
			temp.setName(TextTools.getCapitalizedWords(template.getName()));
			temp.setDescription(template.getDescription());
			temp.setTerminology(terminology);
			
			for(IClass c: templateContent){
				TemplateItem t = convertSlideTutorClass(c,temp);
				temp.getTemplateItems().add(t);
			}
			templates.add(temp);
		}
		return templates;
	}

	private static String getCode(String uri, boolean truncate){
		if(truncate){
			int x = uri.lastIndexOf('/');
			return (x > -1)?uri.substring(x+1):uri;
		}
		return uri;
	}
	/**
	 * create a template item from a given class
	 * @param c
	 * @return
	 */
	private static TemplateItem convertSlideTutorClass(IClass c, Template template) {
		IOntology ont = c.getOntology();
		TemplateItem item = new TemplateItem();
		item.setTemplate(template);
		item.setConcept(c.getConcept());
		item.getConcept().setCode(getCode(item.getConcept().getCode(),true));
		item.setValueDomain(TemplateItem.DOMAIN_BOOLEAN);
		
		// figure out type
		if(isFeature(c)){
			item.setType(TemplateItem.TYPE_FINDING);
		}else if(isAttributeCategory(c)){
			item.setType(TemplateItem.TYPE_ATTRIBUTE);
		}else if(isLocation(c)){
			item.setType(TemplateItem.TYPE_ATTRIBUTE_VALUE);
		}else if(isNumber(c)){
			item.setType(TemplateItem.TYPE_NUMERIC_VALUE);
		}else if(isModifier(c)){
			item.setType(TemplateItem.TYPE_MODIFIER);
		}else if(isDisease(c)){
			item.setType(TemplateItem.TYPE_DIAGNOSIS);
			item.setValueDomain(TemplateItem.DOMAIN_SELF);
		}
		
		// if feature process attributes
		if(isFeature(c)){
			IClass feature = getFeature(c);	
			
			if(!feature.equals(c)){
				item.setFeature(convertSlideTutorClass(feature,template));
				
				// if feature is a child of value then, it is a fully specified feature attr/value, and
				// we just need a feature
				if(isOfParent(c,VALUES))
					item = item.getFeature();
				
				// if we have a more general feature specified, then 
				
			}
			
			// process potential attributes
			for(IClass attribute: getPotentialTemplateAttributes(c)){
				// if used attribute, skip
				//if(!usedAttributes.contains(attribute))
				//	continue;
				
				TemplateItem a = convertSlideTutorClass(attribute,template);
				// handle numbers
				if(isNumber(attribute)){
					item.getValues().add(a);
					item.setValueDomain(TemplateItem.DOMAIN_VALUE);
				// handle units
				}else if(isOfParent(attribute,"UNITS")){
					item.getUnits().add(a);
				// handle locations
				}else if(isLocation(attribute)){
					TemplateItem l = convertSlideTutorClass(ont.getClass(LOCATIONS), template);
					item.addAttributeValue(l,a);
				// handle attributes with categories and modifiers
				}else if(isModifier(attribute)){
					if(!attribute.hasSubClass(c))
						item.setValueDomain(TemplateItem.DOMAIN_ATTRIBUTE);
					for(IClass  acat : attribute.getDirectSuperClasses()){
						if(isAttributeCategory(acat) && !acat.equals(ont.getClass(MODIFIERS))){
							TemplateItem l = convertSlideTutorClass(acat, template);
							item.addAttributeValue(l, a);
						}else{
							item.addModifier(a);
						}
					}
				}else{
					//System.err.println(attribute);
				}
			}
			
			// do something special for worksheet?
			if(isWorksheet(c)){
				item.setValueDomain(TemplateItem.DOMAIN_SELF);
			}else if(isHeader(c)){
				item.setValueDomain(TemplateItem.DOMAIN_SELF);
			} 
			
			// anatomic location?	
			if(isAnatomicLocation(c)){
				item.setType(TemplateItem.TYPE_ORGAN);
				String code = getCode((String) c.getPropertyValue(ont.getProperty("code")),true);
				if(code != null){
					String cd = (code.indexOf("#") > -1)?code.substring(0,code.lastIndexOf("#")):code;
					String nm = (cd.indexOf("/") > -1)?cd.substring(cd.lastIndexOf("/")+1):cd;
					Source src = new Source(nm, "", cd);
					try {
						template.getTerminology().lookupConcept(item.getConcept().getCode()).addCode(code,src);
					} catch (TerminologyException e) {
						e.printStackTrace();
					}
					item.getConcept().addCode(code,src);
					
				}
				
			}
		}else if(isDisease(c)){
			//NOOP
		}
		
		
		return item;
	}
	
	/**
	 * check if this entry is a feature
	 * @return
	 */
	public static boolean isOfParent(IClass cls,String parent){
		if(cls == null)
			return false;
		IOntology o = cls.getOntology();
		IClass p = o.getClass(parent);
		return cls.equals(p) || cls.hasSuperClass(p);
	}
	
	
	/**
	 * export template
	 * @param t
	 * @param out
	 * @throws IOException
	 * @throws ParserConfigurationException 
	 * @throws TransformerException 
	 */
	public void exportTemplate(Template t, OutputStream out) throws Exception{
		// initialize document and root
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		Document doc = factory.newDocumentBuilder().newDocument();
		
		// create DOM object
		doc.appendChild(t.toElement(doc));
		
		// write out XML
		XMLUtils.writeXML(doc, out);
	}
	

	/**
	 * export template
	 * @param t
	 * @param out
	 * @throws IOException
	 */
	public Template importTemplate(InputStream in) throws Exception{
		Document document = XMLUtils.parseXML(in);
		
		//print out some useful info
		Element element = document.getDocumentElement();
		if(element.getTagName().equals("Template")){
			Template temp = new Template();
			temp.fromElement(element);
			getTemplateMap().put(temp.getName(),temp);
			return temp;
		}
		return null;
	}
	
	
	/**
	 * import template from specially crafted Ontology. This terminology is assumed to have 3 top-level classes:
	 * Slot, Template and Terminology
	 * It also suppose to have two object properties hasMentionOf and hasSlot as well as one data property hasDocumentRange 
	 * @param url
	 * @return
	 * @throws IOntologyException 
	 * @throws TerminologyException 
	 * @throws IOException 
	 * @throws Exception
	 */
	public static List<Template> importOntologyTemplate(String url) throws IOntologyException, IOException, TerminologyException {
		List<Template> templates = new ArrayList<Template>();
		IOntology ont = OOntology.loadOntology(url);
		NobleCoderTerminology terminology = new NobleCoderTerminology(ont);
		
		String name = ont.getName();
		if(ont.getLabels().length > 0)
			name = ont.getLabels()[0];
		
		// go over templates defined in a file
		IProperty hasSlot = ont.getProperty("hasSlot");
		for(IClass temp : ont.getClass("Template").getDirectSubClasses()){
			Template template = new Template();
			template.setName(name);
			template.setDescription(ont.getDescription());
			template.setTerminology(terminology);
			
			// get all hasSlot restriction
			for(IRestriction r: temp.getRestrictions(hasSlot)){
				TemplateItem item = convertSlotClass(template,r.getParameter());
				if(item != null){
					template.getTemplateItems().add(item);
				}
			}
			
			templates.add(template);
		}
		return templates;
	}
	
	/**
	 * convert slot class 
	 * @param parameter
	 * @return
	 * @throws TerminologyException 
	 */
	private static TemplateItem convertSlotClass(Template template, ILogicExpression parameter) throws TerminologyException {
		if(!(parameter.getOperand() instanceof IClass))
			return null;
		
		// assume that his is in fact a class
		IClass cls = (IClass) parameter.getOperand();
		
		// find a concept category that needs to be mentioned
		Terminology term = template.getTerminology();
		TemplateContainer t = getTemplateContainer(term,cls);
		
		TemplateItem item = new TemplateItem();
		item.setConcept(t.feature);
		item.setTemplate(template);
		item.setType(TYPE_FINDING);
		if(t.isFAV()){
			item.setType(TYPE_FEATURE);
			item.setValueDomain(TemplateItem.DOMAIN_ATTRIBUTE);
			for(Concept c: t.modifiers){
				TemplateItem m = new TemplateItem();
				m.setConcept(c);
				m.setTemplate(template);
				m.setType(TYPE_MODIFIER);
				item.addModifier(m);
			}
			for(Concept c: t.attributes.keySet()){
				TemplateItem attr = new TemplateItem();
				attr.setConcept(c);
				attr.setTemplate(template);
				attr.setType(TYPE_ATTRIBUTE);
				for(Concept cc: t.attributes.get(c)){
					TemplateItem av = new TemplateItem();
					av.setConcept(cc);
					av.setTemplate(template);
					av.setType(TYPE_VALUE);
					item.addAttributeValue(attr, av);
				}
			}
			if(t.numericValue != null){
				TemplateItem attr = new TemplateItem();
				attr.setConcept(t.numericValue);
				attr.setTemplate(template);
				attr.setType(TYPE_NUMERIC_VALUE);
				item.getValues().add(attr);
				item.setValueDomain(TemplateItem.DOMAIN_VALUE);
			}
		}else{
			item.setValueDomain(TemplateItem.DOMAIN_SELF);
		}
		
		// add filters
		for(String flt: t.filter){
			item.getFilters().add(new DocumentFilter(flt));
		}
		
		
		return item;
	}
	
	private static class TemplateContainer {
		public Concept feature, numericValue;
		public List<Concept> modifiers = new ArrayList<Concept>();
		public Map<Concept,List<Concept>> attributes = new LinkedHashMap<Concept, List<Concept>>();
		public List<String> filter = new ArrayList<String>();
		public boolean isFAV(){
			return !(attributes.isEmpty() && modifiers.isEmpty() && numericValue == null);
		}
	}
	
	private static TemplateContainer getTemplateContainer(Terminology term, IClass cls) throws TerminologyException{
		IOntology ont = cls.getOntology();
		IProperty hasMentionOf = ont.getProperty(PROP_HAS_MENTION);
		IProperty hasFeature = ont.getProperty(PROP_HAS_FEATURE);
		IProperty hasDocumentRange = ont.getProperty(PROP_HAS_DOC_RANGE);
		
		TemplateContainer c = new TemplateContainer();
		for(Object o: cls.getNecessaryRestrictions()){
			if(o instanceof IRestriction){
				IRestriction r = (IRestriction) o;
				if(r.getParameter().isSingleton() && r.getParameter().getOperand() instanceof IClass){
					IClass m = (IClass) r.getParameter().getOperand();
					Concept cc = term.lookupConcept(StringUtils.getAbbreviatedURI(m.getURI().toString()));
					
					if(Arrays.asList(hasMentionOf,hasFeature).contains(r.getProperty())){
						c.feature = cc;
					}else if (r.getProperty().equals(ont.getProperty(PROP_HAS_ATTRIBUTE))){
						c.attributes.put(cc,Arrays.asList(cc.getChildrenConcepts()));
					}else if (r.getProperty().equals(ont.getProperty(PROP_HAS_MODIFIER))){
						Collections.addAll(c.modifiers,cc.getChildrenConcepts());
					}else if (r.getProperty().equals(ont.getProperty(PROP_HAS_NUMERIC_VALUE))){
						c.numericValue = cc;
					}
				}
			}
		}
		
		
		c.filter = new ArrayList<String>();
		for(IRestriction r: cls.getRestrictions(hasDocumentRange)){
			c.filter.add(r.getParameter().getOperand().toString());
		}
		return c;
		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String ontologyFile  = "/home/tseytlin/Data/OrganTransplantExtraction.owl";
		String templateFile = "/home/tseytlin/Data/PICO_OrganTransplant.template";
		
		TemplateFactory tf = TemplateFactory.getInstance();
		for(Template template : TemplateFactory.importOntologyTemplate(ontologyFile)){
			tf.exportTemplate(template,new FileOutputStream(new File(templateFile)));
		}
		/*		// print out template information
		for(Template t: tf.getTemplates()){
			System.out.println("---| "+t.getName()+" |---");
			for(TemplateItem i: t.getTemplateItems()){
				System.out.println("\t"+i);
			}
		}
		
		System.out.println("\n-----------------------------------------\n");
		
		// process sample report with one of the templates
		Template template = tf.getTemplate("invasive_lesion_template");
		
		// get sample document
		File f = new File("/home/tseytlin/Data/Reports/ReportProcessorInput/AP_201.txt");
		String text = TextTools.getText(new FileInputStream(f));
		TemplateDocument doc = new TemplateDocument();
		doc.setName(f.getName());
		doc.setText(text);
		
		
		
		// do a simple parsing of this document
		long time = System.currentTimeMillis();
		int offset = 0;
		for(String line: text.split("\n")){
			for(String phrase: line.split("[,\\:]")){
				for(Concept c: template.getTerminology().search(phrase,NobleCoderTerminology.BEST_MATCH)){
					for(Annotation a: Annotation.getAnnotations(c)){
						a.updateOffset(offset);
						doc.addAnnotation(a);
					}
					doc.addConcept(c);
				}
				offset += (line.length()+1);
			}
			offset++;
		}
		doc.sort();
		time = System.currentTimeMillis()-time;
		System.out.println(doc.getText());
		System.out.println("--------------");
		for(Annotation a: doc.getAnnotations()){
			System.out.println(a.getText()+" ... ("+a.getStartPosition()+","+a.getEndPosition()+ ") \t->\t"+a.getConcept().getName()+" ... "+a.getConcept().getCode());
		}
		System.out.println("\n----------------( "+time+" )-------------------\n");
		
		// now lets do information extraction
		if(template.isAppropriate(doc)){
			time = System.currentTimeMillis();
			List<ItemInstance> items = template.process(doc);
			time = System.currentTimeMillis()-time;
			for(ItemInstance i: items){
				System.out.println(i.getQuestion()+" : "+i.getAnswer());
			}
			System.out.println("\n----------------( "+time+" )-------------------\n");
		}*/
	}


}
