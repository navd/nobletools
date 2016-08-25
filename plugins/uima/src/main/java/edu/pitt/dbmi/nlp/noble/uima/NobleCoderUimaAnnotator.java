package edu.pitt.dbmi.nlp.noble.uima;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import edu.pitt.dbmi.nlp.noble.util.StringUtils;

import org.apache.uima.UimaContext;
import org.apache.uima.analysis_component.JCasAnnotator_ImplBase;
import org.apache.uima.analysis_engine.AnalysisEngineProcessException;
import org.apache.uima.analysis_engine.ResultSpecification;
import org.apache.uima.analysis_engine.annotator.AnnotatorInitializationException;
import org.apache.uima.cas.CAS;
import org.apache.uima.cas.FSIndex;
import org.apache.uima.cas.FSIterator;
import org.apache.uima.cas.FeatureStructure;
import org.apache.uima.cas.Type;
import org.apache.uima.cas.text.AnnotationFS;
import org.apache.uima.jcas.JCas;
import org.apache.uima.jcas.cas.FSArray;
import org.apache.uima.jcas.cas.StringArray;
import org.apache.uima.resource.ResourceInitializationException;
import org.apache.uima.util.Level;
import org.apache.uima.util.Logger;

import edu.pitt.dbmi.nlp.noble.coder.NobleCoder;
import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.coder.model.Modifier;
import edu.pitt.dbmi.nlp.noble.coder.model.Sentence;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Definition;
import edu.pitt.dbmi.nlp.noble.terminology.Relation;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Term;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;

public class NobleCoderUimaAnnotator extends JCasAnnotator_ImplBase {

	private static final String PARAM_DATA_BLOCK_FS = "SpanFeatureStructure";

	private NobleCoderTerminology terminology = null;
	private NobleCoder coder = null;

	/**
	 * type of annotation that defines a block for processing, e.g. a sentence
	 */

	private String spanFeatureStructureName;

	/** The type of structure annotations to consider */
	private Type spanFeatureStructureType;

	private Logger logger;

	private UimaContext uimaContext;

	public NobleCoderUimaAnnotator() {
		// System.out.println("Constructing a NobleCoderUimaAnnotator.");
	}

	/**
	 * Initialize the annotator, which includes compilation of regular
	 * expressions, fetching configuration parameters from XML descriptor file,
	 * and loading of the dictionary file.
	 */
	public void initialize(UimaContext uimaContext) throws ResourceInitializationException {

		this.uimaContext = uimaContext;
		logger = uimaContext.getLogger();
		initializeComponents(uimaContext);
		initializeParameters(uimaContext);
		// displayTerminologyDiagnostics();
	}

	private void initializeComponents(UimaContext uimaContext) throws ResourceInitializationException {
		try {
			String terminologyPath = (String) uimaContext.getConfigParameterValue("terminology.name");
			
			terminology = new NobleCoderTerminology(terminologyPath);
			coder = new NobleCoder(terminology);
			coder.setContextDetection(((Boolean) uimaContext.getConfigParameterValue("context.detection")).booleanValue());
			coder.setAcronymExpansion(((Boolean) uimaContext.getConfigParameterValue("acronym.expansion")).booleanValue());
		
		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	private void initializeParameters(UimaContext uimaContext) throws ResourceInitializationException {
		try {
			terminology.setContiguousMode(((Boolean) uimaContext.getConfigParameterValue("contiguous.mode")).booleanValue());
			terminology.setDefaultSearchMethod((String) uimaContext.getConfigParameterValue("default.search.method"));
			terminology.setIgnoreAcronyms(((Boolean) uimaContext.getConfigParameterValue("ignore.acronyms")).booleanValue());
			terminology.setIgnoreCommonWords(((Boolean) uimaContext.getConfigParameterValue("ignore.common.words")).booleanValue());
			terminology.setIgnoreSmallWords(((Boolean) uimaContext.getConfigParameterValue("ignore.small.words")).booleanValue());
			terminology.setIgnoreUsedWords(((Boolean) uimaContext.getConfigParameterValue("ignore.used.words")).booleanValue());
			terminology.setOrderedMode(((Boolean) uimaContext.getConfigParameterValue("ordered.mode")).booleanValue());
			terminology.setOverlapMode(((Boolean) uimaContext.getConfigParameterValue("overlap.mode")).booleanValue());
			terminology.setPartialMode(((Boolean) uimaContext.getConfigParameterValue("partial.mode")).booleanValue());
			terminology.setScoreConcepts(((Boolean) uimaContext.getConfigParameterValue("score.concepts")).booleanValue());
			terminology.setSelectBestCandidate(((Boolean) uimaContext.getConfigParameterValue("select.best.candidate")).booleanValue());
			terminology.setSubsumptionMode(((Boolean) uimaContext.getConfigParameterValue("subsumption.mode")).booleanValue());
			terminology.setMaximumWordGap(((Integer) uimaContext.getConfigParameterValue("maximum.word.gap")).intValue());
			terminology.setWindowSize(((Integer) uimaContext.getConfigParameterValue("window.size")).intValue());
			
			parameterizeSemanticTypes(uimaContext);
			parameterizeSources(uimaContext);
			
			
			spanFeatureStructureName = (String) uimaContext.getConfigParameterValue(PARAM_DATA_BLOCK_FS);

		} catch (Exception e) {
			throw new ResourceInitializationException(e);
		}
	}

	private void parameterizeSemanticTypes(UimaContext uimaContext) {
		String colonSeparatedTuis = (String) uimaContext.getConfigParameterValue("tui.filter");
		String colonSeparatedStys = (String) uimaContext.getConfigParameterValue("sty.filter");
		if (!StringUtils.isEmpty(colonSeparatedTuis) && !StringUtils.isEmpty(colonSeparatedStys)) {
			final String[] tuis = colonSeparatedTuis.split(":");
			final String[] stys = colonSeparatedStys.split(":");
			final SemanticType[] semTypeFilters = new SemanticType[tuis.length];
			for (int tdx = 0; tdx < tuis.length; tdx++) {
				semTypeFilters[tdx] = SemanticType.getSemanticType(stys[tdx], tuis[tdx]);
			}
			terminology.setSemanticTypeFilter(semTypeFilters);
		} else {
			terminology.setSemanticTypeFilter("");
		}
	}

	private void parameterizeSources(UimaContext uimaContext) {
		String colonSeparatedSources = (String) uimaContext.getConfigParameterValue("source.filter");
		if (!StringUtils.isEmpty(colonSeparatedSources)) {
			final String[] sabs = colonSeparatedSources.split(":");
			final Source[] sourceFilters = new Source[sabs.length];
			for (int tdx = 0; tdx < sabs.length; tdx++) {
				sourceFilters[tdx] = new Source(sabs[tdx]);
			}
			terminology.setSourceFilter(sourceFilters);
		}
	}

	/**
	 * Perform the actual analysis. Iterate over the document content looking
	 * for any matching words or phrases in the loaded dictionary and post an
	 * annotation for each match found.
	 * 
	 * @param tcas
	 *            the current CAS to process.
	 * @param aResultSpec
	 *            a specification of the result annotation that should be
	 *            created by this annotator
	 * 
	 * @see org.apache.uima.analysis_engine.annotator.TextAnnotator#process(CAS,ResultSpecification)
	 */
	public void process(JCas jCas) throws AnalysisEngineProcessException {
		try {
			processWorker(jCas);
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}
	}

	@Override
	public void collectionProcessComplete() {
		if (terminology != null) {
			// System.out.println("DISPOSING of NC Terminology...");
			terminology.dispose();
			terminology = null;
			// System.out.println("FINISHED of NC Terminology...");
		}
	}

	@SuppressWarnings("rawtypes")
	private void processWorker(JCas jCas) throws AnalysisEngineProcessException {
		CAS tcas = jCas.getCas();

		if (spanFeatureStructureName == null) {
			spanFeatureStructureName = (String) uimaContext.getConfigParameterValue(PARAM_DATA_BLOCK_FS);
		}
		// System.out.println("processWorker: spanFeatureStructureName is "
		// + spanFeatureStructureName);
		try {
			setJCas(jCas); // this is needed to get around an issue
			// where UIMA crashes if no JCas is
			// referenced
			// logger.setupDocument (getJCas ());

			spanFeatureStructureType = tcas.getTypeSystem().getType(spanFeatureStructureName);
			if (spanFeatureStructureType == null) {
				logger.log(Level.SEVERE, PARAM_DATA_BLOCK_FS + " '" + spanFeatureStructureName
						+ "' specified, but does not exist: ");
				throw new AnnotatorInitializationException();
			}
			FSIndex dbIndex = tcas.getAnnotationIndex(spanFeatureStructureType);
			FSIterator spanIterator = dbIndex.iterator();
			while (spanIterator.hasNext()) {
				AnnotationFS enclosingSpanAnnotation = (AnnotationFS) spanIterator.next();
				String enclosingRegionSpan = enclosingSpanAnnotation.getCoveredText();
				// System.out.println("Calling NC with " + enclosingRegionSpan);
				Sentence s = coder.process(new Sentence(enclosingRegionSpan, enclosingSpanAnnotation.getBegin(),
						Sentence.TYPE_PROSE));
				List<Mention> mentions = s.getMentions();
				for (Mention m : mentions) {
					Concept srcConcept = m.getConcept();
					int[] annotationRange = deriveAnnotationRange(m.getAnnotations());
					if (annotationRange[0] == Integer.MAX_VALUE) {
						System.err.println(m.getName() + " " + m.getText());
						continue;
					}
					edu.pitt.dbmi.nlp.noble.uima.types.Concept tgtConcept = new edu.pitt.dbmi.nlp.noble.uima.types.Concept(jCas);
					tgtConcept.setBegin(annotationRange[0]);
					tgtConcept.setEnd(annotationRange[1]);
					tgtConcept.setCui(srcConcept.getCode());
					tgtConcept.setCn(srcConcept.getName());
					// tgtConcept.setDefinition(srcConcept.getDefinition());
					Term preferredTerm = srcConcept.getPreferredTerm();
					if (preferredTerm != null) {
						tgtConcept.setPreferredTerm(srcConcept.getPreferredTerm().getText());
					}
					populateSemanticTypes(jCas, srcConcept, tgtConcept);
					// populateSynonyms(jCas, srcConcept, tgtConcept);

					// setup ConText modifiers
					tgtConcept.setModifiers(new FSArray(jCas, m.getModifiers().size()));
					int i = 0;
					for (Modifier mod : m.getModifiers().values()) {
						edu.pitt.dbmi.nlp.noble.uima.types.Modifier mod_a = new edu.pitt.dbmi.nlp.noble.uima.types.Modifier(jCas);
						mod_a.setName(mod.getType());
						mod_a.setValue(mod.getValue());
						int[] modifierRange = deriveAnnotationRange(mod.getAnnotations());
						if (modifierRange[0] != Integer.MAX_VALUE) {
							mod_a.setBegin(modifierRange[0]);
							mod_a.setEnd(modifierRange[1]);
						}
						mod_a.addToIndexes(jCas);
						tgtConcept.setModifiers(i++, mod_a);
					}

					tgtConcept.addToIndexes(jCas);
				}
			}
		} catch (Exception e) {
			throw new AnalysisEngineProcessException(e);
		}

	}

	private void populateSemanticTypes(JCas jCas, Concept c, edu.pitt.dbmi.nlp.noble.uima.types.Concept tgtConcept) {
		SemanticType[] semTypes = c.getSemanticTypes();
		ArrayList<edu.pitt.dbmi.nlp.noble.uima.types.SemanticType> semTypeArray = new ArrayList<edu.pitt.dbmi.nlp.noble.uima.types.SemanticType>();
		for (SemanticType sType : semTypes) {
			edu.pitt.dbmi.nlp.noble.uima.types.SemanticType tgtSemType = new edu.pitt.dbmi.nlp.noble.uima.types.SemanticType(
					jCas);
			tgtSemType.setBegin(tgtConcept.getBegin());
			tgtSemType.setEnd(tgtConcept.getEnd());
			tgtSemType.setTui(sType.getCode());
			tgtSemType.setSty(sType.getName());
			semTypeArray.add(tgtSemType);
		}
		FSArray sTypeArray = new FSArray(jCas, semTypeArray.size());
		FeatureStructure[] semTypeFtrStructArray = new FeatureStructure[semTypeArray.size()];
		semTypeArray.toArray(semTypeFtrStructArray);
		sTypeArray.copyFromArray(semTypeFtrStructArray, 0, 0, semTypeFtrStructArray.length);
		tgtConcept.setTuis(sTypeArray);
	}

	private void populateSynonyms(JCas jCas, Concept c, edu.pitt.dbmi.nlp.noble.uima.types.Concept tgtConcept) {
		StringArray synonymStringArray = new StringArray(jCas, c.getSynonyms().length);
		synonymStringArray.copyFromArray(c.getSynonyms(), 0, 0, c.getSynonyms().length);
		tgtConcept.setSynonyms(synonymStringArray);
	}

	private void displayConceptDiagnostics(Concept c) throws TerminologyException {
		StringBuffer sb = new StringBuffer();

		// Done
		sb.append("cui: " + c.getCode() + "\n");
		sb.append("name: " + c.getName() + "\n");
		sb.append("definition: " + c.getDefinition() + "\n");
		sb.append("cn: " + c.getName() + "\n");
		SemanticType[] semTypes = c.getSemanticTypes();
		for (SemanticType sType : semTypes) {
			String tui = sType.getCode();
			String sty = sType.getName();
			sb.append("tui: " + tui + "sty: " + sty + "\n");
		}

		// Skipping
		sb.append("offset: " + c.getOffset() + "\n");
		sb.append("conceptClass: " + c.getConceptClass() + "\n");
		sb.append("content: " + c.getContent() + "\n");
		sb.append("matchedTerm: " + c.getMatchedTerm() + "\n");
		sb.append("score: " + c.getScore() + "\n");
		String conceptCode = c.getCode(new Source("NCI"));
		sb.append("conceptCode (NCI): " + conceptCode + "\n");
		conceptCode = c.getCode(new Source("LNC"));
		sb.append("conceptCode (LNC): " + conceptCode + "\n");
		conceptCode = c.getCode(new Source("MSH"));
		sb.append("conceptCode (MSH): " + conceptCode + "\n");

		// Todo

		sb.append("preferredTerm: " + c.getPreferredTerm() + "\n");
		String[] synonyms = c.getSynonyms();
		for (String synonym : synonyms) {
			sb.append("synonym: " + synonym + "\n");
		}

		sb.append("searchString: " + c.getSearchString() + "\n");
		c.getProperties();

		Terminology term = c.getTerminology();
		sb.append("terminology name: " + term.getName() + "\n");
		c.getTerms();
		c.getText();
		c.getWordMap();

		c.getRelatedConcepts();
		Relation relation = new Relation("isa");
		c.getRelatedConcepts(relation);
		c.getRelationMap();
		c.getRelations();

		Annotation annots[] = c.getAnnotations();
		c.getParentConcepts();
		edu.pitt.dbmi.nlp.noble.terminology.Concept[] children = c.getChildrenConcepts();
		Definition[] definitions = c.getDefinitions();
		String[] matchedTerms = c.getMatchedTerms();

		c.getCodes();

		System.out.println(sb.toString());

	}

	private void displayTerminologyDiagnostics() {
		StringBuffer sb = new StringBuffer();
		sb.append("isCachingEnabled " + terminology.isCachingEnabled() + "\n");
		sb.append("isContiguousMode " + terminology.isContiguousMode() + "\n");
		sb.append("defaultSearchMethod " + terminology.getDefaultSearchMethod() + "\n");
		sb.append("languageFilter " + Arrays.toString(terminology.getLanguageFilter()) + "\n");
		sb.append("semanticTypeFilter " + Arrays.toString(terminology.getSemanticTypeFilter()) + "\n");
		sb.append("sourceFilter " + Arrays.toString(terminology.getSourceFilter()) + "\n");
		sb.append("isIgnoreAcronyms " + terminology.isIgnoreAcronyms() + "\n");
		sb.append("isIgnoreSmallWords " + terminology.isIgnoreSmallWords() + "\n");
		sb.append("isIgnoreUsedWords " + terminology.isIgnoreUsedWords() + "\n");
		sb.append("isSubsumptionMode " + terminology.isSubsumptionMode() + "\n");
		sb.append("isOrderedMode " + terminology.isOrderedMode() + "\n");
		sb.append("isOverlapMode " + terminology.isOverlapMode() + "\n");
		sb.append("isPartialMode " + terminology.isPartialMode() + "\n");
		sb.append("isContiguousMode " + terminology.isContiguousMode() + "\n");
		sb.append("maxWordGap " + terminology.getMaximumWordGap() + "\n");

		sb.append("isScoreConcepts " + terminology.isScoreConcepts() + "\n");
		sb.append("isSelectBestCandidate " + terminology.isSelectBestCandidate() + "\n");
		System.out.println(sb.toString());
	}

	private int[] deriveAnnotationRange(List<Annotation> annotations) {
		int maxIntValue = Integer.MIN_VALUE;
		int minIntValue = Integer.MAX_VALUE;
		for (Annotation annot : annotations) {
			if (annot.getStartPosition() < minIntValue) {
				minIntValue = annot.getStartPosition();
			}
			if (annot.getEndPosition() > maxIntValue) {
				maxIntValue = annot.getEndPosition();
			}
		}
		final int[] result = new int[2];
		result[0] = minIntValue;
		result[1] = maxIntValue;
		return result;
	}

	private void setJCas(JCas jcas) {
	}

}
