package edu.pitt.dbmi.nlp.noble.tools;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Array;
import java.net.URL;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Formatter;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.StringTokenizer;
import java.util.TreeSet;
import java.util.regex.Pattern;

import edu.pitt.dbmi.nlp.noble.util.Sender;


/**
 * This class provides several convinience methods
 * for text processing
 * @author tseytlin
 *
 */
public class TextTools {
	public static final String DEFAULT_TEXT_TOOLS_URL = "http://slidetutor.upmc.edu/term/servlet/TextToolsServlet";
	public static final int NO_VALUE = Integer.MIN_VALUE;
	private static final String separator = "\t";
	private static Map<String,String> plurals;
	private static Map<Character,String> urlEscapeCodeMap;
	private static Map<Character,String> xmlEscapeCodeMap;
	private static Map<String,String> stopWords,prepostionWords,commonWords;
	private static Map<String,String> timePatterns;
	private Sender sender;
	public static class StringStats {
		public int upperCase,lowerCase,digits,length,whiteSpace,alphabetic;
		public boolean isCapitalized,isLowercase,isUppercase;
		public String toString(){
			return "upperCase="+upperCase+",lowerCase="+lowerCase+",digits="+digits+",length="+length+
					",whiteSpace="+whiteSpace+",alphabetic="+alphabetic+",isCapitalized="+isCapitalized+
					",isLowercase="+isLowercase+",isUppercase="+isUppercase;
		}
	}
	
	//	 load values into map
	/*
	static {
		// load in all processing resources
        plurals = loadResource("/resources/PluralTable.lst");
        stopWords = loadResource("/resources/StopWords.lst");
	}
	*/
	
	/**
	 * get plural table
	 * @return
	 */
	private static Map<String,String> getPluralTable(){
		if(plurals == null)
			plurals = loadResource("/resources/PluralTable.lst");
		return plurals;
	}
	
	/**
	 * get list of stop words
	 * @return
	 */
	public static Set<String> getStopWords(){
		if(stopWords == null){
			stopWords = loadResource("/resources/StopWords.lst");
		}
		return stopWords.keySet();
	}
	
	
	/**
	 * get stop words table
	 * @return
	 */
	public static Set<String> getPrepostitionWords(){
		if(prepostionWords == null)
			prepostionWords = loadResource("/resources/PrepositionWords.lst");
		return prepostionWords.keySet();
	}
	
	/**
	 * get a list of common words
	 * @return
	 */
	public static Set<String> getCommonWords(){
		if(commonWords == null){
			commonWords = new HashMap<String, String>();
			for(String w: loadResource("/resources/CommonWords.lst").keySet()){
				w = normalize(w);
				if(w.length()>0)
					commonWords.put(w,"");
			}
		}
		return commonWords.keySet();
	}
	
	
	/**
	 * Read a list with this name and put its content into a list object
	 */	
	public static Map<String,String> loadResourceAsMap(String name,String sep){
		Map<String,String> list = new LinkedHashMap<String,String>();
		try{
			InputStream in = null;
			File f = new File(name);
			if(f.exists()){
				in = new FileInputStream(f);
			}else if(name.startsWith("http://")){
				in = (new URL(name)).openStream();
			}else{
				in = TextTools.class.getResourceAsStream(name);
			}
			if(in == null){
				System.err.println("ERROR: Could not load resource: "+name);
				return list;
			}	
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			for(String line = reader.readLine();line != null;line=reader.readLine()){
				String t = line.trim();
				// skip blank lines and lines that start with #
				if(t.length() > 0 && !t.startsWith("#")){
					String [] suffixes = line.trim().split(sep);
					if(suffixes.length >= 2)
						list.put(suffixes[1].trim(),suffixes[0].trim());
					else
						list.put(suffixes[0].trim(),null);
				}
			}
			reader.close();
			in.close();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return list;
	}
	
	/**
	 * Read a list with this name and put its content into a Map object
	 * use tab (\t) as a default seperator
	 */	
	public static Map<String,String> loadResource(String name){
		return loadResourceAsMap(name,separator);
	}
	
	
	/**
	 * Read a list with this name and put its content into a list object
	 */	
	public static List<String> loadResourceAsList(String name){
		List<String> list = new ArrayList<String>();
		try{
			InputStream in = null;
			File f = new File(name);
			if(f.exists()){
				in = new FileInputStream(f);
			}else if(name.startsWith("http://")){
				in = (new URL(name)).openStream();
			}else{
				in = TextTools.class.getResourceAsStream(name);
			}
			if(in == null){
				System.err.println("ERROR: Could not load resource: "+name);
				return list;
			}	
			BufferedReader reader = new BufferedReader(new InputStreamReader(in));
			for(String line = reader.readLine();line != null;line=reader.readLine()){
				line = line.trim();
				// skip blank lines and lines that start with #
				if(line.length() > 0 && !line.startsWith("#")){
					list.add(line);
				}
			}
			reader.close();
			in.close();
		}catch(IOException ex){
			ex.printStackTrace();
		}
		return list;
	}
	
	
	/**
	 * TextTools located on a server (forward to some implementation)
	 * @param remote servlet
	 */
	public TextTools(URL servlet){
		sender = new Sender(servlet);
	}
	
	/**
	 * TextTools located on a server (forward to some implementation)
	 * @param remote servlet
	 */
	public TextTools(){
		try{
			sender = new Sender(new URL(DEFAULT_TEXT_TOOLS_URL));
		}catch(Exception ex){
			ex.printStackTrace();
		}
	}
	
	
	/**
	 * Determine if word is plural
	 */
	public static boolean isPlural(String word){
		//iterate over keys
		for(Iterator i=getPluralTable().keySet().iterator();i.hasNext();){
			String pluralSuffix	= (String) i.next();
			if(word.endsWith(pluralSuffix)){	
				return true;
			}
		}
		return false;
	}
	
	/**
	 * Determine if word is a stop word
	 */
	public static boolean isStopWord(String word){
		//check table
		getStopWords();
		if(stopWords.containsKey(word.trim()))
			return true;
		return false;
	}
	
	/**
	 * Determine if word is a stop word
	 */
	public static boolean isPrepositionWord(String word){
		//check table
		getPrepostitionWords();
		if(prepostionWords.containsKey(word.trim()))
			return true;
		return false;
	}
	

	/**
	 * Determine if word is in the top 1000 common english words
	 */
	public static boolean isCommonWord(String word){
		//check table
		getCommonWords();
		if(commonWords.containsKey(normalize(word.trim())))
			return true;
		return false;
	}
	
	/**
	 * Given a word tries to extract its singular form and returns it
	 */	
	public static String convertToSingularForm(String word){
		// strip posessive
		if(word.endsWith("'s")){
			return word.substring(0,(word.length()-2));
		}
		
		//iterate over keys
		for(Iterator i=getPluralTable().keySet().iterator();i.hasNext();){
			String pluralSuffix	= (String) i.next();
			if(word.endsWith(pluralSuffix)){	
				String singularSuffix = (String) getPluralTable().get(pluralSuffix);
				int end = word.length() - pluralSuffix.length();
				return word.substring(0,end)+singularSuffix;
			}
		}
		return word;
	}
	
	/**
	 * stem input word (find root)
	 * Uses Porter stemmer algorithm
	 * @param input word 
	 * @return word root (lowercase)
	 * NOTE: this method doesn't check that input string is a single word.
	 */
	public static String stem(String word){
		if(word == null || word.length() == 0)
			return "";
		Stemmer s = new Stemmer();
		s.add(word.toLowerCase());
		s.stem();
		return s.getResultString();
	}
	
	/**
	 * split text into words.
	 * replace all non-word characters and possesives from query w/ single space
	 * tokenize words by space and add non-empty ones
	 * @param query
	 * @return
	 */
	public static List<String> getWords(String query){
		// replace all non-word characters and possesives from query w/ single space
		//String text = query.replaceAll("('s\\b|\\W+|\\s+)"," ");
		List<String> list = new ArrayList<String>();
		// tokenize words by space and add non-empty ones
		StringTokenizer t = new StringTokenizer(query," ,!?;:-–—~_\\/|\t\n\r<>()[]\"");
		while(t.hasMoreTokens()){
			String s =t.nextToken();
			if(s.length() > 0){
				//|| s.endsWith(",") || s.endsWith("!") || s.endsWith("?") || s.endsWith(";") || s.endsWith(":")
				while(s.endsWith("."))
					s = s.substring(0,s.length()-1);
				if(s.length() > 0)
					list.add(s);
			}
		}
		return list;
		//return (String[]) list.toArray(new String[0]);
	}
	
	
	
	/**
	 * break a given text into a set of ngrams
	 * Ex: input: "quick brown fox jumped" w/ n = 3 will return
	 *  quick brown fox, brown fox jumped, quick brown, brown fox, fox jumped
	 *  quick, brown, fox, jumped
	 * @param input text
	 * @param ngram limit 
	 * @return
	 */
	public static String [] getNGrams(String text, int n){
		List<String> result = new ArrayList<String>();
		List<String> words = getWords(text);
		
		// decrement by number of n in ngram
		for(int e = n; e > 0; e--){
			// w/ given ngram size, get all combinations
			for(int s = 0; s <= words.size() - e; s++){ 
				// inner loop to construct the actual ngram
				StringBuffer b = new StringBuffer();
				for(int i=s;i<s+e; i++){
					b.append(words.get(i)+" ");
				}
				result.add(b.toString().trim());
			}
		}
		
		return result.toArray(new String [0]);
	}
	
	/**
	 * This method gets a text file (HTML too) from input stream 
	 * from given map
	 * @param InputStream text input
	 * @return String that was produced
	 * @throws IOException if something is wrong
	 * WARNING!!! if you use this to read HTML text and want to put it somewhere
	 * you should delete newlines
	 */
	public static String getText(InputStream in) throws IOException {
		return getText(in, "\n");
	}
	
	/**
	 * This method gets a text file (HTML too) from input stream 
	 * from given map
	 * @param InputStream text input
	 * @param lineSeperator to use "\n" or \r\n or System.getProperty("line.separator")
	 * @return String that was produced
	 * @throws IOException if something is wrong
	 * WARNING!!! if you use this to read HTML text and want to put it somewhere
	 * you should delete newlines
	 */
	public static String getText(InputStream in, String lineSeparator) throws IOException {
		StringBuffer strBuf = new StringBuffer();
		BufferedReader buf = new BufferedReader(new InputStreamReader(in));
		try {
			for (String line = buf.readLine(); line != null; line = buf.readLine()) {
				strBuf.append(line + lineSeparator );
			}
		} catch (IOException ex) {
			throw ex;
		} finally {
			buf.close();
		}
		return strBuf.toString();
	}
	
	
	/**
	 * strip diacritics from the string Ex; Protégé -> Protege
	 * a faster solution, that avoids weird non-ascii chars in your code
	 * shamelessly copy/pasted from
	 * http://www.rgagnon.com/javadetails/java-0456.html
	 */
	public static String stripDiacritics(String s) {
		final String PLAIN_ASCII =
				 "AaEeIiOoUu" // grave
				+ "AaEeIiOoUuYy" // acute
				+ "AaEeIiOoUuYy" // circumflex
				+ "AaOoNn" // tilde
				+ "AaEeIiOoUuYy" // umlaut
				+ "Aa" // ring
				+ "Cc" // cedilla
				+ "OoUu"; // double acute;
		final String UNICODE = 
				"\u00C0\u00E0\u00C8\u00E8\u00CC\u00EC\u00D2\u00F2\u00D9\u00F9"
				+ "\u00C1\u00E1\u00C9\u00E9\u00CD\u00ED\u00D3\u00F3\u00DA\u00FA\u00DD\u00FD"
				+ "\u00C2\u00E2\u00CA\u00EA\u00CE\u00EE\u00D4\u00F4\u00DB\u00FB\u0176\u0177"
				+ "\u00C3\u00E3\u00D5\u00F5\u00D1\u00F1"
				+ "\u00C4\u00E4\u00CB\u00EB\u00CF\u00EF\u00D6\u00F6\u00DC\u00FC\u0178\u00FF" + "\u00C5\u00E5"
				+ "\u00C7\u00E7" + "\u0150\u0151\u0170\u0171";
		
		if (s == null)
			return null;
		
		StringBuilder sb = new StringBuilder();
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			int pos = UNICODE.indexOf(c);
			if (pos > -1) {
				sb.append(PLAIN_ASCII.charAt(pos));
			} else {
				sb.append(c);
			}
		}
		return sb.toString();
	}
	
	/**
	 * The normalized string is a version of the original string in lower case, 
	 * without punctuation, genitive markers, or stop words, diacritics, ligatures, 
	 * with each word in it's uninflected (citation) form, the words sorted in alphabetical 
	 * order, and normalize non-ASCII Unicode characters to ASCII by mapping punctuation 
	 * and symbols to ASCII, mapping Unicode to ASCII, stripping diacritics, spliting ligatures, 
	 * and stripping non-ASCII Unicode characrters.
	 * 1. q0: map Uniocde symbols and punctuation to ASCII
   	 * 2. g: remove genitives,
   	 * 3. rs: then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
   	 * 4. o: then replace punctuation with spaces,
   	 * 5. t: then remove stop words,
   	 * 6. l: then lowercase,
   	 * 7. B: then uninflect each word,
   	 * 8. Ct: then get citation form for each base form,
   	 * 9. q7: then Unicode Core Norm
     *     * map Uniocde symbols and punctuation to ASCII
     *     * map Unicode to ASCII
     *     * split ligatures
     *     * strip diacritics 
     * 10. q8: then strip or map non-ASCII Unicode characters,
     * 11. w: and finally sort the words in alphabetic order. 
	 * @param original text
	 * @return normalized string
	 */
	public static String normalize(String text){
		return normalize(text,false);
	}
	
	/**
	 * The normalized string is a version of the original string in lower case, 
	 * without punctuation, genitive markers, or stop words, diacritics, ligatures, 
	 * with each word in it's uninflected (citation) form, the words sorted in alphabetical 
	 * order, and normalize non-ASCII Unicode characters to ASCII by mapping punctuation 
	 * and symbols to ASCII, mapping Unicode to ASCII, stripping diacritics, spliting ligatures, 
	 * and stripping non-ASCII Unicode characrters.
	 * 1. q0: map Uniocde symbols and punctuation to ASCII
   	 * 2. g: remove genitives,
   	 * 3. rs: then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
   	 * 4. o: then replace punctuation with spaces,
   	 * 5. t: then remove stop words,
   	 * 6. l: then lowercase,
   	 * 7. B: then uninflect each word,
   	 * 8. Ct: then get citation form for each base form,
   	 * 9. q7: then Unicode Core Norm
     *     * map Uniocde symbols and punctuation to ASCII
     *     * map Unicode to ASCII
     *     * split ligatures
     *     * strip diacritics 
     * 10. q8: then strip or map non-ASCII Unicode characters,
     * 11. w: and finally sort the words in alphabetic order. 
	 * @param original text
	 * @return normalized string
	 */
	public static String normalize(String text, boolean stem, boolean digits){
		return normalize(text, stem, digits,true);
	}
	
	
	/**
	 * The normalized string is a version of the original string in lower case, 
	 * without punctuation, genitive markers, or stop words, diacritics, ligatures, 
	 * with each word in it's uninflected (citation) form, the words sorted in alphabetical 
	 * order, and normalize non-ASCII Unicode characters to ASCII by mapping punctuation 
	 * and symbols to ASCII, mapping Unicode to ASCII, stripping diacritics, spliting ligatures, 
	 * and stripping non-ASCII Unicode characrters.
	 * 1. q0: map Uniocde symbols and punctuation to ASCII
   	 * 2. g: remove genitives,
   	 * 3. rs: then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
   	 * 4. o: then replace punctuation with spaces,
   	 * 5. t: then remove stop words,
   	 * 6. l: then lowercase,
   	 * 7. B: then uninflect each word,
   	 * 8. Ct: then get citation form for each base form,
   	 * 9. q7: then Unicode Core Norm
     *     * map Uniocde symbols and punctuation to ASCII
     *     * map Unicode to ASCII
     *     * split ligatures
     *     * strip diacritics 
     * 10. q8: then strip or map non-ASCII Unicode characters,
     * 11. w: and finally sort the words in alphabetic order. 
	 * @param original text
	 * @return normalized string
	 */
	public static String normalize(String text, boolean stem, boolean digits,boolean stopWords){
		return normalize(text, stem, digits, stopWords, false);
	}
	
	/**
	 * The normalized string is a version of the original string in lower case, 
	 * without punctuation, genitive markers, or stop words, diacritics, ligatures, 
	 * with each word in it's uninflected (citation) form, the words sorted in alphabetical 
	 * order, and normalize non-ASCII Unicode characters to ASCII by mapping punctuation 
	 * and symbols to ASCII, mapping Unicode to ASCII, stripping diacritics, spliting ligatures, 
	 * and stripping non-ASCII Unicode characrters.
	 * 1. q0: map Uniocde symbols and punctuation to ASCII
   	 * 2. g: remove genitives,
   	 * 3. rs: then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
   	 * 4. o: then replace punctuation with spaces,
   	 * 5. t: then remove stop words,
   	 * 6. l: then lowercase,
   	 * 7. B: then uninflect each word,
   	 * 8. Ct: then get citation form for each base form,
   	 * 9. q7: then Unicode Core Norm
     *     * map Uniocde symbols and punctuation to ASCII
     *     * map Unicode to ASCII
     *     * split ligatures
     *     * strip diacritics 
     * 10. q8: then strip or map non-ASCII Unicode characters,
     * 11. w: and finally sort the words in alphabetic order. 
	 * @param original text
	 * @return normalized string
	 *
	public static String normalize(String text, boolean stem, boolean digits,boolean stopWords, boolean skipAbbriv){
		return normalize(text, stem, digits, stopWords, skipAbbriv, false);
	}
	*/
	
	/**
	 * The normalized string is a version of the original string in lower case, 
	 * without punctuation, genitive markers, or stop words, diacritics, ligatures, 
	 * with each word in it's uninflected (citation) form, the words sorted in alphabetical 
	 * order, and normalize non-ASCII Unicode characters to ASCII by mapping punctuation 
	 * and symbols to ASCII, mapping Unicode to ASCII, stripping diacritics, spliting ligatures, 
	 * and stripping non-ASCII Unicode characrters.
	 * 1. q0: map Uniocde symbols and punctuation to ASCII
   	 * 2. g: remove genitives,
   	 * 3. rs: then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
   	 * 4. o: then replace punctuation with spaces,
   	 * 5. t: then remove stop words,
   	 * 6. l: then lowercase,
   	 * 7. B: then uninflect each word,
   	 * 8. Ct: then get citation form for each base form,
   	 * 9. q7: then Unicode Core Norm
     *     * map Uniocde symbols and punctuation to ASCII
     *     * map Unicode to ASCII
     *     * split ligatures
     *     * strip diacritics 
     * 10. q8: then strip or map non-ASCII Unicode characters,
     * 11. w: and finally sort the words in alphabetic order. 
	 * @param original text
	 * @return normalized string
	 */
	public static String normalize(String text, boolean stem, boolean digits,boolean stopWords, boolean uniqueWords, boolean sort){ 
		Collection<String> words = normalizeWords(text, stem, digits,stopWords);
				
		// sort words alphabeticly (if unique) remove duplicates, else just sort existing list
		if(uniqueWords)
			words = (sort)?new TreeSet<String>(words):new LinkedHashSet<String>(words);
		else if(sort)
			Collections.sort((List)words);
			
		// convert words to single string
		StringBuffer buf = new StringBuffer();
		for(String s: words)
			buf.append(s+" ");
		return buf.toString().trim();
	}
	
	/**
	 * The normalized string is a version of the original string in lower case, 
	 * without punctuation, genitive markers, or stop words, diacritics, ligatures, 
	 * with each word in it's uninflected (citation) form, the words sorted in alphabetical 
	 * order, and normalize non-ASCII Unicode characters to ASCII by mapping punctuation 
	 * and symbols to ASCII, mapping Unicode to ASCII, stripping diacritics, spliting ligatures, 
	 * and stripping non-ASCII Unicode characrters.
	 * 1. q0: map Uniocde symbols and punctuation to ASCII
   	 * 2. g: remove genitives,
   	 * 3. rs: then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
   	 * 4. o: then replace punctuation with spaces,
   	 * 5. t: then remove stop words,
   	 * 6. l: then lowercase,
   	 * 7. B: then uninflect each word,
   	 * 8. Ct: then get citation form for each base form,
   	 * 9. q7: then Unicode Core Norm
     *     * map Uniocde symbols and punctuation to ASCII
     *     * map Unicode to ASCII
     *     * split ligatures
     *     * strip diacritics 
     * 10. q8: then strip or map non-ASCII Unicode characters,
     * 11. w: and finally sort the words in alphabetic order. 
	 * @param original text
	 * @return normalized string
	 */
	public static String normalize(String text, boolean stem, boolean digits,boolean stopWords, boolean uniqueWords){ 
		return normalize(text,stem,digits,stopWords,uniqueWords,true);
	}
	
	/**
	 * The normalized string is a version of the original string in lower case, 
	 * without punctuation, genitive markers, or stop words, diacritics, ligatures, 
	 * with each word in it's uninflected (citation) form, the words sorted in alphabetical 
	 * order, and normalize non-ASCII Unicode characters to ASCII by mapping punctuation 
	 * and symbols to ASCII, mapping Unicode to ASCII, stripping diacritics, spliting ligatures, 
	 * and stripping non-ASCII Unicode characrters.
	 * 1. q0: map Uniocde symbols and punctuation to ASCII
   	 * 2. g: remove genitives,
   	 * 3. rs: then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
   	 * 4. o: then replace punctuation with spaces,
   	 * 5. t: then remove stop words,
   	 * 6. l: then lowercase,
   	 * 7. B: then uninflect each word,
   	 * 8. Ct: then get citation form for each base form,
   	 * 9. q7: then Unicode Core Norm
     *     * map Uniocde symbols and punctuation to ASCII
     *     * map Unicode to ASCII
     *     * split ligatures
     *     * strip diacritics 
     * 10. q8: then strip or map non-ASCII Unicode characters,
     * 11. w: and finally sort the words in alphabetic order. 
     * 
     *
	 * @param text 			- input text
	 * @param stem 			- stem words using porder stemmer
	 * @param stripDigits 	- strip single digits from the string
	 * @param stripStopWord	- strip known stop words from the string
	 * @param skipAbbriv	- if word looks like an abbreviation, do not normalize it
	 * @return normalized string
	 */
	public static String normalize(String text, boolean stem){
		return normalize(text,stem,true);
	}
	
	
	/**
	 * perform normalization of a string @see normalize, but return unsorted list of words 
	 * @param text
	 * @param stem
	 * @return
	 */
	public static List<String> normalizeWords(String text, boolean stem){
		return normalizeWords(text, stem,true);
	}
	
	
	/**
	 * perform normalization of a string @see normalize, but return unsorted list of words 
	 * @param text
	 * @param stem -stem words
	 * @param strip - strip digits
	 * @return
	 */
	public static List<String> normalizeWords(String text, boolean stem, boolean stripDigits){
		return normalizeWords(text, stem, stripDigits,true);
	}
	
	/**
	 * perform normalization of a string @see normalize, but return unsorted list of words 
	 * @param text
	 * @param stem -stem words
	 * @param strip - strip digits
	 * @return
	 
	public static List<String> normalizeWords(String text, boolean stem, boolean stripDigits, boolean stripStopWords){
		return normalizeWords(text, stem, stripDigits,stripStopWords,false);
	}
	*/
	/**
	 * perform normalization of a string @see normalize, but return unsorted list of words 
	 * @param text 			- input text
	 * @param stem 			- stem words using porder stemmer
	 * @param stripDigits 	- strip single digits from the string
	 * @param stripStopWord	- strip known stop words from the string
	 * @param skipAbbriv	- if word looks like an abbreviation, do not normalize it
	 * @return
	 */
	public static List<String> normalizeWords(String text, boolean stem, boolean stripDigits, boolean stripStopWords){
		text = text.trim();
		
		// map to ascii (unicode nomralization)
		text = stripDiacritics(text);
		
		// then lowercase, doing it later
		//if(!skipAbbriv)
		text = text.toLowerCase();
		
		// remove genetives ('s and s')
		text = text.replaceAll("\\b([a-z]+)'s?","$1");
		
		//then remove parenthetc pluralforms of (s), (es), (ies), (S), (ES), and (IES),
		text = text.replaceAll("\\(i?e?s\\)","");
		
		// then replace punctuation with spaces, (and other non word characters)
		//text = text.replaceAll(" ?\\W ?"," ");
		
		// replace punctuations, yet preserve period in float numbers and a dash, since there are
		// useful concepts that have a dash in them Ex: in-situ
		//text = text.replaceAll("\\s?[^\\w\\.]\\s?"," ").replaceAll("([a-zA-Z ])\\.([a-zA-Z ])","$1 $2");
		
		// have only single space between words
		//text = text.replaceAll("\\s+"," ");
		
		// find floating digits and replace . with _
		text = text.replaceAll("(\\d+)\\.(\\d+)","$1_$2").replaceAll("\\.(\\d+)","_$1");
		
		// then replace punctuation with spaces, (and other non word characters)
		text = text.replaceAll("\\s*\\W\\s*"," ");
		
		// now replace all inserted _ back with periods under same conditions as before
		text = text.replaceAll("(\\d+)_(\\d+)","$1.$2").replaceAll("_(\\d+)",".$1");
						
		// split into words
		String [] swords = text.split("\\s+");
		
		
		// go over each word and convert it lowercase IF not abbreviation
		/* for(int i=0;i<swords.length;i++){
			// check if text is a suspected abbreviation, if not, lowercase
			if(skipAbbriv && !isLikelyAbbreviation(swords[i]))
				swords[i] = swords[i].toLowerCase();
		}*/
				
		// convert to array
		List<String> words = new ArrayList<String>(Arrays.asList(swords));
		
		// then remove stop words and numbers
		if(stripStopWords){
			for(int i=0;i<swords.length;i++){
				if(isStopWord(swords[i]) || (stripDigits && swords[i].matches("\\d+")))
					words.remove(swords[i]);
			}
		}
		
		//and stem other words
		if(stem){
			for(int i=0;i<words.size();i++){
				String s = words.get(i);
				words.set(i,isLikelyAbbreviation(s)?s:stem(s));
			}
		}
		return words;
	}


	/**
	 * is this text an abbreviation of sorts, used in normalized method
	 * if abbreviation then no normalization is required
	 * @param text
	 * @return
	 */
	public static boolean isLikelyAbbreviation(String text) {
		// if string contains junk s.a. +()-/ or digit, it might be some protein or whatever
		if(Pattern.compile("[\\(\\)\\[\\]+,0-9]").matcher(text).find() && Pattern.compile("[A-Za-z]").matcher(text).find())
			return true;
		
		// if text is all uppercase and 5 letters or less
		StringStats st = getStringStats(text);
		if(text.length() <=5 && st.isUppercase)
			return true;
		
		// if any text is not Capitalized and has a mix of upper and lower, then abbreviation 
		if(!st.isCapitalized && st.lowerCase > 0 && st.upperCase > 0)
			return true;
		
		return false;
	}

	/**
	 * compute levenshtein (edit) distance between two strings
	 * http://en.wikibooks.org/wiki/Algorithm_Implementation/Strings/Levenshtein_distance#Java
	 * @param str1
	 * @param str2
	 * @return
	 */
	public static int getLevenshteinDistance(CharSequence str1, CharSequence str2) {
		int[][] distance = new int[str1.length() + 1][str2.length() + 1];

		for (int i = 0; i <= str1.length(); i++)
			distance[i][0] = i;
		for (int j = 0; j <= str2.length(); j++)
			distance[0][j] = j;

		for (int i = 1; i <= str1.length(); i++)
			for (int j = 1; j <= str2.length(); j++)
				distance[i][j] = Math.min(Math.min(distance[i - 1][j] + 1, distance[i][j - 1] + 1),
						distance[i - 1][j - 1] + ((str1.charAt(i - 1) == str2.charAt(j - 1)) ? 0 : 1));

		return distance[str1.length()][str2.length()];
	}

	
	/**
	 * compute fuzzy equals /similarity (minimal levenshtein (edit) distance
	 * this class tries to detect similarity by computing edit distance, but 
	 * only as the last resourt. First it checks for nulls, string lengths,
	 * regular equals, makes sure that the first letters of each word are the
	 * same. Only then it attempts to compute the distance and compares it to 
	 * a threshold that is relative to string size
	 * @param s1
	 * @param s2
	 * @return
	 */
	public static boolean similar(String s1, String s2){
		// check for null
		if(s1 == null && s2 == null)
			return true;
		if(s1 == null || s2 == null)
			return false;
		
		// check sizes (no point to compare if size difference is huge)
		if(Math.abs(s1.length() - s2.length()) > 3)
			return false;
		
		// do normal equals first
		if(s1.equalsIgnoreCase(s2))
			return true;
		
		
		// don't do this for really small words
		if(s1.length() <= 4 || s2.length() <= 4)
			return false;
		
		
		// now break into words
		String [] w1 = s1.split("[\\s_]");
		String [] w2 = s2.split("[\\s_]");
		
		// if number of words is different, then false
		if(w1.length != w2.length)
			return false;
		
		// check first letters of each word
		for(int i=0;i<w1.length;i++){
			// if first letters don't match then not similar enough
			if(w1[i].charAt(0) != w2[i].charAt(0))
				return false;
		}
		
		// figure out the minimum edit distance
		int n = (s1.length() < 7)?1:(s1.length() >= 20)?3:2; 
		
		// now comput levenshtein distances
		return getLevenshteinDistance(s1.toLowerCase(),s2.toLowerCase()) <= n;
	}
	
	
	public static Map<Character,String> getURLEscapeCode(){
		if(urlEscapeCodeMap == null){
			urlEscapeCodeMap = new HashMap<Character, String>();
			urlEscapeCodeMap.put(' ',"%20");
			urlEscapeCodeMap.put('<',"%3C");
			urlEscapeCodeMap.put('>',"%3E");
			urlEscapeCodeMap.put('#',"%23");
			urlEscapeCodeMap.put('%',"%25");
			urlEscapeCodeMap.put('{',"%7B");
			urlEscapeCodeMap.put('}',"%7D");
			urlEscapeCodeMap.put('|',"%7C");
			urlEscapeCodeMap.put('\\',"%5C");
			urlEscapeCodeMap.put('^',"%5E");
			urlEscapeCodeMap.put('~',"%7E");
			urlEscapeCodeMap.put('[',"%5B");
			urlEscapeCodeMap.put(']',"%5D");
			urlEscapeCodeMap.put('`',"%60");
			urlEscapeCodeMap.put(';',"%3B");
			urlEscapeCodeMap.put('/',"%2F");
			urlEscapeCodeMap.put('?',"%3F");
			urlEscapeCodeMap.put(':',"%3A");
			urlEscapeCodeMap.put('@',"%40");
			urlEscapeCodeMap.put('=',"%3D");
			urlEscapeCodeMap.put('&',"%26");
			urlEscapeCodeMap.put('$',"%24");
		}
		return urlEscapeCodeMap;
	}
	
	public static Map<Character,String> getHTMLEscapeCode(){
		if(xmlEscapeCodeMap == null){
			xmlEscapeCodeMap = new HashMap<Character, String>();
			xmlEscapeCodeMap.put('"',"&quot;");
			xmlEscapeCodeMap.put('\'',"&apos;");
			xmlEscapeCodeMap.put('<',"&lt;");
			xmlEscapeCodeMap.put('>',"&gt;");
			xmlEscapeCodeMap.put('&'," &amp;");
		}
		return xmlEscapeCodeMap;
	}
	
	/**
	 * URL escape filter
	 * @param s
	 * @return
	 */
	public static String escapeURL(String s){
		StringBuffer str = new StringBuffer();
		Map<Character,String> m = getURLEscapeCode();
		for(char x: s.toCharArray()){
			if(m.containsKey(x))
				str.append(m.get(x));
			else
				str.append(x);
		}
		return str.toString();
	}
	
	/**
	 * URL escape filter
	 * @param s
	 * @return
	 */
	public static String escapeHTML(String s){
		StringBuffer str = new StringBuffer();
		Map<Character,String> m = getHTMLEscapeCode();
		for(char x: s.toCharArray()){
			if(m.containsKey(x))
				str.append(m.get(x));
			else
				str.append(x);
		}
		return str.toString();
	}
	
	/**
	 * add a value to an array and return a new array
	 * @param old array
	 * @param val value to add
	 * @return concatanated arrays
	 */
	public static <T> T[] addAll(T[] a, T b){
		Class cls = null;
		if(a.length > 0)
			cls = a[0].getClass();
		else if(b != null)
			cls = b.getClass();
		else
			cls = Object.class;
		T[] c = (T []) Array.newInstance(cls,a.length+1);
		System.arraycopy(a, 0, c, 0,a.length);
		c[c.length-1] = b;
		return c;
	}
	
	/**
	 * add a value to an array and return a new array
	 * @param old array
	 * @param val value to add
	 * @return concatanated arrays
	 */
	public static <T> T[] addAll(T[] a, T[] b){
		Class cls = null;
		if(a.length > 0)
			cls = a[0].getClass();
		else if(b.length > 0)
			cls = b[0].getClass();
		else
			cls = Object.class;
		T[] c = (T []) Array.newInstance(cls,a.length+b.length);
		System.arraycopy(a, 0, c, 0,a.length);
		System.arraycopy(b, 0, c, a.length, b.length);
		return c;
	}
	
	/**
	 * get character count for a given string
	 * @param text
	 * @param test
	 * @return
	 */
	public static int charCount(String text, char test ){
		int count = 0;
		for(char x: text.toCharArray()){
			if(x == test)
				count++;
		}
		return count;
	}
	
	
	/**
	 * for each character in the target strings sets case in the source string
	 * @param source - string to change case
	 * @param target - example string to take character case info from
	 * @return result string that has case that resembles target string
	 * NOTE: if target string is shorter then source string, then the remainder of
	 * the source string will use the case of the last character of the target string
	 */
	public static String copyCharacterCase(String source, String target){
		StringBuffer str = new StringBuffer();
		char [] s = source.toCharArray();
		char [] t = target.toCharArray();
		int i=0;
		boolean toUpper = false,toLower = false;
		// copy case
		for(i=0;i<s.length && i<t.length;i++){
			toUpper = Character.isUpperCase(s[i]);
			toLower = Character.isLowerCase(s[i]);
			String tt = ""+t[i];
			str.append((toUpper)?tt.toUpperCase():(toLower)?tt.toLowerCase():tt);
		}
		// finish the string
		for(int j=i;j<t.length;j++){
			String tt = ""+t[j];
			str.append((toUpper)?tt.toUpperCase():(toLower)?tt.toLowerCase():tt);
		}
		
		return str.toString();
	}
	
	
	/**
	 * This function attempts to convert vaires types of input into numerical
	 * equivalent
	 */
	public static double parseDecimalValue(String text) {
		double value = 0;
		if(text == null)
			return value;
		
		// check if this is a float
		if (text.matches("\\d+\\.\\d+")) {
			// try to parse regular number
			try {
				value = Double.parseDouble(text);
			} catch (Exception ex) {
				ex.printStackTrace();
			}
		} else {
			value = parseIntegerValue(text);
		}
		return value;
	}

	/**
	 * This function attempts to convert vaires types of input into numerical
	 * equivalent
	 */
	public static int parseIntegerValue(String text) {
		int value = 0;

		// try to parse roman numerals
		if (text.matches("[IiVvXx]+")) {
			boolean oneLess = false;
			for (int i = 0; i < text.length(); i++) {
				switch (text.charAt(i)) {
				case 'i':
				case 'I':
					value++;
					oneLess = true;
					break;
				case 'v':
				case 'V':
					value += ((oneLess) ? 3 : 5);
					oneLess = false;
					break;
				case 'x':
				case 'X':
					value += ((oneLess) ? 8 : 10);
					oneLess = false;
					break;
				}
			}

			return value;
		}
		// try to parse words
		if (text.matches("[a-zA-Z]+")) {
			if (text.equalsIgnoreCase("zero"))
				value = 0;
			else if (text.equalsIgnoreCase("one"))
				value = 1;
			else if (text.equalsIgnoreCase("two"))
				value = 2;
			else if (text.equalsIgnoreCase("three"))
				value = 3;
			else if (text.equalsIgnoreCase("four"))
				value = 4;
			else if (text.equalsIgnoreCase("five"))
				value = 5;
			else if (text.equalsIgnoreCase("six"))
				value = 6;
			else if (text.equalsIgnoreCase("seven"))
				value = 7;
			else if (text.equalsIgnoreCase("eight"))
				value = 8;
			else if (text.equalsIgnoreCase("nine"))
				value = 9;
			else if (text.equalsIgnoreCase("ten"))
				value = 10;
			else if (text.equalsIgnoreCase("eleven"))
				value = 11;
			else if (text.equalsIgnoreCase("twelve"))
				value = 12;
			else
				value = (int) NO_VALUE;

			return value;
		}

		// try to parse regular number
		try {
			value = Integer.parseInt(text);
		} catch (NumberFormatException ex) {
			// ex.printStackTrace();
			return (int) NO_VALUE;
		}
		return value;
	}

	
	
	/**
	 * is string a number
	 * 
	 * @param text
	 * @return
	 */
	public static boolean isNumber(String text) {
		return text.matches("\\d+(\\.\\d+)?");
	}

	/**
	 * pretty print number as integer or 2 precision float format numeric value
	 * as string
	 * 
	 * @return
	 */
	public static String toString(double numericValue) {
		Formatter f = new Formatter();
		if ((numericValue * 10) % 10 == 0)
			f.format("%d", (int) numericValue);
		else
			f.format("%.2f", numericValue);
		return "" + f.out();
	}

	/**
	 * get a string with each word being capitalized
	 * @param text
	 * @return
	 */
	public static String getCapitalizedWords(String text){
		StringBuffer b = new StringBuffer();
		for(String s: text.split("[\\s_]+")){
			if(s.length() > 2)
				b.append(Character.toUpperCase(s.charAt(0))+s.substring(1).toLowerCase()+" ");
			else
				b.append(s.toLowerCase()+" ");
		}
		return b.toString().trim();
	}
	
	
	/**
	 * parse CSV line (take care of double quotes)
	 * @param line
	 * @param delimeter
	 * @return
	 */
	public static List<String> parseCSVline(String line){
		return parseCSVline(line, ',');
	}
	
	/**
	 * parse CSV line (take care of double quotes)
	 * @param line
	 * @param delimeter
	 * @return
	 */
	public static List<String> parseCSVline(String line,char delim){
		List<String> fields = new ArrayList<String>();
		boolean inquotes = false;
		int st = 0;
		for(int i = 0;i<line.length();i++){
			// start/end quotes
			if(line.charAt(i) == '"'){
				inquotes ^= true;
			}
			// found delimeter (use it)
			if(!inquotes && line.charAt(i) == delim){
				String s = line.substring(st,i).trim();
				if(s.startsWith("\"") && s.endsWith("\""))
					s = s.substring(1,s.length()-1);
				fields.add(s.trim());
				st = i+1;
			}
		}
		// handle last field
		if(st < line.length()){
			String s = line.substring(st).trim();
			if(s.startsWith("\"") && s.endsWith("\""))
				s = s.substring(1,s.length()-1);
			fields.add(s.trim());
		}
		return fields;
	}
	
	
	/**
	 * Parse English sentences from a blurb of text. 
	 * Each sentence should be terminated by .! or ?
	 * Periods in digits and some acronyms should be skipped
	 * @param txt
	 * @return
	 */
	public static List<String> getSentences(String txt) {
		return SentenceDetector.getSentences(txt);
	}
	
	/**
	 * determine if input line is a recognizable report section?
	 * @param line
	 * @return
	 */
	public static boolean isReportSection(String line){
		return line.matches("^\\[[A-Za-z \\-]*\\]$") || line.matches("^[A-Z \\-]*:$");
	}
	
	/**
	 * 
	 * @param line
	 * @return int [3] number of upper case chars,
	 */
	public static StringStats getStringStats(String line){
		StringStats st = new StringStats();
		st.length = line.length();
		char [] c = line.toCharArray();
		for(int i=0;i<c.length;i++){
			if(Character.isUpperCase(c[i]))
				st.upperCase++;
			if(Character.isLowerCase(c[i]))
				st.lowerCase++;
			if(Character.isDigit(c[i]))
				st.digits++;
			if(Character.isWhitespace(c[i]))
				st.whiteSpace++;
			if(Character.isAlphabetic(c[i]))
				st.alphabetic++;
		}
		st.isUppercase = st.upperCase == st.length;
		st.isLowercase = st.lowerCase == st.length;
		st.isCapitalized = st.length > 0 && Character.isUpperCase(c[0]) && st.lowerCase == st.alphabetic -1;
		
		return st;
	}
	
	/**
	 * parse Date represented as a string if it matches some 
	 * common date pattern
	 * @param text
	 * @return
	 */
	public static Date parseDate(String text){
		if(timePatterns == null){
			timePatterns = new HashMap<String,String>();
			timePatterns.put("[12][09]\\d{2}[01]\\d{3} \\d{4}","yyyyMMdd HHmm");
			timePatterns.put("[12][09]\\d{2}[01]\\d{3}","yyyyMMdd");
			timePatterns.put("\\d{1,2}/\\d{1,2}/\\d{4}","MM/dd/yyyy");
			timePatterns.put("([A-Z][a-z]+ ){2}\\d{1,2} [\\d:]+ [A-Z]+ \\d{4}","EEE MMM dd kk:mm:ss z yyyy"); //Fri Jan 18 10:50:00 EST 2013
			
			/*
			 * 
			 
			("dd-MMM-yy hh:mm:ss a")
			("dd MMM yyyy hh:mm aa")
			("ddMMyy")
			("dd-MM-yyyy")
			("ddMMyyyy")
			("dd:MM:yyyy HH:mm")
			("EEE, MMM.d.yyyy")
			("E MMM dd HH:mm:ss z yyyy")
			(format)
			(formatString, Locale.US)
			(getDatePattern(), locale)
			("HH:mm:ss.S")
			("HH:mm:ss,SSS")
			("MM")
			("MM-dd-yy HH-mm-ss")
			("MM-dd-yy_HH-mm-ss")
			("MM-dd-yy HH:mm:ss.SSS")
			("MM / dd / yyyy")
			("MM-dd-yyyy")
			("MM/dd/yyyy")
			("MM'/'dd'/'yyyy")
			("MMddyyyy")
			("MM-dd-yyyy HH:mm")
			("MM.dd.yyyy hh:mm a")
			("MM/dd/yyyy hh:mm a z")
			("MM/dd/yyyy HH:mm:ss")
			("MM/dd/yyyy HH:mm:ss.SS")
			("MM / d / yyyy")
			("MMM d yyyy")
			("MMM d yyyy HH:mm")
			("MMMMMMMMM")
			(); //new SimpleDateFormat("dd-MM-yyyy HH:mm:ss")
			(new SimpleDateFormat("MM/dd/yyyy HH:mm:ss.SS"))
			(new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date()))
			("yyMMdd")
			("yyMMdd_hh-mm-ss")
			("yyyy")
			("yyyy-MM-dd")
			("yyyyMMdd")
			("yyyy-MM-dd HH:mm")
			("yyyyMMdd HHmm")
			("yyyyMMddHHmm")
			("yyyy-MM-dd HH:mm:ss")
			("yyyyMMddHHmmss")
			("yyyy-MM-dd HH:mm:ss").parse(date)
			("yyyy-MM-dd HH:mm:ss.S")
			("yyyy-MM-dd hh:mm:ss.SSS")
			("yyyy-MM-dd-hh-mm-ss-SSSS")
			("_yyyy_MM_dd_HH_mm_ss_SSSS")
			("yyyyMMdd kkmm")

			 
			 
			 * 
			 */
			
			
		}
		text = text.trim();
		for(String timeExp: timePatterns.keySet()){
			if(text.matches(timeExp)){
				SimpleDateFormat sdf = new SimpleDateFormat(timePatterns.get(timeExp));
				try {
					return sdf.parse(text);
				} catch (ParseException e) {
					// so we failed... Oh well.
				}
			}
		}
		return null;
	}
	
	public static void main(String [] s) throws Exception{
		// test plurality
		/*System.out.println("testing plurality ..");
		for(String st: Arrays.asList("nevi","cells","buses","dolls","doors","margins","soldier")){ 
			System.out.println("\t"+st+" "+isPlural(st));
		}
		System.out.println("testing abbreviations ..");
		for(String st: Arrays.asList("SKIN","mDNA","BRCA2","Dolls","No","I","help-me")){ 
			System.out.println("\t"+st+" "+isLikelyAbbreviation(st));
		}*/
		
		/*Date dt = new Date();
		System.out.println(dt);
		String st = dt.toString();
		Date d2 = parseDate(st);
		System.out.println(d2);*/
	
		System.out.println(getStringStats("Cancer."));
	}
}
