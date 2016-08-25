Installation Instructions for Noble Coder Gate Processing Resource

I. Prerequisites:

1) Work inside the Eclipse IDE
   https://www.eclipse.org/downloads/
   This work was tested on eclipse Kepler but should be able to run on other version
   combinations.  older or newer versions should work.
2) Configure you Eclipse IDE to work with SVN
   http://www.eclipse.org/subversive/installation-instructions.php
3) Pull the latest nobletools project from SourceForge SVN
   https://svn.code.sf.net/p/nobletools/code/
   Use checkout as anonymous from the trunk
4) Download and Install GATE
   https://gate.ac.uk/download/

II. Build a test Corpus
1) Start GATE
2) Language Resources (right mouse) -> New Document
3) Double click on the Document
4) Past Biology Text into the document (e.g.,

Burkitt Lymphoma.
Fusobacterium.
Giant Lymph Node Hyperplasia.
Angiolymphoid Hyperplasia with Eosinophilia.
Carcinoma .
Chondrodysplasia Punctata.
Dementia, Vascular.
Fanconi Anemia.
Osteochondrodysplasias.

5) Right click on the document name -> New Corpus with this Document
   call it something like TestNobleCorpus

III. Run PlugIn

2) Use Manage Creole Plugins (Puzzle Piece Icon on Toolbar) to
   bring up the Creole Plugin Manager then click the plus button to
   add a Register a new Creole directory.  Navigate to the plugins/gate directory
   in your local nobletools footprint.
3) Once having established the creole directory check to make sure its in the
   the list and that Load Now is selected.  You may also select Load Always if you
   want this Processing Resource to be available on each local Gate startup.
   Click Apply All / Close to return to Gate.
4) Now you can right select Processing Resources (right mouse down) -> New -> 
   NobleCoderGateProcessingResource.
5) Build a preprocessing pipeline of PRs first creating
 
 - Document Resetter
 - ANNIE English Tokeniser
 - ANNIE Sentence Splitter. 
 
   in addition to your NobleCoderGateProcessingResource 
 
   (Applications (right mouse down) New -> Corpus Pipeline
   And move the four PRs into the Application.
   
6) Click on NobleCoderGateProcessingResource and check the 
   input parameters.
   Make sure the NobleCoder's terminologyURL is pointing at the info.txt file beneath the
   plugins/gate/resources/ncit/Meta.term directory.
7) Set the InputAnnotationType to Sentence
8) Set the OutputAnnotationType to Concept
9) Set the Corpus for the test app to TestNobleCorpus
10) Run this Application

IV. Inspect results.

Drill into the sample Document.
Concept annotations should have been generated from the NCIT run over this text.

