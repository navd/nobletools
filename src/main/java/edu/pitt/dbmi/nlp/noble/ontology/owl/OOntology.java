package edu.pitt.dbmi.nlp.noble.ontology.owl;


import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.coode.owlapi.obo.parser.OBOOntologyFormat;
import org.coode.owlapi.turtle.TurtleOntologyFormat;
import org.semanticweb.owlapi.apibinding.OWLManager;
import org.semanticweb.owlapi.io.OWLXMLOntologyFormat;
import org.semanticweb.owlapi.io.RDFXMLOntologyFormat;
import org.semanticweb.owlapi.model.AddImport;
import org.semanticweb.owlapi.model.AddOntologyAnnotation;
import org.semanticweb.owlapi.model.IRI;
import org.semanticweb.owlapi.model.OWLAnnotation;
import org.semanticweb.owlapi.model.OWLAnnotationProperty;
import org.semanticweb.owlapi.model.OWLAnnotationValue;
import org.semanticweb.owlapi.model.OWLAxiom;
import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLEntity;
import org.semanticweb.owlapi.model.OWLImportsDeclaration;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.model.OWLOntologyCreationException;
import org.semanticweb.owlapi.model.OWLOntologyFormat;
import org.semanticweb.owlapi.model.OWLOntologyIRIMapper;
import org.semanticweb.owlapi.model.OWLOntologyLoaderConfiguration;
import org.semanticweb.owlapi.model.OWLOntologyManager;
import org.semanticweb.owlapi.model.OWLOntologyStorageException;
import org.semanticweb.owlapi.model.PrefixManager;
import org.semanticweb.owlapi.model.RemoveImport;
import org.semanticweb.owlapi.model.RemoveOntologyAnnotation;
import org.semanticweb.owlapi.model.UnloadableImportException;
import org.semanticweb.owlapi.reasoner.OWLReasoner;
import org.semanticweb.owlapi.reasoner.structural.StructuralReasonerFactory;
import org.semanticweb.owlapi.util.DefaultPrefixManager;
import org.semanticweb.owlapi.util.OWLEntityRemover;
import org.semanticweb.owlapi.util.OWLEntityRenamer;
import org.semanticweb.owlapi.util.SimpleIRIMapper;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.ILogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyError;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.IQuery;
import edu.pitt.dbmi.nlp.noble.ontology.IQueryResults;
import edu.pitt.dbmi.nlp.noble.ontology.IRepository;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.ontology.IResourceIterator;
import edu.pitt.dbmi.nlp.noble.ontology.IRestriction;
import edu.pitt.dbmi.nlp.noble.ontology.LogicExpression;
import edu.pitt.dbmi.nlp.noble.ontology.OntologyUtils;
import edu.pitt.dbmi.nlp.noble.ontology.concept.ConceptRegistry;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.util.StringUtils;

/**
 * OWL Ontology implementation
 * @author tseytlin
 *
 */
public class OOntology extends OResource implements IOntology{
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private OWLOntologyManager manager;
	private OWLOntology ontology;
	private OWLDataFactory data;
	private OWLReasoner reasoner;
	private OWLEntityRemover remover;
	private OWLEntityRenamer renamer;
	private OWLOntologyLoaderConfiguration ontologyLoaderConfig;
	private IRepository repository;
	private PrefixManager prefixManager;
	private boolean modified;
	private String location;
	private IRI locationIRI;
	private List<String> languageFilter;
	
	/**
	 * create new ow
	 * @param ont
	 */
	private OOntology(OWLOntology ont){
		super(ont);
		manager = ont.getOWLOntologyManager();
		ontology = ont;
		data = manager.getOWLDataFactory();
		setOntology(this);
		prefixManager = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
		prefixManager = new DefaultPrefixManager(prefixManager);
		ontologyLoaderConfig = new OWLOntologyLoaderConfiguration();
	}
	
	
	public OOntology(String location){
		super(null);
		this.location = location;
	}
	
	
	/**
	 * get prefix manager 
	 * @return
	 */
	PrefixManager getPrefixManager(){
		return prefixManager;
	}
	
	private void lazyLoad(){
		try {
			load();
		} catch (IOntologyException e) {
			throw new IOntologyError("Unable to load ontology "+location,e);
		}
	}
	
	
	public void load() throws IOntologyException {
		if(!isLoaded() && location != null){
			try{
				manager = OWLManager.createOWLOntologyManager();
				File f = new File(location);
				// this is file
				if(f.exists()){
					ontology = manager.loadOntologyFromOntologyDocument(f);
				// this is URL	
				}else if(location.matches("[a-zA-Z]+://(.*)")){
					ontology = manager.loadOntologyFromOntologyDocument(IRI.create(location));
				}
				obj = ontology;
				data = manager.getOWLDataFactory();
				setOntology(this);
				prefixManager = manager.getOntologyFormat(ontology).asPrefixOWLOntologyFormat();
				
			} catch (OWLOntologyCreationException e) {
				throw new IOntologyException("Unable to create ontology "+location,e);
			}
		}
	}
	

	private IRI inferIRI(String loc) {
		if(loc.matches("[a-zA-Z]+://(.*)"))
			return IRI.create(loc);
		File f = new File(location);
		// this is file
		if(f.exists()){
			try {
				String url = null;
				Pattern p = Pattern.compile("(ontologyIRI|xml:base)=\"(.*?)\"");
				BufferedReader r = new BufferedReader(new FileReader(f));
				for(String l = r.readLine(); l != null; l = r.readLine()){
					Matcher m = p.matcher(l);
					if(m.find()){
						url = m.group(2);
						break; 
					}
				}
				r.close();
				return IRI.create(url);
			} catch (FileNotFoundException e) {
				throw new IOntologyError("Error reading ontology from "+location,e);
			} catch (IOException e) {
				throw new IOntologyError("Error reading ontology from "+location,e);
			}
		}
		return null;
	}

	/**
	 * load ontology from file
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static OOntology loadOntology(File file) throws IOntologyException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		for(File f: file.getParentFile().listFiles()){
			if(f.getName().endsWith(".owl") && !file.equals(f)){
				URI uri = null;
				try {
					uri = OntologyUtils.getOntologyURI(f);
				} catch (IOException e) {
					new IOntologyException("Error: unable to extract URI from file "+f,e);
				}
				if(uri != null)
					manager.addIRIMapper(new SimpleIRIMapper(IRI.create(uri), IRI.create(f)));
			}
		}
		try{
			return new OOntology(manager.loadOntologyFromOntologyDocument(file));
		} catch (OWLOntologyCreationException e) {
			throw new IOntologyException("Unable to create ontology "+file,e);
		}
	}
	
	
	/**
	 * load ontology from file
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static OOntology loadOntology(String url) throws IOntologyException {
		File f = new File(url);
		if(f.exists())
			return loadOntology(f);
		if(url.startsWith("http://")){
			try {
				return loadOntology(new URL(url));
			} catch (MalformedURLException e) {
				throw new IOntologyError("This is not a valid URL: "+url,e);
			}
		}
		throw new IOntologyException("Unable to load ontology "+url);
	}
	
	
	/**
	 * create new ontology with this URI
	 * @param uri
	 * @return
	 * @throws Exception
	 */
	public static OOntology createOntology(URI uri) throws IOntologyException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		//manager.addOntologyStorer(new OWLXMLOntologyStorer());
		try{
			return new OOntology(manager.createOntology(IRI.create(uri)));
		} catch (OWLOntologyCreationException e) {
			throw new IOntologyException("Unable to create ontology "+uri,e);
		}
	}
	
	/**
	 * load ontology from uri
	 * 
	 * @param file
	 * @throws Exception
	 */
	public static OOntology loadOntology(URL file) throws IOntologyException {
		OWLOntologyManager manager = OWLManager.createOWLOntologyManager();
		try {
			return new OOntology(manager.loadOntologyFromOntologyDocument(IRI.create(file)));
		} catch (OWLOntologyCreationException e) {
			throw new IOntologyException("Unable to create ontology "+file,e);
		} catch (URISyntaxException e) {
			throw new IOntologyException("Unable to create ontology "+file,e);
		}
	}
	
	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}
	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}
	
	public void addImportedOntology(IOntology o) throws IOntologyException {
		lazyLoad();
		IRI toImport=IRI.create(o.getURI());
		OWLImportsDeclaration importDeclaraton = getOWLDataFactory().getOWLImportsDeclaration(toImport);
		getOWLOntologyManager().applyChange(new AddImport(getOWLOntology(),importDeclaraton));
		try {
			getOWLOntologyManager().makeLoadImportRequest(importDeclaraton,ontologyLoaderConfig);
		} catch (UnloadableImportException e) {
			throw new IOntologyError("Unable to load ontology "+o.getURI(),e);
		}
	}
	public void removeImportedOntology(IOntology o) {
		lazyLoad();
		IRI toImport=IRI.create(o.getURI());
		OWLImportsDeclaration importDeclaraton = getOWLDataFactory().getOWLImportsDeclaration(toImport);
		getOWLOntologyManager().applyChange(new RemoveImport(getOWLOntology(),importDeclaraton));
	}
	public IOntology[] getImportedOntologies() {
		lazyLoad();
		List<IOntology> io = new ArrayList<IOntology>();
		for(OWLOntology o: ontology.getImports()){
			io.add(new OOntology(o));
		}
		return io.toArray(new IOntology [0]);
	}
	public IRepository getRepository() {
		return repository;
	}
	public void setRepository(IRepository r) {
		repository = r;
	}
	
	public IClass createClass(String name) {
		return getRoot().createSubClass(name);
	}
	public IClass createClass(ILogicExpression exp) {
		return (IClass)convertOWLObject((OWLClass)convertOntologyObject(exp));
	}
	public IProperty createProperty(String name, int type) {
		IRI iri = getIRI(name);
		switch(type){
			case(IProperty.OBJECT):
				return getTopObjectProperty().createSubProperty(name);
			case(IProperty.DATATYPE):
				return getTopDataProperty().createSubProperty(name);
			case(IProperty.ANNOTATION_OBJECT):
			case(IProperty.ANNOTATION_DATATYPE):
			case(IProperty.ANNOTATION):
				return (IProperty) convertOWLObject(data.getOWLAnnotationProperty(iri));
		}
		return null;
	}
	public ILogicExpression createLogicExpression(int type, Object param) {
		if(param instanceof Collection)
			return new LogicExpression(type,(Collection) param);
		else if(param instanceof Object [])
			return new LogicExpression(type,(Object []) param);
		else
			return new LogicExpression(type,param);
	}
	public ILogicExpression createLogicExpression() {
		return new LogicExpression(ILogicExpression.EMPTY);
	}
	public IRestriction createRestriction(int type) {
		return new ORestriction(type, getOntology());
	}

	public IQueryResults executeQuery(IQuery iQuery) {
		throw new IOntologyError("Not implemented yet");
	}
	
	public IResourceIterator getMatchingResources(IProperty p, Object value) {
		throw new IOntologyError("Not implemented yet");
	}
	
	public IResourceIterator getMatchingResources(String regex) {
		lazyLoad();
		List<OWLEntity> list = new ArrayList<OWLEntity>();
		for(OWLEntity e: ontology.getSignature(true)){
			Pattern p = Pattern.compile(regex);
			Matcher m = p.matcher(e.getIRI().toString());
			if(m.find()){
				list.add(e);
			}else{
				for(OWLAnnotation t: e.getAnnotations(ontology,getOWLDataFactory().getRDFSComment())){
					m = p.matcher((String)convertOWLObject(t.getValue()));
					if(m.find()){
						list.add(e);
					}
				}
			}
		}
		return  new OResourceIterator(list,this);
	}
	
	
	public IResource getResource(String name) {
		IRI iri = getIRI(name);
		if(ontology.containsClassInSignature(iri,true)){
			return (IClass) convertOWLObject(data.getOWLClass(iri));
		}else if(ontology.containsIndividualInSignature(iri,true)){
			return (IInstance) convertOWLObject(data.getOWLNamedIndividual(iri));
		}else if(ontology.containsAnnotationPropertyInSignature(iri,true)){
			return (IProperty) convertOWLObject(data.getOWLAnnotationProperty(iri));
		}else if(ontology.containsDataPropertyInSignature(iri,true)){
			return (IProperty) convertOWLObject(data.getOWLDataProperty(iri));
		}else if(ontology.containsObjectPropertyInSignature(iri,true)){
			return (IProperty) convertOWLObject(data.getOWLObjectProperty(iri));
		}
		return null;
	}
	public IClass getClass(String name) {
		IRI iri = getIRI(name);
		if(ontology.containsClassInSignature(iri,true))
			return (IClass) convertOWLObject(data.getOWLClass(iri));
		return null;
	}
	public IInstance getInstance(String name) {
		IRI iri = getIRI(name);
		if(ontology.containsIndividualInSignature(iri,true))
			return (IInstance) convertOWLObject(data.getOWLNamedIndividual(iri));
		return null;
	}
	public IProperty getProperty(String name) {
		IRI iri = getIRI(name);
		if(ontology.containsAnnotationPropertyInSignature(iri,true)){
			return (IProperty) convertOWLObject(data.getOWLAnnotationProperty(iri));
		}else if(ontology.containsDataPropertyInSignature(iri,true)){
			return (IProperty) convertOWLObject(data.getOWLDataProperty(iri));
		}else if(ontology.containsObjectPropertyInSignature(iri,true)){
			return (IProperty) convertOWLObject(data.getOWLObjectProperty(iri));
		}
		return null;
	}
	public boolean hasResource(String path) {
		return ontology.containsEntityInSignature(getIRI(path),true);
	}
	public IClass[] getRootClasses() {
		return getRoot().getDirectSubClasses();
	}
	public IClass getRoot() {
		return (IClass) convertOWLObject(data.getOWLThing());
	}
	public IClass getThing() {
		return (IClass) convertOWLObject(data.getOWLThing());
	}
	public IClass getNothing() {
		return (IClass) convertOWLObject(data.getOWLNothing());
	}
	public IProperty getTopObjectProperty() {
		return (IProperty) convertOWLObject(data.getOWLTopObjectProperty());
	}
	public IProperty getTopDataProperty() {
		return (IProperty) convertOWLObject(data.getOWLTopDataProperty());
	}
	public IResourceIterator getAllResources() {
		return  new OResourceIterator(ontology.getSignature(true),this);
	}
	public IResourceIterator getAllProperties() {
		List list = new ArrayList();
		list.addAll(ontology.getDataPropertiesInSignature(true));
		list.addAll(ontology.getObjectPropertiesInSignature(true));
		list.addAll(ontology.getAnnotationPropertiesInSignature());
		return new OResourceIterator(list,this);
		
	}
	public IResourceIterator getAllClasses() {
		return new OResourceIterator(ontology.getClassesInSignature(true),this);
	}
	public boolean isLoaded() {
		return ontology != null;
	}

	public void reload() throws IOntologyException {
		dispose();
		load();
		
	}
	public void flush() {}
	
	public String getNameSpace(){
		return getIRI().toString()+"#";
	}
	public void save() throws IOntologyException {
		modified = false;
		try {
			manager.saveOntology(ontology);
		} catch (OWLOntologyStorageException e) {
			if(e.getCause() instanceof ProtocolException)
				throw new IOntologyException("Unable to save ontology opened from URL "+getIRI()+". You should use IOntology.write() to save it as a file first.",e);
			throw new IOntologyException("Unable to save ontology "+getIRI(),e);
		}
	}
	
	public void write(OutputStream out, int format) throws IOntologyException {
		OWLOntologyFormat ontologyFormat = manager.getOntologyFormat(ontology);
		switch(format){
		case OWL_FORMAT:
			ontologyFormat = new OWLXMLOntologyFormat();break;
		case RDF_FORMAT:
			ontologyFormat = new RDFXMLOntologyFormat();break;
		case NTRIPLE_FORMAT:
			throw new IOntologyException("Unsupported export format");
		case OBO_FORMAT:
			ontologyFormat = new OBOOntologyFormat();break;
		case TURTLE_FORMAT:
			ontologyFormat = new TurtleOntologyFormat();break;
		}
		
		
		try {
			manager.saveOntology(ontology, ontologyFormat, out);
		} catch (OWLOntologyStorageException e) {
			if(e.getCause() instanceof ProtocolException)
				throw new IOntologyException("Unable to save ontology opened from URL "+getIRI()+". You should use IOntology.write() to save it as a file first.",e);
			throw new IOntologyException("Unable to save ontology "+getIRI(),e);
		}
		
	}
	public boolean isModified() {
		return modified;
	}
	
	protected OWLEntityRemover getOWLEntityRemover(){
		if(remover == null){ 
			remover = new OWLEntityRemover(manager,Collections.singleton(ontology));
		}
		return remover;
	}
	
	protected OWLEntityRenamer getOWLEntityRenamer(){
		if(renamer == null){ 
			renamer = new OWLEntityRenamer(manager,Collections.singleton(ontology));
		}
		return renamer;
	}
	
	protected OWLOntology getOWLOntology(){
		return ontology;
	}
	
	protected OWLDataFactory getOWLDataFactory(){
		lazyLoad();
		return data;
	}
	
	protected OWLOntologyManager getOWLOntologyManager(){
		lazyLoad();
		return manager;
	}
	
	protected OWLReasoner getOWLReasoner(){
		if(reasoner == null)
			reasoner = new StructuralReasonerFactory().createReasoner(ontology);
		return reasoner;
	}
	
	
	/**
	 * get appropriate concept for a given class
	 * @param cls
	 * @return
	 */
	public Concept getConcept(IResource cls){
		// lets see if we have any special concept handlers defined
		for(String pt: ConceptRegistry.REGISTRY.keySet()){
			// if regular expression or simple equals
			if((pt.matches("/.*/") && getURI().toString().matches(pt.substring(1,pt.length()-1))) || getURI().toString().startsWith(pt)){
				String className = ConceptRegistry.REGISTRY.get(pt);
				try {
					Class c = Class.forName(className);
					return (Concept) c.getConstructors()[0].newInstance(cls);
				}catch(Exception ex){
					ex.printStackTrace();
					//NOOP, just do default
				}
			}
		}
		return new Concept(cls);
	}
	
	protected IRI getIRI(){
		if(!isLoaded()){
			if(locationIRI == null)
				locationIRI = inferIRI(location);
			return locationIRI;
		}
		return ontology.getOntologyID().getOntologyIRI();
	}
	
	

	/**
	 * convert name of resource to IRI
	 * @param name
	 * @return
	 */
	protected IRI getIRI(String name){
		if(name == null)
			return null;
		// lazy load ontology for most things
		lazyLoad();
		
		//full URI given
		if(name.indexOf("://") > -1)
			return IRI.create(name);
	
		//prefix given
		int of = name.indexOf(":"); 
		if( of > -1){
			String p = prefixManager.getPrefix(name.substring(0,of+1));
			return IRI.create(p+name.substring(of+1));
		}
		// just name is given
		Map<String,String> prefixes = prefixManager.getPrefixName2PrefixMap();
		for(String p: prefixes.keySet()){
			String val = prefixes.get(p);
			if(!p.equals(":") && lookupIRI(val)){
				IRI iri = getIRI(val+name);
				if(ontology.containsEntityInSignature(iri,true))
					return iri; 
			}
		}
		// do we have in one of imports?
		for(OWLOntology o: ontology.getImports()){
			IRI iri = getIRI(o.getOntologyID().getOntologyIRI()+"#"+name);
			if(ontology.containsEntityInSignature(iri,true))
				return iri; 
		}
		
		// use default
		return IRI.create(getNameSpace()+name);
	}
	
	private boolean lookupIRI(String val){
		final String [] generic = new String [] {"w3.org","protege.stanford.edu","purl.org","xsp.owl"};
		for(String s: generic){
			if(val.contains(s))
				return false;
		}
		return true;
	}
	
	public String getDescription() {
		String dsc = "";
		IProperty p = getProperty(IProperty.DC_DESCRIPTION);
		if(p != null)
			dsc = ""+getPropertyValue(p);
		if(dsc.length() == 0) 
			dsc = super.getDescription();
		return dsc;
	}
	
	public String getName() {
		return StringUtils.getOntologyName(getURI(),true);
	}
	
	public Object[] getPropertyValues(IProperty prop) {
		OWLOntology e = getOWLOntology();
		if(e != null){
			Set list = new LinkedHashSet();
			for(OWLAnnotation a: e.getAnnotations()){
				if(a.getProperty().equals(convertOntologyObject(prop))){
					Object oo = convertOWLObject(a.getValue());
					if (oo != null){
						list.add(oo);
					}
				}
			}
			return list.toArray();
		}
		return new Object [0];
	}
	
	public IProperty[] getProperties() {
		OWLOntology e = getOWLOntology();
		if(e != null){
			Set<IProperty> list = new LinkedHashSet<IProperty>();
			for(OWLAnnotation a: e.getAnnotations()){
				list.add((IProperty)convertOWLObject(a.getProperty()));
			}
			return list.toArray(new IProperty [0]);
		}
		return new IProperty [0];
	}
	
	
	public List<String> getLanguageFilter() {
		return languageFilter;
	}


	public void setLanguageFilter(List<String> languageFilter) {
		this.languageFilter = languageFilter;
	}


	protected void addAnnotation(OWLAnnotationProperty prop,String str){
		OWLDataFactory df = getOWLDataFactory();
		OWLAnnotation commentAnno = df.getOWLAnnotation(prop,df.getOWLLiteral(str));
		getOWLOntologyManager().applyChange(new AddOntologyAnnotation(ontology, commentAnno));
	}
	
	protected void removeAnnotation(OWLAnnotationProperty prop,String str){
		OWLDataFactory df = getOWLDataFactory();
		OWLAnnotation commentAnno = df.getOWLAnnotation(prop,df.getOWLLiteral(str));
		getOWLOntologyManager().applyChange(new RemoveOntologyAnnotation(ontology, commentAnno));
	}
}
