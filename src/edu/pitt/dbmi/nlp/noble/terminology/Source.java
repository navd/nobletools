package edu.pitt.dbmi.nlp.noble.terminology;

import java.io.Serializable;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

/**
 * Implements Source
 * @author tseytlin
 */
public class Source implements Serializable, Comparable {
	private static final long serialVersionUID = 1234567890L;
	private String code,name,description;
	private transient String version;
	public static final Source URI = getSource("URI");
	
	
	/**
	 * Create empty source
	 */
	public Source(){}
	
	
	/**
	 * Create empty source
	 */
	public Source(String name){
		this(name,"",name);
	}
	
	
	/**
	 * Constract source w all values filled in
	 * @param name
	 * @param description
	 * @param code
	 */
	public Source(String name,String description, String code){
		this.name = name;
		this.description = description;
		this.code = code;
	}
	
	/**
	 * @return the code
	 */
	public String getCode() {
		return code;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the description
	 */
	public String getDescription() {
		return description;
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name == null?code:name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * String representation
	 */
	public String toString(){
		return code;
	}
	
	public String getVersion() {
		if(version == null && description != null){
			Matcher m = Pattern.compile(".*,\\s*(.{1,15})").matcher(description);
			if(m.matches())
				return m.group(1);
		}
		return version;
	}


	public void setVersion(String version) {
		this.version = version;
	}


	/**
	 * compare 2 sources
	 */
	public int compareTo(Object obj){
		if(obj instanceof Source){
			return getCode().compareTo(((Source)obj).getCode());
		}
		return 0;
	}
	
	/**
	 * get instance of definition class
	 * @param text
	 * @return
	 */
	public static Source getSource(String text){
		return new Source(text);
	}
	
	/**
	 * get instance of definition class
	 * @param text
	 * @return
	 */
	public static Source [] getSources(String [] text){
		if(text == null)
			return new Source [0];
		Source [] d = new Source [text.length];
		for(int i=0;i<d.length;i++)
			d[i] = new Source(text[i]);
		return d;
	}
	
	public int hashCode() {
		return toString().hashCode();
	}

	public boolean equals(Object obj) {
		return toString().equals(obj.toString());
	}


	/**
	 * convert to DOM element
	 * @param doc
	 * @return
	 */
	public Element toElement(Document doc) throws TerminologyException {
		Element root = doc.createElement("Source");
		root.setAttribute("name",getName());
		root.setAttribute("code",getCode());
		root.setAttribute("version",getVersion());
		if(getDescription() != null)
			root.setTextContent(getDescription());
		return root;
	}
	/**
	 * convert from DOM element
	 * @param element
	 * @throws TerminologyException
	 */
	public void fromElement(Element element) throws TerminologyException{
		if(element.getTagName().equals("Source")){
			setName(element.getAttribute("name"));
			setCode(element.getAttribute("code"));
			setVersion(element.getAttribute("version"));
			String text = element.getTextContent().trim();
			if(text.length() > 0)
				setDescription(text);
		}
	}
}
