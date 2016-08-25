
package edu.pitt.dbmi.nlp.noble.coder.processor;

import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.coder.model.*;
import edu.pitt.dbmi.nlp.noble.tools.SentenceDetector;
import edu.pitt.dbmi.nlp.noble.tools.SynopticReportDetector;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

public class DocumentProcessor implements Processor<Document> {
	private static final String AB = "AB"; //medline abstract tag
	private static final String PROSE_PATTERN = ".*\\b[a-z]+\\.\\s+[A-Z][a-z]+\\b.*";
	private String documentType;
	private Map<String,Pattern> sectioningMap;
	private long time;
	
	/**
	 * initialize document processor with default (medical report) setting
	 */
	public DocumentProcessor(){
		this(Document.TYPE_MEDICAL_REPORT);
	}
	
	/**
	 * initialize document processor with document type
	 */
	public DocumentProcessor(String type){
		setDocumentType(type);
		sectioningMap = new HashMap<String,Pattern>();
		sectioningMap.put(Document.TYPE_MEDICAL_REPORT,Pattern.compile("^([A-Z/\\- ]{5,40}:)\\s+(.*)",Pattern.DOTALL|Pattern.MULTILINE));
		sectioningMap.put(Document.TYPE_MEDLINE_RECORD,Pattern.compile("^([A-Z]{2})\\s+\\-\\s+(.*)",Pattern.DOTALL|Pattern.MULTILINE));
		sectioningMap.put(Document.TYPE_MEDLINE_RECORD+"-"+AB,Pattern.compile("(?:,\\s*)?([A-Z ]+\\:)\\s+(.*)"));
		
	}
	
	
	/**
	 * get document type that this processor is configured to handle
	 * @return
	 */
	public String getDocumentType() {
		return documentType;
	}

	/**
	 * set document type 
	 * @param documentType that this processor is configured to handle
	 */

	public void setDocumentType(String documentType) {
		this.documentType = documentType;
	}


	/**
	 * suggest document type based on text
	 * @param text
	 * @return
	 */
	public static String suggestDocumentType(String text){
		if(text.matches("(?s)^[A-Z]{2}  - .*"))
			return Document.TYPE_MEDLINE_RECORD;
		return Document.TYPE_MEDICAL_REPORT;
	}


	/**
	 * process document
	 * @param dir
	 */
	public void processFile(File f) throws Exception {
		if(f.isDirectory()){
			for(File c: f.listFiles()){
				processFile(c);
			}
		}else if(f.getName().endsWith(".txt")){
			System.out.println(f.getName());
			BufferedWriter bf = new BufferedWriter(new FileWriter(new File(f.getAbsolutePath()+".sectioned")));
			Document d = process(f);
			for(Section s: d.getSections()){
				bf.write("--------------\n["+s.getTitle()+"]\n--------------\n"+s.getBody()+"--------------\n");
			}
			bf.close();
		
		}
	}
	/**
	 * parse document into Sections and Sentences
	 * @param  file where document is located
	 * @return processed document 
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 */
	public Document process(File file) throws FileNotFoundException, IOException {
		Document doc = process(new FileInputStream(file));
		doc.setTitle(file.getName());
		doc.setLocation(file.getAbsolutePath());
		doc.setDocumentType(getDocumentType());
		return process(doc);
	}
	
	/**
	 * parse document into Sections and Sentences
	 * @param  file where document is located
	 * @return processed document 
	 * @throws IOException 
	 */
	public Document process(InputStream is) throws IOException {
		return process(TextTools.getText(is));
	}
	
	/**
	 * parse document into Sections and Sentences
	 * @param  document text
	 * @return processed document 
	 */
	public Document process(String text) {
		Document doc = new Document();
		doc.setText(text);
		doc.setDocumentType(getDocumentType());
		return process(doc);
	}
	
	/**
	 * process MEDLINE record
	 * @param doc
	 */
	private void processMedline(Document doc){
		String text = doc.getText();
		
		// lets do sectioning first
		Pattern pt = (doc.getDocumentType() != null)?sectioningMap.get(doc.getDocumentType()):null;
		if(pt != null){
			doc.setSections(section(text,0, pt, pt.matcher(text), new ArrayList<Section>()));
		}
		// each section has either english sentences or a single
		// data line Ex; MeSH terms
		List<Section> subsections = new ArrayList<Section>(); 
		for(Section section: doc.getSections()){
			// now if this is a MEDLINE record AND check if it has a structured abbstract
			// THIS creates more problem it seems
			/*if(AB.equals(section.getTitle())){
				pt = sectioningMap.get(doc.getDocumentType()+"-"+AB);
				List<Section> sec = section(section.getBody(),0,pt,pt.matcher(section.getBody()),new ArrayList<Section>());
				subsections.addAll(sec);
				// now if no structured abstract, then do whatever
				if(sec.isEmpty()){
					pasrseSentences(doc,section.getBody(), section.getBodyOffset(), Sentence.TYPE_PROSE);
				}else{
					for(Section sc: sec){
						sc.updateOffset(section.getBodyOffset());
					}
					
					// else we have an extra abstract
					for(Section sc: sec){
						pasrseSentences(doc,sc.getBody(),sc.getBodyOffset(),Sentence.TYPE_PROSE);
					}
				}
			}else{*/
				if(section.getBody().trim().matches(PROSE_PATTERN)){
					parseSentences(doc,section.getBody(), section.getBodyOffset(), Sentence.TYPE_PROSE);
				}else{
					int offs = section.getBodyOffset();
					for(String s: section.getBody().split("\n")){
						parseSentences(doc,s, offs, Sentence.TYPE_LINE);
						offs += s.length()+1;
					}
				}
			//}
		}
		doc.addSections(subsections);
	}
	
	
	/**
	 * process Medical Report
	 * @param doc
	 */
	private void processReport(Document doc){
		String text = doc.getText();
		
		// lets do sectioning first
		Pattern pt = (doc.getDocumentType() != null)?sectioningMap.get(doc.getDocumentType()):null;
		if(pt != null){
			doc.setSections(section(text,0, pt, pt.matcher(text), new ArrayList<Section>()));
		}
		
		int offset = 0, strOffset = 0;
		StringBuffer str = new StringBuffer();
		String last = null;
		for(String s: doc.getText().split("\n")){
			// check if this sentence does not need to be merged
			// with the previous one, lets save it
			if(!mergeLines(last,s)){
				// save previous region
				if(str.toString().trim().length() > 0){
					// if multiline buffer, then do prose parsing
					if(str.toString().trim().contains("\n") || str.toString().trim().matches(PROSE_PATTERN)){
						parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_PROSE);
					}else{
						parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_LINE);
					}
					// start the counter again
					str = new StringBuffer();
					strOffset = offset;
				}
			}
			// add this line to the next buffer
			str.append(s+"\n");
			offset += s.length()+1;
			last  = s;
		}
		// take care of the last sentence
		if(str.length() > 0){
			if(str.toString().trim().contains("\n") || Pattern.compile("[a-z]\\.\\s*[A-Z]").matcher(str.toString()).find()){
				parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_PROSE);
			}else{
				parseSentences(doc, str.toString(), strOffset, Sentence.TYPE_LINE);
			}
		}
	}
	
	
	/**
	 * parse document into Sections and Sentences
	 * @param  unprocessed document
	 * @return processed document 
	 */
	public Document process(Document doc) {
		time = System.currentTimeMillis();
		
		// now lets parse sentences for different report types
		if(Document.TYPE_MEDLINE_RECORD.equals(doc.getDocumentType())){
			processMedline(doc);
		}else{
			processReport(doc);
		}
	
		doc.setDocumentStatus(Document.STATUS_PARSED);
		time = System.currentTimeMillis() - time;
		doc.getProcessTime().put(getClass().getSimpleName(),time);
		return doc;
	}
	
	/**
	 * parse sentences for a region of text based on type
	 * @param doc
	 * @param text
	 * @param offset
	 * @param type
	 */
	private void parseSentences(Document doc, String text, int offset, String type){
		// if sentence starts with lots of 
		Pattern p = Pattern.compile("^(\\s+)\\w.*",Pattern.DOTALL|Pattern.MULTILINE);
		Matcher m = p.matcher(text);
		if(m.matches()){
			String prefix = m.group(1);
			text = text.substring(prefix.length());
			offset = offset + prefix.length();
		}
		
		// start adding sentences
		List<Sentence> sentences = new ArrayList<Sentence>();
		if(Sentence.TYPE_PROSE.equals(type)){
			sentences = SentenceDetector.getSentences(text,offset);
		}else{
			Sentence s = new Sentence(text,offset,Sentence.TYPE_LINE);
			parseProperties(doc,text);
			if(SynopticReportDetector.detect(text)){
				s.setSentenceType(Sentence.TYPE_WORKSHEET);
			}
			sentences.add(s);
		}
			
		// add to section
		if(!sentences.isEmpty()){
			Section sec = doc.getSection(sentences.get(0));
			if(sec != null){
				Sentence s = sentences.get(0);
				if(s.contains(sec.getTitleSpan())){
					//OK, this sentence contains header, do we need to 
					// parse it fruther?
					int en = sec.getTitleSpan().getEndPosition()-offset;
					String first = s.getText().substring(0,en);
					String rest = s.getText().substring(en);
					if(rest.trim().length() > 0){
						// there is more after header, need to break it in two
						sentences.remove(s);
						sentences.add(0,new Sentence(rest,offset+en,s.getSentenceType()));
						sentences.add(0,new Sentence(first,offset,Sentence.TYPE_HEADER));
						
					}else{
						// just set this sentence as header
						s.setSentenceType(Sentence.TYPE_HEADER);
					}
				}
				sec.addSentences(sentences);
			}
		}
		
		// add sentences to document
		doc.addSentences(sentences);
	}
	
	/**
	 * parse properties in a document
	 * @param doc
	 * @param text
	 */
	private void parseProperties(Document doc, String text){
		Pattern p = Pattern.compile("([A-Z][A-Za-z /]{3,25})(?:\\.{2,}|\\:)(.{2,25})");
		Matcher m = p.matcher(text);
		while(m.find()){
			doc.getProperties().put(m.group(1).trim(),m.group(2).trim());
		}
	}
	
	/**
	 * get the process time for the 
	 * @return
	 */
	public long getProcessTime(){
		return time;
	}
	
	private boolean mergeLines(String last, String s) {
		if(last == null)
			return false;
		// if previous item is worksheet ..
		if(SynopticReportDetector.detect(last))
			return false;
		
		// if previous sentence ends with a lower case word or digit or comma
		if(last.matches(".+\\s([A-Z]?[a-z]+|\\d+),?") && s.matches("([A-Z]?[a-z]+)\\b.+")){
			return true;
		}
		return false;
	}

	private List<Section> section(String doc,int offs,Pattern pt, Matcher mt,List<Section> list){
		while(mt.find()){
			int st = offs+mt.start();
			int en = offs+mt.end();
			int bst = offs+mt.start(2);
			String text = mt.group();
			String name = mt.group(1);
			String body = mt.group(2);
			
			// because we have a greedy pattern, look in body for sub-patterns
			Matcher m = pt.matcher(body);
			if(m.find()){
				en = bst+m.start();
				text = doc.substring(st,en);
				body = doc.substring(bst,en);
			}
			
			// create section object with text
			Section s = new Section();
			s.setText(text);
			s.setTitle(name);
			s.setTitleOffset(st);
			s.setBody(body);
			s.setBodyOffset(bst);
			list.add(s);
			
			// reset matcher
			m.reset();
			
			// recurse into body
			section(doc,bst,pt,m,list);
		}
		return list;
	}
	
	
	
	

}
