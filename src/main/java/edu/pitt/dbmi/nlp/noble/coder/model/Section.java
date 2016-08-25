package edu.pitt.dbmi.nlp.noble.coder.model;

import java.util.*;

public class Section extends Text{
	private String body,title;
	private int bodyOffset;
	private List<Sentence> sentences;
	private Section parent;
	private List<Section> sections;
	private Document document;
	private Map<String,String> properties;
	
	public List<Sentence> getSentences() {
		if(sentences == null)
			sentences = new ArrayList<Sentence>();
		return sentences;
	}

	public Document getDocument() {
		return document;
	}

	public void setDocument(Document document) {
		this.document = document;
	}

	public void addSentence(Sentence s){
		getSentences().add(s);
		s.setSection(this);
	}

	public void addSentences(Collection<Sentence> ss){
		getSentences().addAll(ss);
		for(Sentence s: ss)
			s.setSection(this);
	}

	
	public void setSentences(List<Sentence> sentences) {
		this.sentences = sentences;
	}

	public String getBody() {
		return body;
	}

	public void setBody(String body) {
		this.body = body;
	}

	public String getTitle() {
		return title;
	}

	public Spannable getTitleSpan(){
		Text t = new Text();
		t.setText(title);
		t.setOffset(getTitleOffset());
		return t;
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public int getTitleOffset() {
		return getOffset();
	}

	public void setTitleOffset(int titleOffset) {
		setOffset(titleOffset);
	}

	public int getBodyOffset() {
		return bodyOffset;
	}
	public int getBodyLength() {
		return body.length();
	}
	public int getTitleLength(){
		return title.length();
	}
	public void setBodyOffset(int bodyOffset) {
		this.bodyOffset = bodyOffset;
	}
	public void updateOffset(int delta){
		super.updateOffset(delta);
		this.bodyOffset += delta;
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
	public void addSection(Section s){
		getSections().add(s);
		s.setDocument(getDocument());
		s.setParent(this);
	}
	public void addSections(Collection<Section> ss){
		getSections().addAll(ss);
		for(Section s: ss){
			s.setDocument(getDocument());
			s.setParent(this);
		}
	}

	public Section getParent() {
		return parent;
	}

	public void setParent(Section parent) {
		this.parent = parent;
	}

	public List<Mention> getMentions(){
		List<Mention> mentions = new ArrayList<Mention>();
		for(Sentence s: getSentences()){
			mentions.addAll(s.getMentions());
		}
		return mentions;
	}
	public Map<String,String> getProperties() {
		if(properties == null)
			properties = new LinkedHashMap<String, String>();
		return properties;
	}
}
