package edu.pitt.dbmi.nlp.noble.coder.model;

/**
 * An interface that marks a span of text
 * @author tseytlin
 *
 */
public interface Spannable {
	/**
	 * text covered by this span
	 * @return
	 */
	public String getText();
	/**
	 * offset of this span from the start of the document
	 * @return
	 */
	public int getStartPosition();
	
	/**
	 * end offset of this span
	 * @return
	 */
	public int getEndPosition();
	
	
	/**
	 * is this spannable region contains another
	 * @param s
	 * @return
	 */
	public boolean contains(Spannable s);
	
	
	/**
	 * is this spannable region intersects another
	 * @param s
	 * @return
	 */
	public boolean intersects(Spannable s);

	/**
	 * is this spannable region before another
	 * @param s
	 * @return
	 */
	public boolean before(Spannable s);


	/**
	 * is this spannable region before another
	 * @param s
	 * @return
	 */
	public boolean after(Spannable s);
}
