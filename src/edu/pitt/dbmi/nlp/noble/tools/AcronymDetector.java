package edu.pitt.dbmi.nlp.noble.tools;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Processor;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;

public class AcronymDetector implements Processor<Sentence>{
	private long time;
	private Map<String,String> acronymList;
	

	/**
	 * get a map of acronyms that were identified during parsing
	 * @return
	 */
	public Map<String,String> getAcronyms(){
		if(acronymList == null){
			acronymList = new HashMap<String, String>();
		}
		return acronymList;
	}
	
	/**
	 * clear a list of acronyms that was gathered so far.
	 */
	public void clearAcronyms(){
		acronymList = null;
	}
	
	
	/**
	 * is matched concept matches a string
	 * @param c
	 * @param expanded
	 * @return
	 */
	private boolean matches(Concept c, String expanded) {
		List<String> a = new ArrayList<String>(); 
		for(String s: expanded.trim().split("[^A-Za-z]+")){
			if(!TextTools.isStopWord(s))
				a.add(s);
		}
		List<String> b = new ArrayList<String>();
		for(Annotation an : c.getAnnotations()){
			b.add(an.getText());
		}
		return b.containsAll(a);
	}

	
	/**
	 * get expanded form acronym from 
	 * @param a proposed expanded form of the acronym
	 * @param a proposed acronym for the expanded form
	 * @return expanded form or null, if false matched
	 */
	private String getAcronymExapndedForm(String expanded, String acronym) {
		List<String> words = Arrays.asList(expanded.trim().split("[^A-Za-z]+"));
		
		//if(acronym.endsWith("s"))
		//	acronym = acronym.substring(0,acronym.length()-1);
		// remove non capital letters from acronym
		acronym = acronym.replaceAll("[^A-Z]", "");
		int k = 0, s =0;;
		for(int i=acronym.length()-1;i>=0;i--){
			String c = ""+acronym.charAt(i);
			int j = (words.size()-acronym.length())+i-s;
			
			// if less words then in acronym, die
			if(j >= words.size() || j < 0)
				return null;
			
			// skip stop words or empty word
			if(words.get(j).length() == 0 || TextTools.isStopWord(words.get(j))){
				j--;
				s++;
			}
			// if run out of words, quit
			if(j < 0)
				return null;
			
			// if first char of the word doesn't equal to letter of acronym, die
			if(words.get(j).length() > 0 && !c.equalsIgnoreCase(""+words.get(j).charAt(0))){
				// we might have an acronym that has several letters from each word
				if(words.get(j).toLowerCase().contains(c.toLowerCase())){
					s--;
				}else{
					return null;
				}
			}
			// increment offset
			k = expanded.lastIndexOf(words.get(j));
		}
		return expanded.substring(k);
	}

	
	/**
	 * process sentence
	 */
	public Sentence process(Sentence sentence) throws TerminologyException {
		time = System.currentTimeMillis();
		// handle acronyms that are mentioned in a document
		List<Mention> mentions = sentence.getMentions();
		Terminology terminology = !mentions.isEmpty()?mentions.get(0).getConcept().getTerminology():null;
		
		// check for acronyms in expanded form
		String phrase = sentence.getText();
		Matcher m = Pattern.compile("(([A-Z]?[a-z-0-9]+ )+)\\(([A-Z-0-9]+s?)\\)").matcher(TextTools.stripDiacritics(phrase));
		if(m.find()){
			String expanded = m.group(1);
			String acronym  = m.group(m.groupCount());
			expanded = getAcronymExapndedForm(expanded,acronym);
			// don't match to single words acronyms and don't match digits
			if(expanded != null && acronym.length() > 1 && !acronym.matches("\\d+")){
				Mention exp = null;
				List<Mention> acr = new ArrayList<Mention>();
				// find annotations assigned to expanded part of the acronym
				for(Mention mn: mentions){
					if(matches(mn.getConcept(),expanded))
						exp = mn;
					else if(matches(mn.getConcept(),acronym))
						acr.add(mn);
				}
				// if expanded form was matched as a single concept
				if(exp != null){
					// fix annotations
					Annotation an = new Annotation();
					an.setText(acronym);
					an.setOffset(sentence.getOffset()+m.start(m.groupCount()));
					exp.getConcept().addMatchedTerm(acronym);
					exp.getConcept().addAnnotation(an);
					exp.getAnnotations().add(an);
					
					// save acronym with expanded form code
					getAcronyms().put(acronym,exp.getConcept().getCode());
					// if there was a different acronym selected, then remove them
					for(Mention a: acr){
						if(!a.getConcept().getCode().equals(exp.getConcept().getCode()))
							mentions.remove(a);
					}
				}
			}
		}else{
			// check if acronyms exist
			for(String acronym: getAcronyms().keySet()){
				m = Pattern.compile("\\b"+acronym+"\\b").matcher(phrase);
				while(m.find()){
					String code = getAcronyms().get(acronym);
					// remove an already matched one
					for(Mention c: new ArrayList<Mention>(mentions)){
						// if we have a mention that covers this
						if(acronym.equals(c.getText())){
							if(!c.getConcept().getCode().equals(code))
								mentions.remove(c);
							else
								code = null;
						}
					}
					
					// add new concept for this acronym
					if(code != null){
						Concept c = new Concept(code,acronym);
						c.setTerminology(terminology);
						c.setSearchString(phrase);
						c.setMatchedTerm(acronym);
						c.initialize();
						Annotation a = new Annotation();
						a.setText(acronym);
						a.setOffset(sentence.getOffset()+m.start());
						a.setSearchString(phrase);
						mentions.addAll(Mention.getMentions(c,Arrays.asList(a)));
					}
				}
			}
		}
		time = System.currentTimeMillis() -time;
		return sentence;
	}

	public long getProcessTime() {
		return time;
	}
	
	
	public static void main(String [] args) throws IOException, TerminologyException{
		String text = "There was a case when World Health Organization (WHO) was operating here. WHO was there.";
		NobleCoderTerminology t = new NobleCoderTerminology("NCI_Thesaurus");
		t.setSelectBestCandidate(true);
		AcronymDetector ad = new AcronymDetector();
		for(String tx : text.split("\\.")){
			Sentence s = ad.process(t.process(new Sentence(tx)));
			System.out.println(tx);
			for(Mention m: s.getMentions()){
				System.out.println(m+" | "+m.getConcept()+" | "+m.getAnnotations());
			}
		}
		
	}
}
