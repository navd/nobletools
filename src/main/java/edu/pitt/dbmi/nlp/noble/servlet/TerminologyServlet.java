package edu.pitt.dbmi.nlp.noble.servlet;

import java.io.File;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import edu.pitt.dbmi.nlp.noble.coder.NobleCoder;
import edu.pitt.dbmi.nlp.noble.coder.model.Document;
import edu.pitt.dbmi.nlp.noble.ontology.DefaultRepository;
import edu.pitt.dbmi.nlp.noble.ontology.IRepository;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Definition;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.BioPortalTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.LexEVSRestTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.UMLSTerminology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.HTMLExporter;
import edu.pitt.dbmi.nlp.noble.util.Parcel;

/**
 * Servlet that handles all service requests
 * @author tseytlin
 */
public class TerminologyServlet extends HttpServlet {
	private String servletName = "/term/servlet/TerminologyServlet";
	private DefaultRepository repository;
	private String defaultTerminology;
	
	/**
	 * init servlet
	 */
	public void init(ServletConfig config) throws ServletException {
		String location = config.getInitParameter("terminology.location");
		String def = config.getInitParameter("default.terminology");
		
		repository = new DefaultRepository();
		repository.setTerminologyLocation(new File(location));
		if(def != null)
			defaultTerminology = def;
	}
	
	/**
	 * cleanup after
	 */
	public void destroy(){
		
	}
	
	/**
	 * get terminology
	 * @param type of terminology, null to get default
	 * @return
	 */
	private Terminology getTerminology(String t){
		Terminology term = null;
		if(t != null)
			term = repository.getTerminology(t);
		if(term == null && defaultTerminology  != null)
			term = repository.getTerminology(defaultTerminology);
		return term;
	}

	/**
	 * Get requests are text requests
	 * @param req
	 * @param res
	 * @throws IOException
	 */
	public void doGet(HttpServletRequest req, HttpServletResponse res)
		throws ServletException, IOException {
		
		servletName = req.getContextPath()+"/"+req.getServletPath();
		String response = "";
		String action = req.getParameter("action");
		
		try{
			Terminology terminology = getTerminology(req.getParameter("term"));
			if ( action == null ) {
				response = "Unrecognized parameter";	
			}else if (action.equals("set_sources")){
				String src  = req.getParameter("src");
				Source [] srcs = (src != null)?terminology.getSources(src):null;
				terminology.setSourceFilter(srcs);
			}else if (action.equals("get_terminologies")){
				response = processOutput(repository.getTerminologies());
			}else if ( action.equals("search")){
				String text = req.getParameter("text");
				//check if in fact we want to lookup concept
				Object c = null;
				if(text.matches("CL?\\d+") || text.startsWith("http://")){
					c = terminology.lookupConcept(text.trim());
				}else if("ROOT".equals(text)){
					c = terminology.getRootConcepts();
				}else{
					c = terminology.search(text);
				}
				response = processOutput(c);
			}else if ( action.equals("get_roots")){
				response = processOutput(terminology.getRootConcepts());
			}else if ( action.equals("lookup_concept")){
				String text = req.getParameter("code");
				Concept c = terminology.lookupConcept(text.trim());
				response = processOutput(c);
			}else if ( action.equals("get_sources")){
				Source [] src = terminology.getSources();
				response = processOutput(src);
			}else if ( action.equals("get_related_concepts")){
				String code = req.getParameter("code");
				String rel  = req.getParameter("relation");
				Concept c = terminology.lookupConcept(code.trim());
				
				/*
				try{
					code = URLDecoder.decode(code,"utf-8");
				}catch(Exception ex){}
				*/
				
				// find relationship
				if(c != null){
					Relation r = Relation.BROADER; // default
					rel = rel.toLowerCase();
					Relation [] rs = c.getRelations();
					for(int i=0;i<rs.length;i++){
						if(rs[i].getName().toLowerCase().contains(rel) ||
						   rs[i].getCode().toLowerCase().contains(rel)){
							r = rs[i];
							break;
						}
					}
					Concept [] cs = terminology.getRelatedConcepts(c, r);
					response = processOutput(cs);
				}else{
					response = processOutput(new Concept [0]);
				}
			}else if ( action.equals("get_related_concept_map")){
				String code = req.getParameter("code");
				Concept c = terminology.lookupConcept(code);
				// find relationship
				Map cs = terminology.getRelatedConcepts(c);
				response = processOutput(cs);
			}else if ( action.equals("code")){
				String text = req.getParameter("text");
				//check if in fact we want to lookup concept
				boolean best = ((NobleCoderTerminology)terminology).isSelectBestCandidate();
				((NobleCoderTerminology)terminology).setSelectBestCandidate(true);
				NobleCoder coder = new NobleCoder(terminology);
				Document doc = coder.process(new Document(text.replaceAll("\r\n","\n")));
				((NobleCoderTerminology)terminology).setSelectBestCandidate(best);
				String showReport = req.getParameter("show.report");
				String showConcepts = req.getParameter("show.concepts");
				if(showReport != null)
					doc.getProperties().put("show.report",showReport);
				if(showConcepts != null)
					doc.getProperties().put("show.concepts",showConcepts);
				if(servletName != null)
					doc.getProperties().put("servlet.path",servletName);
				response = processOutput(doc);
			}
		}catch(TerminologyException ex){
			response = "Terminology Error: "+ex.getMessage();
			ex.printStackTrace();
		}
		
		// write out response
		if(res != null && response != null){
			res.setContentType("text/html");
        	PrintWriter out = res.getWriter();
        	if(response.startsWith("<!DOCTYPE") || response.startsWith("<html>")){
        		out.println(response);
        	}else{
            	out.println("<html><body><head>");
	        	out.println("<title>Response</title>");
	        	out.println("</head><body>");
	        	out.println(response);
	        	out.println("</body></html>");
        	}
		}
	}
	
	/**
	 * Process post requests
	 * POST requests are object requests
	 */
	public void doPost(HttpServletRequest req, HttpServletResponse res) 
		throws ServletException, IOException {
		Object obj = null;
		try {
			ObjectInputStream objIn = new ObjectInputStream( req.getInputStream() );
			obj = objIn.readObject();
			objIn.close();
		} catch ( IOException e ) {
			e.printStackTrace();
		}
		catch ( ClassNotFoundException e ) {
			e.printStackTrace();
		}
		if ( obj instanceof Parcel )
			processRequest( req, res, ( Parcel ) obj );
	}
	
	/**
	 * Process output in HTML format
	 * @param obj
	 * @return
	 */
	private String processOutput(Object obj){
		//if(obj == null)
		//	System.err.println("object is null");
		StringBuffer buff = new StringBuffer();
		if(obj instanceof Concept){
			String clr = " bgcolor=\"#EEEEEE\"";
			Concept c = (Concept) obj;
			buff.append("<table border=0>");
			buff.append("<tr valign=top><td"+clr+">code</td><td>"+c.getCode()+"</td></tr>");
			buff.append("<tr valign=top><td"+clr+">name</td><td>"+c.getName()+"</td></tr>");
			SemanticType [] sem = c.getSemanticTypes();
			StringBuffer sembuff = new StringBuffer();
			if(sem != null && sem.length >0){
				for(int j=0;j<sem.length;j++)
					sembuff.append(sem[j].getName()+" ");
			}
			buff.append("<tr valign=top><td"+clr+">semantic types</td><td>"+sembuff+"</td></tr>");
			buff.append("<tr valign=top><td"+clr+">definitions</td><td><ul>");
			Definition [] def = c.getDefinitions();
			if(def != null)
				for(int j=0;j<def.length;j++)
					buff.append("<li>"+def[j].getDefinition()+" ["+def[j].getSource()+"]</li>");
			buff.append("</ul></td></tr>");
			buff.append("<tr valign=top><td"+clr+">synonyms</td><td>");
			String [] syn = c.getSynonyms();
			if(syn != null)
				for(int j=0;j<syn.length;j++)
					buff.append(syn[j]+", ");
			buff.append("</td></tr>");
			buff.append("<tr valign=top><td"+clr+">sources</td><td>");
			Source [] src = c.getSources();
			if(src != null)
				for(int j=0;j<src.length;j++)
					buff.append(src[j]+", ");
			buff.append("</td></tr>");
			buff.append("<tr valign=top><td"+clr+">relations</td><td>");
			Relation [] rs = c.getRelations();
			if(rs != null)
				for(int j=0;j<rs.length;j++){
					String code = c.getCode();
					try{
						code = URLEncoder.encode(c.getCode(),"utf-8");
					}catch(Exception ex){}
					String url = servletName+"?action=get_related_concepts&relation="+rs[j].getName()+"&code="+code;
					buff.append("<a href=\""+url+"\">"+rs[j].getName()+"</a>, ");
				}
			buff.append("</td></tr>");
			buff.append("<tr valign=top><td"+clr+">codes</td><td>");
			Map mp = c.getCodes();
			if(mp != null)
				for(Object o: mp.keySet())
					buff.append(o+" = "+mp.get(o)+", ");
			buff.append("</td></tr>");
			buff.append("</table>");
		}else if(obj instanceof Concept []){
			Concept [] cs = (Concept []) obj;
			String term = (cs.length > 0)?cs[0].getTerminology().getName():null;
			buff.append("<table border=0>");
			for(int i=0;i<cs.length;i++){
				String url = servletName+"?action=lookup_concept"+((term != null)?"&term="+term:"")+"&code=";
				try{
					url += URLEncoder.encode(cs[i].getCode(),"utf-8");
				}catch(UnsupportedEncodingException ex){
					ex.printStackTrace();
				}
				buff.append("<tr>");
				buff.append("<td><a href=\""+url+"\">"+cs[i].getCode()+"</a></td><td>&nbsp;</td>");
				buff.append("<td><b>"+cs[i].getName()+"</b></td><td>&nbsp;</td>");
				SemanticType [] sem = cs[i].getSemanticTypes();
				StringBuffer sembuff = new StringBuffer();
				if(sem != null && sem.length >0){
					for(int j=0;j<sem.length;j++)
						sembuff.append(sem[j].getName()+" ");
				}
				buff.append("<td>"+sembuff+"</td>");
				buff.append("</tr>");
			}
			buff.append("</table>");
		}else if(obj instanceof Source []){
			
		}else if(obj instanceof String []){
			buff.append("<h3>Did you mean?</h3>");
			String [] str = (String []) obj;
			for(int i=0;i<str.length;i++){
				String url = servletName+"?action=search&text="+TextTools.escapeURL(str[i]);
				buff.append("<a href=\""+url+"\">"+str[i]+"</a><br>");
			}
		}else if (obj instanceof Document){
			StringWriter writer = new StringWriter();
			HTMLExporter exporter = new HTMLExporter();
			try {
				
				Document doc = (Document) obj;
				//exporter.setShowFooter(false);
				if(doc.getProperties().containsKey("servlet.path"))
					exporter.setTerminologySerlvet(doc.getProperties().get("servlet.path"));
				if(doc.getProperties().containsKey("show.report"))
					exporter.setShowReport(Boolean.parseBoolean(doc.getProperties().get("show.report")));
				if(doc.getProperties().containsKey("show.concepts"))
					exporter.setShowConceptList(Boolean.parseBoolean(doc.getProperties().get("show.concepts")));
				exporter.export(doc, writer);
			} catch (Exception e) {
				e.printStackTrace();
			}
			buff.append(writer.toString());
		}else if (obj instanceof Terminology []){
			for(Terminology t: (Terminology [])obj){
				buff.append(t.getName()+"<br>");
			}
		}
		return buff.toString();
	}
	

	
	
	/**
	 * This is used for normal get requests s.a.  answer submits and authentication.
	 */
	private void processRequest( HttpServletRequest req, HttpServletResponse res , Parcel parcel )
		throws ServletException, IOException {

		String action = parcel.getAction();
		Object tosend = null;

		// perform appropriate action
		try{
			Terminology terminology = getTerminology(parcel.getProperties().getProperty("term"));
			if ( action == null ) {
				// do nothing
			}else if ( action.equals("set_sources")){
				Source [] src = (Source []) parcel.getPayload();
				terminology.setSourceFilter(src);
			}else if ( action.equals("search")){
				String txt = (String) parcel.getPayload();
				tosend = terminology.search(txt);
			}else if ( action.equals("lookup_concept")){
				tosend = terminology.lookupConcept((String) parcel.getPayload());
			}else if ( action.equals("get_sources")){
				tosend = terminology.getSources();
			}else if ( action.equals("get_related_concepts")){
				Object [] p = (Object []) parcel.getPayload();
				Concept c = (Concept) p[0];
				Relation r = (Relation) p[1];
				tosend = terminology.getRelatedConcepts(c, r);
			}else if ( action.equals("get_related_concept_map")){
				Concept c = (Concept) parcel.getPayload();
				tosend = terminology.getRelatedConcepts(c);
			}else if ( action.equals("get_terminologies")){
				tosend = Arrays.asList(repository.getTerminologies());
			}
		}catch(TerminologyException ex){
			ex.printStackTrace();
			tosend = null;
		}
		
		//debug(tosend);
			
		// Do a dispatch if possible, otherwise stream the object back (if possible)
		if ( tosend != null ) {
			// output serialized object
			ObjectOutputStream out = new ObjectOutputStream( res.getOutputStream() );
			out.writeObject( tosend );
			out.flush();
			out.close();
			tosend = null;
		}
	}
	
}
