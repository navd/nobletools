package edu.pitt.dbmi.nlp.noble.uima;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.jcas.JCas;
import org.apache.uima.resource.ResourceInitializationException;

import edu.pitt.dbmi.nlp.noble.coder.model.Document;
import edu.pitt.dbmi.nlp.noble.coder.model.Section;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.coder.processor.DocumentProcessor;
import edu.pitt.dbmi.nlp.noble.coder.processor.PartProcessor;
import edu.pitt.dbmi.nlp.noble.uima.types.SynopticSection;

/**
 * Define document structure s.a. Sentences, Sections, Parts and SynopticSections
 * @author tseytlin
 *
 */
public class DocumentStructureAnnotator extends JCasAnnotator_ImplBase {
	private DocumentProcessor documentProcessor;
	private PartProcessor partProcessor;
	
	
	public void initialize(UimaContext aContext) throws ResourceInitializationException {
		super.initialize(aContext);
		documentProcessor = new DocumentProcessor();
		partProcessor = new PartProcessor();
	}
	
	/**
	 * process a document
	 */
	public void process(JCas jcas) throws AnalysisEngineProcessException {
		String text = jcas.getDocumentText();
		Document doc = documentProcessor.process(text);
		
		// annotate sentences
		int synopticStart = -1, synopticEnd = -1;
		for(Sentence sentence : doc.getSentences()){
			edu.pitt.dbmi.nlp.noble.uima.types.Sentence sentenceAnnotation = new edu.pitt.dbmi.nlp.noble.uima.types.Sentence(jcas);
			sentenceAnnotation.setBegin(sentence.getStartPosition());
			sentenceAnnotation.setEnd(sentence.getEndPosition());
			sentenceAnnotation.setSentenceType(sentence.getSentenceType());
			sentenceAnnotation.addToIndexes();
			
			// if this sentence belongs to synoptic section, then remember its start and end position
			if(Sentence.TYPE_WORKSHEET.equals(sentence.getSentenceType())){
				if(synopticStart < 0)
					synopticStart = sentence.getStartPosition();
				synopticEnd = sentence.getEndPosition();
			}else if(synopticStart >= 0){
				
				SynopticSection synopticAnnotation = new SynopticSection(jcas);
				synopticAnnotation.setBegin(synopticStart);
				synopticAnnotation.setEnd(synopticEnd);
				synopticAnnotation.addToIndexes();
				// reset the counters
				synopticStart = synopticEnd = -1;
				
			}
			
		}

		// annotate sections of the document
		for(Section section : doc.getSections()){
			edu.pitt.dbmi.nlp.noble.uima.types.Section sectionAnnotation = new edu.pitt.dbmi.nlp.noble.uima.types.Section(jcas);
			sectionAnnotation.setBegin(section.getStartPosition());
			sectionAnnotation.setEnd(section.getEndPosition());
			sectionAnnotation.setTitle(section.getTitle());
			sectionAnnotation.setBody(section.getBody());
			sectionAnnotation.setBodyOffset(section.getBodyOffset());
			sectionAnnotation.addToIndexes();
		
			// given this section, lets detect parts
			section = partProcessor.process(section);
			for(Section part: section.getSections()){
				edu.pitt.dbmi.nlp.noble.uima.types.Part partAnnotation = new edu.pitt.dbmi.nlp.noble.uima.types.Part(jcas);
				partAnnotation.setBegin(part.getStartPosition());
				partAnnotation.setEnd(part.getEndPosition());
				partAnnotation.setTitle(part.getTitle());
				partAnnotation.setBody(part.getBody());
				partAnnotation.setBodyOffset(part.getBodyOffset());
				partAnnotation.addToIndexes();
			}
		
		}
		
	}

}
