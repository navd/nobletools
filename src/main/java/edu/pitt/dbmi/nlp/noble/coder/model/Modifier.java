package edu.pitt.dbmi.nlp.noble.coder.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.tools.ConText;

public class Modifier {
	private String type,value;
	private Mention mention;
	private boolean defaultValue;
	
	public Modifier(String type, String value){
		this.type = type;
		this.value = value;
	}
	
	/**
	 * get a list of modifiers from a given ConText mention
	 * @param m
	 * @return
	 */
	public static List<Modifier> getModifiers(Mention m){
		List<Modifier> list = new ArrayList<Modifier>();
		for(String type : ConText.getModifierTypes(m.getConcept())){
			String value = ConText.getModifierValue(type,m.getConcept());
			Modifier mod = new Modifier(type,value);
			mod.setMention(m);
			list.add(mod);
		}
		return list;
	}
	
	/**
	 * get a list of modifiers from a given ConText mention
	 * @param m
	 * @return
	 */
	public static Modifier getModifier(String type, String value, Mention m){
		Modifier mod = new Modifier(type,value);
		mod.setMention(m);
		return mod;
	}
	
	
	public boolean isDefaultValue(){
		return defaultValue; 
	}
	
	public void setDefaultValue(boolean defaultValue) {
		this.defaultValue = defaultValue;
	}

	/**
	 * get modifier with type/value
	 * @param type
	 * @param value
	 * @return
	 */
	public static Modifier getModifier(String type, String value){
		return new Modifier(type,value);
	}
	
	public String getType() {
		return type;
	}
	public void setType(String type) {
		this.type = type;
	}
	public String getValue() {
		return value;
	}
	public void setValue(String value) {
		this.value = value;
	}
	public Mention getMention() {
		return mention;
	}
	public void setMention(Mention mention) {
		this.mention = mention;
	}
	
	public String toString(){
		return value;
	}
	
	/**
	 * get annotations for this modifier
	 * @return
	 */
	public List<Annotation> getAnnotations(){
		if(mention != null)
			return mention.getAnnotations();
		return Collections.EMPTY_LIST;
	}
}
