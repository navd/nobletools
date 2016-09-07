package edu.pitt.dbmi.nlp.noble.ontology;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Definition;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;



/**
 * singleton class with lots of usefull ontology related methods
 * @author tseytlin
 */
public class OntologyUtils {
	public static final String DEFAULT_ONTOLOGY_BASE_URL = "http://ontologies.dbmi.pitt.edu/";
	public static final String TERMINOLOGY_CORE = "http://blulab.chpc.utah.edu/ontologies/TermMapping.owl";
	
	
	public static final String CODE = "code";
	public static final String SYNONYM = "synonym";
	public static final String DEFINITION = "definition";
	public static final String SEM_TYPE = "semanticType";
	public static final String ALT_CODE = "alternateCode";
	public static final String PREF_TERM = "preferredTerm";
	
	
	/**
	 * get a paths to root for a given concept
	 * @param name  - name of the class in question
	 * @return list of paths (path is list of classes)
	 */
	public static List<ClassPath> getRootPaths(IClass cls){
		if(cls != null){
			// get paths to root
			List<ClassPath> paths = new ArrayList<ClassPath>();
			getPath(cls,new ClassPath(), paths);
			return paths;
		}
		return Collections.EMPTY_LIST;
	}
	
	/**
	 * get multiple paths to root
	 * @param cls
	 * @param path
	 * @param paths
	 */
	private static void getPath(IClass cls,ClassPath path, List<ClassPath> paths){
		// add to paths if path is not in paths
		if(!paths.contains(path)){
			paths.add(path);
		}
		
		// add to current path
		path.add(0,cls);
		// iterate over parents
		List<IClass> parents = new ArrayList<IClass>();
		for(IClass p: cls.getDirectSuperClasses())
			if(!p.hasSuperClass(cls))
				parents.add(p);
		
		// if only one parent, then add it to path
		if(parents.size() == 1){
			getPath(parents.get(0),path,paths);
		}else if(parents.size() > 1){
			// else clone current path and start new ones
			for(int i=1;i<parents.size();i++){
				getPath(parents.get(i),new ClassPath(path),paths);
			}
			getPath(parents.get(0),path,paths);
		}
	}
	
	/**
	 * convert object to String
	 * @param o
	 * @return
	 */
	public static String toHTML(Object o){
		if(o == null)
			return "";
		
		// do varias pretty printing things
		if(o instanceof IRestriction){
			IRestriction r = (IRestriction) o;
			StringBuffer str = new StringBuffer();
			str.append(toHTML(r.getProperty()));
			switch(r.getRestrictionType()){
			case IRestriction.ALL_VALUES_FROM:
				str.append(" <b>all</b> "); break;
			case IRestriction.SOME_VALUES_FROM:
				str.append(" <b>some</b> "); break;
			case IRestriction.HAS_VALUE:
				str.append(" <b>has</b> "); break;
			default:
				str.append(" <b> = </b> "); 
			}
			str.append(toHTML(((IRestriction) o).getParameter()));
			return str.toString();
		}else if(o instanceof IInstance){
			return "<i>"+o+"</i>";
		}else if(o instanceof IProperty){
			return "<a href=\"property:"+o+"\">"+o+"</a>";
		}else if(o instanceof IClass){
			IClass c = (IClass)o;
			return (c.isAnonymous())?"anonymous":"<a href=\"class:"+o+"\">"+o+"</a>";
		}else if(o instanceof Collection){
			String sep = ", ";
			if(o instanceof ILogicExpression){
				ILogicExpression exp = (ILogicExpression) o;
				if(exp.getExpressionType() == ILogicExpression.AND)
					sep = " and ";
				else if(exp.getExpressionType() == ILogicExpression.OR)
					sep = " or ";
			}else if(o instanceof ClassPath){
				sep = " &rArr; ";
			}
			
			StringBuffer str = new StringBuffer();
			for(Object i: (Collection) o){
				str.append(toHTML(i)+sep);
			}
			String s = (str.length() > 0)?str.substring(0,str.length()-sep.length()):"";
			return (o instanceof ILogicExpression)?"("+s+")":s;
		}else if (o instanceof Object []){
			String sep = ", ";
			StringBuffer str = new StringBuffer();
			for(Object i: (Object []) o){
				str.append(toHTML(i)+sep);
			}
			return (str.length() > 0)?str.substring(0,str.length()-sep.length()):"";
		}
		// else just convert		
		return o.toString();
	}
	
	
	/**
	 * derive valid class name from any string
	 * @param name
	 * @return
	 */
	public static String toResourceName(String name){
		return name.trim().replaceAll("\\s*\\(.+\\)\\s*","").replaceAll("[^\\w\\-]","_").replaceAll("_+","_");
	}
	
	/**
	 * Derive prettier version of a class name
	 * @param resourceName
	 * @return
	 */
	public static String toPrettyName(String resourceName){
		// if name is in fact URI, just get a thing after hash
		int i = resourceName.lastIndexOf("#");
		if(i > -1){
			resourceName = resourceName.substring(i+1);
		}
				
		// strip prefix (if available)
		i = resourceName.indexOf(":");
		if(i > -1){
			resourceName = resourceName.substring(i+1);
		}
		
		// if name is in fact a URI, but not a fragment 
		i =  resourceName.lastIndexOf("/");
		if(i > -1)
			resourceName = resourceName.substring(i+1);
	
		
		// possible lowercase values to make things look prettier
		if(!resourceName.matches("[A-Z_\\-\\'0-9]+") && !resourceName.matches("[a-z][A-Z_\\-\\'0-9]+[\\w\\-]*"))
			resourceName = resourceName.toLowerCase();
			
		// now replace all underscores with spaces
		return resourceName.replaceAll("_"," ");
	}
	
	/**
	 * get ontology URI from an actual XML file
	 * @param file
	 * @return
	 * @throws IOException 
	 */
	public static URI getOntologyURI(File file) throws IOException{
		URI uri = null;
		BufferedReader r = new BufferedReader(new FileReader(file));
		Pattern p = Pattern.compile("xml:base=\"(.*)\"");
		for(String l=r.readLine(); l != null; l = r.readLine()){
			Matcher m = p.matcher(l);
			if(m.find()){
				uri = URI.create(m.group(1));
				break;
			}
		}
		r.close();
		return uri;
	}
	
	
	
	/**
	 * get or create property from an ontology
	 * @param ont
	 * @param name
	 * @return
	 */
	public static IProperty getOrCreateProperty(IOntology ont, String name, int type){
		IProperty prop = ont.getProperty(name);
		if(prop == null)
			prop = ont.createProperty(name,type);
		return prop;
	}
	
	/**
	 * get or create property from an ontology
	 * @param ont
	 * @param name
	 * @return
	 */
	public static IProperty getOrCreateProperty(IOntology ont, String name){
		return getOrCreateProperty(ont, name,IProperty.ANNOTATION);
	}
	
	/**
	 * add concept info from concept object to class
	 * @param c
	 * @param cls
	 */
	public static void copyConceptToClass(Concept c, IClass cls) {
		IOntology ont = cls.getOntology();
		
		IProperty code = getOrCreateProperty(ont,CODE);
		IProperty synonym = getOrCreateProperty(ont,SYNONYM);
		IProperty definition = getOrCreateProperty(ont,DEFINITION);
		IProperty semType = getOrCreateProperty(ont,SEM_TYPE);
		IProperty altCode = getOrCreateProperty(ont,ALT_CODE);
		IProperty prefTerm = getOrCreateProperty(ont,PREF_TERM);
		
			
		// add preferred term
		cls.addPropertyValue(prefTerm,c.getName());
		
		
		// add synonyms
		for(String s: c.getSynonyms()){
			if(!cls.hasPropetyValue(synonym, s))
				cls.addPropertyValue(synonym, s);
		}
		
		// add definitions
		for(Definition d: c.getDefinitions()){
			if(!cls.hasPropetyValue(definition,d.getDefinition()))
				cls.addPropertyValue(definition,d.getDefinition());
		}
		
		// get concept code 
		cls.setPropertyValue(code,c.getCode());
		for(Object src: c.getCodes().keySet()){
			String cui = (String) c.getCodes().get(src)+" ["+src+"]";
			if(!cls.hasPropetyValue(altCode,cui))
				cls.addPropertyValue(altCode,cui);
		}
		
		// get semantic types
		for(SemanticType st: c.getSemanticTypes()){
			if(!cls.hasPropetyValue(semType,st.getName()))
				cls.addPropertyValue(semType,st.getName());
		}
		
	}

	/**
	 * does the string looks loke UMLS (like) concept unique identifier
	 * @param text
	 * @return
	 */
	public static boolean isCUI(String text){
		return text.matches("CL?\\d{4,7}");
	}
	
	
	/**
	 * does the string looks loke UMLS (like) concept unique identifier
	 * @param text
	 * @return
	 */
	public static boolean isTUI(String text){
		return text.matches("T\\d{2,3}");
	}
	
}
