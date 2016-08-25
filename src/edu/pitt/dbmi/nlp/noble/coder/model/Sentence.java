package edu.pitt.dbmi.nlp.noble.coder.model;

import java.util.*;

/**
 * This object represent a sentence in a document
 * "Sentence" can be of different types: from standard prose, to list, to synoptic
 * @author tseytlin
 *
 */
public class Sentence extends Text {
	public static final String TYPE_PROSE = "Prose";
	public static final String TYPE_LINE  = "Line";
	public static final String TYPE_WORKSHEET = "Worksheet";
	public static final String TYPE_HEADER = "Header";
	
	private String sentenceType = TYPE_PROSE;
	private List<Mention> mentions;
	private Document document;
	private Section section;
	private Map<String,String> properties;
	
	public Sentence(){}
	public Sentence(String text){
		this(text,0,TYPE_PROSE);
	}
	public Sentence(Sentence s){
		this(s.getText(),s.getOffset(),s.getSentenceType());
	}
	
	
	public Sentence(String text,int offs,String type){
		setText(text);
		setOffset(offs);
		this.sentenceType = type;
	}
	
	public Map<String,String> getProperties() {
		if(properties == null)
			properties = new LinkedHashMap<String, String>();
		return properties;
	}
	
	public Document getDocument() {
		return document;
	}
	public void setDocument(Document document) {
		this.document = document;
	}
	
	public Section getSection() {
		return section;
	}
	public void setSection(Section section) {
		this.section = section;
	}
	public String getSentenceType() {
		return sentenceType;
	}
	public void setSentenceType(String sentenceType) {
		this.sentenceType = sentenceType;
	}
	public List<Mention> getMentions() {
		if(mentions == null)
			mentions = new ArrayList<Mention>();
		return mentions;
	}
	public void setMentions(List<Mention> mentions) {
		this.mentions = mentions;
		Collections.sort(this.mentions);
		for(Mention m: this.mentions){
			m.setSentence(this);
		}
	}
	
}
