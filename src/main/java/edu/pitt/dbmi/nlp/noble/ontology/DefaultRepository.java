package edu.pitt.dbmi.nlp.noble.ontology;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OReasoner;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;

public class DefaultRepository implements IRepository{
	public static final File DEFAULT_TERMINOLOGY_LOCATION = new File(System.getProperty("user.home")+File.separator+".noble"+File.separator+"terminologies");
	public static final File DEFAULT_ONTOLOGY_LOCATION = new File(System.getProperty("user.home")+File.separator+".noble"+File.separator+"ontologies");
	
	
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private Map<URI,IOntology> ontologies; 
	private Map<String,Terminology> terminologies ; 
	private File terminologyLocation,ontologyLocation;
	
	public DefaultRepository(){
		terminologyLocation = DEFAULT_TERMINOLOGY_LOCATION;
		ontologyLocation = DEFAULT_ONTOLOGY_LOCATION;
	}
	
	
	public File getTerminologyLocation() {
		return terminologyLocation;
	}


	public void setTerminologyLocation(File terminologyLocation) {
		this.terminologyLocation = terminologyLocation;
		NobleCoderTerminology.setPersistenceDirectory(terminologyLocation);
	}


	public File getOntologyLocation() {
		return ontologyLocation;
	}


	public void setOntologyLocation(File ontologyLocation) {
		this.ontologyLocation = ontologyLocation;
	}


	public void addOntology(IOntology ontology) {
		ontologies.put(ontology.getURI(),ontology);
	}

	public void addPropertyChangeListener(PropertyChangeListener listener) {
		pcs.addPropertyChangeListener(listener);
	}

	public void addTerminology(Terminology terminology) {
		terminologies.put(terminology.getName(),terminology);
	}

	public IOntology createOntology(URI path) throws IOntologyException {
		return OOntology.createOntology(path);
	}

	public void exportOntology(IOntology ont, int format, OutputStream out) throws IOntologyException {
		ont.write(out,format);
	}

	public String getDescription() {
		return "OWL Ontology and NOBLE Terminology Repository.";
	}

	public String getName() {
		return "OWL Ontology Repository";
	}

	public IOntology[] getOntologies() {
		if(ontologies == null){
			ontologies = new HashMap<URI, IOntology>();
			File dir = ontologyLocation;
			if(!dir.exists())
				dir.mkdirs();
			for(File f: dir.listFiles()){
				if(f.getName().endsWith("*.owl")){
					addOntology(new OOntology(f.getAbsolutePath()));
				}
			}
			
		}
		return ontologies.values().toArray(new IOntology [0]);
	}

	public IOntology[] getOntologies(String name) {
		List<IOntology> list = new ArrayList<IOntology>();
		for(URI key: ontologies.keySet()){
			if(key.toString().contains(name)){
				list.add(ontologies.get(key));
			}
		}
		return list.toArray(new IOntology [0]);
	}

	public IOntology getOntology(URI name) {
		return ontologies.get(name);
	}

	public IReasoner getReasoner(IOntology ont) {
		if(ont instanceof OOntology){
			return new OReasoner((OOntology)ont);
		}
		return null;
	}

	/**
	 * convinience method
	 * get resource from one of the loaded ontologies
	 * @param path - input uri
	 * @return resource or null if resource was not found
	 */
	public IResource getResource(URI path){
		String uri = ""+path;
		int i = uri.lastIndexOf("#");
		uri = (i > -1)?uri.substring(0,i):uri;
		// get ontology
		IOntology ont = getOntology(URI.create(uri));
		
		// if ontology is all you want, fine Girish
		if(i == -1)
			return ont;
		// 
		if(ont != null){
			return ont.getResource(""+path);
		}
		return null;
	}
	

	public Terminology[] getTerminologies() {
		if(terminologies == null){
			terminologies = new HashMap<String, Terminology>();
			File dir = terminologyLocation;
			if(!dir.exists())
				dir.mkdirs();
			for(File f: dir.listFiles()){
				String sf = NobleCoderTerminology.TERM_SUFFIX;
				if(f.getName().endsWith(sf)){
					Terminology t;
					try {
						String name = f.getName().substring(0,f.getName().length()-sf.length());
						t = new NobleCoderTerminology(name);
						/*
						((NobleCoderTerminology)t).load(name,false);
						((NobleCoderTerminology)t).save();*/
						
						terminologies.put(t.getName(),t);
					} catch (UnsupportedOperationException e){
						System.err.println("Corrupted termonology detected at "+f.getAbsolutePath()+". skipping ...");
						//e.printStackTrace();
					//} catch (Error e){
					//	System.err.println("Corrupted termonology detected at "+f.getAbsolutePath()+". skipping ...");
					//	e.printStackTrace();
					} catch (Exception e) {
						System.err.println("Corrupted termonology detected at "+f.getAbsolutePath()+". skipping ...");
						e.printStackTrace();
					} 
				}
			}
			
		}
		Set<Terminology> terms = new TreeSet<Terminology>(new Comparator<Terminology>(){
			public int compare(Terminology o1, Terminology o2) {
				return o1.getName().compareTo(o2.getName());
			}});
		terms.addAll(terminologies.values());
		return terms.toArray(new Terminology [0]);
	}

	public Terminology getTerminology(String path) {
		getTerminologies();
		return terminologies.get(path);
	}

	public boolean hasOntology(String name) {
		return getOntologies(name).length > 0;
	}

	public IOntology importOntology(URI path) throws IOntologyException {
		URL url = null;
		try {
			url = path.toURL();
		} catch (MalformedURLException e) {
			throw new IOntologyException("Invalid URI supplied: "+path,e);
		}
		IOntology ont = OOntology.loadOntology(url);
		importOntology(ont);
		return getOntology(ont.getURI());
		
	}

	public void importOntology(IOntology ont) throws IOntologyException {
		File file = new File(DEFAULT_ONTOLOGY_LOCATION,ont.getName());
		try {
			ont.write(new FileOutputStream(file),IOntology.OWL_FORMAT);
		} catch (FileNotFoundException e) {
			throw new IOntologyException("Unable to save file in the local cache at "+file.getAbsolutePath(),e);
		}
		// reload from cache
		ont.dispose();
		ont = OOntology.loadOntology(file);
		addOntology(ont);
	}

	public void removeOntology(IOntology ontology) {
		ontologies.remove(ontology.getURI());
	}

	public void removePropertyChangeListener(PropertyChangeListener listener) {
		pcs.removePropertyChangeListener(listener);
	}

	public void removeTerminology(Terminology terminology) {
		terminologies.remove(terminology.getName());
	}

	public IOntology getOntology(URI name, String version) {
		return getOntology(name);
	}

	public String[] getVersions(IOntology ont) {
		return (ont != null)?new String [] {ont.getVersion()}:new String [0];
	}
}
