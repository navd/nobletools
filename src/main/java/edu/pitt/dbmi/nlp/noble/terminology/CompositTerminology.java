package edu.pitt.dbmi.nlp.noble.terminology;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;



/**
 * this terminology combines several terminologies to create one contigues access point
 * @author tseytlin
 *
 */
public class CompositTerminology extends AbstractTerminology {
	private List<Terminology> terminologies;
	private Concept [] roots;
	private long time;
	
	/**
	 * add terminology to a stack
	 * @param t
	 */
	public void addTerminology(Terminology t){
		getTerminologies().add(t);
		roots = null;
	}
	
	/**
	 * add terminology to a stack
	 * @param t
	 */
	public void removeTerminology(Terminology t){
		getTerminologies().remove(t);
		roots = null;
	}
	
	/**
	 * get all terminologies
	 */
	public List<Terminology> getTerminologies(){
		if(terminologies == null)
			terminologies = new ArrayList<Terminology>();
		return terminologies;
			
	}
	
	public String getName() {
		return getClass().getSimpleName();
	}

	public String getDescription() {
		return "Access multiple terminologies through a single interface";
	}

	public String getVersion() {
		return "1.0";
	}

	public URI getURI() {
		return null;
	}

	public String getFormat() {
		return "composit";
	}

	public String getLocation() {
		return "memory";
	}

	public Source[] getSources() {
		Set<Source> src = new LinkedHashSet<Source>();
		for(Terminology t: getTerminologies()){
			Collections.addAll(src,t.getSources());
		}
		return src.toArray(new Source [0]);
	}
	/**
	 * Get all supported relations between concepts
	 */
	public Relation[] getRelations() throws TerminologyException {
		Set<Relation> rel = new LinkedHashSet<Relation>();
		for(Terminology t: getTerminologies()){
			Collections.addAll(rel,t.getRelations());
		}
		return rel.toArray(new Relation [0]);
	}
	
	
	public Source[] getSourceFilter() {
		List<Source> src = new ArrayList<Source>();
		for(Terminology t: getTerminologies()){
			Collections.addAll(src,t.getSourceFilter());
		}
		return src.toArray(new Source [0]);
	}

	public void setSourceFilter(Source[] srcs) {
		for(Terminology t: getTerminologies()){
			t.setSourceFilter(srcs);
		}
	}
	
	/**
	 * search multiple terminologies
	 */
	public Concept[] search(String text) throws TerminologyException {
		return search(text,getSearchMethods()[0]);
	}

	/**
	 * lookup from multiple terminologies
	 */
	public Concept lookupConcept(String cui) throws TerminologyException {
		for(Terminology t: getTerminologies()){
			Concept c = t.lookupConcept(cui);
			if(c != null)
				return c;
		}
		return null;
	}

	protected Concept convertConcept(Object obj) {
		//NOOP
		return null;
	}

	public Concept[] getRelatedConcepts(Concept c, Relation r) throws TerminologyException {
		// get related concepts from source terminolgies
		return c.getRelatedConcepts(r);
	}

	public Map getRelatedConcepts(Concept c) throws TerminologyException {
		// get related concepts from source terminolgies
		return c.getRelatedConcepts();
	}

	public Concept[] search(String text, String method) throws TerminologyException {
		List<Concept> result = new ArrayList<Concept>();
		for(Terminology t: getTerminologies()){
			Collections.addAll(result,t.search(text,method));
		}
		return result.toArray(new Concept [0]);
	}

	/**
	 * process sentence
	 */
	public Sentence process(Sentence s) throws TerminologyException {
		time = System.currentTimeMillis();
		List<Mention> mentions = new ArrayList<Mention>();
		for(Terminology t: getTerminologies()){
			mentions.addAll(t.process(s).getMentions());
		}
		s.setMentions(mentions);
		time = System.currentTimeMillis() - time;
		return s;
	}
	
	public long getProcessTime() {
		return time;
	}
	
	public String[] getSearchMethods() {
		Set<String> result = new LinkedHashSet<String>();
		for(Terminology t: getTerminologies()){
			Collections.addAll(result,t.getSearchMethods());
		}
		return result.toArray(new String [0]);
	}

	public Concept[] getRootConcepts() throws TerminologyException {
		if(roots == null){
			List<Concept> result = new ArrayList<Concept>();
			for(Terminology t: getTerminologies()){
				Collections.addAll(result,t.getRootConcepts());
			}
			roots = result.toArray(new Concept [0]);
		}
		return roots;
		
	}

	public Collection<Concept> getConcepts() throws TerminologyException {
		Collection<Concept> list = new ArrayList<Concept>();
		for(Terminology t: getTerminologies())
			list.addAll(t.getConcepts());
		return list;
	}

	
	public SemanticType[] getSemanticTypeFilter() {
		List<SemanticType> src = new ArrayList<SemanticType>();
		for(Terminology t: getTerminologies()){
			Collections.addAll(src,t.getSemanticTypeFilter());
		}
		return src.toArray(new SemanticType [0]);
	}

	public void setSemanticTypeFilter(SemanticType[] srcs) {
		for(Terminology t: getTerminologies()){
			t.setSemanticTypeFilter(srcs);
		}
	}
	
}
