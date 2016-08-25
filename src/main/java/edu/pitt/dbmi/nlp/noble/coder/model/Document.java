package edu.pitt.dbmi.nlp.noble.coder.model;

import java.util.*;

import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;

/**
 * This class represents an object that represents an input document
 * @author tseytlin
 */
public class Document extends Text {
	public static final String TYPE_MEDICAL_REPORT = "Medical Report";
	public static final String TYPE_MEDLINE_RECORD = "Medline Record";
	public static final String TYPE_ARTICLE  = "Article";
	
	public static final String STATUS_UNPROCESSED = "Unprocessed";
	public static final String STATUS_PARSED = "Parsed";
	public static final String STATUS_CODED = "Coded";
	
	private String name,location;
	private String documentStatus = STATUS_UNPROCESSED,documentType = TYPE_MEDICAL_REPORT;
	private Map<String,String> properties;
	private List<Section> sections;
	private List<Sentence> sentences;
	
	public Document(){}
	public Document(String text){
		setText(text);
	}
	
	public String getTitle() {
		return name;
	}
	public void setTitle(String name) {
		this.name = name;
	}
	public String getLocation() {
		return location;
	}
	public void setLocation(String location) {
		this.location = location;
	}
	public List<Section> getSections() {
		if(sections == null)
			sections = new ArrayList<Section>();
		return sections;
	}
	public void setSections(List<Section> sections) {
		this.sections = null;
		addSections(sections);
	}
	public void addSentence(Sentence s){
		getSentences().add(s);
		s.setDocument(this);
	}
	public void addSentences(Collection<Sentence> ss){
		getSentences().addAll(ss);
		for(Sentence s: ss)
			s.setDocument(this);
	}
	public void addSection(Section s){
		getSections().add(s);
		s.setDocument(this);
	}
	public void addSections(Collection<Section> ss){
		getSections().addAll(ss);
		for(Section s: ss)
			s.setDocument(this);
	}

	public List<Sentence> getSentences() {
		if(sentences == null)
			sentences = new ArrayList<Sentence>();
		return sentences;
	}
	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}
	public List<Mention> getMentions(){
		List<Mention> mentions = new ArrayList<Mention>();
		for(Sentence s: getSentences()){
			mentions.addAll(s.getMentions());
		}
		return mentions;
	}
	public Set<Concept> getConcepts(){
		Set<Concept> mentions = new LinkedHashSet<Concept>();
		for(Mention s: getMentions()){
			mentions.add(s.getConcept());
		}
		return mentions;
	}
	public List<Annotation> getAnnotations(){
		List<Annotation> annotations = new ArrayList<Annotation>();
		for(Mention s: getMentions()){
			annotations.addAll(s.getAnnotations());
			for(Annotation a: s.getModifierAnnotations()){
				annotations.add(a);
			}
		}
		Collections.sort(annotations);
		return annotations;
	}
	
	public String getDocumentStatus() {
		return documentStatus;
	}
	public void setDocumentStatus(String documentStatus) {
		this.documentStatus = documentStatus;
	}
	public String getDocumentType() {
		return documentType;
	}
	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}
	public Map<String,String> getProperties() {
		if(properties == null)
			properties = new LinkedHashMap<String, String>();
		return properties;
	}
	public void setProperties(Map<String,String> properties) {
		this.properties = properties;
	}
	
	/**
	 * get section that this spannable object belongs to
	 * @param sp
	 * @return null- if no such section exists
	 */
	public Section getSection(Spannable sp){
		for(Section s: getSections()){
			if(s.contains(sp))
				return s;
		}
		return null;
	}
}
