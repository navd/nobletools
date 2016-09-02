package edu.pitt.dbmi.nlp.noble.util;

import java.net.URI;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class StringUtils {
	public static boolean isEmpty(String s) {
		return s == null || s.length() == 0 || "null".equals(s);
	}

	/**
	 * <p>
	 * Removes a substring only if it is at the end of a source string,
	 * otherwise returns the source string.
	 * </p>
	 * 
	 * <p>
	 * A {@code null} source string will return {@code null}. An empty ("")
	 * source string will return the empty string. A {@code null} search string
	 * will return the source string.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.removeEnd(null, *)      = null
	 * StringUtils.removeEnd("", *)        = ""
	 * StringUtils.removeEnd(*, null)      = *
	 * StringUtils.removeEnd("www.domain.com", ".com.")  = "www.domain.com"
	 * StringUtils.removeEnd("www.domain.com", ".com")   = "www.domain"
	 * StringUtils.removeEnd("www.domain.com", "domain") = "www.domain.com"
	 * StringUtils.removeEnd("abc", "")    = "abc"
	 * </pre>
	 * 
	 * @param str
	 *            the source String to search, may be null
	 * @param remove
	 *            the String to search for and remove, may be null
	 * @return the substring with the string removed if found, {@code null} if
	 *         null String input
	 * @since .
	 */
	public static String removeEnd(final String str, final String remove) {
		if (isEmpty(str) || isEmpty(remove)) {
			return str;
		}
		if (str.endsWith(remove)) {
			return str.substring(0, str.length() - remove.length());
		}
		return str;
	}

	/**
	 * <p>
	 * Removes {@code separator} from the end of {@code str} if it's there,
	 * otherwise leave it alone.
	 * </p>
	 * 
	 * <p>
	 * NOTE: This method changed in version .. It now more closely matches Perl
	 * chomp. For the previous behavior, use
	 * {@link #substringBeforeLast(String, String)}. This method uses
	 * {@link String#endsWith(String)}.
	 * </p>
	 * 
	 * <pre>
	 * StringUtils.chomp(null, *)         = null
	 * StringUtils.chomp("", *)           = ""
	 * StringUtils.chomp("foobar", "bar") = "foo"
	 * StringUtils.chomp("foobar", "baz") = "foobar"
	 * StringUtils.chomp("foo", "foo")    = ""
	 * StringUtils.chomp("foo ", "foo")   = "foo "
	 * StringUtils.chomp(" foo", "foo")   = " "
	 * StringUtils.chomp("foo", "foooo")  = "foo"
	 * StringUtils.chomp("foo", "")       = "foo"
	 * StringUtils.chomp("foo", null)     = "foo"
	 * </pre>
	 * 
	 * @param str
	 *            the String to chomp from, may be null
	 * @param separator
	 *            separator String, may be null
	 * @return String without trailing separator, {@code null} if null String
	 *         input
	 */
	public static String chomp(final String str, final String separator) {
		return removeEnd(str, separator);
	}
	
	
	public static String join(final String [] str, final String s){
		StringBuffer buff = new StringBuffer();
		for (String st : str) {
			if (buff.length() > 0) {
				buff.append(s);
			}
			buff.append(st);
		}
		return buff.toString();
	}
	
	
	/**
	 * extract owl filename from path
	 * @param path
	 * @return
	 */
	public static String getOntologyName(URI path,boolean stripSuffix) {
		String p = path.getPath();
		int i = p.lastIndexOf("/");
		String s = p;
		if(i > -1){
			// if URI ends with slash, then
			if(i == p.length()-1){
				i = p.lastIndexOf("/",i-1);
				p = p.substring(0,p.length()-1);
				if(i > -1)
					s = p.substring(i+1,p.length());
			}else
				s = p.substring(i+1);
		}
		
		// if name is composed of digits, it could be a version
		// then get the next best match
		if(s.matches("[\\d\\.]+")){
			i = p.lastIndexOf("/",i-1);
			if(i > -1)
				s = p.substring(i+1,p.length()).replaceAll("/","-");
		}
		
		// strip suffix
		if(stripSuffix){
			int n = s.lastIndexOf(".");
			if(n > -1)
				s = s.substring(0,n);
		}
		return s;
	}
	
	
	/**
	 * extract owl filename from path
	 * @param path
	 * @return
	 */
	public static String getOntologyName(URI path) {
		return getOntologyName(path,false);
	}
	
	
	/**
	 * create abbreviated version of URI to use that instead of concept code
	 * @param uri
	 * @return
	 */
	
	public static String getAbbreviatedURI(String uri){
		int x = uri.lastIndexOf('#');
		if(x > -1){
			String ont = uri.substring(0,x);
			// get ontology name
			ont = getOntologyName(URI.create(ont),true);
			
			// abbreviate it by capital letter
			String abbr = ont.replaceAll("[^A-Z]+","");
			if(abbr.length() == 0){
				abbr = (ont.length()>5)?ont.substring(0,5):ont;
			}
			
			// get class name
			return abbr+":"+uri.substring(x+1);
		}
		return uri;
	}
	
	
	/**
	 * index of a word in an array 
	 * @param words
	 * @param text
	 * @param lastWordOffset
	 * @return
	 */
	public static int indexOf(String[] words, String text, int lastWordOffset) {
		for(int i=Math.max(0,lastWordOffset);i<words.length;i++){
			if(words[i].equals(text))
				return i;
		}
		return -1;
	}
	
	
	/**
	 * if input text ends with something like a suffix, strip it
	 * @param text
	 * @return
	 */
	
	public static String stripSuffix(String text){
		Pattern p = Pattern.compile("(.+)\\.[A-Za-z0-9]{1,10}");
		Matcher m = p.matcher(text);
		return m.matches()?m.group(1):text;	
	}
	
	
	/**
	 * get number of tabs that are prefixed in a string
	 * @param s
	 * @return
	 */
	public static int getTabOffset(String str){
		int count = 0;
		for(int i = 0;i<str.length();i++){
			if(str.charAt(i) == '\t')
				count ++;
			else
				break;
		}
		return count;
	}
	
}
