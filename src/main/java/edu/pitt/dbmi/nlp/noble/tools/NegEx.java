/**
 * This is a Java implementation of NegEx algorithm
 * http://web.cbmi.pitt.edu/chapman/NegEx.html
 */
package edu.pitt.dbmi.nlp.noble.tools;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Modifier;
import edu.pitt.dbmi.nlp.noble.coder.model.Processor;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;


/**
 * NegEx 1.0 implementation of 
 * TODO: Need to handle multiple Concepts matching the same thing
 * @author algorithm: Wendy Chapman implementation: tseytlin
 *
 */


public class NegEx implements Processor<Sentence>{
	//private static NegEx instance;
	private static List preNegationList,postNegationList,stopWords;
	private static List indeterminateList, pseudoNegationList;//,possibilityList;
	private long time;
	
	// constants
	private final int PRE    = 0;
	private final int POST   = 1;
	private final int MAYBE  = 2;
	private final int PSEUDO = 3;
	private final int STOP    = 4;
	private final int CONCEPT = 5;
	private final int PLAIN  = -1;
	private final int INDETERMINATE = 6;
	
	
	private List<Concept> negatedConcepts,indeterminateConcepts,negations,indeterminates;
	
	static {
		// load in all processing resources
		preNegationList = TextTools.loadResourceAsList("/resources/NegExPreNegationPhrases.lst");
		postNegationList = TextTools.loadResourceAsList("/resources/NegExPostNegationPhrases.lst");
		stopWords = TextTools.loadResourceAsList("/resources/NegExPseudoConjunctions.lst");
		indeterminateList = TextTools.loadResourceAsList("/resources/NegExIndeterminatePhrases.lst");
		pseudoNegationList = TextTools.loadResourceAsList("/resources/NegExPseudoNegationPhrases.lst");
		//possibilityList = loadResource("/resources/NegExPossibilityPhrase.lst");
	}
	

	
	
	/**
	 * create negex instance
	 */
	public NegEx(){
		negatedConcepts = new ArrayList<Concept>();
		indeterminateConcepts = new ArrayList<Concept>();
		negations = new ArrayList<Concept>();
		indeterminates = new ArrayList<Concept>();
	}
	
	/**
	 * get negated concepts (call after process operation
	 * @return
	 */
	public List<Concept> getNegatedConcepts(){
		return negatedConcepts;
	}
	
	/**
	 * get negated concepts (call after process operation
	 * @return
	 */
	public List<Concept> getIndeterminateConcepts(){
		return indeterminateConcepts;
	}
	
	/**
	 * get negated concepts (call after process operation
	 * @return
	 */
	public List<Concept> getNegations(){
		return negations;
	}
	
	/**
	 * get negated concepts (call after process operation
	 * @return
	 */
	public List<Concept> getIndeterminates(){
		return indeterminates;
	}
	
	/**
	 * Was NegEx triggered (negations were found)
	 * @return
	 */
	public boolean isTriggered(){
		return !negatedConcepts.isEmpty() || !indeterminates.isEmpty();
	}
	

	/**
	 * clear lists
	 */
	public void clear(){
		negatedConcepts.clear();
		indeterminateList.clear();
		negations.clear();
		indeterminates.clear();
	}

	
	/**
	 * scan list and see if it matches a string
	 */
	private String findMatch(String origtext,List list){
		String text = origtext.toLowerCase().trim();
		for(int i=0;i<list.size();i++){
			 String term = (String)list.get(i);
			 //System.out.println(text+" vs "+term);
			 if(text.length() > term.length()){
			 	 if(text.startsWith(term+" "))
					 return origtext.substring(0,term.length()); //term;
			 }else if(text.length() == term.length()){
				 if(text.startsWith(term))
					 return origtext.substring(0,term.length()); //term;
			 }
		}
		return null;
	}
	
	
	
	/**
	 * scan list and see if it matches a string
	 */
	private Object findConceptMatch(String origtext,List list){
		String text = origtext.toLowerCase();
		for(int i=0;i<list.size();i++){
			 Object term = list.get(i);
			 String ttext = (term instanceof Concept)?((Concept)term).getText():((Mention)term).getText();
			 
			 if(ttext == null)
				 continue;
			 String term_text = ttext.toLowerCase().replaceAll("\\W"," ");
			 if(text.length() > ttext.length()){
			 	 try{
					 if(text.matches(term_text+"\\b.*")){
				 		 return term;
					 }
			 	 }catch(Exception ex){
			 		 //If we have something funny in term_text that will be misinterpreted by regex
			 		if(text.startsWith(term_text)){
				 		 return term;
					}
			 	 }
			 }else if(text.length() == ttext.length()){
				 if(text.startsWith(term_text))
					 return term;
				 
			 }
		}
		return null;
	}
	
	
	private static class LexicalElement {
		private String text;
		private int start,end;
		public LexicalElement(String text,int st, int en){
			this.text = text;
			this.start = st;
			this.end = en;
		}
		public int getCharOffset() {
			return start;
		}
		public String getTrimmedString() {
			return text;
		}
		public int getEndCharacter() {
			return end;
		}
	}
	
	/**
	 * Given a sentence, build a list of Elements that has negations, elements and negatable
	 * concepts
	 */
	private List<SentenceElement> parse(String sentence, List conceptKeys){
		List<SentenceElement> tokens = new ArrayList<SentenceElement>();	 
		List lems = null;
		String text = sentence;
		int sOffset = 0;
		
		// get rid of punctuation
		text = text.replaceAll("\\W"," ");
		
		// if there are no lexical elements, this means sentence has not been parsed
		// lets do some manual labor then in somewhat stupid fashion
		if(lems == null){
			lems = new ArrayList<LexicalElement>();
			for(String word: text.split("\\s+")){
				int i = sOffset+text.indexOf(word);
				lems.add(new LexicalElement(word,i,i+word.length()));
			}
		}
		
		
		// iterate over lexical elements
		for(int i=0,offset=sOffset;i<lems.size();i++){
			LexicalElement lex = (LexicalElement) lems.get(i);
			String tmp = text.substring(lex.getCharOffset()-sOffset);//.toLowerCase();
			SentenceElement se = null;
			
			//System.out.println(lex+" "+lex.getCharOffset()+" vs "+offset+" -"+tmp+"-");
			// if lexElement "behind" offset, then skip it
			if(lex.getCharOffset() < offset)
				continue;			
			
			String t="";
			
			// check if it is indeterminate
			t = findMatch(tmp,indeterminateList);
			if(t != null){
				// create new element
				se = new SentenceElement(lex.getCharOffset(),t,INDETERMINATE);
				tokens.add(se);
				offset = offset+t.length(); // update offset
				continue;
			}			
			
			// check if it is pre-negation
			t = findMatch(tmp,pseudoNegationList);
			if(t != null){
				// create new element
				//if(se == null){
				se = new SentenceElement(lex.getCharOffset(),t,PSEUDO);
				tokens.add(se);
				offset = offset+t.length(); // update offset
				//}else{
				//	se.setPreNegation(true);
				//}
				continue;
			}
			
			// check if it is post-negation
			t = findMatch(tmp,postNegationList);
			if(t != null){
				// create new element
				se = new SentenceElement(lex.getCharOffset(),t,POST);
				tokens.add(se);
				offset = offset+t.length(); // update offset
				continue; //<-- some negations can be pre and post negations
			}
			
			
			// check if it is pre-negation
			t = findMatch(tmp,preNegationList);
			if(t != null){
				// create new element
				//if(se == null){
				se = new SentenceElement(lex.getCharOffset(),t,PRE);
				tokens.add(se);
				offset = offset+t.length(); // update offset
				//}else{
				//	se.setPreNegation(true);
				//}
				continue;
			}
		
			// check if it is stop word
			t = findMatch(tmp,stopWords);
			if(t != null){
				// create new element
				tokens.add(new SentenceElement(lex.getCharOffset(),t,STOP));
				offset = offset+t.length(); // update offset
				continue;
			}
							
			// check if it is a recognized concept
			Object key = findConceptMatch(tmp,conceptKeys);
			//System.err.println(tmp+" "+conceptKeys+" "+key);
			if(key != null){
				String ctext = null;
				Concept concept = null;
				Mention mention = null;
				
				if(key instanceof Concept){
					concept = (Concept) key;
					ctext = concept.getText();
				}else if(key instanceof  Mention){
					mention = (Mention)key;
					concept = mention.getConcept();
					ctext = mention.getText();
				}
				
				SentenceElement skey = new SentenceElement(lex.getCharOffset(),ctext,concept);
				skey.setMention(mention);
				// create new element
				tokens.add(skey);
				offset = offset+ctext.length(); // update offset
				continue;
			}
			
			// else it is just a token
			tokens.add(new SentenceElement(lex.getCharOffset(),lex.getTrimmedString()));
			offset = lex.getEndCharacter();
			
		}
		
		//System.out.println(tokens);
		return tokens;
	}
	 
	
	/**
	 * This class represents a negation phrase
	 */	
	private class SentenceElement implements Comparable {
		private String text;
		private int offset;
		private int type;
		private Concept key;
		private Mention mention;
		private boolean is_pseudo,is_pre,is_post,is_maybe,is_stop,is_concept,is_indeterminate;
		
		// constructors
		public SentenceElement(int o, String str){
			this(o,str,PLAIN,null);	
		}
		public SentenceElement(int o,String str, Concept e){
			this(o,str,CONCEPT,e);
		}
		public SentenceElement(int o,String str, int type){
			this(o,str,type,null);	
		}
		public SentenceElement(int o,String str, int type,Concept e){
			text = str;
			offset = o;
			
			// set type
			setType(type);
			
			key = e;
		}
		public Mention getMention() {
			return mention;
		}
		public void setMention(Mention mention) {
			this.mention = mention;
		}
	
		
		public void setType(int type){
			this.type = type;
			is_pseudo = (type == PSEUDO);
			is_pre    = (type == PRE);
			is_post   = (type == POST);
			is_maybe  = (type == MAYBE);
			is_stop   = (type == STOP);
			is_concept  = (type == CONCEPT);
			is_indeterminate = (type == INDETERMINATE);
		}
		
		public boolean isNegation(){
			return is_pre ||is_post|| is_maybe || is_indeterminate;	
		}
		
		public boolean isPseudoNegation(){
			return is_pseudo;	
		}
		public boolean isPreNegation(){
			return is_pre;	
		}
		public boolean isPostNegation(){
			return is_post;	
		}
		public boolean isPossible(){
			return is_maybe;	
		}
		public boolean isStopWord(){
			return is_stop;	
		}
		public boolean isIndeterminate(){
			return is_indeterminate;
		}		
		public boolean isConcept(){
			return is_concept;	
		}
		public String getText(){
			return text;	
		}
		public int getOffset(){
			return offset;	
		}
		public int getLength(){
			return text.length();	
		}
		public Concept getConcept(){
			return key;
		}
		
		public int compareTo(Object obj){
			// should be always true
			if(obj instanceof SentenceElement){
				SentenceElement np = (SentenceElement) obj;
				return offset - np.getOffset();
			}
			return 0;
		}
		public String toString(){
			return text+"("+type+")";	
		}
		/**
		 * @param is_post the is_post to set
		 */
		public void setPostNegation(boolean is_post) {
			this.is_post = is_post;
		}
		/**
		 * @param is_pre the is_pre to set
		 */
		public void setPreNegation(boolean is_pre) {
			this.is_pre = is_pre;
		}
	}

	/**
	 * process sentence for negations and uncertain mentions
	 */
	public Sentence process(Sentence sent) throws TerminologyException {
		time = System.currentTimeMillis();
		if(sent == null)
			return sent;
		
		String sentence = sent.getText();
		
		// return on empty sentence
		if(sentence == null || sentence.length() == 0)
			return sent;
		
		// parse sentence into tokens
		List<SentenceElement> tokens = parse(sentence,sent.getMentions());
		
		//
		SentenceElement negationConcept = null;
		int windowSize = 6;
		List<SentenceElement> negatableConcepts = new ArrayList<SentenceElement>();
		
		// iterate over sentence elements
		for(int i=0,window = 0;i<tokens.size();i++){
			SentenceElement token = tokens.get(i);
			// if this is negation token, then do the holkey-poky
			if(token.isNegation()){
				//System.out.println("Negation: "+token);
				negationConcept = null;
				window = 0;;
				// if pre negation then mark all concepts as negative
				// until other negation or stop word
				if(token.isPreNegation()){
					negationConcept = token;
				}
				// if post negation then mark all concepts as negative
				// until other negation or stop word (retroactively)
				else if(token.isPostNegation() || token.isIndeterminate()){
					for(int j=0;j<negatableConcepts.size();j++){
						SentenceElement cns = negatableConcepts.get(j);
						if(cns.isConcept()){
							Mention lex = cns.getMention();
							if(token.isPostNegation()){
								Annotation a = new Annotation();
								a.setOffset(token.getOffset()+sent.getOffset());
								a.setText(token.getText());
								
								lex.addModifier(getNegativeMention(a));
							}else{
								Annotation a = new Annotation();
								a.setOffset(token.getOffset()+sent.getOffset());
								a.setText(token.getText());
								
								lex.addModifier(getHedgedMention(a));
							}
						}
					}
				}
				
				// clear retro concepts
				negatableConcepts.clear();
				
			// if token is stop word then stop the negation process
			}else if (token.isStopWord()){
				negationConcept = null;
				negatableConcepts.clear();
				window = 0;
			// do the actual negation here
			}else if (token.isConcept()){
				if(negationConcept != null){
					Mention lex = token.getMention();
					Annotation a = new Annotation();
					a.setOffset(negationConcept.getOffset()+sent.getOffset());
					a.setText(negationConcept.getText());
					
					lex.addModifier(getNegativeMention(a));
		
				}else{
					negatableConcepts.add(token);
				}
				window ++;
			// else it is just a token	
			}else {
				if(negationConcept == null)
					negatableConcepts.add(token);
				window ++;
			}
			
			// stop if window size is excedded
			if(window >= windowSize){
				// if we had pre_negation then we need to stop completly
				if(negationConcept != null){
					window = 0;
					negationConcept = null;
					negatableConcepts.clear();
				// else we have post_negation and we need
				}else{
					window --;
					if(negatableConcepts.size() > 0)
						negatableConcepts.remove(0);
				}
			}
			
		}
		time = System.currentTimeMillis() - time;
		return sent;
	}
	
	private Modifier getNegativeMention(Annotation a){
		Concept c = new Concept(ConText.MODIFIER_VALUE_NEGATIVE);
		c.addSemanticType(SemanticType.getSemanticType(ConText.MODIFIER_TYPE_POLARITY));
		c.addProperty(ConText.MODIFIER_TYPE_POLARITY,ConText.MODIFIER_VALUE_NEGATIVE);
		
		Mention m = new Mention();
		m.setConcept(c);
		m.setAnnotations(Collections.singletonList(a));
		
		return Modifier.getModifier(ConText.MODIFIER_TYPE_POLARITY,ConText.MODIFIER_VALUE_NEGATIVE,m);
	}
	
	private Modifier getHedgedMention(Annotation a){
		Concept c = new Concept(ConText.MODIFIER_VALUE_HEDGED);
		c.addSemanticType(SemanticType.getSemanticType(ConText.MODIFIER_TYPE_MODALITY));
		c.addProperty(ConText.MODIFIER_TYPE_MODALITY,ConText.MODIFIER_VALUE_HEDGED);
		
		Mention m = new Mention();
		m.setConcept(c);
		m.setAnnotations(Collections.singletonList(a));
		
		return Modifier.getModifier(ConText.MODIFIER_TYPE_MODALITY,ConText.MODIFIER_VALUE_HEDGED,m);
	}
	
	
	public long getProcessTime() {
		return time;
	}
}
