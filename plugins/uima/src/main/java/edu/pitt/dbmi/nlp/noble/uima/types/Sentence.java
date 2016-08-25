

/* First created by JCasGen Wed Aug 24 13:31:44 EDT 2016 */
package edu.pitt.dbmi.nlp.noble.uima.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Noble Coder Sentence
 * Updated by JCasGen Wed Aug 24 13:31:44 EDT 2016
 * XML source: /home/tseytlin/Work/nobletools/plugins/uima/src/main/resources/edu/pitt/dbmi/nlp/noble/uima/types/TypeSystem.xml
 * @generated */
public class Sentence extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Sentence.class);
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int type = typeIndexID;
  /** @generated
   * @return index of the type  
   */
  @Override
  public              int getTypeIndexID() {return typeIndexID;}
 
  /** Never called.  Disable default constructor
   * @generated */
  protected Sentence() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Sentence(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Sentence(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Sentence(JCas jcas, int begin, int end) {
    super(jcas);
    setBegin(begin);
    setEnd(end);
    readObject();
  }   

  /** 
   * <!-- begin-user-doc -->
   * Write your own initialization here
   * <!-- end-user-doc -->
   *
   * @generated modifiable 
   */
  private void readObject() {/*default - does nothing empty block */}
     
 
    
  //*--------------*
  //* Feature: sentenceType

  /** getter for sentenceType - gets Type of sentence.
   * @generated
   * @return value of the feature 
   */
  public String getSentenceType() {
    if (Sentence_Type.featOkTst && ((Sentence_Type)jcasType).casFeat_sentenceType == null)
      jcasType.jcas.throwFeatMissing("sentenceType", "edu.pitt.dbmi.nlp.noble.uima.types.Sentence");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Sentence_Type)jcasType).casFeatCode_sentenceType);}
    
  /** setter for sentenceType - sets Type of sentence. 
   * @generated
   * @param v value to set into the feature 
   */
  public void setSentenceType(String v) {
    if (Sentence_Type.featOkTst && ((Sentence_Type)jcasType).casFeat_sentenceType == null)
      jcasType.jcas.throwFeatMissing("sentenceType", "edu.pitt.dbmi.nlp.noble.uima.types.Sentence");
    jcasType.ll_cas.ll_setStringValue(addr, ((Sentence_Type)jcasType).casFeatCode_sentenceType, v);}    
  }

    