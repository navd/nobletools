package edu.pitt.dbmi.nlp.noble.coder.model;

import java.util.LinkedHashMap;
import java.util.Map;

public class Text implements Spannable {
	private String text;
	private int offset;
	private Map<String,Long> processTime;
	
	public Text() {}
	public Text(String text) {
		this.text = text;
	}
	public Text(String text, int offset) {
		this.text = text;
		this.offset = offset;
	}

	public String getText() {
		return text;
	}
	public String getTrimmedText() {
		return text.trim();
	}
	public int getStartPosition() {
		return offset;
	}
	public int getEndPosition() {
		return offset+getLength();
	}
	public int getOffset() {
		return offset;
	}
	public void setOffset(int offset) {
		this.offset = offset;
	}
	public void updateOffset(int delta){
		this.offset += delta;
	}
	public void setText(String text) {
		this.text = text;
	}
	public int getLength(){
		return text.length();
	}
	public boolean contains(Spannable s) {
		return getStartPosition() <= s.getStartPosition() && s.getEndPosition() <= getEndPosition();
	}
	public boolean intersects(Spannable s) {
		//NOT this region ends before this starts or other region ends before this one starts
		return !(getEndPosition() < s.getStartPosition() || s.getEndPosition() < getStartPosition());
	}
	public boolean before(Spannable s) {
		return getEndPosition() <= s.getStartPosition();
	}

	public boolean after(Spannable s) {
		return s.getEndPosition() <= getStartPosition();
	}
	public String toString(){
		return getText();
	}
	
	public Map<String,Long> getProcessTime(){
		if(processTime == null){
			processTime = new LinkedHashMap<String, Long>();
		}
		return processTime;
	}

	
	/**
	 * get a distance between two spannable objects in a same search string
	 * in a number of words
	 * @param Spannable a
	 * @param Spannable b
	 * @return 0 - if one span contains or intersects the other, or distance in words, -1 something went wrong
	 */
	public static int getWordDistance(Spannable search, Spannable a, Spannable b){
		if(a.contains(b) || a.intersects(b))
			return 0;
		
		int s = a.getStartPosition() < b.getStartPosition()?a.getEndPosition():b.getEndPosition();
		int e = a.getStartPosition() < b.getStartPosition()?b.getStartPosition():a.getStartPosition();
		
		// make sure that spannable search contains those annotations
		if(s < search.getStartPosition() || e > search.getEndPosition()){
			return -1;
		}
		String sub = search.getText().substring(s-search.getStartPosition(),e-search.getStartPosition());
		return sub.replaceAll("\\w+","").length()-1;
	}
}
