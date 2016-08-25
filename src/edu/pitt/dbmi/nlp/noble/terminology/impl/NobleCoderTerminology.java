package edu.pitt.dbmi.nlp.noble.terminology.impl;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.ListIterator;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Processor;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.ontology.DefaultRepository;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.terminology.AbstractTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Term;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.tools.TextTools.StringStats;
import edu.pitt.dbmi.nlp.noble.util.CacheMap;
import edu.pitt.dbmi.nlp.noble.util.ConceptImporter;
import edu.pitt.dbmi.nlp.noble.util.JDBMMap;
import edu.pitt.dbmi.nlp.noble.util.StringUtils;
import edu.pitt.dbmi.nlp.noble.util.XMLUtils;

/**
 * improve concept scoring code
 */


/**
 * Implementation of Terminology using IndexFinder algorithm
 * @author Eugene Tseytlin (University of Pittsburgh)
 */

public class NobleCoderTerminology extends AbstractTerminology implements Processor<Sentence>{
	// names of property events to monitor progress
	public static final int CACHE_LIMIT = 10000;
	public static final String LOADING_MESSAGE  = "INDEX_FINDER_MESSAGE";
	public static final String LOADING_PROGRESS = "INDEX_FINDER_PROGRESS";
	public static final String LOADING_TOTAL    = "INDEX_FINDER_TOTAL";
	// names of search methods
	public static final String BEST_MATCH = "best-match";
	public static final String ALL_MATCH = "all-match";
	public static final String PRECISE_MATCH= "precise-match";
	public static final String PARTIAL_MATCH= "partial-match";
	public static final String NONOVERLAP_MATCH = "nonoverlap-match";
	public static final String CUSTOM_MATCH = "custom-match";
	
	public static final String TERM_SUFFIX = ".term";
	public static final String MEM_FILE = "terminology.mem";
	public static final String TERM_FILE = "terms";
	public static final String CONCEPT_FILE = "concepts";
	public static final String INFO_FILE = "info.txt";
	public static final String SEARCH_PROPERTIES = "search.properties";
	public static final String TEMP_WORD_DIR = "tempWordTable";
	
	private File location;
	private String name;
	private Storage storage;
	private CacheMap<String,Concept []> cache;
	
	// print rough size and time
	//private final boolean DEBUG = false;
	
	// setting parameters
	/*
	 * stripDigits 			- do not consider digits in text for lookup  (false)
	 * stemWords   			- do porter stemming of terms for storing and lookup (true)
	 * ignoreSmallWords		- do not lookup one-letter words (true)
	 * selectBestCandidate	- if multiple matches for the same term, returned the highest scored one (false)
	 * handleProblemTerms   - if term looks like it can be an abbreviation, do not normalize it for matching (true)
	 * ignoreCommonWords	- do not lookup 100 most frequent English words (false)
	 * scoreConcepts		- perform scoring for best candidate concept for a match (false)
	 * ignoreUsedWords		- do not lookup on a word in text if it is already part of another matched term (false)
	 * 						  If true, there is a big speedup, but there is a potential to miss some matches.
	 * subsumptionMode		- If true, the narrowest concept Ex: 'deep margin', subsumes broader concepts: 'deep' and 'margin' (true)
	 * overlapMode			- If true, concepts are allowed to overlap and share words: 
	 * orderedMode			- If true, an order of words in text has to reflect the synonym term (false)
	 * contiguousMode		- If true, words in a term must be next to eachother within (maxWordGap) (false)
	 * partialMode			- If true, text will match if more then 50% of the synonym words are in text (false)
	 * 
	 * windowSize			- Both a maximum number of words that can form a matched term AND a gap between words to make a match (disabled) 
	 * maxWordGap		- How far words can be apart to be apart as part of a term 
	 */
	
	private boolean stripDigits,crashing,stemWords = true,ignoreSmallWords = true,selectBestCandidate = false, handlePossibleAcronyms = true, stripStopWords = true;
	private boolean ignoreCommonWords,scoreConcepts = true,ignoreAcronyms,ignoreUsedWords = true, compacted;
	private boolean subsumptionMode = true,overlapMode=true, orderedMode, contiguousMode,partialMode; 
	private int windowSize = -1;
	private int maxWordGap = 1;
	private int maxWordsInTerm = 10;
	private double partialMatchThreshold = 0.5;
	private String defaultSearchMethod = BEST_MATCH;
	
	
	
	
	private static File dir;
	private Set<Source> filteredSources;
	private Set<SemanticType> filteredSemanticTypes;
	private Set<String> filteredLanguages;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private boolean cachingEnabled = false,truncateURI = false;
	private long processTime;
	
	/**
	 * isolated storage object that deals with all of the MAPS
	 * @author tseytlin
	 *
	 */
	public static class Storage implements Serializable{
		public int maxTermsPerWord,totalTermsPerWord;
		public boolean useTempWordFolder;
		private File location;
		private Map<String,Set<String>> wordMap,blacklist;
		private Map<String,WordStat> wordStatMap;
		private Map<String,Set<String>> termMap;
		private Map<String,String> regexMap;
		private Map<String,Concept.Content> conceptMap;
		private Map<String,String> infoMap;
		private Map<String,Source> sourceMap;
		private Map<String,String> rootMap;
		private Map<String,String> codeMap;
		
		
		public Storage(){
			init();
		}
		public Storage(File file) throws IOException{
			load(file);
		}
		public Map<String, String> getInfoMap() {
			return infoMap;
		}
		public void setInfoMap(Map<String, String> infoMap) {
			this.infoMap = infoMap;
		}
		public Map<String, Set<String>> getWordMap() {
			return wordMap;
		}
		public Map<String, WordStat> getWordStatMap() {
			return wordStatMap;
		}
		public Map<String, Set<String>> getTermMap() {
			return termMap;
		}
		public Map<String, String> getRegexMap() {
			return regexMap;
		}
		public Map<String, Concept.Content> getConceptMap() {
			return conceptMap;
		}
		public Map<String, Source> getSourceMap() {
			return sourceMap;
		}
		public Map<String, String> getRootMap() {
			return rootMap;
		}
		public Map<String, String> getCodeMap() {
			return codeMap;
		}
		public Map<String, Set<String>> getBlacklist() {
			return blacklist;
		}
		public void init(){
			wordMap = new HashMap<String,Set<String>>();
			blacklist = new HashMap<String,Set<String>>();
			wordStatMap = new HashMap<String, WordStat>();
			termMap = new HashMap<String,Set<String>>();
			regexMap = new HashMap<String,String>();
			conceptMap = new HashMap<String,Concept.Content>();
			infoMap = new HashMap<String,String>();
			sourceMap = new HashMap<String,Source>();
			rootMap = new HashMap<String,String>();
			codeMap = new HashMap<String,String>();
		}
		public void load(File name) throws IOException{
			load(name,false);
		}
		
		public void load(File location,boolean readonly) throws IOException{
			this.location = location;
			String prefix = location.getAbsolutePath()+File.separator+"table";
			
			wordMap = new JDBMMap<String,Set<String>>(prefix,"wordMap",readonly);
			termMap = new JDBMMap<String,Set<String>>(prefix,"termMap",readonly);
			regexMap = new JDBMMap<String,String>(prefix,"regexMap",readonly);
			wordStatMap = new JDBMMap<String,WordStat>(prefix,"wordStatMap",readonly);
			blacklist = new JDBMMap<String,Set<String>>(prefix,"blacklist",readonly);
			conceptMap = new JDBMMap<String,Concept.Content>(prefix,"conceptMap",readonly);
			infoMap = new JDBMMap<String,String>(prefix,"infoMap",readonly);
			sourceMap = new JDBMMap<String,Source>(prefix,"sourceMap",readonly);
			rootMap = new JDBMMap<String,String>(prefix,"rootMap",readonly);
			codeMap = new JDBMMap<String,String>(prefix,"codeMap",readonly);
		}
		
		public boolean tableExists(String tablename){
			String f = location.getAbsolutePath()+File.separator+"table"+"_"+tablename;
			return new File(f+JDBMMap.JDBM_SUFFIX ).exists();
		}
		
		public File getLocation() {
			return location;
		}
		
		public void clear(){
			wordMap.clear();
			blacklist.clear();
			wordStatMap.clear();
			termMap.clear();
			regexMap.clear();
			conceptMap.clear();
			infoMap.clear();
			sourceMap.clear();
			rootMap.clear();
			codeMap.clear();
		}
		
		
		
		public boolean isReadOnly(Map map){
			return map instanceof JDBMMap && ((JDBMMap)map).isReadOnly();
		}
		
		public void commit(Map map){
			if(map instanceof JDBMMap){
				((JDBMMap) map).commit();
			}
		}
		public void commit(){
			if(wordMap instanceof JDBMMap){
				//commit
				((JDBMMap) wordMap).commit();
				((JDBMMap) blacklist).commit();
				((JDBMMap) wordStatMap).commit();
				((JDBMMap) termMap).commit();
				((JDBMMap) conceptMap).commit();
				((JDBMMap) regexMap).commit();
				((JDBMMap) infoMap).commit();
				((JDBMMap) sourceMap).commit();
				((JDBMMap) rootMap).commit();
				((JDBMMap) codeMap).commit();
			}
		}
		public void defrag(){
			if(wordMap instanceof JDBMMap){
				//defrag
				((JDBMMap) wordMap).compact();
				((JDBMMap) blacklist).compact();
				((JDBMMap) wordStatMap).compact();
				((JDBMMap) termMap).compact();
				((JDBMMap) conceptMap).compact();
				((JDBMMap) regexMap).compact();
				((JDBMMap) infoMap).compact();
				((JDBMMap) sourceMap).compact();
				((JDBMMap) rootMap).compact();
				((JDBMMap) codeMap).compact();
			}
		}
		/**
		 * save all information to disc
		 */
		public void save(){
			commit();
			defrag();
		}
		
		public void dispose(){
			if(wordMap instanceof JDBMMap){
				((JDBMMap) wordMap).dispose();
				((JDBMMap) blacklist).dispose();
				((JDBMMap) wordStatMap).dispose();
				((JDBMMap) termMap).dispose();
				((JDBMMap) conceptMap).dispose();
				((JDBMMap) regexMap).dispose();
				((JDBMMap) infoMap).dispose();
				((JDBMMap) sourceMap).dispose();
				((JDBMMap) rootMap).dispose();
				((JDBMMap) codeMap).dispose();
			}
		}
		
		/**
		 * save object
		 * @param location
		 * @throws FileNotFoundException
		 * @throws IOException
		 */
		public void saveObject(File location) throws FileNotFoundException, IOException{
			ObjectOutputStream os = new ObjectOutputStream(new FileOutputStream(location));
			os.writeObject(this);
			os.close();
		}
		
		/**
		 * save object
		 * @param location
		 * @throws FileNotFoundException
		 * @throws IOException
		 * @throws ClassNotFoundException 
		 */
		public static Storage loadObject(File location) throws FileNotFoundException, IOException, ClassNotFoundException{
			ObjectInputStream os = new ObjectInputStream(new FileInputStream(location));
			Object obj = os.readObject();
			os.close();
			return (Storage) obj;
		}
	}

	
	
	
	public boolean isCachingEnabled() {
		return cachingEnabled;
	}

	public void setCachingEnabled(boolean b) {
		this.cachingEnabled = b;
	}

	// init default persistence directory
	static{
		setPersistenceDirectory(DefaultRepository.DEFAULT_TERMINOLOGY_LOCATION);
	}

	/**
	 * set directory where persistence files should be saved
	 * @param f
	 */
	public static void setPersistenceDirectory(File f){
		dir = f;
	}
	
	public void setLocation(File location) {
		this.location = location;
		if(storage != null)
			storage.location = location;
	}

	/**
	 * set directory where persistence files should be saved
	 * @param f
	 */
	public static File getPersistenceDirectory(){
		if(dir != null && !dir.exists())
			dir.mkdirs();
		return dir;
	}
	
	
	/**
	 * represents several word stats
	 * @author tseytlin
	 */
	public static class WordStat implements Serializable {
		public int termCount;
		public boolean isTerm;
	}
	
	
	/**
	 * initialize empty in-memory terminology that has to be
	 * filled up manual using Terminology.addConcept()
	 */
	public NobleCoderTerminology(){
		init();
	}
	
	/**
	 * initialize empty in-memory terminology that has to be
	 * filled up manual using Terminology.addConcept()
	 * @throws IOntologyException 
	 * @throws TerminologyException 
	 * @throws IOException 
	 */
	public NobleCoderTerminology(IOntology ont) throws IOException, TerminologyException, IOntologyException{
		init();
		loadOntology(ont,null,true,true);
	}
	
	/**
	 * initialize with in memory maps
	 */
	public void init(){
		storage = new Storage();
		cache = new CacheMap<String, Concept []>(CacheMap.FREQUENCY);
		cache.setSizeLimit(CACHE_LIMIT);		
	}
	
	/**
	 * initialize a named terminology that either has already been 
	 * persisted on disk, or will be persisted on disk
	 */
	public NobleCoderTerminology(String name) throws IOException{
		load(name,true);
	}
	
	/**
	 * initialize a named terminology that either has already been 
	 * persisted on disk, or will be persisted on disk from file
	 */
	public NobleCoderTerminology(File dir) throws IOException{
		setPersistenceDirectory(dir.getParentFile());
		load(dir.getName(),true);
	}
	
	/**
	 * check if terminology with a given name exists inside
	 * default persisted directory
	 * @param name
	 * @return
	 */
	public static boolean hasTerminology(String name){
		if(name.endsWith(TERM_SUFFIX))
			name = name.substring(0,name.length()-TERM_SUFFIX.length());
		return new File(getPersistenceDirectory(),name+TERM_SUFFIX).isDirectory();
	}
	
	
	
	/**
	 * get Object representing NobleCoder storage
	 * @return
	 */
	public Storage getStorage() {
		return storage;
	}

	protected void finalize() throws Throwable {
		dispose();
	}

	/**
	 * add property change listener to subscribe to progress messages
	 * @param l
	 */
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
		ConceptImporter.getInstance().addPropertyChangeListener(l);
	}
	
	/**
	 * add property change listener to subscribe to progress messages
	 * @param l
	 */
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
		ConceptImporter.getInstance().removePropertyChangeListener(l);
	}
	
	/**
	 * load persitent tables
	 */
	public void load(String name) throws IOException{
		load(name,false);
	}
	/**
	 * load persitent tables
	 */
	public void load(String name,boolean readonly) throws IOException{
		if(name.endsWith(TERM_SUFFIX))
			name = name.substring(0,name.length()-TERM_SUFFIX.length());
		this.name = name;
		
		// setup location
		if(name.contains(File.separator))
			location = new File(name+TERM_SUFFIX);
		else
			location = new File(getPersistenceDirectory(),name+TERM_SUFFIX);
		
		// check if location exists
		if(readonly && !location.exists())
			throw new FileNotFoundException("Cannot open a non-existing terminology file in read-only mode: "+location.getAbsolutePath());
		
		// create a directory
		if(!location.exists())
			location.mkdirs();
		
		// split into two seperate 
		File memFile = new File(location,MEM_FILE);
		if(memFile.exists()){
			//TODO: not very efficient
			try {
				storage = Storage.loadObject(memFile);
			} catch (ClassNotFoundException e) {
				throw new IOException(e);
			}
		}else{
			storage = new Storage();
			storage.load(location,readonly);
		}
	
		cache = new CacheMap<String, Concept []>(CacheMap.FREQUENCY);
		cache.setSizeLimit(CACHE_LIMIT);
		
		
		// load default values
		if(storage.getInfoMap().containsKey("stem.words"))
			stemWords = Boolean.parseBoolean(storage.getInfoMap().get("stem.words"));
		if(storage.getInfoMap().containsKey("strip.digits"))
			stripDigits = Boolean.parseBoolean(storage.getInfoMap().get("strip.digits"));
		if(storage.getInfoMap().containsKey("strip.stop.words"))
			stripStopWords = Boolean.parseBoolean(storage.getInfoMap().get("strip.stop.words"));
		if(storage.getInfoMap().containsKey("ignore.small.words"))
			ignoreSmallWords = Boolean.parseBoolean(storage.getInfoMap().get("ignore.small.words"));
		if(storage.getInfoMap().containsKey("max.words.in.term"))
			maxWordsInTerm = Integer.parseInt(storage.getInfoMap().get("max.words.in.term"));
		if(storage.getInfoMap().containsKey("compacted"))
			compacted = Boolean.parseBoolean(storage.getInfoMap().get("compacted"));
		
		//if(storage.getInfoMap().containsKey("handle.possible.acronyms"))
		//	handleProblemTerms = Boolean.parseBoolean(storage.getInfoMap().get("handle.possible.acronyms"));
		
		// load optional search options
		File sp = new File(location,SEARCH_PROPERTIES);
		if(sp.exists()){
			// pull this file
			Properties p = new Properties();
			FileReader r = new FileReader(sp);
			p.load(r);
			r.close();
			
			// lookup default search method
			setSearchProperties(p);
		}
		
		// load info file for better meta-info
		File ip = new File(location,INFO_FILE);
		if(!readonly && ip.exists()){
			loadMetaInfo(ip);
		}
		
	}
	
	/**
	 * save meta information
	 * @param f
	 */
	private void saveSearchProperteis(){
		try{
			FileWriter w = new FileWriter(new File(location,SEARCH_PROPERTIES));
			getSearchProperties().store(w,"Optional Search Options");
			w.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	/**
	 * get properties map with search options
	 * @return
	 */
	public Properties getSearchProperties(){
		Properties p = new Properties();
		p.setProperty("default.search.method",defaultSearchMethod);
		p.setProperty("ignore.small.words",""+ignoreSmallWords);
		p.setProperty("source.filter",toString(getSourceFilter()));
		p.setProperty("language.filter",toString(getLanguageFilter()));
		p.setProperty("semantic.type.filter",toString(getSemanticTypeFilter()));
		p.setProperty("ignore.common.words",""+isIgnoreCommonWords());
		p.setProperty("ignore.acronyms",""+isIgnoreAcronyms());
		p.setProperty("select.best.candidate",""+isSelectBestCandidate());
		p.setProperty("score.concepts",""+scoreConcepts);
		p.setProperty("window.size",""+getWindowSize());
		p.setProperty("maximum.word.gap",""+getMaximumWordGap());
		p.setProperty("enable.search.cache",""+cachingEnabled);
		p.setProperty("ignore.used.words",""+ignoreUsedWords);
		p.setProperty("subsumption.mode",""+subsumptionMode);
		p.setProperty("overlap.mode",""+overlapMode);
		p.setProperty("contiguous.mode",""+contiguousMode);
		p.setProperty("ordered.mode",""+orderedMode);
		p.setProperty("partial.mode",""+partialMode);
		p.setProperty("partial.mode",""+partialMode);
		p.setProperty("stem.words",""+stemWords);
		p.setProperty("ignore.digits",""+stripDigits);
		p.setProperty("handle.possible.acronyms",""+handlePossibleAcronyms);
		p.setProperty("partial.match.theshold",""+partialMatchThreshold);
		p.setProperty("max.words.in.term",""+maxWordsInTerm);
		return p;
	}
	
	
	/**
	 * set search properties
	 * @param p
	 */
	public void setSearchProperties(Properties p){
		// load default values
		
		/*
		//those values should not be reset by user
		if(p.containsKey("stem.words"))
			stemWords = Boolean.parseBoolean(p.getProperty("stem.words"));
		if(p.containsKey("ignore.digits"))
			stripDigits = Boolean.parseBoolean(p.getProperty("ignore.digits"));
		if(p.containsKey("ignore.small.words"))
			ignoreSmallWords = Boolean.parseBoolean(p.getProperty("ignore.small.words"));
		if(p.containsKey("handle.possible.acronyms"))
			handleProblemTerms = Boolean.parseBoolean(p.getProperty("handle.possible.acronyms"));
		*/
		// lookup default search method
		if(p.containsKey("default.search.method")){
			defaultSearchMethod = BEST_MATCH;
			String s = p.getProperty("default.search.method",BEST_MATCH);
			for(String m: getSearchMethods()){
				if(s.equals(m)){
					defaultSearchMethod = s;
					break;
				}	
			}
		}
		
		if(p.containsKey("ignore.common.words"))
			ignoreCommonWords = Boolean.parseBoolean(p.getProperty("ignore.common.words"));
		if(p.containsKey("ignore.acronyms"))
			ignoreAcronyms = Boolean.parseBoolean(p.getProperty("ignore.acronyms"));
		if(p.containsKey("select.best.candidate"))
			selectBestCandidate = Boolean.parseBoolean(p.getProperty("select.best.candidate"));
		if(p.containsKey("window.size")){
			try{
				windowSize = Integer.parseInt(p.getProperty("window.size"));
			}catch(Exception ex){}
		}
		if(p.containsKey("word.window.size")){
			try{
				maxWordGap = Integer.parseInt(p.getProperty("word.window.size"))-1;
			}catch(Exception ex){}
		}
		if(p.containsKey("maximum.word.gap")){
			try{
				maxWordGap = Integer.parseInt(p.getProperty("maximum.word.gap"));
			}catch(Exception ex){}
		}
		
		if(p.containsKey("ignore.used.words"))
			ignoreUsedWords = Boolean.parseBoolean(p.getProperty("ignore.used.words"));
		if(p.containsKey("subsumption.mode"))
			subsumptionMode = Boolean.parseBoolean(p.getProperty("subsumption.mode"));
		if(p.containsKey("overlap.mode"))
			overlapMode = Boolean.parseBoolean(p.getProperty("overlap.mode"));
		if(p.containsKey("contiguous.mode"))
			contiguousMode = Boolean.parseBoolean(p.getProperty("contiguous.mode"));
		if(p.containsKey("ordered.mode"))
			orderedMode = Boolean.parseBoolean(p.getProperty("ordered.mode"));
		if(p.containsKey("partial.mode"))
			partialMode = Boolean.parseBoolean(p.getProperty("partial.mode"));
		if(p.containsKey("enable.search.cache"))
			cachingEnabled = Boolean.parseBoolean(p.getProperty("enable.search.cache"));
		if(p.containsKey("partial.match.theshold"))
			partialMatchThreshold = Double.parseDouble(p.getProperty("partial.match.theshold"));
		if(p.containsKey("max.words.in.term"))
			maxWordsInTerm = Integer.parseInt(p.getProperty("max.words.in.term"));
		
		// language filter
		String v = p.getProperty("language.filter");
		if(v != null && v.length() > 0){
			ArrayList<String> val = new ArrayList<String>();
			String sep = (v.indexOf(';') > -1)?";":",";
			for(String s: v.split(sep))
				val.add(s.trim());
			setLanguageFilter(val.toArray(new String [0]));
		}
		
		// source filter
		v = p.getProperty("source.filter");
		if(v != null && v.length() > 0){
			ArrayList<Source> val = new ArrayList<Source>();
			String sep = (v.indexOf(';') > -1)?";":",";
			for(String s: v.split(sep))
				val.add(Source.getSource(s.trim()));
			setSourceFilter(val.toArray(new Source [0]));
		}
		
		// semantic type filter
		v = p.getProperty("semantic.type.filter");
		if(v != null && v.length() > 0){
			ArrayList<SemanticType> val = new ArrayList<SemanticType>();
			String sep = (v.indexOf(';') > -1)?";":",";
			for(String s: v.split(sep))
				val.add(SemanticType.getSemanticType(s.trim()));
			setSemanticTypeFilter(val.toArray(new SemanticType [0]));
		}
	
		
	}
	
	
	private String toString(Object [] list){
		StringBuffer b = new StringBuffer();
		for(Object o: list){
			b.append(o+";"); 
		}
		return (b.length()>1)?b.substring(0,b.length()-1):"";
	}
	
	/**
	 * save meta information
	 * @param f
	 */
	private void loadMetaInfo(File f){
		try{
			for(String l: TextTools.getText(new FileInputStream(f)).split("\n")){
				if(l.trim().length() > 0){
					int n = l.indexOf(':');
					String key = l.substring(0,n).trim();
					String val = l.substring(n+1).trim();
					storage.getInfoMap().put(key,val);
				}
			}
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
	}
	
	/**
	 * save meta information
	 * @param f
	 */
	private void saveMetaInfo(File f){
		try{
			BufferedWriter writer = new BufferedWriter(new FileWriter(f));
			writer.write("name:\t\t"+getName()+"\n");
			writer.write("uri:\t\t"+getURI()+"\n");
			writer.write("version:\t"+getVersion()+"\n");
			writer.write("location:\t"+getLocation()+"\n");
			if(storage.getInfoMap().containsKey("languages"))
				writer.write("languages:\t"+storage.getInfoMap().get("languages")+"\n");
			writer.write("description:\t"+getDescription()+"\n");
			if(storage.getInfoMap().containsKey("semantic.types"))
				writer.write("semantic types:\t"+storage.getInfoMap().get("semantic.types")+"\n");
			if(storage.getInfoMap().containsKey("word.count"))
				writer.write("word count:\t"+storage.getInfoMap().get("word.count")+"\n");
			if(storage.getInfoMap().containsKey("term.count"))
				writer.write("term count:\t"+storage.getInfoMap().get("term.count")+"\n");
			if(storage.getInfoMap().containsKey("concept.count"))
				writer.write("concept count:\t"+storage.getInfoMap().get("concept.count")+"\n");
			writer.write("configuration:\t");
			writer.write("stem.words="+stemWords+", ");
			writer.write("strip.digits="+stripDigits+", ");
			writer.write("strip.stop.words="+stripStopWords+", ");
			//writer.write("handle.possible.acronyms="+handlePossibleAcronyms+", ");
			writer.write("max.words.in.term="+maxWordsInTerm+", ");
			writer.write("ignore.small.words="+ignoreSmallWords+"\n");
			writer.write("\nsources:\n\n");
			for(String name: new TreeSet<String>(storage.getSourceMap().keySet())){
				writer.write(name+": "+storage.getSourceMap().get(name).getDescription()+"\n");
			}
			
			
			writer.close();
		}catch(Exception ex){
			ex.printStackTrace();
		}
		
		// save info in map
		storage.getInfoMap().put("strip.digits",""+stripDigits);
		storage.getInfoMap().put("strip.stop.words",""+stripStopWords);
		storage.getInfoMap().put("stem.words",""+stemWords);
		storage.getInfoMap().put("ignore.small.words",""+ignoreSmallWords);
		storage.getInfoMap().put("max.words.in.term",""+maxWordsInTerm);
		//storage.getInfoMap().put("handle.possible.acronyms",""+handlePossibleAcronyms);
	}
	
	

	
	
	/**
	 * get the entire set of concept codes
	 * @return
	 */
	public Set<String> getAllConcepts(){
		return storage.getConceptMap().keySet();
	}
	
	/**
	 * get all available concept objects in terminology. Only sensible for small terminologies
	 * @return
	 */
	public Collection<Concept> getConcepts()  throws TerminologyException{
		List<Concept> list = new ArrayList<Concept>();
		for(Concept.Content c: storage.getConceptMap().values()){
			list.add(convertConcept(c));
		}
		return list;
	}
	
	/**
	 * reload tables to save space
	 */
	public void crash(){
		if(crashing)
			return;
		crashing = true;
		// save current content 
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Running low on memory.Saving work and crashing ...");
		save();
		System.exit(1);
		crashing = false;
	}
	
	
	/**
	 * load index finder tables from an IOntology object
	 * @param ontology
	 * @throws IOException
	 * @throws TerminologyException 
	 */
	public void loadOntology(IOntology ontology) throws IOException, TerminologyException, IOntologyException {
		loadOntology(ontology,null);
	}
	
	/**
	 * load index finder tables from an IOntology object
	 * @param ontology
	 * @throws IOException
	 * @throws TerminologyException 
	 */
	public void loadOntology(IOntology ontology, String name) throws IOException, TerminologyException, IOntologyException {
		loadOntology(ontology,name,false,false);
	}
	
	/**
	 * load index finder tables from an IOntology object
	 * @param ontology
	 * @throws IOException
	 * @throws TerminologyException 
	 */
	public void loadOntology(IOntology ontology, String name, boolean inmemory,boolean truncateURI) throws IOException, TerminologyException, IOntologyException {
		this.truncateURI = truncateURI;
		ConceptImporter.getInstance().loadOntology(this, ontology, name, inmemory, truncateURI);
	}
	
	/**
	 * load from RRF files (Rich Release Files)
	 * This is a common distribution method for UMLS and NCI Meta
	 * @param directory that contains MRCONSO.RRF, MRDEF.RRF, MRSTY.RRF etc...
	 * by default uses ALL sources, but only for English language
	 */
	public void loadRRF(File dir) throws FileNotFoundException, IOException, TerminologyException {
		Map<String,List<String>> params = new HashMap<String, List<String>>();
		params.put("languages",Arrays.asList("ENG"));
		loadRRF(dir,params);
	}
	
	
	/**
	 * load from RRF files (Rich Release Files)
	 * This is a common distribution method for UMLS and NCI Meta
	 * @param directory that contains MRCONSO.RRF, MRDEF.RRF, MRSTY.RRF etc...
	 * @param Map<String,List<String>> filter property object, where some properties are:
	 * name - change ontology name
	 * languages - only include languages in a given list languages
	 * sources - only include concepts from a given list of sources
	 * semanticTypes - filter result by a list of semantic types attached
	 * hierarchySources - only include hierarhy information from a list of sources
	 */
	public void loadRRF(File dir, Map<String,List<String>> params) throws FileNotFoundException, IOException, TerminologyException {
		ConceptImporter.getInstance().loadRRF(this, dir, params);
	}
	
	/**
	 * load from RRF files (Rich Release Files)
	 * This is a common distribution method for UMLS and NCI Meta
	 * @param directory that contains MRCONSO.RRF, MRDEF.RRF, MRSTY.RRF etc...
	 * @param Map<String,List<String>> filter property object, where some properties are:
	 * name - change ontology name
	 * languages - only include languages in a given list languages
	 * sources - only include concepts from a given list of sources
	 * semanticTypes - filter result by a list of semantic types attached
	 * hierarchySources - only include hierarhy information from a list of sources
	 */
	public void loadRRF(File dir, Map<String,List<String>> params,boolean inmem) throws FileNotFoundException, IOException, TerminologyException {
		ConceptImporter.getInstance().loadRRF(this, dir, params,inmem);
	}
	
	/**
	 * load terms file
	 * @param file
	 * @throws Exception
	 */
	public void loadText(File file,String name) throws Exception {
		ConceptImporter.getInstance().loadText(this,file, name);
	}
	
	
	/**
	 * returns true if this terminology doesn't contain any terms
	 * @return
	 */
	public boolean isEmpty(){
		return storage.getWordMap().isEmpty();
	}
	
	
	/**
	 * get all concept objects in the entire terminology
	 * @return
	 *
	public Collection<Concept> getAllConcepts(){
		return storage.getConceptMap().values();
	}
	*/
	
	public void clear(){
		storage.clear();
	}
	
	/**
	 * clear cache
	 */
	public void clearCache(){
		cache.clear();
	}
	
	/**
	 * save all information to disc
	 */
	public void save(){
		pcs.firePropertyChange(LOADING_PROGRESS,null,"Saving Index Finder Tables ...");
		saveMetaInfo(new File(location,INFO_FILE));
		saveSearchProperteis();
		storage.save();
	}
	
	public void dispose(){
		storage.dispose();
	}
	
	/**
	 * ignore digits in concept names for matching
	 * default is false
	 * @param b
	 */
	public void setIgnoreDigits(boolean b){
		stripDigits = b;
	}
	
	
	
	public boolean isStripStopWords() {
		return stripStopWords;
	}

	public void setStripStopWords(boolean stripStopWords) {
		this.stripStopWords = stripStopWords;
	}

	/**
	 * use porter stemmer to stem words during search
	 * default is true
	 * @param stemWords
	 */
	public void setStemWords(boolean stemWords) {
		this.stemWords = stemWords;
	}

	
	/**
	 * ignore one letter words to avoid parsing common junk
	 * default is true
	 * @param stemWords
	 */

	public void setIgnoreSmallWords(boolean ignoreSmallWords) {
		this.ignoreSmallWords = ignoreSmallWords;
	}



	/**
	 * add concept to terminology
	 */
	public boolean addConcept(Concept c) throws TerminologyException {
		// don't go into classes that we already visited
		if(storage.getConceptMap().containsKey(c.getCode()))
			return true;
		
		// check if read only
		if(storage.isReadOnly(storage.getConceptMap())){
			dispose();
			try {
				load(name,false);
			} catch (IOException e) {
				throw new TerminologyException("Unable to gain write access to data tables",e);
			}
		}
		
		
		// get list of terms
		Set<String> terms = getTerms(c);
		for(String term: terms){
			// check if term is a regular expression
			if(isRegExp(term)){
				String regex = term.substring(1,term.length()-1);
				try{
					Pattern.compile(regex);
					storage.getRegexMap().put("\\b("+regex+")\\b",c.getCode());
				}catch(PatternSyntaxException ex){
					pcs.firePropertyChange(LOADING_MESSAGE,null,"Warning: failed to add regex /"+regex+"/ as synonym, because of pattern error : "+ex.getMessage());
				}
			}else{
				// insert concept concept into a set
				Set<String> codeList = new HashSet<String>();
				codeList.add(c.getCode());
				// add concept codes thate were already in a set
				if(storage.getTermMap().containsKey(term)){
					codeList.addAll(storage.getTermMap().get(term));
				}
				// insert the set
				storage.getTermMap().put(term,codeList);
				
				// insert words
				for(String word: TextTools.getWords(term)){
					setWordTerms(word,terms);	
				}
			}
			
		}
		storage.getConceptMap().put(c.getCode(),c.getContent());
		
		// now, why can't we insert on other valid codes :) ???? I think we can 
		for(Object code: c.getCodes().values()){
			if(!storage.getCodeMap().containsKey(code))
				storage.getCodeMap().put(code.toString(),c.getCode());
		}
		
		return true;
	}
	
	/**
	 * add concept as a root
	 * @param code
	 */
	public boolean addRoot(String code){
		if(storage.getConceptMap().containsKey(code)){
			storage.getRootMap().put(code,"");	
			return true;
		}
		return false;
	}
	
	/**
	 * only return terms where given word occures
	 * @param workd
	 * @param terms
	 * @return
	 */
	private Set<String> filterTerms(String word, Set<String> terms){
		Set<String> result = new HashSet<String>();
		for(String t: terms){
			if(t.contains(word))
				result.add(t);
		}
		return result;
	}
	
	public boolean removeConcept(Concept c) throws TerminologyException {
		// find concept terms
		if(storage.getConceptMap().containsKey(c.getCode()))
			c = convertConcept(storage.getConceptMap().get(c.getCode()));
		Set<String> terms = getTerms(c);
		// remove all terms and words
		for(String term: terms){
			storage.getTermMap().remove(term);
			//remove from words
			for(String word: TextTools.getWords(term)){
				Set<String> list = getWordTerms(word);
				if(list != null){
					list.remove(term);
					// if the only entry, remove the word as well
					if(list.isEmpty())
						storage.getWordMap().remove(word);	
				}
			}
		}
		return true;
	}

	public boolean updateConcept(Concept c) throws TerminologyException {
		removeConcept(c);
		addConcept(c);
		return true;
	}

	/**
	 * Get Search Methods supported by this terminology 
	 * Values are
	 * 
	 * best-match : subsumption of concepts, overlap of concepts
	 * all-match  : overlap of concepts
	 * precise-match: subsumption of concepts, overlap of concepts, contiguity of term
	 * nonoverlap-match: subsumption of concepts
	 * partial-match: partial term match, overlap of concepts
	 * custom-match: use flags to tweak search 
	 */
	public String[] getSearchMethods() {
		return new String [] {BEST_MATCH,ALL_MATCH,PRECISE_MATCH,PARTIAL_MATCH,CUSTOM_MATCH};
	}
	
	/**
	 * try to find the best possible match for given query
	 */
	public Concept[] search(String text) throws TerminologyException {
		return search(text,defaultSearchMethod);
	}
	
	/**
	 * setup search method
	 * @param metho
	 */
	private void setupSearch(String method){
		if(method == null)
			method = defaultSearchMethod;
		
		if(BEST_MATCH.equals(method)){
			subsumptionMode = true;
			overlapMode = true;
			contiguousMode = true;
			orderedMode = false;
			partialMode = false;
			maxWordGap = 1;
		}else if(ALL_MATCH.equals(method)){
			subsumptionMode = false;
			overlapMode = true;
			contiguousMode = false;
			orderedMode = false;
			partialMode = false;
			ignoreUsedWords = false;
		}else if(PRECISE_MATCH.equals(method)){
			subsumptionMode = true;
			overlapMode = true;
			contiguousMode = true;
			orderedMode = true;
			partialMode = false;
			maxWordGap = 0;
		}else if(NONOVERLAP_MATCH.equals(method)){
			subsumptionMode = true;
			overlapMode = false;
			contiguousMode = false;
			orderedMode = false;
			partialMode = false;
		}else if(PARTIAL_MATCH.equals(method)){
			subsumptionMode = false;
			overlapMode = false;
			contiguousMode = false;
			orderedMode = false;
			partialMode = true;
		}
		
		// if compacted, you want to disable ignore used words
		if(compacted)
			ignoreUsedWords = false;
		
	}
	
	
	/**
	 * How far can the words in a matched term be apart for it to match.
	 * Stop words are ignored in this count
	 * Example: 'red swift dog' won't match 'red dog' if word gap is 0,
	 *          but will match if it is 1 or more.
	 * @return
	 */
	public int getMaximumWordGap() {
		return maxWordGap;
	}

	/**
	 * How far can the words in a matched term be apart for it to match.
	 * Stop words are ignored in this count
	 * Example: 'red swift dog' won't match 'red dog' if word gap is 0,
	 *          but will match if it is 1 or more.
	 * @return
	 */
	public void setMaximumWordGap(int wordWindowSize) {
		this.maxWordGap = wordWindowSize;
	}


	/**
	 * represents a tuple of hashtable and list
	 */
	private static class NormalizedWordsContainer {
		public Map<String,String> normalizedWordsMap;
		public List<String> normalizedWordsList;
		public List<String> originalWordsList;
		
	}
	
	/**
	 * perform normalization of a string @see normalize, but return unsorted list of words 
	 * @param text
	 * @param stem -stem words
	 * @param strip - strip digits
	 * @return Map<String,String> normalized word for its original form
	 */
	private NormalizedWordsContainer getNormalizedWordMap(String text){
		NormalizedWordsContainer c = new NormalizedWordsContainer();
		c.normalizedWordsMap = new LinkedHashMap<String, String>();
		c.normalizedWordsList = new ArrayList<String>();
		c.originalWordsList = TextTools.getWords(text);
		//boolean skipAbbr = false;
		
		for(String w: c.originalWordsList){
			List<String> ws = TextTools.normalizeWords(w, stemWords, stripDigits, stripStopWords);
			if(!ws.isEmpty() && !c.normalizedWordsMap.containsKey(ws.get(0)))
				c.normalizedWordsMap.put(ws.get(0),w);
			c.normalizedWordsList.addAll(ws);
		}
		return c;
	}
	
	/**
	 * Get a list of contiguous concept annotations from a given concept
	 * Essentially converts a single concepts that annotates multiple related words to text
	 * to potentially multiple instances of a concept in text
	 * @param c
	 * @return
	 */
	private List<Annotation> getAnnotations(Concept c,List<String> searchWords){
		List<Annotation> list = new ArrayList<Annotation>();
		List<String> matchedWords = TextTools.getWords(c.getMatchedTerm()); //Arrays.asList(c.getMatchedTerm().split(" "));
		int n = 0;
		for(String w: searchWords){
			if(matchedWords.contains(w)){
				Annotation a = new Annotation();
				a.setText(w);
				a.setOffset(c.getSearchString().indexOf(w,n));
				a.setSearchString(c.getSearchString());
				list.add(a);
			}
			n += w.length()+1;
		}
		return list;
	}
	
	/**
	 * try to find the best possible match for given query
	 */
	public Concept[] search(String text,String method) throws TerminologyException {
		Map<Concept,Concept> result = new TreeMap<Concept,Concept>(new Comparator<Concept>() {
			public int compare(Concept o1, Concept o2) {
				if(o2.getCode().equals(o1.getCode()))
					return 0;
				int n = (int)(1000 * (o2.getScore()-o1.getScore()));
				if(n == 0)
					return o2.getCode().compareTo(o1.getCode());
				return n;
			}
		});
		// replace default search for the druation of this query
		String ds = defaultSearchMethod;
		defaultSearchMethod = method;
		
		// process sentences with mentions
		List<Mention> mentions  = process(new Sentence(text)).getMentions();
		
		// switch back the search
		defaultSearchMethod = ds;
		
		// now add concepts from mentions back into results
		for(Mention m: mentions){
			Concept c = m.getConcept();
			Concept o = result.get(c);
			if(o == null){
				result.put(c,c);
			}else{
				o.addMatchedTerm(c.getMatchedTerm());
				for(Annotation a: c.getAnnotations()){
					o.addAnnotation(a);
				}
			}
		}
		
		//return result
		return result.keySet().toArray(new Concept[0]);
	}
	
	
	private boolean isAcronym(Concept c) {
		for(Term t: c.getTerms()){
			if(("ACR".equals(t.getForm()) || t.getForm().endsWith("AB")) && t.getText().equalsIgnoreCase(c.getMatchedTerm()))
				return true;
		}
		return false;
	}

	/**
	 * optionally limit to a sublist of words
	 * @param words
	 * @return
	 */
	private List<String> getTextWords(List<String> words,int count) {
		// currently there is a bug, so can't use window size with used words
		if(ignoreUsedWords)
			return words;
		// decrement to compensate
		count --;
		if(windowSize > 0 && words.size() > windowSize && count < words.size()){
			int end = (count+windowSize)<words.size()?count+windowSize:words.size();
			return words.subList(count,end);
		}
		return words;
	}
	
	
	
	/**
	 * set the maximum size a single term can take, to limit the search for very long input
	 * default is 0, which means no limit
	 * @param n
	 */
	public void setWindowSize(int n){
		windowSize = n;
	}
	
	/**
	 * get original string
	 * @param text
	 * @param term
	 * @param map
	 * @return
	 */
	private String getOriginalTerm(String text, String term, Map<String,String> map){
		StringBuffer ot = new StringBuffer();
		final String txt = text.toLowerCase();
		Set<String> words = new TreeSet<String>(new Comparator<String>() {
			public int compare(String o1, String o2) {
				if(o1.length() > 3)
					o1 = o1.substring(0,o1.length()-1);
				if(o2.length() > 3)
					o2 = o2.substring(0,o2.length()-1);
				int x = txt.indexOf(o1) - txt.indexOf(o2);
				if(x == 0)
					return o1.compareTo(o2);
				return x;
			}
		});
		Collections.addAll(words, term.split(" "));
		for(String s: words){
			String w = map.get(s);
			if(w == null)
				w = s;
			ot.append(w+" ");
		}
		String oterm = ot.toString().trim();
		return oterm;
	}
	
	
	/**
	 * get best candidates for all concepts that match a single term
	 * @param text
	 * @param concepts
	 * @return
	 */
	private List<Concept> getBestCandidates(List<Concept> concepts){
		final double THRESHOLD = 0.0;
		// do default return original list
		// if concepts were not scored or list is empty
		if(concepts.isEmpty() || !scoreConcepts)
			return concepts;
	
		// if selecting one best candidate
		if(selectBestCandidate){
			// now find best scoring concept in a list
			Concept best = null;
			for(Concept c: concepts){
				if(best == null || best.getScore() < c.getScore())
					best = c;
			}
			return best.getScore() >= THRESHOLD?Collections.singletonList(best):Collections.EMPTY_LIST;
		// else we have scored concepts, but not best candidate
		}else if(scoreConcepts){
			// filter out concepts that FAIL basic scoring
			// independent of how they compare to to other candidates
			for(ListIterator<Concept> i=concepts.listIterator();i.hasNext();){
				if(i.next().getScore() < THRESHOLD)
					i.remove();
			}
		}
		return concepts;
	}
	
	
	
	/**
	 * get all terms associated with a word
	 * @param word
	 * @return
	 */
	private Set<String> getWordTerms(String word){
		return storage.getWordMap().get(word);
	}
	
	
	/**
	 * add entry to word table
	 * @param word
	 * @param terms
	 */
	public void setWordTerms(String word,Set<String> terms){
		// filter terms to only include those that contain a given word
		Set<String> termList = filterTerms(word,terms);
		
		// if in temp word folder mode, save in temp directory instead of map
		if(storage.useTempWordFolder && location != null && location.exists()){
			try {
				ConceptImporter.saveTemporaryTermFile(location, word, termList);
			} catch (IOException e) {
				pcs.firePropertyChange(LOADING_MESSAGE,null,"Warning: failed to create file \""+word+"\", reason: "+e.getMessage());
			}
		// else do the normal save to MAP	
		}else{
			if(storage.getWordMap().containsKey(word)){
				termList.addAll(getWordTerms(word));
			}
			try{
				storage.getWordMap().put(word,termList);
				storage.commit(storage.getWordMap());
			}catch(IllegalArgumentException e ){
				storage.getWordMap().put(word,new HashSet<String>(Collections.singleton(word)));
				pcs.firePropertyChange(LOADING_MESSAGE,null,"Warning: failed to insert word \""+word+"\", reason: "+e.getMessage());
				
			}
			// if word already existed, subtract previous value from the total
			if(storage.getWordStatMap().containsKey(word))
				storage.totalTermsPerWord -= storage.getWordStatMap().get(word).termCount;
			
			WordStat ws = new WordStat();
			ws.termCount = termList.size();
			ws.isTerm = termList.contains(word);
			storage.getWordStatMap().put(word,ws);
			storage.totalTermsPerWord += termList.size();
			if(termList.size() > storage.maxTermsPerWord)
				storage.maxTermsPerWord = termList.size();
			
		}
	}
	
	/**
	 * get all used words from this term
	 * @param term
	 * @return
	 */
	private List<String> getUsedWords(List<String> words, String term){
		// if not ignore used words and in overlap mode, return
		if(!ignoreUsedWords && overlapMode)
			return Collections.EMPTY_LIST;
				
		List<String> termWords = TextTools.getWords(term);
		List<String> usedWords = new ArrayList<String>();
		// remove words that are involved in term
		if(overlapMode){
			for(String w: termWords){
				usedWords.add(w);
			}
		}else{
			boolean span = false;
			for(String w: words){
				// if text word is inside terms, then
				if(termWords.contains(w)){
					usedWords.add(w);
					termWords.remove(w);
					span = true;
				}
				if(termWords.isEmpty())
					break;
				if(span)
					usedWords.add(w);
			}
		}
		return usedWords;
	}
	
	
	/**
	 * search through regular expressions
	 * @param text
	 * @return
	 */
	private Collection<Concept> searchRegExp(String term){
		List<Concept> result = null;
		term = new String(term);
		// iterate over expression
		for(String re: storage.getRegexMap().keySet()){
			// match regexp from file to
			Pattern p = Pattern.compile(re,Pattern.CASE_INSENSITIVE);
			Matcher m = p.matcher( term );
			while ( m.find() ){
				if(result == null)
					result = new ArrayList<Concept>();
				
				String cls_str = storage.getRegexMap().get(re);
				String txt = m.group(1);    // THIS BETTER BE THERE,
				//System.out.println(cls_str+" "+txt+" for re: "+re);	
				// create concept from class
				Concept c = convertConcept(storage.getConceptMap().get(cls_str));
				c = c.clone();
				c.setTerminology(this);
				c.setSearchString(term);
				Annotation.addAnnotation(c, txt,m.start());
				
				// check if results already have similar entry
				// if new entry is better replace the old one
				boolean toadd = true;
				for(ListIterator<Concept> it = result.listIterator();it.hasNext();){
					Concept b = it.next();
					
					// get offsets of concepts
					int st = c.getOffset();
					int en = c.getOffset()+c.getText().length();
					
					int stb = b.getOffset();
					int enb = b.getOffset()+b.getText().length();
					
					// if concept b (previous concept) is within concept c (new concept)
					if(st <=  stb && enb <= en)
						it.remove();
					else if (stb <=  st && en <= enb)
						toadd = false;
					
				}
				
				// add concept to result
				if(toadd)
					result.add(c);
				
				// this is bad, cause this fucks up next match
				// that can potentially overlap, use case ex: \\d vs \\d.\\d
				//term = term.replaceAll(txt,"");
			}
		}
						
		return (result != null)?result:Collections.EMPTY_LIST;
	}
	
	/**
	 * get best term that spans most words
	 * @param words in search string
	 * @param word in question
	 * @return
	 */
	private Collection<String> getBestTerms(List<String> words, Set<String> swords,Set<String> usedWords, String word){
		// get list of terms that have a given word associated with it
		Set<String> terms = storage.getBlacklist().containsKey(word)?storage.getBlacklist().get(word):getWordTerms(word);
		if(terms == null || words.isEmpty())
			return Collections.EMPTY_LIST;
		
		// best-match vs all-match
		// in best-match terms that are subsumed by others are excluded 
		List<String> best = new ArrayList<String>();
		int bestCount = 0;
		for(String term: terms){
			// check if term should not be used 
			//if(isFilteredOut(term))
			//	continue;
			
			boolean all = true;
			int hits = 0;
			List<String> twords  = TextTools.getWords(term);
			
			// if at least one word not in list of words, don't have a match
			for(String tword : twords ){
				// if term word doesn't occur in text, then NO match
				if(!swords.contains(tword)){
					all = false;
					if(!partialMode)
						break;
				}else{
					// if not in overlap mode,then make sure that this term word is not used already
					if(!overlapMode){
						if(usedWords.contains(tword)){
							all = false;
							hits --;
							if(!partialMode)
								break;
						}
					}
					hits++;	
				}
			}
			
			// do partial match
			if(partialMode && !all && hits > 0){
				//all = hits >= twords.length/2.0;
				all = ((double)hits/twords.size()) >= partialMatchThreshold;
			}
			
			// optionally inforce term contiguity in text
			if(all && contiguousMode && twords.size() > 1){
				// go over every word in a sentence
				all = checkContiguity(words, twords);
			}
			
			
			// optionally inforce term order in text
			if(all && orderedMode && twords.size() > 1){
				// if we are here, lets find the original synonym that matched this normalized term
				// reset all variable, if not ordered
				all = checkWordOrder(words, twords, term);
			}

			
			// if all words match
			if(all){
				// if best-match mode, then keep the best term only
				if(subsumptionMode){
					// select the narrowest best
					if(twords.size() > bestCount){
						best = new ArrayList<String>();
						best.add(term);
						bestCount = twords.size();
					}else if(twords.size() == bestCount){
						best.add(term);
					}
				// else use all-matches mode and keep all of them
				}else{
					best.add(term);
				}
			}	
		}
		return best;
	}
	
	/**
	 * check that the term is contigous within the limits allowed
	 * @param words
	 * @param twords
	 * @return
	 */
	private boolean checkContiguity(List<String> words,List<String> twords){
		// go over every word in a sentence
		boolean continguous = false;
		for(int i=0;i<words.size();i++){
			// if term words contain that word, then
			// look at the sublist that includes it and + allowed gap
			// if this sublist contains ALL term words, then we have contigous match
			// FROM MELISSA: the word window span is actually good, just need to do gap analysis after to 
			// make sure that no gap exceeds the word gap 
			
			if(twords.contains(words.get(i))){
				int n = i+((maxWordGap+1)*(twords.size()-1))+1;
				if(n > words.size())
					n = words.size();
				if(words.subList(i,n).containsAll(twords)){
					continguous = true;
					break;
				}
			}
		}
		return continguous; 
	}
	
	
	private int indexOf(List<String> list,String w, int n){
		for(int i=n;i<list.size();i++){
			if(list.get(i).equals(w))
				return i;
		}
		return -1;
	}
	
	/**
	 * check word order
	 * @param term
	 * @return
	 */
	private boolean checkWordOrder(List<String> words,List<String> twords,String term){
		boolean ordered = true;
		
		// assume that term word order is the same, since we stopped sorting terms when storing them
		int lastI = 0;
		for(String tw: twords){
			int i = indexOf(words,tw,lastI);
			if(i < lastI){
				ordered = false;
				break;
			}
			lastI = i;	
		}
		return ordered;
	
	}
	
	
	
	/**
	 * should the concept be filtered out based on some filtering technique
	 * @param c
	 * @return
	 */
	private boolean isFilteredOut(Concept c) {
		boolean filteredOut = false;
		
		// do not filter anything if filtered sources are not set
		if(filteredSources != null && !filteredSources.isEmpty()){
			filteredOut = true;
			Source [] src = c.getSources();
			if(src != null){
				for(Source s: src){
					// if at least one source is contained 
					// in filter list, then do not filter it out
					if(filteredSources.contains(s)){
						filteredOut = false;
						break;
					}
				}
			}else{
				// if we have no sources set,
				// well, meybe we should use this concept
				filteredOut =  false;
			}
			// if we can't find concept or it doesn't have and sources difined
			// discard it (filter out)
			if(filteredOut)
				return true;
		}
		
		// do not filter anything if filtered semantic types are not set
		if(filteredSemanticTypes != null && !filteredSemanticTypes.isEmpty()){
			filteredOut = true;
			SemanticType [] src = c.getSemanticTypes();
			if(src != null){
				for(SemanticType s: src){
					// if at least one source is contained 
					// in filter list, then do not filter it out
					if(filteredSemanticTypes.contains(s)){
						filteredOut = false;
						break;
					}
				}
			}else{
				// if we have no semantic types set,
				// well, meybe we should use this concept
				filteredOut = false;
			}
			// if we can't find concept or it doesn't have and semantic types difined
			// discard it (filter out)
			if(filteredOut)
				return true;
		}
		// keep concept , if everything else is cool
		
		// if we have a match of a small word that is a common word
		// then check exact case, we don't want to match abbreviations
		// by mistake
		// NEW matching strategy should take care of it now
		/*
		if(handleProblemTerms){
			String term = c.getMatchedTerm();
			if(term != null && term.length() < 5 && TextTools.isCommonWord(term)){
				boolean exactMatch = false;
				for(String s: c.getSynonyms()){
					if(s.equals(term) || (s.startsWith("/") && term.matches("\\b("+s.substring(1,s.length()-1)+")\\b"))){
						exactMatch = true;
						break;
					}
				}
				if(!exactMatch)
					return true;
			}
			
		}
		*/
		
		return filteredOut;
			
	}



	/**
	 * get list of normalized terms from from the class
	 * @param name
	 * @return
	 */
	protected Set<String> getTerms(Concept cls){
		return getTerms(cls,stemWords);
	}
	
	/**
	 * get list of normalized terms from from the class
	 * @param name
	 * @return
	 */
	protected Set<String> getTerms(Concept cls, boolean stem){
		if(cls == null)
			return Collections.EMPTY_SET;
		
		String name = cls.getName();
		//Pattern pt = Pattern.compile("(.*)[\\(\\[].*[\\)\\]]");
		Set<String> terms = new HashSet<String>();
		Set<String> synonyms = new LinkedHashSet<String>();
		synonyms.add(name);
		Collections.addAll(synonyms,cls.getSynonyms());
		for(String str: synonyms){
			if(isRegExp(str))
				terms.add(str);
			else{
				// if we have a limit on size of words in a term, enforce it.
				boolean addTerm = true;
				if(maxWordsInTerm > -1 && maxWordsInTerm < TextTools.charCount(str,' ')){
					addTerm = false;
				}
				if(addTerm)
					terms.add(TextTools.normalize(str,stem,stripDigits,stripStopWords,true,false));
			}
		}
		return terms;
	}

	/**
	 * check if string is a regular expression
	 * @param s
	 * @return
	 */
	protected boolean isRegExp(String s){
		return s != null && s.startsWith("/") && s.endsWith("/");
	}
	
	
	/**
	 * get all root concepts. This makes sence if Terminology is in fact ontology
	 * that has heirchichal structure
	 * @return
	 */
	public Concept[] getRootConcepts() throws TerminologyException {
		List<Concept> roots = new ArrayList<Concept>();
		for(String code: storage.getRootMap().keySet()){
			Concept c = lookupConcept(code);
			if(c != null)
				roots.add(c);
		}
		return roots.toArray(new Concept [0]);
	}
	

	/**
	 * get related concepts map
	 */
	public Map getRelatedConcepts(Concept c) throws TerminologyException {
		// if we have a class, build the map from it, forget concept
		IClass cls = null; // c.getConceptClass();
		if(cls != null){
			Map<Relation,Concept []> map = new HashMap<Relation,Concept []>();
			map.put(Relation.BROADER,getRelatedConcepts(c,Relation.BROADER));
			map.put(Relation.NARROWER,getRelatedConcepts(c,Relation.NARROWER));
			return map;
		// else see if there is a relation map attached to concept
		}else{
			Map<String,Set<String>> relationMap = c.getRelationMap();
			if(relationMap != null){
				Map<Relation,Concept []> map = new HashMap<Relation,Concept []>();
				for(String key: relationMap.keySet()){
					List<Concept> list = new ArrayList<Concept>();
					for(String cui: relationMap.get(key)){
						Concept con = lookupConcept(cui);
						if(con != null)
							list.add(con);
					}
					map.put(Relation.getRelation(key),list.toArray(new Concept [0]));
				}
				return map;
			}
		}
		// else return an empty map
		return Collections.EMPTY_MAP;
	}
	
	public Concept[] getRelatedConcepts(Concept c, Relation r) throws TerminologyException {
		// if we have a class already, use the ontology
		IClass cls = null; //c.getConceptClass();
		if(cls != null){
			if(r == Relation.BROADER){
				return convertConcepts(cls.getDirectSuperClasses());
			}else if(r == Relation.NARROWER){
				return convertConcepts(cls.getDirectSubClasses());
			}else if(r == Relation.SIMILAR){
				List<IClass> clses = new ArrayList<IClass>();
				for(IClass eq: cls.getEquivalentClasses()){
					if(!eq.isAnonymous()){
						clses.add(eq);
					}
				}
				return convertConcepts(clses);
			}
		// if we don't have a class, use the concept map
		}else if(getRelatedConcepts(c).containsKey(r)){
			return (Concept []) getRelatedConcepts(c).get(r);
		}
		// else return empty list
		return new Concept [0];
	}
	
	private Concept [] convertConcepts(IClass [] clses){
		Concept [] concepts = new Concept[clses.length];
		for(int i=0;i<concepts.length;i++){
			concepts[i] = clses[i].getConcept();
			concepts[i].setCode(getCode(clses[i]));
		}
		return concepts;
	}
	
	private Concept [] convertConcepts(Collection<IClass> clses){
		Concept [] concepts = new Concept[clses.size()];
		int i=0;
		for(IClass cls: clses){
			concepts[i] = cls.getConcept();
			concepts[i].setCode(getCode(cls));
			i++;
		}
		return concepts;
	}
	
	private String getCode(IClass cls){
		return getCode(cls.getConcept().getCode());
	}
	
	private String getCode(String uri){
		if(truncateURI){
			return StringUtils.getAbbreviatedURI(uri);
		}
		return uri;
	}

	public Concept convertConcept(Object obj) {
		if(obj instanceof Concept)
			return (Concept) obj;
		if(obj instanceof Concept.Content){
			Concept.Content c = (Concept.Content)obj;
			return (c.concept == null)?new Concept(c):c.concept;
		}
		if(obj instanceof String || obj instanceof URI){
			try{
				return lookupConcept(""+obj);
			}catch(Exception ex){
				// should not generate one
			}
		}
		return null;
	}
	
	public Concept lookupConcept(String cui) throws TerminologyException {
		Concept c =  convertConcept(storage.getConceptMap().get(cui));
		// try other code mappings
		if(c == null && storage.getCodeMap().containsKey(cui)){
			c =  convertConcept(storage.getConceptMap().get(storage.getCodeMap().get(cui)));
		}
		
		if(c != null){
			c.setTerminology(this);
			c.setInitialized(true);
		}
		return c;
	}
	
	/**
	 * Get all supported relations between concepts
	 */
	public Relation[] getRelations() throws TerminologyException {
		return new Relation [] { Relation.BROADER, Relation.NARROWER, Relation.SIMILAR };
	}

	/**
	 * Get all relations for specific concept, one actually needs to explore
	 * a concept graph (if available) to determine those
	 */
	public Relation[] getRelations(Concept c) throws TerminologyException {
		return getRelations();
	}
	
	
	public Source[] getSourceFilter() {
		return (filteredSources == null)?new Source [0]:filteredSources.toArray(new Source [0]);
	}
	
	public SemanticType[] getSemanticTypeFilter() {
		return (filteredSemanticTypes == null)?new SemanticType [0]:filteredSemanticTypes.toArray(new SemanticType [0]);
	}
	
	public String [] getLanguageFilter() {
		return (filteredLanguages == null)?new String [0]:filteredLanguages.toArray(new String [0]);
	}
	
	public Source[] getSources() {
		if(storage != null && !storage.getSourceMap().isEmpty())
			return storage.getSourceMap().values().toArray(new Source [0]);
		return new Source[]{new Source(getName(),getDescription(),""+getURI())};
	}

	public void setSourceFilter(Source[] srcs) {
		if(srcs == null || srcs.length == 0)
			filteredSources = null;
		else{
			//if(filteredSources == null)
			filteredSources = new LinkedHashSet();
			Collections.addAll(filteredSources, srcs);
		}
	}

	public void setSemanticTypeFilter(SemanticType[] srcs) {
		if(srcs == null || srcs.length == 0)
			filteredSemanticTypes = null;
		else{
		//if(filteredSemanticTypes == null)
			filteredSemanticTypes = new LinkedHashSet();
			Collections.addAll(filteredSemanticTypes, srcs);
		}
	}
	
	public void setLanguageFilter(String [] lang) {
		if(filteredLanguages == null)
			filteredLanguages = new LinkedHashSet();
		Collections.addAll(filteredLanguages, lang);
	}
	
	public void setSelectBestCandidate(boolean selectBestCandidate) {
		this.selectBestCandidate = selectBestCandidate;
		if(selectBestCandidate)
			this.scoreConcepts = selectBestCandidate;
	}

	public void setDefaultSearchMethod(String s){
		this.defaultSearchMethod = s;
	}
	
	public String getDescription() {
		if(storage != null && storage.getInfoMap().containsKey("description"))
			return storage.getInfoMap().get("description");
		return "NobleCoderTerminlogy uses an IndexFinder-like algorithm to map text to concepts.";
	}

	public String getFormat() {
		return "index finder tables";
	}

	public String getLocation() {
		return (location != null)?location.getAbsolutePath():"memory";
	}

	public String getName() {
		if(name != null)
			return name;
		if(storage != null && storage.getInfoMap().containsKey("name"))
			return storage.getInfoMap().get("name");
		if(location != null)
			return location.getName();
		return "NobleCoderTool Terminology";
	}

	public URI getURI() {
		if(storage != null && storage.getInfoMap().containsKey("uri"))
			return URI.create(storage.getInfoMap().get("uri"));
		return URI.create("http://slidetutor.upmc.edu/curriculum/terminolgies/"+getName().replaceAll("\\W+","_"));
	}

	public String getVersion() {
		if(storage != null && storage.getInfoMap().containsKey("version"))
			return storage.getInfoMap().get("version");
		return "1.0";
	}

	public String toString(){
		return getName();
	}
	
	/**
	 * don't try to match common English words
	 * @param ignoreCommonWords
	 */
	public void setIgnoreCommonWords(boolean ignoreCommonWords) {
		this.ignoreCommonWords = ignoreCommonWords;
	}
	
	public double getPartialMatchThreshold() {
		return partialMatchThreshold;
	}

	public void setPartialMatchThreshold(double partialMatchThreshold) {
		this.partialMatchThreshold = partialMatchThreshold;
	}


	
	/**
	 * comput concept match score
	 * @param b
	 */
	public void setScoreConcepts(boolean b) {
		this.scoreConcepts = b;
	}
	
	public boolean isScoreConcepts(){
		return scoreConcepts;
	}

	public boolean isIgnoreDigits() {
		return stripDigits;
	}

	public boolean isIgnoreSmallWords() {
		return ignoreSmallWords;
	}

	public boolean isIgnoreCommonWords() {
		return ignoreCommonWords;
	}

	public boolean isSelectBestCandidate() {
		return selectBestCandidate;
	}
	
	public int getWindowSize() {
		return windowSize;
	}
	
	

	public int getMaximumWordsInTerm() {
		return maxWordsInTerm;
	}

	public void setMaximumWordsInTerm(int maxWordsInTerm) {
		this.maxWordsInTerm = maxWordsInTerm;
	}

	public String getDefaultSearchMethod() {
		return defaultSearchMethod;
	}
	public boolean isIgnoreAcronyms() {
		return ignoreAcronyms;
	}

	public void setIgnoreAcronyms(boolean ignoreAcronyms) {
		this.ignoreAcronyms = ignoreAcronyms;
	}
	public boolean isIgnoreUsedWords() {
		return ignoreUsedWords;
	}

	public void setIgnoreUsedWords(boolean ignoreUsedWords) {
		this.ignoreUsedWords = ignoreUsedWords;
	}

	public boolean isSubsumptionMode() {
		return subsumptionMode;
	}

	public void setSubsumptionMode(boolean subsumptionMode) {
		this.subsumptionMode = subsumptionMode;
	}

	public boolean isOverlapMode() {
		return overlapMode;
	}

	public void setOverlapMode(boolean overlapMode) {
		this.overlapMode = overlapMode;
	}

	public boolean isOrderedMode() {
		return orderedMode;
	}

	public void setOrderedMode(boolean orderedMode) {
		this.orderedMode = orderedMode;
	}

	public boolean isContiguousMode() {
		return contiguousMode;
	}

	public void setContiguousMode(boolean contiguousMode) {
		this.contiguousMode = contiguousMode;
	}

	public boolean isPartialMode() {
		return partialMode;
	}

	public void setPartialMode(boolean partialMode) {
		this.partialMode = partialMode;
	}
	
	public boolean isHandlePossibleAcronyms() {
		return handlePossibleAcronyms;
	}

	public void setHandlePossibleAcronyms(boolean handleProblemTerms) {
		this.handlePossibleAcronyms = handleProblemTerms;
	}

	/**
	 * convert Template to XML DOM object representation
	 * @return
	 */
	public Element toElement(Document doc)  throws TerminologyException{
		Element root = super.toElement(doc);
		Element options = doc.createElement("Options");
		Properties p = getSearchProperties();
		for(Object key: p.keySet()){
			Element opt = doc.createElement("Option");
			opt.setAttribute("name",""+key);
			opt.setAttribute("value",""+p.get(key));
			options.appendChild(opt);
		}
		root.appendChild(options);
		return root;
	}
	
	/**
	 * convert Template to XML DOM object representation
	 * @return
	 */
	public void fromElement(Element element) throws TerminologyException{
		name = element.getAttribute("name");
		String str = element.getAttribute("version");
		if(str.length() > 0)
			storage.getInfoMap().put("version",str);
		str = element.getAttribute("uri");
		if(str.length() > 0)
			storage.getInfoMap().put("uri",str);
		str = element.getAttribute("location");
		if(str.length() > 0)
			storage.getInfoMap().put("location",str);
		
		// get child element
		for(Element e: XMLUtils.getChildElements(element)){
			if("Sources".equals(e.getTagName())){
				for(Element cc: XMLUtils.getElementsByTagName(e,"Source")){
					Source c = new Source("");
					c.fromElement(cc);
					storage.getSourceMap().put(c.getCode(),c);
				}
			}else if("Relations".equals(e.getTagName())){
				//NOOP
			}else if("Languages".equals(e.getTagName())){
				storage.getInfoMap().put("languages",e.getTextContent().trim());
			}else if("Roots".equals(e.getTagName())){
				for(String r: e.getTextContent().trim().split(",")){
					storage.getRootMap().put(r.trim(),"");
				}
			}else if("Description".equals(e.getTagName())){
				storage.getInfoMap().put("description",e.getTextContent().trim());
			}else if("Concepts".equals(e.getTagName())){
				for(Element cc: XMLUtils.getElementsByTagName(e,"Concept")){
					Concept c = new Concept("");
					c.fromElement(cc);
					addConcept(c);
				}
			}else if("Options".equals(e.getTagName())){
				Properties p = new Properties();
				for(Element op: XMLUtils.getElementsByTagName(e,"Option")){
					p.setProperty(op.getAttribute("name"),op.getAttribute("value"));
				}
				setSearchProperties(p);
			}
		}
	}

	/**
	 * process sentence and add Mentions to it
	 */
	
	public Sentence process(Sentence sentence) throws TerminologyException {
		processTime = System.currentTimeMillis();
		
		// setup flags
		setupSearch(getDefaultSearchMethod());
		
		String text = sentence.getText();
		
		
		// split text into words (don't strip digits)
		NormalizedWordsContainer nwc = getNormalizedWordMap(text);
		List<String> words = nwc.normalizedWordsList;
		Map<String,String> normWords = nwc.normalizedWordsMap;
		Set<String> resultTerms = new LinkedHashSet<String>();
		List<Mention> result = new ArrayList<Mention>();
		
		// sort if possible
		
		Set<String> swords = null; //words
		if(ignoreUsedWords){
			swords = new TreeSet<String>(new Comparator<String>() {
				public int compare(String o1, String o2) {
					if(storage.getWordStatMap().containsKey(o1) && storage.getWordStatMap().containsKey(o2)){
						if( storage.getWordStatMap().get(o1).termCount == storage.getWordStatMap().get(o2).termCount){
							return o1.compareTo(o2);
						}
						return storage.getWordStatMap().get(o1).termCount-storage.getWordStatMap().get(o2).termCount;
					}
					if(storage.getWordStatMap().containsKey(o1))
						return -1;
					return 1;
				}
			});
			swords.addAll(words);
		}else{
			swords = new LinkedHashSet<String>(words);
		}
		

		// search regexp
		for(Concept c: searchRegExp(text)){
			if(!isFilteredOut(c)){
				c.setScore(1.0);
				result.addAll(Mention.getMentions(c));
			}
		}
		
		
		// for each word
		Set<String> usedWords = new HashSet<String>();
		Set<String> hashWords = new HashSet<String>(words); // for faster term matching
		int count = 0;
		for(String word : swords){
			count ++;
			
			// filter out junk
			if(ignoreSmallWords && word.length() <= 1)
				continue;
			
			// filter out common words
			if(ignoreCommonWords && TextTools.isCommonWord(word))
				continue;
				
			// if word is already in list of used words
			// save time and go on this time, but re-added for
			// later use in case the word is repeated later on
			if(ignoreUsedWords && usedWords.contains(word)){
				continue;
			}
			
			List<String> textWords = getTextWords(words,count);
			// if textWords is not the same size, regenerate the hash set
			if(words.size() != textWords.size())
				hashWords = new HashSet<String>(textWords);
			
			// select matched terms for a given word
			for(String term: getBestTerms(textWords,hashWords,usedWords,word)){
				resultTerms.add(term);
				if(ignoreUsedWords)
					usedWords.addAll(getUsedWords(textWords,term));
			}
			
		}
		
		
		// now lets remove subsumed terms
		if(subsumptionMode){
			List<String> torem = new ArrayList<String>();
			for(String a: resultTerms){
				for(String b: resultTerms){
					if(a.length() > b.length()){
						List<String> aa = Arrays.asList(a.split(" "));
						List<String> bb = Arrays.asList(b.split(" "));
						if(aa.size() > bb.size() && aa.containsAll(bb)){
							torem.add(b);
						}
					}
				}
			}
			resultTerms.removeAll(torem);
		}
		
		
		// create result list
		//time = System.currentTimeMillis();
		for(String term: resultTerms){
			Set<String> codes = storage.getTermMap().get(term);
			if(codes == null){
				continue;
			}
			// Derive original looking term
			String oterm = getOriginalTerm(text, term, normWords);
		
			// create 
			List<Concept> termConcepts = new ArrayList<Concept>();
			for(String code: codes){
				Concept c = convertConcept(code);
				if(c != null){
					c.setInitialized(true);
				}else{
					c = new Concept(code,term);
				}
				// clone
				c = c.clone();
				c.setTerminology(this);
				c.addMatchedTerm(oterm);
				c.setSearchString(text);
				
				if(ignoreAcronyms && isAcronym(c))
					continue;
			
				// score concepts, based on several parameters
				scoreConcept(c,term,resultTerms);
				
				// filter out really bad ones
				//if(!scoreConcepts || c.getScore() >= 0.5)
				termConcepts.add(c);
			}
			// add to results
			for(Concept c: getBestCandidates(termConcepts)){
				if(!isFilteredOut(c)){
					// if we have multiple annotations, deal with it better
					result.addAll(Mention.getMentions(c,getAnnotations(c,nwc.originalWordsList)));
				}
			}
		}
		// add mentions to Sentence
		sentence.setMentions(result);
		processTime = System.currentTimeMillis() - processTime;
		sentence.getProcessTime().put(getClass().getSimpleName(),processTime);
		return sentence;
	}
	
	public long getProcessTime() {
		return processTime;
	}

	

	/**
	 * 
	 * @param c
	 * @param normalizedTerm
	 */
	
	private void scoreConcept(Concept c, String normalizedTerm, Set<String> resultTerms){
		if(!scoreConcepts)
			return;
		
		// get original text
		boolean singleWord = TextTools.charCount(normalizedTerm,' ') == 0;
		boolean exactMatch = false, caseMatch = false, stemmedMatch = false;
		
		String originalTerm = c.getMatchedTerm();
		String synonymTerm = null;
		
		// assign default weight
		double weight = 1.0;
		
		// if this term subsumes any other in the list, give it 5 points
		if(!singleWord){
			List<String> wt = Arrays.asList(normalizedTerm.split(" "));
			for(String t: resultTerms){
				if(!t.equals(normalizedTerm) && wt.containsAll(Arrays.asList(t.split(" ")))){
					weight += 5.0;
				}
			}
		// if single word, then try to identify matched synonym
		}else{
			for(String s: c.getSynonyms()){
				if(normalizedTerm.equalsIgnoreCase(TextTools.stem(s))){
					synonymTerm = s;
					stemmedMatch = true;
					exactMatch = s.equalsIgnoreCase(originalTerm);
					if(exactMatch)
						caseMatch = s.equals(originalTerm);
					break;
				}
			}
		}
		
		
		
		// if concept contains a synonym that matches some nasty pattern, remove 10 points
		//NOTE: we really want to select an original matched synonym
		/*boolean exactMatch = false, caseMatch = false, synonymLikelyAbbrev = false;
		for(String s: c.getSynonyms()){
			String ss = s;
			s = s.toLowerCase();
			String o = originalTerm.toLowerCase();
			// check if this concept is listed after proposition Ex: melanoma of skin, with skin being original term
			if(!s.startsWith("structure of ") && s.matches("(?i).*\\s+(of|at|in|from)\\s+"+Pattern.quote(o)+".*")){
				weight -= 10;
				break;
			}else 
			// check if it is part of some device ex 123,SKIN,234
			if(s.contains(","+o+",")){
				weight -= 10;
				break;
			}
			if(s.equals(o)){
				exactMatch = true;
				if(ss.equals(originalTerm))
					caseMatch = true;
				else	
					synonymLikelyAbbrev = TextTools.isLikelyAbbreviation(ss);
			}
		}*/
		
		// handle possible acronyms here
		// if matched term is a single word && likely abbreviation && rest of text is not uppercase
		if(singleWord && !caseMatch && ((synonymTerm == null || TextTools.isLikelyAbbreviation(synonymTerm)) ^ TextTools.isLikelyAbbreviation(originalTerm))){
			 StringStats st = TextTools.getStringStats(c.getSearchString());
			 // if input IS NOT mostly uppercase text
			 if(!(st.upperCase > st.lowerCase && st.whiteSpace > 0 && st.length > 5)){
				 weight -= 10;
			 }
		}
	
		// check if we have a normalized match only for a single word that is not plural
		// it is OK to have plural match though
		if(singleWord && !exactMatch && stemmedMatch && !TextTools.isPlural(originalTerm)){
			weight -= 10;
		}
		
		// add small points for more sources (cannot be more then 0.5)
		weight += 0.05*(c.getSources().length>10?10:c.getSources().length);
		
		
		// add some points if exact match to preferred name
		if(c.getName().equalsIgnoreCase(originalTerm))
			weight += 2.0;
		
		
		// now if we have filtered sources add points for source priority
		if(filteredSources != null){
			for(Source s: c.getSources()){
				int n = indexOf(s,filteredSources);
				if(n > 0)
					weight += 1.0/n;
			}
		}
		// now if we have filtered semtypes add points for source priority
		if(filteredSemanticTypes != null){
			for(SemanticType s: c.getSemanticTypes()){
				int n = indexOf(s,filteredSemanticTypes);
				if(n > 0)
					weight += 2.0/n;
			}
		}
		// set score
		c.setScore(weight);
	}
	
	private int indexOf(Object o, Collection list){
		int n = 1;
		for(Object oo: list){
			if(oo.equals(0)){
				return n;
			}
			n++;
		}
		return -1;
	}
	
	
	public static void main(String [] args) throws Exception{
		NobleCoderTerminology t = new NobleCoderTerminology("NCI_Thesaurus_R");
		t.dispose();
		/*;
		System.out.println("compacting .. ");
		ConceptImporter.getInstance().compactTerminology(t);
		System.out.println("done");*/
		/*for(Source s: t.getSources()){
			System.out.println(s.getCode()+" v. "+s.getVersion());
		}
		
		//t.setSelectBestCandidate(false);
		//t.setScoreConcepts(false);
		for(String text : Arrays.asList("The nasal septum deviates to the left with a rather large spur.")){
			//,"It was found at that time that the patient had a third aneurysm and that the MCA aneurysm which was clipped was at the origin of the anterior temporal artery and not the one at the MCA bifurcation.")){
			//"age","sexes","sex","There is a fish under the sea.", "I had a genetic test done using a FISH method.", "WHERE ARE ALL OF THE FISH?", "He has DCIS as a diagnosis", "What about dcis", "skin, hello","SKIN, BACKWORDS")){
			System.out.println("\n\n"+text);
			for(Concept c: t.search(text)){
				System.out.println("matched: "+c.getMatchedTerm()+", score: "+c.getScore());
				c.printInfo(System.out);
			}
		}*/
		
	}

}
