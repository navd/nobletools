
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

/** Noble Coder Sentence
 * Updated by JCasGen Wed Aug 24 13:31:44 EDT 2016
 * @generated */
public class Sentence_Type extends Annotation_Type {
  /** @generated 
   * @return the generator for this type
   */
  @Override
  protected FSGenerator getFSGenerator() {return fsGenerator;}
  /** @generated */
  private final FSGenerator fsGenerator = 
    new FSGenerator() {
      public FeatureStructure createFS(int addr, CASImpl cas) {
  			 if (Sentence_Type.this.useExistingInstance) {
  			   // Return eq fs instance if already created
  		     FeatureStructure fs = Sentence_Type.this.jcas.getJfsFromCaddr(addr);
  		     if (null == fs) {
  		       fs = new Sentence(addr, Sentence_Type.this);
  			   Sentence_Type.this.jcas.putJfsFromCaddr(addr, fs);
  			   return fs;
  		     }
  		     return fs;
        } else return new Sentence(addr, Sentence_Type.this);
  	  }
    };
  /** @generated */
  @SuppressWarnings ("hiding")
  public final static int typeIndexID = Sentence.typeIndexID;
  /** @generated 
     @modifiable */
  @SuppressWarnings ("hiding")
  public final static boolean featOkTst = JCasRegistry.getFeatOkTst("edu.pitt.dbmi.nlp.noble.uima.types.Sentence");
 
  /** @generated */
  final Feature casFeat_sentenceType;
  /** @generated */
  final int     casFeatCode_sentenceType;
  /** @generated
   * @param addr low level Feature Structure reference
   * @return the feature value 
   */ 
  public String getSentenceType(int addr) {
        if (featOkTst && casFeat_sentenceType == null)
      jcas.throwFeatMissing("sentenceType", "edu.pitt.dbmi.nlp.noble.uima.types.Sentence");
    return ll_cas.ll_getStringValue(addr, casFeatCode_sentenceType);
  }
  /** @generated
   * @param addr low level Feature Structure reference
   * @param v value to set 
   */    
  public void setSentenceType(int addr, String v) {
        if (featOkTst && casFeat_sentenceType == null)
      jcas.throwFeatMissing("sentenceType", "edu.pitt.dbmi.nlp.noble.uima.types.Sentence");
    ll_cas.ll_setStringValue(addr, casFeatCode_sentenceType, v);}
    
  



  /** initialize variables to correspond with Cas Type and Features
	 * @generated
	 * @param jcas JCas
	 * @param casType Type 
	 */
  public Sentence_Type(JCas jcas, Type casType) {
    super(jcas, casType);
    casImpl.getFSClassRegistry().addGeneratorForType((TypeImpl)this.casType, getFSGenerator());

 
    casFeat_sentenceType = jcas.getRequiredFeatureDE(casType, "sentenceType", "uima.cas.String", featOkTst);
    casFeatCode_sentenceType  = (null == casFeat_sentenceType) ? JCas.INVALID_FEATURE_CODE : ((FeatureImpl)casFeat_sentenceType).getCode();

  }
}



    