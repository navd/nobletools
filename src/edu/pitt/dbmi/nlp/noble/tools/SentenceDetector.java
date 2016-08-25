package edu.pitt.dbmi.nlp.noble.tools;

import java.util.*;

import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;


/**
 * This class handles sentence chunking of text. 
 * @author tseytlin
 *
 */

public class SentenceDetector {
	private static final List<String> exceptionList = new ArrayList<String>();
	// list of known exceptions where period is allowed 
	static {
		exceptionList.add(".*\\W(vs|Fig|al|etc)\\."); // vs. Fig. al.
		exceptionList.add(".*\\W[A-Z][a-z]?\\."); // A.B. Dr. Mr. etc... 
	}

	
	private static boolean isException(String s){
		// check if this period is some known abreviation
		for(String ex: exceptionList)
			if(s.matches(ex))
				return true;
		return false;
	}

	
	/**
	 * Parse English sentences from a blurb of text. 
	 * Each sentence should be terminated by .! or ?
	 * Periods in digits and some acronyms should be skipped
	 * @param txt
	 * @return
	 */
	public static List<String> getSentences(String txt) {
		List<String> sentences =new ArrayList<String>();
		char [] tc = txt.toCharArray();
		//int st = 0;
		StringBuffer s = new StringBuffer();
		for(int i=0;i<txt.length();i++){
			// skip control characters and replace them with space
			if(tc[i] == '\n' || tc[i] == '\r')
				s.append(" ");
			else
				s.append(tc[i]);
			
			if(tc[i] == '.' || tc[i] == '!' || tc[i] == '?'){
				// get candidate sentence
				//String s = txt.substring(st,i+1);
				
				// check if this period is a decimal point
				if(i+1 < tc.length && Character.isDigit(tc[i+1]))
					continue;
				
				// check if the next character is not a whitespace
				// check if this period is a decimal point
				if(i+1 < tc.length && !Character.isWhitespace(tc[i+1]))
					continue;
				
				// check if this period is some known abreviation
				if(isException(s.toString()))
					continue;
				
				
				// save sentence
				sentences.add(s.toString());	
				
				// move start 
				//st = i+1;
				s = new StringBuffer();
			}
		}
		// mop up in case you don't have a period at the end
		if(s.toString().trim().length() >0){
			sentences.add(s.toString()); //+"." bad idea, we don't want to mess with offsets
		}
		
		return sentences;
	}
	/**
	 * Parse English sentences from a blurb of text. 
	 * Each sentence should be terminated by .! or ?
	 * Periods in digits and some acronyms should be skipped
	 * @param txt
	 * @return
	 */
	public static List<Sentence> getSentences(String txt,int offset) {
		List<Sentence> sentences =new ArrayList<Sentence>();
		char [] tc = txt.toCharArray();
		//int st = 0;
		StringBuffer s = new StringBuffer();
		int s_offs = offset;
		for(int i=0;i<txt.length();i++){
			// skip control characters and replace them with space
			if(tc[i] == '\n' || tc[i] == '\r')
				s.append(" ");
			else
				s.append(tc[i]);
			
			if(tc[i] == '.' || tc[i] == '!' || tc[i] == '?'){
				// get candidate sentence
				//String s = txt.substring(st,i+1);
				
				// check if this period is a decimal point
				if(i+1 < tc.length && Character.isDigit(tc[i+1]))
					continue;
				
				// check if the next character is not a whitespace
				// check if this period is a decimal point (comma after period is OK, cause it may occur in some strucutred abstracts)
				if(i+1 < tc.length && !(Character.isWhitespace(tc[i+1]) || tc[i+1] == ','))
					continue;
				
				// check if this period is some known abreviation
				if(isException(s.toString()))
					continue;
				
				
				// save sentence
				sentences.add(new Sentence(s.toString(),s_offs,Sentence.TYPE_PROSE));	
				
				// move start 
				//st = i+1;
				s = new StringBuffer();
				s_offs = offset+i+1;
			}
		}
		// mop up in case you don't have a period at the end
		if(s.toString().trim().length() >0){
			sentences.add(new Sentence(s.toString(),s_offs,Sentence.TYPE_PROSE)); //+"." bad idea, we don't want to mess with offsets
		}
		
		return sentences;
	}

}
