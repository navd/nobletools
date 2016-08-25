

/* First created by JCasGen Wed Aug 24 13:31:44 EDT 2016 */
package edu.pitt.dbmi.nlp.noble.uima.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** Noble Coder Section
 * Updated by JCasGen Wed Aug 24 13:31:44 EDT 2016
 * XML source: /home/tseytlin/Work/nobletools/plugins/uima/src/main/resources/edu/pitt/dbmi/nlp/noble/uima/types/TypeSystem.xml
 * @generated */
public class Section extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Section.class);
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
  protected Section() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Section(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Section(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Section(JCas jcas, int begin, int end) {
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
  //* Feature: title

  /** getter for title - gets Name of section header
   * @generated
   * @return value of the feature 
   */
  public String getTitle() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_title == null)
      jcasType.jcas.throwFeatMissing("title", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Section_Type)jcasType).casFeatCode_title);}
    
  /** setter for title - sets Name of section header 
   * @generated
   * @param v value to set into the feature 
   */
  public void setTitle(String v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_title == null)
      jcasType.jcas.throwFeatMissing("title", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    jcasType.ll_cas.ll_setStringValue(addr, ((Section_Type)jcasType).casFeatCode_title, v);}    
   
    
  //*--------------*
  //* Feature: body

  /** getter for body - gets Body of the section
   * @generated
   * @return value of the feature 
   */
  public String getBody() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_body == null)
      jcasType.jcas.throwFeatMissing("body", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Section_Type)jcasType).casFeatCode_body);}
    
  /** setter for body - sets Body of the section 
   * @generated
   * @param v value to set into the feature 
   */
  public void setBody(String v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_body == null)
      jcasType.jcas.throwFeatMissing("body", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    jcasType.ll_cas.ll_setStringValue(addr, ((Section_Type)jcasType).casFeatCode_body, v);}    
   
    
  //*--------------*
  //* Feature: bodyOffset

  /** getter for bodyOffset - gets Offset of section body
   * @generated
   * @return value of the feature 
   */
  public int getBodyOffset() {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_bodyOffset == null)
      jcasType.jcas.throwFeatMissing("bodyOffset", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    return jcasType.ll_cas.ll_getIntValue(addr, ((Section_Type)jcasType).casFeatCode_bodyOffset);}
    
  /** setter for bodyOffset - sets Offset of section body 
   * @generated
   * @param v value to set into the feature 
   */
  public void setBodyOffset(int v) {
    if (Section_Type.featOkTst && ((Section_Type)jcasType).casFeat_bodyOffset == null)
      jcasType.jcas.throwFeatMissing("bodyOffset", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    jcasType.ll_cas.ll_setIntValue(addr, ((Section_Type)jcasType).casFeatCode_bodyOffset, v);}    
  }

    