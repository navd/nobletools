
/* First created by JCasGen Wed Aug 24 13:31:44 EDT 2016 */
package edu.pitt.dbmi.nlp.noble.uima.types;

import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.JCasRegistry;
import org.apache.uima.cas.impl.CASImpl;
import org.apache.uima.cas.impl.FSGenerator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.impl.TypeImpl;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.impl.FeatureImpl;
import org.apache.uima.cas.Feature;
import org.apache.uima.jcas.tcas.Annotation_Type;

/** Noble Coder Section
 * Updated by JCasGen Wed Aug 24 13:31:44 EDT 2016
 * @generated */
public class Section_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Section_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Section_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Section(addr, Section_Type.this);
  			   Section_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Section(addr, Section_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Section.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.pitt.dbmi.nlp.noble.uima.types.Section");
 
  /** @generated */
  final Feature casFeat_title;
  /** @generated */
  final int     casFeatCode_title;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getTitle(int addr) {
        if (featOkTst && casFeat_title == null)
      jcas.throwFeatMissing("title", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    return ll_cas.ll_getStringValue(addr, casFeatCode_title);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setTitle(int addr, String v) {
        if (featOkTst && casFeat_title == null)
      jcas.throwFeatMissing("title", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    ll_cas.ll_setStringValue(addr, casFeatCode_title, v);}
    
  
 
  /** @generated */
  final Feature casFeat_body;
  /** @generated */
  final int     casFeatCode_body;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getBody(int addr) {
        if (featOkTst && casFeat_body == null)
      jcas.throwFeatMissing("body", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    return ll_cas.ll_getStringValue(addr, casFeatCode_body);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setBody(int addr, String v) {
        if (featOkTst && casFeat_body == null)
      jcas.throwFeatMissing("body", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    ll_cas.ll_setStringValue(addr, casFeatCode_body, v);}
    
  
 
  /** @generated */
  final Feature casFeat_bodyOffset;
  /** @generated */
  final int     casFeatCode_bodyOffset;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public int getBodyOffset(int addr) {
        if (featOkTst && casFeat_bodyOffset == null)
      jcas.throwFeatMissing("bodyOffset", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    return ll_cas.ll_getIntValue(addr, casFeatCode_bodyOffset);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setBodyOffset(int addr, int v) {
        if (featOkTst && casFeat_bodyOffset == null)
      jcas.throwFeatMissing("bodyOffset", "edu.pitt.dbmi.nlp.noble.uima.types.Section");
    ll_cas.ll_setIntValue(addr, casFeatCode_bodyOffset, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Section_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_title = jcas.getRequiredFeatureDE(casType, "title", "uima.cas.String", featOkTst);
    casFeatCode_title  = (null == casFeat_title) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_title).getCode();

 
    casFeat_body = jcas.getRequiredFeatureDE(casType, "body", "uima.cas.String", featOkTst);
    casFeatCode_body  = (null == casFeat_body) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_body).getCode();

 
    casFeat_bodyOffset = jcas.getRequiredFeatureDE(casType, "bodyOffset", "uima.cas.Integer", featOkTst);
    casFeatCode_bodyOffset  = (null == casFeat_bodyOffset) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_bodyOffset).getCode();

  }
}



    