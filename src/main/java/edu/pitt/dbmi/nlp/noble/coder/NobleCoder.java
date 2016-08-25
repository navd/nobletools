package edu.pitt.dbmi.nlp.noble.coder;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintStream;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import edu.pitt.dbmi.nlp.noble.coder.model.Document;
import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Processor;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.coder.processor.DocumentProcessor;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyError;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.AcronymDetector;
import edu.pitt.dbmi.nlp.noble.tools.ConText;
import edu.pitt.dbmi.nlp.noble.tools.NegEx;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.DeIDUtils;

/**
 * NobleCoder
 * TODO: implement abbreviation whitelist/blacklist issues
 */
public class NobleCoder implements Processor<Document>{
	public static int FILTER_DEID = 1;
	public static int FILTER_HEADER = 2;
	public static int FILTER_WORKSHEET = 4;
	
	//private final String ABBREV_TERMINOLOGY = "BiomedicalAbbreviations";
	//private DefaultRepository repository;
	private Terminology terminology; //,abbreviations;
	private Processor<Document> documentProcessor;
	private AcronymDetector acronymDetector;
	private ConText conText;
	private boolean handleAcronyms = true,handleNegation = true; //skipAbbrreviationLogic
	private int processFilter = FILTER_DEID|FILTER_HEADER;
	//private Map<String,String> abbreviationWhitelist;
	private long time;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		String domain = "Melanoma";
		
		NobleCoder nc = new NobleCoder("NCI_Thesaurus");
		nc.setProcessFilter(nc.getProcessFilter()|FILTER_WORKSHEET);
		((NobleCoderTerminology)nc.getTerminology()).setSelectBestCandidate(true);
		
		for(File file : new File("/home/tseytlin/Data/DeepPhe/"+domain+"/sample/deid/").listFiles()){
			if(file.getName().endsWith(".txt")){
				System.out.print("processing\t"+file.getName()+"\t..\t");
				Document doc = nc.process(file);
				PrintStream out = new PrintStream(new File(file.getParentFile(),file.getName()+".processed"));
				out.println(doc.getTitle());
				out.println("---------------------------------------");
				for(Object prop : doc.getProperties().keySet()){
					out.println(prop+"\t->\t"+doc.getProperties().get(prop));
				}
				out.println("---------------------------------------");
				for(Sentence s: doc.getSentences()){
					String sec = s.getSection() != null?s.getSection().getTitle():"none";
					String tm = s.getProperties().containsKey("time")?s.getProperties().get("time"):"";
					out.println("sentence:\t|"+s.getOffset()+"|\t"+s.getSentenceType()+"|\t"+sec+"|\t"+s+"\t|"+tm);
					//System.out.println("extracted:\t"+doc.getText().substring(s.getStartPosition(),s.getEndPosition()));
					for(Mention m: s.getMentions()){
						out.println("\tmention:\t"+m+" | "+m.getConcept().getCode()+" | "+m.getConcept().getName()+" | "+m.getAnnotations());
					}
				}
				out.println("---------------------------------------");
				out.println(nc.getProcessTime()+" ms");
				out.close();
				System.out.println(nc.getProcessTime()+" ms");
			}
		}
		
	}

	/**
	 * invoke NobleCoderTool pointing to a terminology .term direcotry
	 * all of the relevant settings should be set in .term/search.properties
	 * @param location
	 */
	public NobleCoder(File location) throws IOException {
		NobleCoderTerminology.setPersistenceDirectory(location.getParentFile());
		setTerminology(new NobleCoderTerminology(location.getName()));
	}
	
	/**
	 * invoke NobleCoderTool pointing to a terminology .term direcotry
	 * all of the relevant settings should be set in .term/search.properties
	 * @param location
	 */
	public NobleCoder(String name) throws IOException {
		setTerminology(new NobleCoderTerminology(name));
	}
	
	
	/**
	 * invoke NobleCoderTool pointing to a terminology .term direcotry
	 * all of the relevant settings should be set in .term/search.properties
	 * @param location
	 */
	public NobleCoder(Terminology term) {
		setTerminology(term);
	}
	
	/**
	 * empty NobleCoder instance, need to use setTerminology() to specify
	 * terminology to use
	 */
	public NobleCoder(){}
	
	
	/**
	 * get document processor for parsing documents
	 * @return
	 */
	public Processor<Document> getDocumentProcessor() {
		if(documentProcessor == null)
			documentProcessor = new DocumentProcessor();
		return documentProcessor;
	}
	
	/**
	 * set document processor 
	 * @param documentProcessor
	 */

	public void setDocumentProcessor(Processor<Document> documentProcessor) {
		this.documentProcessor = documentProcessor;
	}

	
	/**
	 * do not process certain aspects of input text
	 * Ex: DeID tags, headers, etc.. 
	 * FILTER_DEID   - filter out DeID tags 
	 * FILTER_HEADER - filter out section headers Ex: FINAL DIAGNOSIS, COMMENT etc.
	 * FILTER_WORKSHEET - do not process sentences that are marked as Sentence.TYPE_WORKSHEET
	 * return processFilter - a conjunction (OR) of filters
	 */
	public int getProcessFilter() {
		return processFilter;
	}

	/**
	 * do not process certain aspects of input text
	 * Ex: DeID tags, headers, etc.. 
	 * FILTER_DEID   - filter out DeID tags 
	 * FILTER_HEADER - filter out section headers Ex: FINAL DIAGNOSIS, COMMENT etc.
	 * FILTER_WORKSHEET - do not process sentences that are marked as Sentence.TYPE_WORKSHEET
	 * @param processFilter - a conjunction (OR) of filters
	 */
	public void setProcessFilter(int processFilter) {
		this.processFilter = processFilter;
	}

	/**
	 * set custom terminology
	 * @param terminology
	 */
	public void setTerminology(Terminology term) {
		terminology = term;
		try {
			setupAcronyms(new File(term.getLocation()));
		} catch (IOException e) {
			throw new TerminologyError("Unable to fine terminology location", e);
		}
	}

	/**
	 * get an instance of acronym detector that 
	 * @return
	 */
	
	public AcronymDetector getAcronymDetector() {
		if(acronymDetector == null)
			acronymDetector = new AcronymDetector();
		return acronymDetector;
	}

	
	/**
	 * setup acronyms
	 * @param name
	 * @throws IOException
	 */
	private void setupAcronyms(File location) throws IOException{
		// load abbreviation information
		/*
		skipAbbrreviationLogic = true;
		File props = new File(location,"search.properties");
		if(props.exists()){
			Properties p = new Properties();
			p.load(new FileInputStream(props));
			if(Boolean.parseBoolean(p.getProperty("ignore.acronyms","false"))){
				File af = new File(p.getProperty("abbreviation.whitelist"));
				if(!af.exists()){
					af = new File(location,af.getName());
				
				}
				abbreviationWhitelist = TextTools.loadResource(af.getAbsolutePath());
				abbreviations = (NobleCoderTerminology) new NobleCoderTerminology(
						new File(location,p.getProperty("abbreviation.terminology",ABBREV_TERMINOLOGY)).getAbsolutePath());
				skipAbbrreviationLogic = false;
			}
		}
		*/
	}
	
	public Terminology getTerminology() {
		return terminology;
	}


	
	/**
	 * process abbreviations and acronyms
	 * @param phrase
	 * @param foundConcepts
	 * @return
	 * @throws TerminologyException
	 *
	private Concept [] processAcronyms(String phrase, Concept [] foundConcepts) throws TerminologyException {
		Concept [] r = foundConcepts;
		
		// handle acronyms that are mentioned in a document
		if(handleAcronyms){
			r = getAcronymDetector().processAcronyms(phrase, foundConcepts);
		}
		
		if(skipAbbrreviationLogic)
			return r;
		
		// look at abbreviations
		Set<String> acronyms = new HashSet<String>();
		for(Concept a: getAbbreviations().search(phrase)){
			acronyms.add(a.getName());
		}
	
		// don't do anything if nothing found
		if(acronyms.isEmpty())
			return r;
		
		// if abbreviations found
		Set<Concept> list = new LinkedHashSet<Concept>();
		for(Concept c: r){
			// add only what is not in the list
			if(!acronyms.contains(c.getMatchedTerm().toLowerCase())){
				list.add(c);
			}
		}
		// add abbreviations that are in whitelist
		for(String txt: acronyms){
			if(getAbbreviationWhitelist().containsKey(txt)){
				String cui = getAbbreviationWhitelist().get(txt);
				Concept c1 = getTerminology().lookupConcept(cui);
				if(c1 == null)
					c1 = getAbbreviations().lookupConcept(cui);
				if(c1 != null){
					c1.setSearchString(phrase);
					c1.setMatchedTerm(txt);
					list.add(c1);
				}
			}
		}
		
		return list.toArray(new Concept [0]);
	}
	*/
	
	/**
	 * is acronym expansion enabled
	 * @return
	 */
	public boolean isAcronymExpansion() {
		return handleAcronyms;
	}

	/**
	 * handle acronym expansion
	 * @param handleAcronyms
	 */
	public void setAcronymExpansion(boolean handleAcronyms) {
		this.handleAcronyms = handleAcronyms;
	}

	
	
	public boolean isContextDetection() {
		return handleNegation;
	}

	public void setContextDetection(boolean handleNegation) {
		this.handleNegation = handleNegation;
	}

	
	/**
	 * process document represented as a string
	 * @param document
	 * @return
	 * @throws IOException 
	 * @throws FileNotFoundException 
	 * @throws TerminologyException 
	 */
	public Document process(File document) throws FileNotFoundException, IOException, TerminologyException {
		Document doc = new Document(TextTools.getText(new FileInputStream(document)));
		doc.setLocation(document.getAbsolutePath());
		doc.setTitle(document.getName());
		return process(getDocumentProcessor().process(doc));
	}
	
	/**
	 * process a document represented as a document object
	 * @throws TerminologyException 
	 */
	public Document process(Document doc) throws TerminologyException {
		time = System.currentTimeMillis();
		getAcronymDetector().clearAcronyms();
		
		// check if document has been parsed
		if(Document.STATUS_UNPROCESSED.equals(doc.getDocumentStatus())){
			doc = getDocumentProcessor().process(doc);
		}
		
		// go over all sentences  
		for(Sentence s : doc.getSentences()){
			if(!filterSentence(s))
				process(s);
		}
		doc.setDocumentStatus(Document.STATUS_CODED);
		
		// return processed document
		time = System.currentTimeMillis() - time;
		doc.getProcessTime().put(getClass().getSimpleName(),time);
		return doc;
	}
	
	/**
	 * process text string to get a list of mentions
	 * @param document
	 * @return
	 * @throws TerminologyException 
	 */
	public List<Mention> process(String text) throws TerminologyException {
		return process(new Sentence(text)).getMentions();
	}
	
	
	/**
	 * 
	 * @param text
	 * @return
	 * @throws TerminologyException 
	 */
	public Sentence process(Sentence  sentence) throws TerminologyException {
		long time = System.currentTimeMillis();
		
		// optionally filter text
		String text = sentence.getText(); 
		sentence.setText(filterText(text));		
		
		// search for concepts from main terminology
		getTerminology().process(sentence);
		
		// handle acronyms that are mentioned in a document
		if(handleAcronyms){
			getAcronymDetector().process(sentence);
		}
		
		// now lets do negation detection
		if(handleNegation){
			getConText().process(sentence);
		}
		
		// set process time and roll-back oritinal text
		sentence.setText(text);
		sentence.getProcessTime().put(getClass().getSimpleName(),(System.currentTimeMillis()-time));
		return sentence;
	}
	
	
	
	public ConText getConText() {
		if(conText == null)
			conText = new ConText();
		return conText;
	}
	
	public void setConText(ConText ct){
		conText = ct;
	}

	/**
	 * return true if sentence should not be parsed
	 * Ex: blank, section heading, de-id string etc..
	 * @param line
	 * @return
	 */
	private boolean filterSentence(Sentence line){
		// skip blank lines
		if(line.getText().length() == 0){
			return true;
		}
		// don't process section headings
		if((getProcessFilter()&FILTER_HEADER) > 0 && Sentence.TYPE_HEADER.equals(line.getSentenceType())){
			return true;
		}
		
		// skip worksheet sentences
		if((getProcessFilter()&FILTER_WORKSHEET) > 0 && Sentence.TYPE_WORKSHEET.equals(line.getSentenceType())){
			return true;
		}
		// skip DeID header
		if((getProcessFilter()&FILTER_DEID) > 0 && DeIDUtils.isDeIDHeader(line.getText())){
			return true;
		}
		
		return false;
	}
	
	
	/**
	 * filter junk out
	 * @param line
	 * @return
	 */
	private String filterText(String line){
		if((getProcessFilter()&FILTER_DEID) > 0)
			return DeIDUtils.filterDeIDTags(line);
		return line;
	}

	public long getProcessTime() {
		return time;
	}

}
