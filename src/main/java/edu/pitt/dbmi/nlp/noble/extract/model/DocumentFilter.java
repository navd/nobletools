package edu.pitt.dbmi.nlp.noble.extract.model;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import edu.pitt.dbmi.nlp.noble.tools.TextTools;

/**
 * a regex filter that can mask a portion of the documents that either fits regex
 * or does not fit regex
 * @author tseytlin
 *
 */
public class DocumentFilter {
	public static final String TYPE_SECTION = "section";
	public static final String TYPE_MASK = "mask";
	protected String filter,type=TYPE_SECTION;
	protected boolean invertMatch;
	
	/**
	 * init new filter using a RegEx filter
	 * @param filter - only include text matching filter
	 * @param invert - invert match, exclude matched text
	 */
	public DocumentFilter(String filter,boolean invert) {
		this.filter = filter;
		this.invertMatch = invert;
	}
	
	/**
	 * init new filter using a RegEx filter
	 * @param filter
	 */
	public DocumentFilter(String filter) {
		this(filter,false);
	}

	/**
	 * init empty filter
	 */
	public DocumentFilter() {
		this(null,false);
	}
	/**
	 * get RegEx filter being used by this filter
	 * @return
	 */
	public String getFilter() {
		return filter;
	}

	public String getType() {
		return type;
	}

	public void setType(String type) {
		this.type = type;
	}


	
	/**
	 * set a RegEx filter used by this filter
	 * @param filter
	 */
	public void setFilter(String filter) {
		this.filter = filter;
	}


	/**
	 * is match inverted
	 * @return
	 */
	public boolean isInvertMatch() {
		return invertMatch;
	}

	/**
	 * set invert match
	 * @param invertMatch
	 */
	public void setInvertMatch(boolean invertMatch) {
		this.invertMatch = invertMatch;
	}

	
	/**
	 * convert Template to XML DOM object representation
	 * @return
	 */
	public Element toElement(Document doc) throws Exception {
		Element e = doc.createElement("Filter");
		if(isInvertMatch())
			e.setAttribute("invert.match","true");
		if(type != null)
			e.setAttribute("type",type);
		e.setTextContent(TextTools.escapeHTML(getFilter()));
		return e;
	}
	
	/**
	 * initialize template from XML DOM object representation
	 * @param element
	 */
	public void fromElement(Element element) throws Exception{
		if("Filter".equals(element.getTagName())){
			setInvertMatch(Boolean.parseBoolean(element.getAttribute("invert.match")));
			String str = element.getAttribute("type");
			if(str != null)
				setType(str);
			setFilter(element.getTextContent().trim());
		}
	}
	
	/**
	 * get De-ID filters, to mask out De-ID junk
	 * @return
	 */
	public static List<DocumentFilter> getDeIDFilters(){
		List<DocumentFilter> list = new ArrayList<DocumentFilter>();
		list.add(new DocumentFilter("^\\[Report de\\-identified.*\\]$",true));
		list.add(new DocumentFilter("\\*\\*[A-Z\\-]+(\\[.+\\])?[\\s,]*([0-9:]+|[MD\\.]+)?",true));
		return list;
	}
	
	/**
	 * filter input
	 * @param text
	 * @return
	 */
	public String filter(String text){
		if(filter == null)
			return text;	
		
		// apply RegEx filter
		Matcher m = Pattern.compile(filter,Pattern.MULTILINE).matcher(text);
		StringBuffer b = new StringBuffer();
		int offset = 0;
		while(m.find()){
			if(invertMatch){
				b.append(text.substring(offset,m.start()));
				b.append(getMask(m.end()-m.start()));
			}else{
				b.append(getMask(m.start()-offset));
				b.append(m.group());
			}
			offset = m.end();
		}
		b.append(text.substring(offset));
		return b.toString();
	}

	private String getMask(int n) {
		StringBuffer b = new StringBuffer("");
		for(int i=0;i<n;i++)
			b.append(' ');
		return b.toString();
	}
}
