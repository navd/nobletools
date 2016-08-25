package edu.pitt.dbmi.nlp.noble.terminology;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.pitt.dbmi.nlp.noble.ontology.IOntologyError;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;


/**
 * This class describes a sematic type of a concept
 * @author tseytlin
 *
 */
public class SemanticType implements Serializable, Comparable<SemanticType>{
	private static final String SEMANTIC_TYPES = "/resources/SemanticTypes.txt";
	private static Map<String,SemanticType> semanticTypes;
	private static final long serialVersionUID = 1234567890L;
	private String name, code;

	
	/**
	 * get a list of predefined semantic types
	 * @return
	 */
	public static SemanticType [] getDefinedSemanticTypes(){
		return new TreeSet<SemanticType>(getSemanticTypeMap().values()).toArray(new SemanticType [0]);
	}
	
	/**
	 * get a list of predefined semantic types
	 * @return
	 */
	private static Map<String,SemanticType> getSemanticTypeMap(){
		if(semanticTypes == null){
			semanticTypes = new HashMap<String, SemanticType>();
			InputStream in = TextTools.class.getResourceAsStream(SEMANTIC_TYPES);
			try {
				for(String s: TextTools.getText(in).split("\n")){
					String [] p = s.split("\t");
					if(p.length == 2){
						String tui = p[0].trim();
						String sty = p[1].trim();
						SemanticType st = new SemanticType(sty,tui);
						semanticTypes.put(tui,st);
						semanticTypes.put(sty,st);
					}
				}
			} catch (IOException e) {
				throw new IOntologyError("Unable to parse SemanticTypes file "+SEMANTIC_TYPES,e);
			}
			
		}
		return semanticTypes;
	}
	
	
	
	/**
	 * Create semantic type w/ name and code
	 * @param name
	 * @param code
	 */
	private SemanticType(String name){
		this(name,name);
	}
	
	/**
	 * Create semantic type w/ name and code
	 * @param name
	 * @param code
	 */
	private SemanticType(String name, String code){
		this.name = name;
		this.code = code;
	}
	
	/**
	 * @return the code
	 */
	public String getCode() {
		return (code != null)?code:name;
	}

	/**
	 * @param code the code to set
	 */
	public void setCode(String code) {
		this.code = code;
	}

	/**
	 * @return the name
	 */
	public String getName() {
		return name;
	}

	/**
	 * @param name the name to set
	 */
	public void setName(String name) {
		this.name = name;
	}
	
	/**
	 * string representation (name)
	 */
	public String toString(){
		return name;
	}
	
	/**
	 * get instance of definition class
	 * @param text
	 * @return
	 */
	public static SemanticType getSemanticType(String text){
		SemanticType st = getSemanticTypeMap().get(text);
		return (st != null)?st:new SemanticType(text);
	}
	
	/**
	 * get instance of definition class
	 * @param text
	 * @return
	 */
	public static SemanticType getSemanticType(String text,String code){
		SemanticType st = getSemanticTypeMap().get(text);
		if(st == null)
			st = getSemanticTypeMap().get(code);
		return (st != null)?st:new SemanticType(text,code);
	}
	
	
	
	/**
	 * get instance of definition class
	 * @param text
	 * @return
	 */
	public static SemanticType [] getSemanticTypes(String [] text){
		if(text == null)
			return new SemanticType [0];
		SemanticType [] d = new SemanticType [text.length];
		for(int i=0;i<d.length;i++)
			d[i] = getSemanticType(text[i]);
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
		Element e = doc.createElement("SemanticType");
		e.setAttribute("name",getName());
		e.setAttribute("code",getCode());
		return e;
	}
	
	/**
	 * convert from DOM element
	 * @param element
	 * @throws TerminologyException
	 */
	public void fromElement(Element element) throws TerminologyException{
		if(element.getTagName().equals("SemanticType")){
			setName(element.getAttribute("name"));
			setCode(element.getAttribute("code"));
		}
	}

	public int compareTo(SemanticType o) {
		return getName().compareTo(o.getName());
	}
}
