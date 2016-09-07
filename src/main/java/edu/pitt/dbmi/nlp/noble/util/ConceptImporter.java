package edu.pitt.dbmi.nlp.noble.util;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IResourceIterator;
import edu.pitt.dbmi.nlp.noble.ontology.OntologyUtils;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Definition;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Term;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology.WordStat;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

import static edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology.*;
/**
 * import an OBO file to a collection of concept objects
 * @author tseytlin
 */
public class ConceptImporter {
	public static final String LOADING_MESSAGE  = "MESSAGE";
	public static final String LOADING_PROGRESS = "PROGRESS";
	public static final String LOADING_TOTAL    = "TOTAL";
	
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private static ConceptImporter instance;
	private boolean inMemory, compact;
	
	
	
	
	/**
	 * get instance
	 * @return
	 */
	public static ConceptImporter getInstance(){
		if(instance == null)
			instance = new ConceptImporter();
		return instance;
	}
	
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	
	public boolean isInMemory() {
		return inMemory;
	}

	public void setInMemory(boolean inMemory) {
		this.inMemory = inMemory;
	}

	public boolean isCompact() {
		return compact;
	}

	public void setCompact(boolean compact) {
		this.compact = compact;
	}

	/**
	 * add concept
	 * @param term
	 * @param c
	 * @throws TerminologyException
	 */
	private void addConcept(NobleCoderTerminology term, Concept c) throws TerminologyException{
		if(term == null || c == null)
			return;
		//pcs.firePropertyChange(LOADING_MESSAGE,null,"importing concept "+c.getName()+" ..");
		term.addConcept(c);
		// if not relations or no breader relations, then it is root
		if(c.getRelationMap() == null || !c.getRelationMap().containsKey(Relation.BROADER))
			term.addRoot(c.getCode());
	}

	
	/**
	 * load OBO file into terminology
	 * @param file
	 * @throws IOException
	 */
	public void loadOBO(NobleCoderTerminology term, File  file) throws IOException, TerminologyException {
		loadOBO(term,Arrays.asList(file), null);
	}
	
	
	/**
	 * load OBO file into terminology
	 * @param file
	 * @throws IOException
	 */
	public void loadOBO(NobleCoderTerminology terminology, List<File> files, String name) throws IOException, TerminologyException {
		if(files == null || files.isEmpty())
			return;
		
		// pick a default name if absent
		if(name == null){
			name = files.get(0).getName();
			if(name.endsWith(".obo"))
				name = name.substring(0,name.length()-4);
		}
		
		// load terminology with name
		terminology.load(name);
		
		// load ontology from a set of files
		for(File file: files){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading "+file.getName()+" ...");
			Map<String,Concept> content = loadOBO(file);
			pcs.firePropertyChange(LOADING_TOTAL,null,content.size());
			int i = 0;
			for(Concept c: content.values()){
				addConcept(terminology, c);
				if(i % 10 == 0)
					pcs.firePropertyChange(LOADING_PROGRESS,null,i++);
			}
		}
		
		if(!compact){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Creating a Blacklist of High Frequency Words ...");
			BlacklistHandler handler = new BlacklistHandler(terminology);
			terminology.getStorage().getBlacklist().putAll(handler.getBlacklist());
		}
		
		// save
		if(!isInMemory()){
			terminology.save();
			terminology.getStorage().getInfoMap().put("status","done");
		}
		
		// compact terminology
		if(compact){
			compact(terminology);
		}
	}
	
	/**
	 * load OBO file into terminology
	 * @param file
	 * @throws IOException
	 */
	public Map<String,Concept> loadOBO(File file) throws IOException, TerminologyException {
		Map<String,Concept> list = new LinkedHashMap<String,Concept>();
		String name = file.getName();
		if(name.endsWith(".obo"))
			name = name.substring(0,name.length()-4);
		Source src = Source.getSource(name);
		
		BufferedReader r = null;
		try{
			r = new BufferedReader(new FileReader(file));
			Concept c = null;
			Pattern p = Pattern.compile("\"(.*)\"\\s*([A-Z_]*)\\s*(.*)?\\[.*\\]");
			for(String l=r.readLine();l != null;l=r.readLine()){
				if("[Term]".equals(l.trim())){
					if(c != null)
						list.put(c.getCode(),c);
					c = new Concept("X");
					c.addSource(src);
				}else if(c != null){
					int i = l.indexOf(':');
					if(i > -1){
						String key = l.substring(0,i).trim();
						String val = l.substring(i+1).trim();
						
						// fill in values
						if("id".equals(key)){
							c.setCode(val);
						}else if("name".equals(key)){
							c.setSynonyms(new String [0]);
							c.setName(val);
							Term t = Term.getTerm(val);
							t.setPreferred(true);
							c.addTerm(t);
						}else if("namespace".equals(key)){
							c.addSemanticType(SemanticType.getSemanticType(val));
						}else if("def".equals(key)){
							Matcher m = p.matcher(val);
							if(m.matches())
								val = m.group(1);
							c.addDefinition(Definition.getDefinition(val));
						}else if(key != null && key.matches("(exact_|narrow_|broad_)?synonym")){
							Matcher m = p.matcher(val);
							String form = null;
							if(m.matches()){
								val = m.group(1);
								form = m.group(2);
							}
							Term t = Term.getTerm(val);
							if(form != null)
								t.setForm(form);
							c.addTerm(t);
						}else if("is_a".equals(key)){
							int j = val.indexOf("!");
							if(j > -1)
								val = val.substring(0,j).trim();
							c.addRelatedConcept(Relation.BROADER,val);
							Concept pr = list.get(val);
							if(pr != null)
								pr.addRelatedConcept(Relation.NARROWER,c.getCode());
						}else if("relationship".equals(key)){
							int j = val.indexOf("!");
							int k = val.indexOf(" ");
							if(k > -1){
								String rel = val.substring(0,k).trim();
								if(j > -1)
									val = val.substring(k,j).trim();
								c.addRelatedConcept(Relation.getRelation(rel),val);
							}
						}else if("is_obsolete".equals(key)){
							if(Boolean.parseBoolean(val)){
								c = null;
							}
						}else if("consider".equals(key)){
							// NOOP only relevant when term is obsolete
						}else if("comment".equals(key)){
							// NOOP only relevant when term is obsolete
						}else if("alt_id".equals(key)){
							c.addCode(val,Source.getSource(""));
						}else if("subset".equals(key)){
							// NOOP, don't know what to do with that
						}else if("xref".equals(key)){
							// NOOP, handle external references
						}
					}
				}else if(l.startsWith("default-namespace:")){
					src.setDescription(l.substring("default-namespace:".length()+1).trim());
				}
			}
			list.put(c.getCode(),c);
		}catch(IOException ex){
			throw ex;
		}finally{
			if(r != null)
				r.close();
		}
		return list;
	}
	
	/**
	 * load index finder tables from an IOntology object
	 * @param ontology
	 * @throws IOException
	 * @throws TerminologyException 
	 */
	public void loadOntology(NobleCoderTerminology term, IOntology ontology) throws IOException, TerminologyException, IOntologyException {
		loadOntology(term,ontology,null);
	}
	

	
	/**
	 * load index finder tables from an IOntology object
	 * @param ontology
	 * @throws IOException
	 * @throws TerminologyException 
	 */
	public void loadOntology(NobleCoderTerminology term, IOntology ontology, String name) throws IOException, TerminologyException, IOntologyException {
		boolean inmemory = isInMemory();
		name = (name != null)?name:ontology.getName();
		
		// clear tables
		if(inmemory)
			term.init();
		else
			term.load(name);
		
		NobleCoderTerminology.Storage storage = term.getStorage();
		
		
		// check if already loaded
		if("done".equals(storage.getInfoMap().get("status")))
			return;
		
		// handle memory nightmare (save when you reach 90%)
		/*
		final NobleCoderTerminology t = term;
		MemoryManager.setMemoryThreshold(new Runnable(){
			public void run() {
				t.crash();
			}
		},0.95);
		*/
		
		// load classes for the very first time
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading Ontology "+ontology.getName()+" from "+ontology.getLocation()+" ...");
		ontology.load();
		
		// save meta information
		storage.getInfoMap().put("name",""+ontology.getName());
		storage.getInfoMap().put("description",""+ontology.getDescription());
		storage.getInfoMap().put("version",""+ontology.getVersion());
		storage.getInfoMap().put("uri",""+ontology.getURI().toASCIIString());
		Source src = Source.getSource(""+ontology.getName());
		src.setDescription(ontology.getDescription());
		storage.getSourceMap().put(ontology.getName(),src);
		
		// get all classes
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Iterating Over Ontology Classes ...");
		IResourceIterator it = ontology.getAllClasses();
		pcs.firePropertyChange(LOADING_TOTAL,null,it.getTotal());
		
		int i = 0;
		while(it.hasNext()){
			i++;
			
			IClass cls = (IClass)it.next();
			if(cls == null)
				continue;
			
			String code = getCode(cls,false);
			if(storage.getConceptMap().containsKey(code))
				continue;
			
			Concept concept = cls.getConcept();
			concept.setCode(code);
			concept.addCode(""+cls.getURI(),Source.URI);
			
			// fix sources
			for(Source sr: concept.getSources())
				sr.setCode(getCode(sr.getCode(),false));
			
			// add relations to concept
			for(IClass c: cls.getDirectSuperClasses()){
				concept.addRelatedConcept(Relation.BROADER,getCode(c,false));
			}
			
			// add relations to concept
			for(IClass c: cls.getDirectSubClasses()){
				concept.addRelatedConcept(Relation.NARROWER,getCode(c,false));
			}
						
			// add concept
			term.addConcept(concept);
			
			// commit ever so often
			if((i % 3000) == 0){
				pcs.firePropertyChange(LOADING_PROGRESS,null,i);
				//save();
			}
			storage.getInfoMap().put("offset",""+i);
		}
		for(IClass r: ontology.getRootClasses())
			storage.getRootMap().put(getCode(r,false),"");
		
		// if need to compact, then compact, else create a blacklist
		if(!compact){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Creating a Blacklist of High Frequency Words ...");
			BlacklistHandler handler = new BlacklistHandler(term);
			storage.getBlacklist().putAll(handler.getBlacklist());
		}
		
		if(!inmemory){
			term.save();
			storage.getInfoMap().put("status","done");
		}
		
		// compact terminology
		if(compact){
			compact(term);
		}
	}

	private String getCode(IClass cls, boolean truncateURI){
		return getCode(cls.getConcept().getCode(),truncateURI);
	}
	
	private String getCode(String uri, boolean truncateURI){
		if(truncateURI){
			return StringUtils.getAbbreviatedURI(uri);
		}
		return uri;
	}
	
	
	/**
	 * load terms file
	 * @param file
	 * @throws Exception
	 */
	public void loadText(NobleCoderTerminology term, File file,String name) throws Exception {
		loadText(term, file, name,null);
	}
	
	
	/**
	 * load terms file
	 * @param file
	 * @throws Exception
	 */
	public void loadText(NobleCoderTerminology term, File file,String name, Terminology meta) throws Exception {
		// get the file name 
		name = (name != null)?name:file.getName();
		
		// strip suffix
		if(name.endsWith(".txt"))
			name = name.substring(0,name.length()-".txt".length());
		
		// initialize the ontology
		IOntology ontology = OOntology.createOntology(URI.create(OntologyUtils.DEFAULT_ONTOLOGY_BASE_URL+name+".owl"));
		ontology.addImportedOntology(OOntology.loadOntology(new URL(OntologyUtils.TERMINOLOGY_CORE)));
		
	
		// load classes for the very first time
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading Text from "+file.getAbsolutePath()+" ...");

		
		// create two root classes
		Stack<IClass> parentStack = new Stack<IClass>();
		parentStack.push(ontology.getRoot());
		String lastLine = null;
		IClass lastClass = null;
		BufferedReader r = new BufferedReader(new FileReader(file));
		for(String line = r.readLine(); line != null; line = r.readLine()){
			if(line.trim().startsWith("#") || line.trim().length() == 0){
				continue;
			}
			
			// figure out hierarchy
			if(lastLine != null){
				int l=  StringUtils.getTabOffset(lastLine);
				int n = StringUtils.getTabOffset(line);
				// this means that this entry is a child of new entry
				if(n > l){
					parentStack.push(lastClass);
				}else if(n < l){
					// else we move back to previous parent
					// depending on the depth
					for(int i=0;!parentStack.isEmpty() && i< (l-n);i++)
						parentStack.pop();
				}
			}
			
			IClass parent = parentStack.peek();
			IClass cls = createClass(line,parent, meta); 
			
			// save for next iteration
			lastClass = cls;
			lastLine = line;
		}
		r.close();
		
		
		// save ontology
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Saving Ontology ...");
		ontology.write(new FileOutputStream(new File(file.getParentFile(),name+".owl")),IOntology.OWL_FORMAT);
		
		// now load as an ontology
		loadOntology(term, ontology);
	}
	
	
	/**
	 * create a class from a given line of text
	 * @param line - String that has a sperated list of synonyms
	 * @param parent
	 * @param meta
	 * @return
	 * @throws TerminologyException 
	 */
	private IClass createClass(String line, IClass parent, Terminology umls) throws TerminologyException {
		// split line into synonyms
		String [] synonyms = line.split("[\\|;,]");
		
		// create class
		String name = synonyms[0].trim();
		IOntology ont = parent.getOntology();
		IClass cls = parent.createSubClass(OntologyUtils.toResourceName(name));
		cls.addLabel(name);
		
		// process other synonyms
		if(synonyms.length > 1){
			for(String txt: synonyms){
				txt = txt.trim();
				
				// prefered term
				if(txt.equals(name)){
					cls.addPropertyValue(OntologyUtils.getOrCreateProperty(ont,OntologyUtils.PREF_TERM),txt);
				}else if(OntologyUtils.isCUI(txt)){
					cls.addPropertyValue(OntologyUtils.getOrCreateProperty(ont,OntologyUtils.ALT_CODE),txt);
				}else if(OntologyUtils.isTUI(txt)){
					cls.addPropertyValue(OntologyUtils.getOrCreateProperty(ont,OntologyUtils.SEM_TYPE),txt);	
				}else{
					cls.addPropertyValue(OntologyUtils.getOrCreateProperty(ont,OntologyUtils.SYNONYM),txt);	
				}
			}
		}
		
		// lookup in metathesaurus
		if(umls != null){
			for(String text: synonyms){
				text = text.trim();
				for(Concept c: umls.search(text)){
					// make sure that concept matches completly
					if(c.getMatchedTerm().equals(text)){
						OntologyUtils.copyConceptToClass(c, cls);
					}
				}
			}
		}
		
		return cls;
	}

	/**
	 * load from RRF files (Rich Release Files)
	 * This is a common distribution method for UMLS and NCI Meta
	 * @param directory that contains MRCONSO.RRF, MRDEF.RRF, MRSTY.RRF etc...
	 * by default uses ALL sources, but only for English language
	 */
	public void loadRRF(NobleCoderTerminology term,File dir) throws FileNotFoundException, IOException, TerminologyException {
		Map<String,List<String>> params = new HashMap<String, List<String>>();
		params.put("languages",Arrays.asList("ENG"));
		loadRRF(term,dir,params);
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
	public void loadRRF(NobleCoderTerminology terminology,File dir,Map<String,List<String>> params) throws FileNotFoundException, IOException, TerminologyException {
		boolean inmemory = isInMemory();
		long time = System.currentTimeMillis();
		
		// get known params
		String name = (params.containsKey("name") && !params.get("name").isEmpty())?params.get("name").get(0):null; 
		List<String> filterLang = params.get("languages");
		List<String> filterSources = params.get("sources");
		List<String> filterSemTypes = params.get("semanticTypes");
		List<String> relationSources = params.get("hierarchySources");
		if(relationSources == null)
			relationSources = filterSources;
		else if(relationSources.size() == 1 && "*".equals(relationSources.get(0)))
			relationSources = null;
		File location = null;
		boolean supressObsoleteTerms = true;
		if(params.containsKey("suppressObsoleteTerms"))
			supressObsoleteTerms = Boolean.parseBoolean(params.get("suppressObsoleteTerms").get(0));
		
		
		// load tables 
		//terminology.load((name != null)?name:dir.getName());
		if(inmemory){
			terminology.init();
			name = (name != null)?name:dir.getName();
			if(name.endsWith(TERM_SUFFIX))
				name = name.substring(0,name.length()-TERM_SUFFIX.length());
			// setup location
			if(name.contains(File.separator))
				location = new File(name+TERM_SUFFIX);
			else
				location = new File(getPersistenceDirectory(),name+TERM_SUFFIX);
			
			// create a directory
			if(!location.exists())
				location.mkdirs();
			
			terminology.setLocation(location);
		}else{
			terminology.load((name != null)?name:dir.getName());
		}
			
		NobleCoderTerminology.Storage storage = terminology.getStorage();
		
		// check if already loaded
		if("done".equals(storage.getInfoMap().get("status")))
			return;
		
		// handle memory nightmare (save when you reach 90%)
		final NobleCoderTerminology t = terminology;
		MemoryManager.setMemoryThreshold(new Runnable(){
			public void run() {
				t.crash();
			}
		},0.95);
		
		// try to extract the name from the directory
		int i=0,offset = 0;
		storage.getInfoMap().put("name",dir.getName());
		Pattern pt = Pattern.compile("([a-zA-Z\\s_]+)[_\\-\\s]+([\\d_]+[A-Z]?)");
		Matcher mt = pt.matcher(dir.getName());
		if(mt.matches()){
			storage.getInfoMap().put("name",mt.group(1));
			storage.getInfoMap().put("version",mt.group(2));
		}
		
		// fill out some more info
		if(filterLang != null){
			String s = filterLang.toString();
			storage.getInfoMap().put("languages",s.substring(1,s.length()-1));
		}
		
		// fill out some more info
		if(filterSemTypes != null){
			String s = filterSemTypes.toString();
			storage.getInfoMap().put("semantic.types",s.substring(1,s.length()-1));
		}

		// fill out some more info
		if(filterSources != null){
			String s = filterSources.toString();
			storage.getInfoMap().put("sources",s.substring(1,s.length()-1));
		}
		
		if(storage.getInfoMap().containsKey("total.terms.per.word"))
			storage.totalTermsPerWord = Integer.parseInt(storage.getInfoMap().get("total.terms.per.word"));
		if(storage.getInfoMap().containsKey("max.terms.per.word"))
			storage.maxTermsPerWord = Integer.parseInt(storage.getInfoMap().get("max.terms.per.word"));
		
		
		// first read in meta information
		Map<String,Integer> rowCount = new HashMap<String, Integer>();
		for(String f: Arrays.asList("MRSAB.RRF","MRCONSO.RRF","MRDEF.RRF","MRSTY.RRF","MRREL.RRF")){
			rowCount.put(f,Integer.MAX_VALUE);
		}
		
		BufferedReader r = null;
		if(new File(dir,"MRFILES.RRF").exists()){
			r = new BufferedReader(new FileReader(new File(dir,"MRFILES.RRF")));
			for(String line = r.readLine(); line != null; line = r.readLine()){
				// parse each line ref: http://www.ncbi.nlm.nih.gov/books/NBK9685/
				String [] fields = line.split("\\|");
				rowCount.put(fields[0].trim(),Integer.parseInt(fields[4]));
			}
			r.close();
		}
		
		// read in source information
		offset = 0;
		String RRFile = "MRSAB.RRF";
		if(storage.getInfoMap().containsKey(RRFile)){
			offset = Integer.parseInt(storage.getInfoMap().get(RRFile));
		}
		if(!new File(dir,RRFile).exists()){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"RRF file "+(new File(dir,RRFile).getAbsolutePath()+" does not exist, sipping .."));
			offset = Integer.MAX_VALUE;
		}
		// if offset is smaller then total, read file
		if(offset < rowCount.get(RRFile)){
			i = 0;
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading "+RRFile+" file ...");
			r = new BufferedReader(new FileReader(new File(dir,RRFile)));
			for(String line = r.readLine(); line != null; line = r.readLine()){
				if(i < offset){
					i++;
					continue;
				}
				//http://www.ncbi.nlm.nih.gov/books/NBK9685/table/ch03.T.source_information_file__mrsabrrf/?report=objectonly
				String [] fields = line.split("\\|");
				String code = fields[3].trim();
				String desc = fields[4].trim();
				String ver = fields[6].trim();
				String nm = fields[23];
				
				Source src = new Source(code);
				src.setDescription(desc); // fields.length-1
				src.setVersion(ver);
				src.setName(nm);
				if(filterSources == null || filterSources.contains(src.getCode())){
					storage.getSourceMap().put(src.getCode(),src);
				}
				i++;
				storage.getInfoMap().put(RRFile,""+i);
			}
			r.close();
		}else{
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Skipping "+RRFile+" file ...");
		}
			
		
		// save meta information
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Saving Meta Information ...");
		storage.commit(storage.getInfoMap());
		storage.commit(storage.getSourceMap());
		
		// if filtering by semantic types, lets preload a list of kosher CUIs
		Set<String> filteredCUIs = new HashSet<String>();
		if(filterSemTypes != null && !filterSemTypes.isEmpty()){
			boolean checkTUIs = filterSemTypes.get(0).matches("T[0-9]+");
			// go over semantic types
			RRFile = "MRSTY.RRF";
			if(!new File(dir,RRFile).exists()){
				pcs.firePropertyChange(LOADING_MESSAGE,null,"RRF file "+(new File(dir,RRFile).getAbsolutePath()+" does not exist, sipping .."));
				offset = Integer.MAX_VALUE;
			}
			// if offset is smaller then total, read file
			i=0;
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading "+RRFile+" file ...");
			pcs.firePropertyChange(LOADING_TOTAL,null,rowCount.get(RRFile));
			r = new BufferedReader(new FileReader(new File(dir,RRFile)));
			for(String line = r.readLine(); line != null; line = r.readLine()){
				// parse each line ref: http://www.ncbi.nlm.nih.gov/books/NBK9685/table/ch03.T.definitions_file__mrdefrrf/?report=objectonly
				String [] fields = line.split("\\|");
				if(fields.length >= 2 ){
					String cui = fields[0].trim();
					String tui = fields[1].trim();
					String text = (fields.length>2)?fields[3].trim():"";
					
					// get concept from map
					SemanticType st = SemanticType.getSemanticType(text,tui);
					if(filterSemTypes.contains(checkTUIs?st.getCode():st.getName()))
						filteredCUIs.add(cui);
				}
			}
			r.close();
			if(filteredCUIs.isEmpty()){
				pcs.firePropertyChange(LOADING_MESSAGE,null,"Error: Could not find any concepts matching semantic type filter");
				return;
			}
		}
		
		// lets first build a map of concepts using existing concept map
		Set<String> rootCUIs = new LinkedHashSet<String>();
		int rowcount = 0,step;
		storage.useTempWordFolder = true;
		String prefNameSource = null;
		offset = 0;
		RRFile = "MRCONSO.RRF";
		if(!new File(dir,RRFile).exists())
			throw new TerminologyException("RRF file "+(new File(dir,RRFile).getAbsolutePath()+" does not exist!"));
		
		if(storage.getInfoMap().containsKey(RRFile)){
			offset = Integer.parseInt(storage.getInfoMap().get(RRFile));
		}
		// if offset is smaller then total, read file
		if(offset < rowCount.get(RRFile)){
			i = 0;
			rowcount = rowCount.get(RRFile);
			step = rowcount/100;
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading "+RRFile+" file ...");
			pcs.firePropertyChange(LOADING_TOTAL,null,rowcount);
			r = new BufferedReader(new FileReader(new File(dir,RRFile)));
			Concept previousConcept = null;
			//boolean crash = false;
			for(String line = r.readLine(); line != null; line = r.readLine()){
				if(i < offset){
					i++;
					continue;
				}
				// parse each line ref: http://www.ncbi.nlm.nih.gov/books/NBK9685/table/ch03.T.concept_names_and_sources_file__m/?report=objectonly
				String [] fields = line.split("\\|");
				if(fields.length >= 14 ){
					String cui = fields[0].trim();
					String ts =  fields[2].trim();
					String src  = fields[11].trim();
					String text = fields[14].trim();
					String lang = fields[1].trim();
					String form = fields[12].trim();
					String code = fields[13].trim();
					String pref = fields[6].trim();
					String sup  = fields[16].trim();
					
					Source source = Source.getSource(src);
					
					// display progress bar
					if((i % step) == 0){
						pcs.firePropertyChange(LOADING_PROGRESS,null,i);
						storage.commit(storage.getInfoMap());
						storage.commit(storage.getTermMap());
						storage.commit(storage.getRegexMap());
						storage.commit(storage.getConceptMap());
						/*if(i > 0 && i % 500000 == 0){
							crash = true;
						}*/
					}
					i++;
					
					// filter out by language
					if(!isIncluded(filterLang,lang))
						continue;
					
					// add a root candidate
					if("SRC".equals(src) && code.startsWith("V-"))
						rootCUIs.add(cui);
					
					// filter out by source
					if(!isIncluded(filterSources,src)){
						if(!(code.startsWith("V-") && isIncluded(filterSources,code.substring(2)))){
							continue;
						}
					}
					
					// filter out by semantic types (except if it is a root)
					if(filterSemTypes != null && !filteredCUIs.contains(cui)){
						if(!(code.startsWith("V-") && isIncluded(filterSources,code.substring(2)))){
							continue;
						}
					}
					
					// honor suppress flag
					if(supressObsoleteTerms && "O".equals(sup))
						continue;
					
					// get concept from map
					Concept c = terminology.convertConcept(storage.getConceptMap().get(cui));
					if(c == null){
						// if concept is not in map, see if previous is it
						if(previousConcept != null && previousConcept.getCode().equals(cui)){
							c = previousConcept;
						}else{
							c = new Concept(cui,text);
							prefNameSource = null;
						}
					}
					
					// create a term
					Term term = new Term(text);
					term.setForm(form);
					term.setLanguage(lang);
					term.setSource(source);
					if("y".equalsIgnoreCase(pref) && "P".equalsIgnoreCase(ts))
						term.setPreferred(true);
					
					// add to concept
					c.addSynonym(text);
					c.addSource(source);
					c.addTerm(term);
					c.addCode(code, source);
					
					// set preferred name for the first time
					if(term.isPreferred()){
						// if prefered name source is not set OR
						// we have filtering and the new source offset is less then old source offset (which means higher priority)
						if(prefNameSource == null || (filterSources != null && filterSources.indexOf(src) < filterSources.indexOf(prefNameSource))){
							c.setName(text);
							prefNameSource = src;
							
						}
					}
					term = null;
					
					// now see if we pretty much got the entire concept and should put it in
					if(previousConcept != null && !previousConcept.getCode().equals(cui)){
						terminology.addConcept(previousConcept);
						storage.getInfoMap().put("max.terms.per.word",""+storage.maxTermsPerWord);
						storage.getInfoMap().put("total.terms.per.word",""+storage.totalTermsPerWord);
						/*if(crash)
							crash();*/
					}
					previousConcept = c;
				}
				storage.getInfoMap().put(RRFile,""+i);
			
			}
			// save last one
			if(previousConcept != null)
				terminology.addConcept(previousConcept);
			r.close();
		}else{
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Skipping "+RRFile+" file ...");
		}
		
		// commit info terms and regex
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Saving Term Information ...");
		storage.commit(storage.getInfoMap());
		storage.commit(storage.getTermMap());
		storage.commit(storage.getRegexMap());
		storage.commit(storage.getConceptMap());

		// now do temp word dir
		File tempDir = new File(storage.getLocation(),NobleCoderTerminology.TEMP_WORD_DIR);
		if(storage.useTempWordFolder && tempDir.exists()){
			storage.useTempWordFolder = false;
			File [] files = tempDir.listFiles();
			offset = 0;
			RRFile = NobleCoderTerminology.TEMP_WORD_DIR;
			if(storage.getInfoMap().containsKey(RRFile)){
				offset = Integer.parseInt(storage.getInfoMap().get(RRFile));
			}
			// if offset is smaller then total, read file
			if(offset < files.length){
				pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading temporary word files ...");
				pcs.firePropertyChange(LOADING_TOTAL,null,files.length);
				i = 0;
				for(File f: files){
					if(i < offset){
						i++;
						continue;
					}
					// display progress bar
					if((i % (files.length/100)) == 0){
						pcs.firePropertyChange(LOADING_PROGRESS,null,i);
					}
					i++;
					
					//load file content
					String word = f.getName();
					Set<String> terms = new HashSet<String>();
					BufferedReader rd = new BufferedReader(new FileReader(f));
					for(String l = rd.readLine();l != null; l = rd.readLine()){
						terms.add(l.trim());
					}
					rd.close();
				
					// set words
					saveWordTerms(storage,word,terms);
					storage.getInfoMap().put(RRFile,""+i);
				}
			}else{
				pcs.firePropertyChange(LOADING_MESSAGE,null,"Skipping "+RRFile+" file ...");
			}
		}
		
		// save some meta information
		storage.getInfoMap().put("word.count",""+storage.getWordMap().size());
		storage.getInfoMap().put("term.count",""+storage.getTermMap().size());
		storage.getInfoMap().put("concept.count",""+storage.getConceptMap().size());
		if(!storage.getWordMap().isEmpty())
			storage.getInfoMap().put("average.terms.per.word",""+storage.totalTermsPerWord/storage.getWordMap().size());
		storage.getInfoMap().put("max.terms.per.word",""+storage.maxTermsPerWord);
		
		// good time to save term info
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Saving Word Information ...");
		
		storage.commit(storage.getInfoMap());
		storage.commit(storage.getWordMap());
		storage.commit(storage.getWordStatMap());
		
		// lets go over definitions
		offset = 0;
		RRFile = "MRDEF.RRF";
		if(storage.getInfoMap().containsKey(RRFile)){
			offset = Integer.parseInt(storage.getInfoMap().get(RRFile));
		}
		
		if(!new File(dir,RRFile).exists()){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"RRF file "+(new File(dir,RRFile).getAbsolutePath()+" does not exist, sipping .."));
			offset = Integer.MAX_VALUE;
		}
			
		// if offset is smaller then total, read file
		if(offset < rowCount.get(RRFile)){
			i = 0;
			rowcount = rowCount.get(RRFile);
			step = rowcount/100;
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading "+RRFile+" file ...");
			pcs.firePropertyChange(LOADING_TOTAL,null,rowcount);
			r = new BufferedReader(new FileReader(new File(dir,RRFile)));
			for(String line = r.readLine(); line != null; line = r.readLine()){
				if(i < offset){
					i++;
					continue;
				}
				// parse each line ref: http://www.ncbi.nlm.nih.gov/books/NBK9685/table/ch03.T.definitions_file__mrdefrrf/?report=objectonly
				String [] fields = line.split("\\|");
				if(fields.length >= 5 ){
					String cui = fields[0].trim();
					String src = fields[4].trim();
					String text = fields[5].trim();
					
					Definition d = Definition.getDefinition(text);
					d.setSource(Source.getSource(src));
					
					// get concept from map
					Concept c = terminology.convertConcept(storage.getConceptMap().get(cui));
					if(c != null){
						c.addDefinition(d);
						// replace with new concept
						storage.getConceptMap().put(cui,c.getContent());
					}
					if((i % step) == 0)
						pcs.firePropertyChange(LOADING_PROGRESS,null,i);
				}
				i++;
				storage.getInfoMap().put(RRFile,""+i);
			}
			r.close();
		}else{
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Skipping "+RRFile+" file ...");
		}
		
		// go over semantic types
		offset = 0;
		RRFile = "MRSTY.RRF";
		if(storage.getInfoMap().containsKey(RRFile)){
			offset = Integer.parseInt(storage.getInfoMap().get(RRFile));
		}
		if(!new File(dir,RRFile).exists()){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"RRF file "+(new File(dir,RRFile).getAbsolutePath()+" does not exist, sipping .."));
			offset = Integer.MAX_VALUE;
		}
		// if offset is smaller then total, read file
		if(offset < rowCount.get(RRFile)){
			i=0;
			rowcount = rowCount.get(RRFile);
			step = rowcount/100;
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading "+RRFile+" file ...");
			pcs.firePropertyChange(LOADING_TOTAL,null,rowcount);
			r = new BufferedReader(new FileReader(new File(dir,RRFile)));
			for(String line = r.readLine(); line != null; line = r.readLine()){
				if(i < offset){
					i++;
					continue;
				}
				// parse each line ref: http://www.ncbi.nlm.nih.gov/books/NBK9685/table/ch03.T.definitions_file__mrdefrrf/?report=objectonly
				String [] fields = line.split("\\|");
				if(fields.length >= 2 ){
					String cui = fields[0].trim();
					String tui = fields[1].trim();
					String text = (fields.length>2)?fields[3].trim():"";
					
					// get concept from map
					Concept c = terminology.convertConcept(storage.getConceptMap().get(cui));
					if(c != null){
						c.addSemanticType(SemanticType.getSemanticType(text,tui));
						// replace with new concept
						storage.getConceptMap().put(cui,c.getContent());
					}
				}
				if((i % step) == 0)
					pcs.firePropertyChange(LOADING_PROGRESS,null,i);
				i++;
				storage.getInfoMap().put(RRFile,""+i);
			}
			r.close();
		}else{
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Skipping "+RRFile+" file ...");
		}
		
		//process relationships?
		offset = 0;
		RRFile = "MRREL.RRF";
		if(storage.getInfoMap().containsKey(RRFile)){
			offset = Integer.parseInt(storage.getInfoMap().get(RRFile));
		}
		if(!new File(dir,RRFile).exists()){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"RRF file "+(new File(dir,RRFile).getAbsolutePath()+" does not exist, sipping .."));
			offset = Integer.MAX_VALUE;
		}
		// if offset is smaller then total, read file
		if(offset < rowCount.get(RRFile)){
			i=0;
			rowcount = rowCount.get(RRFile);
			step = rowcount/100;
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading "+RRFile+" file ...");
			pcs.firePropertyChange(LOADING_TOTAL,null,rowcount);
			r = new BufferedReader(new FileReader(new File(dir,RRFile)));
			List<String> filterRelations = Arrays.asList("RB","RN","PAR","CHD");
			//Concept previousConcept = null;
			for(String line = r.readLine(); line != null; line = r.readLine()){
				if(i < offset){
					i++;
					continue;
				}
				// parse each line ref: http://www.ncbi.nlm.nih.gov/books/NBK9685/table/ch03.T.definitions_file__mrdefrrf/?report=objectonly
				String [] fields = line.split("\\|");
				if(fields.length >= 5 ){
					String cui1 = fields[0].trim();
					String cui2 = fields[4].trim();
					String rel = fields[3].trim();
					String src = fields[10].trim();
					
					// filter by known source if
					if(!isIncluded(relationSources,src,true) && !"SRC".equals(src))
						continue;
					
					// filter by known relationship
					if(filterRelations.contains(rel) && !cui1.equals(cui2)){
						Relation re = null;
						Relation ire = null;
						if("RB".equals(rel) || "PAR".equals(rel)){
							re = Relation.BROADER;
							ire = Relation.NARROWER;
						}else if("RN".equals(rel) || "CHD".equals(rel)){
							re = Relation.NARROWER;
							ire = Relation.BROADER;
						}
						
						// get concept from map
						Concept c1 = terminology.convertConcept(storage.getConceptMap().get(cui1));
						if(c1 != null && re != null){
							Concept c2 = terminology.convertConcept(storage.getConceptMap().get(cui2));
							if(c2 != null){
								// if there is SRC to SRC mapping, skip it
								boolean s1 = c1.getSources().length == 1 && "SRC".equals(c1.getSources()[0].getCode());
								boolean s2 = c2.getSources().length == 1 && "SRC".equals(c2.getSources()[0].getCode());
								// skip mappings between SRC and SRC, since they are useless
								if(!(s1 && s2)){
									// replace with new concept on the source
									c1.addRelatedConcept(re,cui2);
									storage.getConceptMap().put(cui1,c1.getContent());
									// replace with new concept on destination
									c2.addRelatedConcept(ire, cui1);
									storage.getConceptMap().put(cui2,c2.getContent());
								}
							}
						}
					}	
				}
				if((i % step) == 0)
					pcs.firePropertyChange(LOADING_PROGRESS,null,i);
				i++;
				storage.getInfoMap().put(RRFile,""+i);
			}
			r.close();
		}else{
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Skipping "+RRFile+" file ...");
		}
		
		
		// try to create root table, by going over all concepts
		offset = 0;
		i = 0;
		RRFile = "ROOTS";
		if(storage.getInfoMap().containsKey(RRFile)){
			offset = Integer.parseInt(storage.getInfoMap().get(RRFile));
		}
		if(offset < storage.getConceptMap().size()){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Finding Root Concepts ...");
			rowcount = rootCUIs.size();
			step = 100;
			for(String key : rootCUIs){
				if(i < offset){
					i++;
					continue;
				}
				Concept.Content c = storage.getConceptMap().get(key);
				if(c != null && c.relationMap != null && c.relationMap.containsKey(Relation.NARROWER)){
					storage.getRootMap().put(c.code,"");
				}
				/*//anything that doesn't have brader concepts AND is from SRC terminology
				if(c.relationMap != null && c.relationMap.containsKey(Relation.NARROWER) && !c.relationMap.containsKey(Relation.BROADER)){
					// is it a source?
					boolean issource = false;
					for(Source s: c.sources)
						if(s.getName().equals("SRC"))
							issource = true;
					if(issource)
						storage.getRootMap().put(c.code,"");
				}*/
				if((i % step) == 0)
					pcs.firePropertyChange(LOADING_PROGRESS,null,i);
				i++;
				storage.getInfoMap().put(RRFile,""+i);
			}
		}else{
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Skipping Root Inference ...");
		}
		
		// generate blacklist
		if(!compact){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Creating a Blacklist of High Frequency Words ...");
			BlacklistHandler handler = new BlacklistHandler(terminology);
			storage.getBlacklist().putAll(handler.getBlacklist());
		}
		
		
		// last save
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Saving Concept Information ...");
		storage.getInfoMap().put("status","done");
		terminology.save();
		
				
		// remove temp word files 
		if(tempDir.exists()){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Deleting Temporary Files ...");
			for(File f: tempDir.listFiles()){
				f.delete();
			}
			tempDir.delete();
		}
		
		
		// compact terminology
		if(compact){
			compact(terminology);
		}

		// serialize boject if appropriate
		/*if(inmemory){
			File f = new File(location,NobleCoderTerminology.MEM_FILE);
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Saving Serialized Object to ... "+f.getAbsolutePath());
			storage.saveObject(f);
		}*/
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Total Load Time: "+(System.currentTimeMillis()-time)/60000.0+" minutes");
	}
	
	
	
	private boolean isIncluded(List<String> list, String src){
		return isIncluded(list,src,true);
	}
	private boolean isIncluded(List<String> list, String src,boolean strict){
		if(list == null)
			return true;
		
		// filter by known source if
		if(strict)
			return list.contains(src);
		
		// else look at substring
		for(String s: list){
			if(src.contains(s))
				return true;
		}	
		
		return false;
	}
	
	private String getRarestWord(Storage storage, String term){
		String rarest = null;
		int rarestTermCount = Integer.MAX_VALUE;
		for(String word: TextTools.getWords(term)){
			WordStat st = storage.getWordStatMap().get(word);
			int i = (st != null)?st.termCount:Integer.MAX_VALUE;
			if( i < rarestTermCount){
				rarest = word;
				rarestTermCount = i;
			}
		}
		return rarest;
	}
	
	private void saveTemporaryTermFile(File location, String word, Collection<String> termList) throws IOException{
		if(word == null)
			return;
		// if windows OS, check for some speccial files
		if(System.getProperty("os.name").toLowerCase().startsWith("win")){
			if(Arrays.asList("con","prn").contains(word))
				return;
		}
		
		File d = new File(location,TEMP_WORD_DIR);
		if(!d.exists())
			d.mkdirs();
		File f = new File(d,word);
		BufferedWriter w = new BufferedWriter(new FileWriter(f,true));
		for(String t: termList){
			w.write(t+"\n");
		}
		w.close();
	}
	
	
	/**
	 * add entry to word table
	 * @param word
	 * @param terms
	 */
	public void saveWordTerms(NobleCoderTerminology.Storage storage, String word,Set<String> terms){
		//TODO: it would be more efficient to do the rare-word index feature here instead of using compact function after import
		
		// filter terms to only include those that contain a given word
		File location = storage.getLocation();
		Set<String> termList = filterTerms(word,terms);
		
		// if in temp word folder mode, save in temp directory instead of map
		if(storage.useTempWordFolder && location != null && location.exists()){
			try {
				saveTemporaryTermFile(location, word, termList);
			} catch (IOException e) {
				pcs.firePropertyChange(LOADING_MESSAGE,null,"Warning: failed to create file \""+word+"\", reason: "+e.getMessage());
			}
		// else do the normal save to MAP	
		}else{
			if(storage.getWordMap().containsKey(word)){
				termList.addAll(storage.getWordMap().get(word));
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
	
	
	
	/**
	 * compact terminology 
	 * @param term
	 */
	public void compact(NobleCoderTerminology terminology) throws IOException{
		NobleCoderTerminology.Storage storage = terminology.getStorage();
		
		int n = storage.getTermMap().size();
		
		// first create a temporary term files
		storage.useTempWordFolder = true;
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Saving terms as files ...");
		pcs.firePropertyChange(LOADING_TOTAL,null,storage.getTermMap().size());
		
		int i=0;
		for(String term:  storage.getTermMap().keySet()){
			// get rarest word
			String word = getRarestWord(storage, term);
			saveTemporaryTermFile(storage.getLocation(), word,Arrays.asList(term));
			// progress bar
			if((i % (n/100)) == 0){
				pcs.firePropertyChange(LOADING_PROGRESS,null,i);
			}
			i++;
		}
		
		// re shuffle wordTable table
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Backing up original tables ...");
		File location = storage.getLocation();
		storage.dispose();
		for(File f: location.listFiles()){
			if(f.getName().startsWith("table_wordMap")){
				f.renameTo(new File(f.getAbsolutePath()+".backup"));
			}else if(f.getName().startsWith("table_blacklist")){
				f.renameTo(new File(f.getAbsolutePath()+".backup"));
			}
		}
		storage.load(location, false);
		
		
		// reload the word map file
		pcs.firePropertyChange(LOADING_MESSAGE,null,"Loading terms into datastructure ...");
		storage.useTempWordFolder = false;
		File tempDir = new File(location,NobleCoderTerminology.TEMP_WORD_DIR);
		File [] fileList = tempDir.listFiles();
		pcs.firePropertyChange(LOADING_TOTAL,null,fileList.length);
		i = 0;
		for(File f: fileList){
				
			//load file content
			String word = f.getName();
			Set<String> terms = new HashSet<String>();
			BufferedReader rd = new BufferedReader(new FileReader(f));
			for(String l = rd.readLine();l != null; l = rd.readLine()){
				terms.add(l.trim());
			}
			rd.close();
			
			// set words
			saveWordTerms(storage,word,terms);

			
			// progress bar
			if((i % (n/100)) == 0){
				pcs.firePropertyChange(LOADING_PROGRESS,null,i);
			}
			i++;
		}
		// check the fact that it has been compacted
		storage.getInfoMap().put("compacted", "true");
		
		// remove temp word files 
		if(tempDir.exists()){
			pcs.firePropertyChange(LOADING_MESSAGE,null,"Deleting Temporary Files ...");
			for(File f: tempDir.listFiles()){
				f.delete();
			}
			tempDir.delete();
		}
		
		storage.save();
	
	}
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		/*File dir = new File("/home/tseytlin/Data/Coropora/craft-1.0/ontologies");
		//File out = new File("/home/tseytlin/Data/Coropora/craft-1.0/terminologies");
		File out = new File("/home/tseytlin/Data/Coropora/CRAFT_ORF_update");
		//File out = new File("/home/tseytlin/Data/Coropora/CRAFT_RRF_update");
		for(File file: dir.listFiles()){
			if(file.getName().endsWith(".obo")){
				System.out.println("converting "+file.getName()+" ..");
				Collection<Concept> concepts = ConceptImporter.getInstance().loadOBO(file).values();
				//ConceptExporter.getInstances().exportRRF(concepts,out);
				ConceptExporter.getInstances().exportORF(concepts,out);
			}
		}*/

	}

}
