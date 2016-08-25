

Installation Instructions for Noble Coder UIMA Text Annotation Engines.

I. Prerequisites:

1) Work inside the Eclipse IDE
   https://www.eclipse.org/downloads/
   This work was tested on eclipse Kepler but should be able to run on other version
   combinations.  older or newer versions should work.
2) Configure you Eclipse IDE to work with SVN
   http://www.eclipse.org/subversive/installation-instructions.php
3) Establish UIMA 
   https://uima.apache.org/doc-uima-examples.html
4) Establish cTAKES 
   https://cwiki.apache.org/confluence/display/CTAKES/cTAKES+3.2+Developer+Install+Guide
   (You can start from cTAKES and follow the cTAKES prerequisite steps which
    will not establish as rich a UIMA examples environment.  But that environment
    is not necessary for NobleCoder Annotator deployment.)
5) Pull the latest nobletools project from SourceForge SVN
   https://svn.code.sf.net/p/nobletools/code/
   Use checkout as anonymous from the trunk
   
If all goes well you should have a robust Eclipse/UIMA/cTAKE/NobleCoder environment

The nobletools should contain a plugins/uima directory.
This contains all of the files necessary to deploy a Noble Coder in a UIMA
pipeline.

II. Run Scenarios

We provide two run scenarios hopefully helpful to demonstrate the Noble Coder
UIMA wrapper.   

1) Running Stand Alone on an arbitrary Cas Visual Debugger

The first is a stand alone aggregate analysis engine that utilizes a
sentence parser from the LingPipe Information Extraction System.
Document text is annotated as a set of Sentences and each Sentence underlying
text is fed into NobleCoder to be conceptualized.   The National Cancer Institute
Thesaurus is provided as a NobleCoder ready terminology along with the SVN footprint
you checked out.

From steps 2 and 3 you will have sample run configurations for the UIMA CVD.
(CAS Visual Debugger). Choose one in which to run the NobleCoder standalone
version.

  a) Edit the CAS Visual Debugger run configuration and add

<installation nc>\nobletools\plugins\uima\target\classes
<installation nc>\nobletools\plugins\uima\lib\NobleTools.jar
<installation nc>\nobletools\plugins\uima\lib\jdbm-3.0.jar
<installation nc>\nobletools\plugins\uima\lib\lingpipe-4.1.0.jar
<installation nc>\nobletools\plugins\uima\lib\commons-lang3-3.3.2.jar

where <installation nc> is where you established the SVN nobletools project.

  b) Run the CAS Visual Debugger.
  
  c) In the Text Pane cut and past the following
  
Burkitt Lymphoma.
Fusobacterium.
Giant Lymph Node Hyperplasia.
Angiolymphoid Hyperplasia with Eosinophilia.
Carcinoma .
Chondrodysplasia Punctata.
Dementia, Vascular.
Fanconi Anemia.
Osteochondrodysplasias.

   d) Run -> Load AE
   
 <installation nc>\nobletools\plugins\uima\desc\aggregate\aggNcStandAlone.xml
 
   This will load and initialize the Aggregate Annotation Engine.
   
   e) Run -> Run aggNcStandAlone
   
   This will run the aggregate TAE on the sample text. You should be able
 to drill into a number of Sentence and Concept annotations.

2) Create a new Aggregate Text Analyzer that uses cTAKES front end to provide
conceptualization target text.

   a)  Prepare the ctakes-clinical-pipeline project class path as in 1.a above but also add resources
   as show here:  Not you could also add resources in the last example and use a partial path
   to link up the NobleCoder terminology Resource.

       <installation nc>\nobletools\plugins\uima\target\classes
       <installation nc>\nobletools\plugins\uima\resources
       <installation nc>\nobletools\plugins\uima\lib\NobleTools.jar
       <installation nc>\nobletools\plugins\uima\lib\jdbm-3.0.jar
       <installation nc>\nobletools\plugins\uima\lib\lingpipe-4.1.0.jar
       <installation nc>\nobletools\plugins\uima\lib\commons-lang3-3.3.2.jar
   
   b) Navigate to desc/analysis_engine 
      right mouse and New -> Other -> UIMA -> Analysis Engine Descriptor File
      Use Next to continue.
      File name: aggNcCtakes.xml  (the name is arbitrary but will be assumed 
                                for the remainder of the example)

    c) Right Mouse Open the aggNcCtakes.xml with the UIMA component editor
       Choose Aggregate.  Accept any warnings and defaults.

     d) On the Aggregate Tab load primitive TAs to make
        the aggregate behavior.  Use 3 TAs from cTAKES and the nobletools
        NobleCoder wrapper.

    Component Engines ADD then Import by Location (do process for each...)
   
        ctakes-core -> desc -> analysis_engine -> SimpleSegmentAnnotator.xml
        ctakes-core -> desc -> analysis_engine -> SentenceDetectorAnnotator.xml
        ctakes-core -> desc -> analysis_engine -> TokenizerAnnotator.xml
        nobletools -> plugins -> uima -> desc -> primitive -> AeNobleCoderUimaAnnotator.xml

      e) Go to the parameters tab and override unshared the SpanFeatureStructure
         Then on the parameters settings tab  add it and set its value to 
         
         org.apache.ctakes.typesystem.type.textspan.Sentence
         
       f) go to the resources tab and link the NobleCoder terminology to file:/ncit/META.term/info.txt
       
       g) go to the capabilities tab and set Sentence from uima as an input and Concept from nobletools
          as oputput.
          
       h) Configure UIMA_CVD--clinical_documents_pipeline as in 1.a.  And run the aggregate as in 1.d, 1.e.
       
 
An example aggNcCtakes is released in plugins/uima/desc/aggregates/aggNcCtakes.xml.  This file may work.
But it may require file path adjustments for the local cTAKES environemnt.
