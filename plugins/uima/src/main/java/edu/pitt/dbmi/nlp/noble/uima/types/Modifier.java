

/* First created by JCasGen Wed Aug 24 13:31:44 EDT 2016 */
package edu.pitt.dbmi.nlp.noble.uima.types;

import org.apache.uima.jcas.JCas; 
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.jcas.cas.TOP_Type;

import org.apache.uima.jcas.tcas.Annotation;


/** mention modifier
 * Updated by JCasGen Wed Aug 24 13:31:44 EDT 2016
 * XML source: /home/tseytlin/Work/nobletools/plugins/uima/src/main/resources/edu/pitt/dbmi/nlp/noble/uima/types/TypeSystem.xml
 * @generated */
public class Modifier extends Annotation {
  /** @generated
   * @ordered 
   */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = JCasRegistry.register(Modifier.class);
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
  protected Modifier() {/* intentionally empty block */}
    
  /** Internal - constructor used by generator 
   * @generated
   * @param addr low level Feature Structure reference
   * @param type the type of this Feature Structure 
   */
  public Modifier(int addr, TOP_Type type) {
    super(addr, type);
    readObject();
  }
  
  /** @generated
   * @param jcas JCas to which this Feature Structure belongs 
   */
  public Modifier(JCas jcas) {
    super(jcas);
    readObject();   
  } 

  /** @generated
   * @param jcas JCas to which this Feature Structure belongs
   * @param begin offset to the begin spot in the SofA
   * @param end offset to the end spot in the SofA 
  */  
  public Modifier(JCas jcas, int begin, int end) {
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
  //* Feature: name

  /** getter for name - gets type of modifier
   * @generated
   * @return value of the feature 
   */
  public String getName() {
    if (Modifier_Type.featOkTst && ((Modifier_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "edu.pitt.dbmi.nlp.noble.uima.types.Modifier");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Modifier_Type)jcasType).casFeatCode_name);}
    
  /** setter for name - sets type of modifier 
   * @generated
   * @param v value to set into the feature 
   */
  public void setName(String v) {
    if (Modifier_Type.featOkTst && ((Modifier_Type)jcasType).casFeat_name == null)
      jcasType.jcas.throwFeatMissing("name", "edu.pitt.dbmi.nlp.noble.uima.types.Modifier");
    jcasType.ll_cas.ll_setStringValue(addr, ((Modifier_Type)jcasType).casFeatCode_name, v);}    
   
    
  //*--------------*
  //* Feature: value

  /** getter for value - gets Modifier value
   * @generated
   * @return value of the feature 
   */
  public String getValue() {
    if (Modifier_Type.featOkTst && ((Modifier_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "edu.pitt.dbmi.nlp.noble.uima.types.Modifier");
    return jcasType.ll_cas.ll_getStringValue(addr, ((Modifier_Type)jcasType).casFeatCode_value);}
    
  /** setter for value - sets Modifier value 
   * @generated
   * @param v value to set into the feature 
   */
  public void setValue(String v) {
    if (Modifier_Type.featOkTst && ((Modifier_Type)jcasType).casFeat_value == null)
      jcasType.jcas.throwFeatMissing("value", "edu.pitt.dbmi.nlp.noble.uima.types.Modifier");
    jcasType.ll_cas.ll_setStringValue(addr, ((Modifier_Type)jcasType).casFeatCode_value, v);}    
  }

    