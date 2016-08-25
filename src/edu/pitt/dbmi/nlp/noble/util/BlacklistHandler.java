package edu.pitt.dbmi.nlp.noble.util;

import java.io.*;
import java.util.*;

import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology.WordStat;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

/**
 * create a blacklist for a terminology that takes top 1% of the most frequent words (by number of terms that include them)
 * and creates a blacklist that avoids an expensive lookup
 * @author tseytlin
 */
public class BlacklistHandler {
	public static final double CUTOFF = 0.002;
	public static final double TOP_CUTOFF = 0.1;
	public static final String BLACKLIST = "table_blacklist.d.0";
	private NobleCoderTerminology.Storage st;
	private Map<String,Set<String>> blacklist;
	private boolean debug = true;
	
	/**
	 * initialize blacklist handler with a terminology
	 * @param term
	 */
	public BlacklistHandler(NobleCoderTerminology term){
		st = term.getStorage();
	}
	
	/**
	 * get top words based on global cutoff
	 * @param map
	 * @return
	 */
	private List<String> getTopWords(){
		// make a smaller copy of reasanable size
		final Map<String,WordStat> map = st.getWordStatMap();
		TreeMap<String,Integer> tree = new TreeMap<String, Integer>(new Comparator<String>() {
			public int compare(String o1, String o2) {
				int n = map.get(o2).termCount-map.get(o1).termCount;
				return n == 0?o2.compareTo(o1):n;
			}
		});
		// add anything that is larger then 100 terms per word to smaller sorted map
		for(String word: map.keySet()){
			int num = map.get(word).termCount;
			if(num > 100)
				tree.put(word,num);
		}
		if(debug){
			System.out.println("total words: "+map.size()+", frequent words (> 100): "+tree.size()+", cutoff: "+CUTOFF);
		}
		// now take top 1% off
		return getTopWords(tree.keySet(),(int)(map.size()* CUTOFF));
	}
	
	/**
	 * get the top words from a set of top words
	 * @param top
	 * @return
	 */
	
	private List<String> getTopWords(Collection<String> list, int size) {
		// now take top 1% off
		int i = 1;
		List<String> words = new ArrayList<String>();
		for(String word: list){
			if(i++ > size)
				break;
			words.add(word);
		}
		return words;
	}
	
	/**
	 * create a blacklist of most common words that are associated with
	 * a list of terms that are in this list
	 * @param top
	 * @return
	 */
	public Map<String,Set<String>> getBlacklist(){
		if(blacklist == null){
			List<String> top = getTopWords();
			List<String> ttop = getTopWords(top,(int)(top.size()*TOP_CUTOFF));
			if(debug){
				System.out.println("top words: "+top.size()+", super top words: "+ttop.size()+", cutoff: "+TOP_CUTOFF);
			}
			LinkedHashMap<String,Set<String>> list = new LinkedHashMap<String, Set<String>>();
			for(String word: top){
				boolean isTTOP = ttop.contains(word);
				Set<String> terms = new LinkedHashSet<String>();
				// find terms that are within this set
				Set<String> tterms = st.getWordMap().get(word);
				for(String t: tterms){
					if(contains(t,isTTOP?ttop:top)){ //isTTOP?ttop:top
						terms.add(t);
					}
				}
				//System.out.println(word+"\t"+tterms.size()+" -> "+terms.size());
				list.put(word,terms);
			}
			blacklist = list;
		}
		return blacklist;
	}
	
	

	/**
	 * does the blacklist exist for this terminology
	 * @return
	 */
	
	public boolean hasBlacklist(){
		return new File(st.getLocation(),BLACKLIST).exists();
	}
	
	/**
	 * save blacklist in an appropriate location
	 * @throws IOException 
	 */
	public void save() throws IOException{
		String prefix = st.getLocation().getAbsolutePath()+File.separator+"table";
		JDBMMap blacklist = new JDBMMap<String,Set<String>>(prefix,"blacklist",false);
		blacklist.putAll(getBlacklist());
		blacklist.commit();
		blacklist.compact();
		blacklist.dispose();
	}
	
	/**
	 * load existing blacklist
	 * @throws IOException
	 */
	public void load() throws IOException {
		if(hasBlacklist()){
			String prefix = st.getLocation().getAbsolutePath()+File.separator+"table";
			blacklist = new JDBMMap<String,Set<String>>(prefix,"blacklist",true);
		}
	}
	
	/**
	 * Are all words in this term contain words menitioned in a list
	 * @param t
	 * @param top
	 * @return
	 */
	
	private boolean contains(String term, List<String> words) {
		boolean all = true;
		List<String> twords  = TextTools.getWords(term);
		// if at least one word not in list of words, don't have a match
		for(String tword : twords ){
			// if term word doesn't occur in text, then NO match
			if(!words.contains(tword)){
				all = false;
				break;
			}
		}
		return all;
	}
	
	
	public static void main(String[] args) throws Exception {
		String name = "RadLex";
		NobleCoderTerminology term = new NobleCoderTerminology(name);
		BlacklistHandler bh = new BlacklistHandler(term);
		System.out.println("calculating blacklist ...");
		Map<String,Set<String>> list = bh.getBlacklist();
		System.out.println("top words: "+list.keySet());
		int n = 0;
		for(String word: list.keySet()){
			n+= list.get(word).size();
			//System.out.println("\t"+word+" : "+(list.get(word).size()));
		}
		System.out.println("identified "+list.size()+" high frequency words with "+n+" associated terms ..");
		
		
		System.out.println("saving ..");
		bh.save();
	}

}
