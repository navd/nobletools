package edu.pitt.dbmi.nlp.noble.extract.model;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.pitt.dbmi.nlp.noble.terminology.CompositTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.util.PathHelper;
import edu.pitt.dbmi.nlp.noble.util.XMLUtils;


/**
 * this class represents a set of pieces of information that need to be 
 * extracted from free text
 * @author tseytlin
 */
public class Template {
	public static String OPTION_PREFERRED_CODE_PATTERN = "preferred.concept.code.pattern";
	private String name, description,version;
	private List<TemplateItem> templateItems;
	private List<DocumentFilter> filters;
	private Terminology terminology;
	private PathHelper pathHelper;
	private Properties properties;
	
	/**
	 * get template name.
	 * @return
	 */
	public String getName() {
		return name;
	}

	/**
	 * set template name.
	 * @return
	 */
	public void setName(String name) {
		this.name = name;
	}

	/**
	 * get template desciption
	 * @return
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * set template description
	 * @param description
	 */
	public void setDescription(String description) {
		this.description = description;
	}



	public String getVersion() {
		return version;
	}



	public void setVersion(String version) {
		this.version = version;
	}



	public Properties getProperties() {
		if(properties == null)
			properties = new Properties();
		return properties;
	}

	/**
	 * get a list  of template items that need to be extracted
	 * @return
	 */
	public List<TemplateItem> getTemplateItems(){
		if(templateItems == null)
			templateItems = new ArrayList<TemplateItem>();
		return templateItems;
	}

	
	/**
	 * get terminology that covers this template
	 * @return
	 */
	public Terminology getTerminology() {
		return terminology;
	}
	
	/**
	 * set terminology that covers template
	 * @param terminology
	 */
	public void setTerminology(Terminology terminology) {
		this.terminology = terminology;
	}

	
	/**
	 * get path helper to determine the hiearchy between concepts
	 * @return
	 */
	public PathHelper getPathHelper() {
		if(pathHelper == null){
			pathHelper = new PathHelper(getTerminology());
			pathHelper.setPathDepthLimit(-1);
		}
		return pathHelper;
	}

	/**
	 * Check if this template is appropriate for this document
	 * @param doc
	 * @return
	 */
	public boolean isAppropriate(TemplateDocument doc){
		//TODO: 
		return true;
	}
	
	
	/**
	 * process this template against annotated document. 
	 * If template is not appropriate, it will not be evaluated
	 * @param annotated document
	 * @return list if ItemInstance object that are instances of TemplateItems in order
	 */
	public List<ItemInstance> process(TemplateDocument doc) throws TerminologyException{
		List<ItemInstance> list = new ArrayList<ItemInstance>();
		doc.getItemInstances().put(this,list);
		if(isAppropriate(doc)){
			for(TemplateItem item: getTemplateItems()){
				list.addAll(item.process(doc));
			}
		}
		return list;
	}
	
	/**
	 * convert to string
	 */
	public String toString(){
		return getName();
	}
	
	/**
	 * get document filters
	 * @return
	 */
	public List<DocumentFilter> getFilters() {
		if(filters == null)
			filters = new ArrayList<DocumentFilter>();
		return filters;
	}

	/**
	 * convert Template to XML DOM object representation
	 * @return
	 */
	public Element toElement(Document doc) throws Exception {
		Element root = doc.createElement("Template");
		root.setAttribute("name",getName());
		root.setAttribute("version",getVersion());
		
		// add description
		Element desc = doc.createElement("Description");
		desc.setTextContent(getDescription());
		root.appendChild(desc);
		
		// add options 
		Element options = doc.createElement("Options");
		Properties p = getProperties();
		for(Object key: p.keySet()){
			Element opt = doc.createElement("Option");
			opt.setAttribute("name",""+key);
			opt.setAttribute("value",""+p.get(key));
			options.appendChild(opt);
		}
		root.appendChild(options);
		
		
		// add filters
		if(!getFilters().isEmpty()){
			Element filt = doc.createElement("Filters");
			for(DocumentFilter f: getFilters()){
				filt.appendChild(f.toElement(doc));
			}
			root.appendChild(filt);
		}
		
		// add template items
		Element items = doc.createElement("TemplateItems");
		root.appendChild(items);
		
		for(TemplateItem item: getTemplateItems()){
			items.appendChild(item.toElement(doc));
		}
		
		// add terminology 
		Terminology term = getTerminology();
		if(term instanceof CompositTerminology){
			for(Terminology t : ((CompositTerminology)term).getTerminologies())
				root.appendChild(t.toElement(doc));
		}else{
			root.appendChild(term.toElement(doc));
		}
		// append template element
		return root;
	}
	
	/**
	 * initialize template from XML DOM object representation
	 * @param element
	 */
	public void fromElement(Element element) throws Exception {
		setName(element.getAttribute("name"));
		setVersion(element.getAttribute("version"));
		setDescription(XMLUtils.getElementByTagName(element,"Description").getTextContent().trim());
		
		// load filters
		Element flt = XMLUtils.getElementByTagName(element,"Filters");
		if(flt != null){
			for(Element t: XMLUtils.getElementsByTagName(flt,"Filter")){
				DocumentFilter df = new DocumentFilter();
				df.fromElement(t);
				getFilters().add(df);
			}
		}
		// load options
		Element opts = XMLUtils.getElementByTagName(element,"Options");
		if(opts != null){
			for(Element op: XMLUtils.getElementsByTagName(opts,"Option")){
				getProperties().setProperty(op.getAttribute("name"),op.getAttribute("value"));
			}
		}
		
		
		//load terminology
		List<Terminology> terms = new ArrayList<Terminology>();
		for(Element t: XMLUtils.getElementsByTagName(element,"Terminology")){
			NobleCoderTerminology term = new NobleCoderTerminology();
			term.fromElement(t);
			terms.add(term);
		}
		//term.setCachingEnabled(false);
		if(terms.size() == 1){
			setTerminology(terms.get(0));
		}else{
			CompositTerminology ct = new CompositTerminology();
			ct.getTerminologies().addAll(terms);
			setTerminology(ct);
		}
		
		Element items = XMLUtils.getElementByTagName(element,"TemplateItems");
		for(Element i: XMLUtils.getElementsByTagName(items,"TemplateItem")){
			TemplateItem item = new TemplateItem();
			item.setTemplate(this);
			item.fromElement(i);
			getTemplateItems().add(item);
		}
	}
}
