package edu.pitt.dbmi.nlp.noble.coder.processor;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.coder.model.*;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;

public class PartProcessor implements Processor<Section> {
	public static final String PART_PATTERN = "PARTS?\\s+\\d+(\\s+AND\\s+\\d+)?:"; 
	private long time;
	
	/**
	 * identify parts in a section and attach them to this section
	 */
	public Section process(String text) {
		Section section = new Section();
		section.setText(text);
		section.setBody(text);
		return process(section);
	}
	/**
	 * identify parts in a section and attach them to this section
	 */
	public Section process(Section section)  {
		time = System.currentTimeMillis();
		Pattern pt = Pattern.compile(PART_PATTERN,Pattern.MULTILINE|Pattern.DOTALL);
		Matcher mt = pt.matcher(section.getBody());
		List<Section> parts = new ArrayList<Section>();
		String text = section.getBody();
		Section part = null;
		while(mt.find()){
			// finish previous part
			if(part != null){
				part.setText(text.substring(part.getOffset(),mt.start()));
				part.setBody(text.substring(part.getBodyOffset(),mt.start()));
			}
			// init new text section
			part = new Section();
			part.setTitle(mt.group());
			part.setTitleOffset(mt.start());
			part.setBodyOffset(mt.end());
			parts.add(part);
		}
		// finish the last part
		if(part != null){
			part.setText(text.substring(part.getOffset()));
			part.setBody(text.substring(part.getBodyOffset()));
		}
		// reset offsets
		for(Section p: parts){
			p.updateOffset(section.getOffset());
			for(Sentence s: section.getSentences()){
				if(p.contains(s))
					p.addSentence(s);
			}
		}
		section.setSections(parts);
		time = System.currentTimeMillis() - time;
		return section;
	}

	public long getProcessTime() {
		return time;
	}

	public static void main(String [] args) throws TerminologyException{
		String text = "";
		DocumentProcessor dp = new DocumentProcessor();
		PartProcessor pp = new PartProcessor();
		List<Section> sections = dp.process(text).getSections();
		if(sections.isEmpty()){
			System.out.println("no sections found");
			return;
		}
			
		Section sec = pp.process(sections.get(0));
		System.out.println("["+sec.getTitle()+"]");
		System.out.println(sec.getBody());
		System.out.println("--");
		for(Section s: sec.getSections()){
			System.out.println("|"+s.getTitle()+"|");
			System.out.println(s.getBody());
			System.out.println("--");
			System.out.println(s.getSentences().size());
		}
	}
	
}
