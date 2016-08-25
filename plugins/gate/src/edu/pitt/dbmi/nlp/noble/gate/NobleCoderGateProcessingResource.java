/*
 *  NobleCoderGateProcessingResource.java
 *
 * Copyright (c) 2000-2012, The University of Sheffield.
 *
 * This file is part of GATE (see http://gate.ac.uk/), and is free
 * software, licenced under the GNU Library General Public License,
 * Version 3, 29 June 2007.
 *
 * A copy of this licence is included in the distribution in the file
 * licence.html, and is also available at http://gate.ac.uk/gate/licence.html.
 *
 *  mitchellkj, 25/8/2014
 *
 * For details on the configuration options, see the user guide:
 * http://gate.ac.uk/cgi-bin/userguide/sec:creole-model:config
 */

package edu.pitt.dbmi.nlp.noble.gate;

import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.util.StringUtils;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.creole.AbstractLanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.metadata.CreoleParameter;
import gate.creole.metadata.CreoleResource;
import gate.creole.metadata.RunTime;
import gate.util.GateRuntimeException;
import gate.util.InvalidOffsetException;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * This class is the implementation of the resource NOBLE CODER GATE PROCESSING
 * RESOURCE.
 */
@CreoleResource(name = "NobleCoderGateProcessingResource", comment = "Runs the NobleCoder against current Sentences")
public class NobleCoderGateProcessingResource extends AbstractLanguageAnalyser {

	private static final long serialVersionUID = 6557077464573515950L;

	private AnnotationSet inputAnnotSet;
	private gate.Annotation currentCodableSpanAnnotation;
	private String currentCodeableSpanText;

	private NobleCoderTerminology coder;

	private boolean isInterrupted;

	private URL terminologyURL;

	@RunTime
	@CreoleParameter(defaultValue = "./resources/ncit/META.term/info.txt", comment = "Terminology URL", suffixes = "txt")
	public void setTerminologyURL(URL terminologyURL) {
		this.terminologyURL = terminologyURL;
	}

	public URL getTerminologyURL() {
		return this.terminologyURL;
	}

	private Boolean cachingEnabled = false;

	@RunTime
	@CreoleParameter(defaultValue = "false", comment = "Caching Enabled")
	public void setCachingEnabled(Boolean cachingEnabled) {
		this.cachingEnabled = cachingEnabled;
	}

	public Boolean getCachingEnabled() {
		return this.cachingEnabled;
	}

	private Boolean contiguousMode = false;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Contiguous Mode")
	public void setContiguousMode(Boolean contiguousMode) {
		this.contiguousMode = contiguousMode;
	}

	public Boolean getContiguousMode() {
		return this.contiguousMode;
	}

	private List<String> languages = new ArrayList<String>();

	@RunTime
	@CreoleParameter(defaultValue = "ENG", comment = "Languages")
	public void setLanguages(List<String> languages) {
		this.languages = languages;
	}

	public List<String> getLanguages() {
		return this.languages;
	}

	private List<String> tuis = new ArrayList<String>();

	@RunTime
	@CreoleParameter(defaultValue = "T001;T002;T004;T005;T007;T008;T010;T011;T012;T013;T014;T015;T016;T017;T018;T019;T020;T021;T022;T023;T024;T025;T026;T028;T029;T030;T031;T032;T033;T034;T037;T038;T039;T040;T041;T042;T043;T044;T045;T046;T047;T048;T049;T050;T051;T052;T053;T054;T055;T056;T057;T058;T059;T060;T061;T062;T063;T064;T065;T066;T067;T068;T069;T070;T071;T072;T073;T074;T075;T077;T078;T079;T080;T081;T082;T083;T085;T086;T087;T088;T089;T090;T091;T092;T093;T094;T095;T096;T097;T098;T099;T100;T101;T102;T103;T104;T109;T110;T111;T114;T115;T116;T118;T119;T120;T121;T122;T123;T124;T125;T126;T127;T129;T130;T131;T167;T168;T169;T170;T171;T184;T185;T190;T191;T192;T194;T195;T196;T197;T200;T201;T203;T204", comment = "Tuis")
	public void setTuis(List<String> tuis) {
		this.tuis = tuis;
	}

	public List<String> getTuis() {
		return this.tuis;
	}

	private List<String> stys = new ArrayList<String>();

	@RunTime
	@CreoleParameter(defaultValue = "Organism;Plant;Fungus;Virus;Bacterium;Animal;Vertebrate;Amphibian;Bird;Fish;Reptile;Mammal;Human;Anatomical Structure;Embryonic Structure;Congenital Abnormality;Acquired Abnormality;Fully Formed Anatomical Structure;Body System;Body Part, Organ, or Organ Component;Tissue;Cell;Cell Component;Gene or Genome;Body Location or Region;Body Space or Junction;Body Substance;Organism Attribute;Finding;Laboratory or Test Result;Injury or Poisoning;Biologic Function;Physiologic Function;Organism Function;Mental Process;Organ or Tissue Function;Cell Function;Molecular Function;Genetic Function;Pathologic Function;Disease or Syndrome;Mental or Behavioral Dysfunction;Cell or Molecular Dysfunction;Experimental Model of Disease;Event;Activity;Behavior;Social Behavior;Individual Behavior;Daily or Recreational Activity;Occupational Activity;Health Care Activity;Laboratory Procedure;Diagnostic Procedure;Therapeutic or Preventive Procedure;Research Activity;Molecular Biology Research Technique;Governmental or Regulatory Activity;Educational Activity;Machine Activity;Phenomenon or Process;Human-caused Phenomenon or Process;Environmental Effect of Humans;Natural Phenomenon or Process;Entity;Physical Object;Manufactured Object;Medical Device;Research Device;Conceptual Entity;Idea or Concept;Temporal Concept;Qualitative Concept;Quantitative Concept;Spatial Concept;Geographic Area;Molecular Sequence;Nucleotide Sequence;Amino Acid Sequence;Carbohydrate Sequence;Regulation or Law;Occupation or Discipline;Biomedical Occupation or Discipline;Organization;Health Care Related Organization;Professional Society;Self-help or Relief Organization;Group;Professional or Occupational Group;Population Group;Family Group;Age Group;Patient or Disabled Group;Group Attribute;Chemical;Chemical Viewed Structurally;Organic Chemical;Steroid;Eicosanoid;Nucleic Acid, Nucleoside, or Nucleotide;Organophosphorus Compound;Amino Acid, Peptide, or Protein;Carbohydrate;Lipid;Chemical Viewed Functionally;Pharmacologic Substance;Biomedical or Dental Material;Biologically Active Substance;Neuroreactive Substance or Biogenic Amine;Hormone;Enzyme;Vitamin;Immunologic Factor;Indicator, Reagent, or Diagnostic Aid;Hazardous or Poisonous Substance;Substance;Food;Functional Concept;Intellectual Product;Language;Sign or Symptom;Classification;Anatomical Abnormality;Neoplastic Process;Receptor;Archaeon;Antibiotic;Element, Ion, or Isotope;Inorganic Chemical;Clinical Drug;Clinical Attribute;Drug Delivery Device;Eukaryote", comment = "Stys")
	public void setStys(List<String> stys) {
		this.stys = stys;
	}

	public List<String> getStys() {
		return this.stys;
	}

	private List<String> sources = new ArrayList<String>();

	@RunTime
	@CreoleParameter(defaultValue = "AIR;ALT;AOD;AOT;ATC;BI;CCC;CCPSS;CCS;CDT;CHV;COSTAR;CPM;CPT;CSP;CST;CVX;DDB;DSM3R;DSM4;DXP;FMA;GO;GS;HCDT;HCPCS;HCPT;HGNC;HL7V2.5;HL7V3.0;ICD10;ICD10AE;ICD10AM;ICD10AMAE;ICD10CM;ICD10PCS;ICD9CM;ICF;ICF-CY;ICNP;ICPC;ICPC2EENG;ICPC2ICD10ENG;ICPC2P;JABL;LCH;LNC;MCM;MDDB;MDR;MEDCIN;MEDLINEPLUS;MMSL;MMX;MSH;MTH;MTHFDA;MTHHH;MTHICD9;MTHICPC2EAE;MTHICPC2ICD10AE;MTHMST;MTHSPL;NAN;NCBI;NCI;NDDF;NDFRT;NEU;NIC;NOC;OMIM;OMS;PCDS;PDQ;PNDS;PPAC;PSY;QMR;RAM;RCD;RCDAE;RCDSA;RCDSY;RXNORM;SNM;SNMI;SNOMEDCT_US;SNOMEDCT_VET;SOP;SPN;SRC;ULT;UMD;USPMG;UWDA;VANDF;WHO", comment = "Sources")
	public void setSources(List<String> sources) {
		this.sources = sources;
	}

	public List<String> getSources() {
		return this.sources;
	}

	private String defaultSearchMethod = "best-match";

	@RunTime
	@CreoleParameter(defaultValue = "best-match", comment = "Default Search Strategy")
	public void setDefaultSearchMethod(String defaultSearchMethod) {
		this.defaultSearchMethod = defaultSearchMethod;
	}

	public String getDefaultSearchMethod() {
		return this.defaultSearchMethod;
	}

	private Boolean ignoreAcronyms = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Ignore Acronyms")
	public void setIgnoreAcronyms(Boolean ignoreAcronyms) {
		this.ignoreAcronyms = ignoreAcronyms;
	}

	public Boolean getIgnoreAcronyms() {
		return this.ignoreAcronyms;
	}

	private Boolean ignoreCommonWords = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Ignore Common Words")
	public void setIgnoreCommonWords(Boolean ignoreCommonWords) {
		this.ignoreCommonWords = ignoreCommonWords;
	}

	public Boolean getIgnoreCommonWords() {
		return this.ignoreCommonWords;
	}

	private Boolean ignoreDigits = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Ignore Digits")
	public void setIgnoreDigits(Boolean ignoreDigits) {
		this.ignoreDigits = ignoreDigits;
	}

	public Boolean getIgnoreDigits() {
		return this.ignoreDigits;
	}

	private Boolean ignoreSmallWords = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Ignore Small Words")
	public void setIgnoreSmallWords(Boolean ignoreSmallWords) {
		this.ignoreSmallWords = ignoreSmallWords;
	}

	public Boolean getIgnoreSmallWords() {
		return this.ignoreSmallWords;
	}

	private Boolean ignoreUsedWords = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Ignore Used Words")
	public void setIgnoreUsedWords(Boolean ignoreUsedWords) {
		this.ignoreUsedWords = ignoreUsedWords;
	}

	public Boolean getIgnoreUsedWords() {
		return this.ignoreUsedWords;
	}

	private Boolean orderedMode = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Ordered Mode")
	public void setOrderedMode(Boolean orderedMode) {
		this.orderedMode = orderedMode;
	}

	public Boolean getOrderedMode() {
		return this.orderedMode;
	}

	private Boolean overlapMode = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Overlap Mode")
	public void setOverlapMode(Boolean overlapMode) {
		this.overlapMode = overlapMode;
	}

	public Boolean getOverlapMode() {
		return this.overlapMode;
	}

	private Boolean partialMode = true;

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Partial Mode")
	public void setPartialMode(Boolean partialMode) {
		this.partialMode = partialMode;
	}

	public Boolean getPartialMode() {
		return this.partialMode;
	}

	private Boolean scoreConcepts = true;

	@RunTime
	@CreoleParameter(defaultValue = "false", comment = "Score Concepts")
	public void setScoreConcepts(Boolean scoreConcepts) {
		this.scoreConcepts = scoreConcepts;
	}

	public Boolean getScoreConcepts() {
		return this.scoreConcepts;
	}

	private Boolean selectBestCandidate = true;

	@RunTime
	@CreoleParameter(defaultValue = "false", comment = "Select Best Candidate")
	public void setSelectBestCandidate(Boolean selectBestCandidate) {
		this.selectBestCandidate = selectBestCandidate;
	}

	public Boolean getSelectBestCandidate() {
		return this.selectBestCandidate;
	}

	private Boolean stemWords = true;

	@RunTime
	@CreoleParameter(defaultValue = "false", comment = "Stem Words")
	public void setStemWords(Boolean stemWords) {
		this.stemWords = stemWords;
	}

	public Boolean getStemWords() {
		return this.stemWords;
	}

	private Boolean subsumptionMode = true;

	@RunTime
	@CreoleParameter(defaultValue = "false", comment = "Subsumption Mode")
	public void setSubsumptionMode(Boolean subsumptionMode) {
		this.subsumptionMode = subsumptionMode;
	}

	public Boolean getSubsumptionMode() {
		return this.subsumptionMode;
	}

	private Integer windowSize = -1;

	@RunTime
	@CreoleParameter(defaultValue = "-1", comment = "Window Size")
	public void setWindowSize(Integer windowSize) {
		this.windowSize = windowSize;
	}

	public Integer getWindowSize() {
		return this.windowSize;
	}

	private String inputAS = "";

	@RunTime
	@CreoleParameter(defaultValue = "", comment = "Input Annotation Set")
	public void setInputAS(String inputAS) {
		this.inputAS = inputAS;
	}

	public String getInputAS() {
		return this.inputAS;
	}

	private String inputAnnotationType = "";

	@RunTime
	@CreoleParameter(defaultValue = "", comment = "Input Annotation Type")
	public void setInputAnnotationType(String inputAnnotationType) {
		this.inputAnnotationType = inputAnnotationType;
	}

	public String getInputAnnotationType() {
		return this.inputAnnotationType;
	}

	private String outputAS = "";

	@RunTime
	@CreoleParameter(defaultValue = "", comment = "Output Annotation Set")
	public void setOutputAS(String outputAS) {
		this.outputAS = outputAS;
	}

	public String getOutputAS() {
		return this.outputAS;
	}

	private String outputAnnotationType = "";

	@RunTime
	@CreoleParameter(defaultValue = "", comment = "Output Annotation Type")
	public void setOutputAnnotationType(String outputAnnotationType) {
		this.outputAnnotationType = outputAnnotationType;
	}

	public String getOutputAnnotationType() {
		return this.outputAnnotationType;
	}

	private Boolean debugging = new Boolean(true);

	@RunTime
	@CreoleParameter(defaultValue = "true", comment = "Subsumption Mode")
	public void setDebugging(Boolean debugging) {
		this.debugging = debugging;
	}

	public Boolean getDebugging() {
		return this.debugging;
	}

	public Document getDocument() {
		return document;
	}

	public Corpus getCorpus() {
		return corpus;
	}

	public void reInit() throws ResourceInstantiationException {
	}

	public boolean isInterrupted() {
		return isInterrupted;
	}

	public void interrupt() {
	}

	public void execute() throws ExecutionException {

		try {
			if (coder == null) {
				openNobleCoder();
			}
		} catch (Exception e) {
			throw (new GateRuntimeException(e));
		}

		inputAnnotSet = document.getAnnotations(getInputAS());
		document.getAnnotations(getOutputAS());
		if (inputAnnotSet == null || inputAnnotSet.isEmpty()) {
			System.err.println("Null or empty input annotations.");
		} else {
			AnnotationSet codableSpanAnnotations = inputAnnotSet.get(getInputAnnotationType());
			if (codableSpanAnnotations == null || codableSpanAnnotations.size() == 0) {
				codableSpanAnnotations = deriveCodableSpansFromDocumentLines();
			}
			if (codableSpanAnnotations == null || codableSpanAnnotations.isEmpty()) {
				throw new GateRuntimeException("NobleCoder Warning:"
						+ "No codable spans found for processing!");
			}
			for (gate.Annotation codableSpanAnnotation : codableSpanAnnotations) {
				currentCodableSpanAnnotation = codableSpanAnnotation;
				processCodableSpanAnnotation();
			}
		}

	}

	private void initializeParameters() {
		coder.setCachingEnabled(getCachingEnabled());
		coder.setContiguousMode(getContiguousMode());
		coder.setDefaultSearchMethod(getDefaultSearchMethod());
		coder.setLanguageFilter(parameterizeLanguages(languages));
		parameterizeSemanticTypes();
		parameterizeSources();
		coder.setIgnoreAcronyms(getIgnoreAcronyms());
		coder.setIgnoreCommonWords(getIgnoreCommonWords());
		coder.setIgnoreDigits(getIgnoreDigits());
		coder.setIgnoreSmallWords(getIgnoreSmallWords());
		coder.setIgnoreUsedWords(getIgnoreUsedWords());
		coder.setOrderedMode(getOrderedMode());
		coder.setOverlapMode(getOverlapMode());
		coder.setPartialMode(getPartialMode());
		coder.setScoreConcepts(getScoreConcepts());
		coder.setSelectBestCandidate(getSelectBestCandidate());
		coder.setStemWords(getStemWords());
		coder.setSubsumptionMode(getSubsumptionMode());
		coder.setWindowSize(getWindowSize());
	}

	private String[] parameterizeLanguages(List<String> languages) {
		 String[] lArray = new String[languages.size()];
		 int ldex = 0;
		 for (String language : languages) {
			 lArray[ldex++] = language;
		 }
		 return lArray;
	}

	private void parameterizeSemanticTypes() {
		if (tuis != null && !tuis.isEmpty()) {
			final SemanticType[] semTypeFilters = new SemanticType[tuis.size()];
			Iterator<String> tuiIterator = tuis.iterator();
			Iterator<String> styIterator = stys.iterator();
			int tdx = 0;
			while (tuiIterator.hasNext() && styIterator.hasNext()) {
				String sty = styIterator.next();
				String tui = tuiIterator.next();
				semTypeFilters[tdx++] = SemanticType.getSemanticType(sty, tui);
			}
			coder.setSemanticTypeFilter(semTypeFilters);
		} else {
			coder.setSemanticTypeFilter("");
		}
	}

	private void parameterizeSources() {
		if (sources != null && !sources.isEmpty()) {
			final Source[] sourceFilters = new Source[sources.size()];
			int tdx = 0;
			for (String sab : sources) {
				sourceFilters[tdx++] = new Source(sab);
			}
			coder.setSourceFilter(sourceFilters);
		} else {
			coder.setLanguageFilter(null);
		}
	}

	private void processCodableSpanAnnotation() {
		try {
			currentCodeableSpanText = deriveNobleCoderInputFromSentenceAnnotation(currentCodableSpanAnnotation);
			if (currentCodeableSpanText != null && currentCodeableSpanText.length() > 0) {
				Long sentenceStartPos = currentCodableSpanAnnotation
						.getStartNode().getOffset();
				if (debugging) {
					System.out.println("Calling Noble Coder with => "
							+ currentCodeableSpanText);
				}
				Concept[] concepts = coder.search(currentCodeableSpanText);
				for (Concept srcConcept : concepts) {
					int[] annotationRange = deriveAnnotationRange(srcConcept
							.getAnnotations());
					annotationRange[0] += currentCodableSpanAnnotation
							.getStartNode().getOffset();
					annotationRange[1] += currentCodableSpanAnnotation
							.getStartNode().getOffset();
					addConcept(srcConcept, sentenceStartPos);
				}
			}
		} catch (TerminologyException e) {
			e.printStackTrace();
		} catch (InvalidOffsetException e) {
			e.printStackTrace();
		}
	}

	private AnnotationSet deriveCodableSpansFromDocumentLines() {
		try {
			String documentContent = getDocument().getContent().toString();
			String[] lines = documentContent.split("\n");
			Long offset = 0L;
			for (String line : lines) {
				if (debugging) {
					System.out.println("Processing sentence:\n\t" + line);
				}

				Long sPos = offset;
				Long ePos = offset + line.length();
				if (line.length() > 0 && line.trim().length() > 0) {
					FeatureMap features = Factory.newFeatureMap();
					 document.getAnnotations(getInputAS()).add(sPos, ePos, getInputAnnotationType(), features);
					if (debugging) {
						System.out.println("Adding sentence:\n\t at (" + sPos
								+ ", " + ePos + ")");
					}

				} else {
					if (debugging) {
						System.out.println("Skipping zero length line at: at ("
								+ sPos + ", " + ePos + ")");
					}

				}
				offset += line.length() + 1;
			}
		} catch (InvalidOffsetException e) {
			e.printStackTrace();
		}

		return document.getAnnotations(getInputAS()).get(getInputAnnotationType());
	}

	private void openNobleCoder() throws Exception {
		if (coder == null) {
			URL terminologyURL = getTerminologyURL();
			File nobleCoderInfoFile = new File(terminologyURL.getFile());
			if (!nobleCoderInfoFile.exists() || !nobleCoderInfoFile.isFile()) {
				throw (new Exception("Cannot find noble coder info for  "
						+ nobleCoderInfoFile.getAbsolutePath()));
			}
			File nobleDirectory = nobleCoderInfoFile.getParentFile();
			if (!nobleDirectory.exists() || !nobleDirectory.isDirectory()) {
				throw (new Exception("Cannot find directory for  "
						+ nobleDirectory.getAbsolutePath()));
			}

			coder = new NobleCoderTerminology(nobleDirectory.getAbsolutePath());
			initializeParameters();

			if (debugging) {
				System.out
						.println("Successfully opened NobleCoder data source.");
			}

		}
	}

	private void addConcept(Concept concept, Long sentenceStartPos) {
		Long conceptSPos = (long) concept.getOffset();
		Long sPos = sentenceStartPos + conceptSPos;
		Long ePos = sPos + (long) concept.getText().length();
		try {
			FeatureMap features = Factory.newFeatureMap();
			features.put("cn", concept.getName());
			features.put("cui", concept.getCode());
			features.put("definition", concept.getDefinition());
			features.put("preferredTerm", concept.getPreferredTerm().getText());
			features.put("synonyms", StringUtils.join(concept.getSynonyms(),";"));
			features.put("tuis", joinTuis(concept.getSemanticTypes()));
			features.put("stys", joinStys(concept.getSemanticTypes()));
			AnnotationSet nobleCoderSet = getDocument().getAnnotations(
					getOutputAS());
			nobleCoderSet.add(sPos, ePos, getOutputAnnotationType(), features);
		} catch (Exception x) {
			if (debugging) {
				StringBuffer sb = new StringBuffer();
				sb.append("\n\nFailed to add Concept:");
				sb.append("\n\tDocument name ==> " + getDocument().getName());
				sb.append("\n\t text ==> '" + concept.getText() + "'");
				sb.append("\n\t concept offset ==> " + concept.getOffset());
				sb.append("'\n\t annoation offsets ==> (" + sPos + "," + ePos
						+ ")");
				sb.append("\n\t current sentence ==> '" + currentCodeableSpanText
						+ "'");
				sb.append("\n\t sentence offset ==> ("
						+ currentCodableSpanAnnotation.getStartNode().getOffset()
						+ ", "
						+ currentCodableSpanAnnotation.getEndNode().getOffset()
						+ ")");
				sb.append("\n\t document size ==> "
						+ getDocument().getContent().toString().length());
				System.err.println(sb.toString());
				concept.printInfo(System.err);
			}
		}

	}

	private String joinStys(SemanticType[] semanticTypes) {
		String stys = "";
		for (SemanticType semanticType : semanticTypes) {
			if (stys.length() > 0) {
				stys += ";";
			}
			stys += semanticType.getName();
		}
		return stys;
	}

	private String joinTuis(SemanticType[] semanticTypes) {
		String tuis = "";
		for (SemanticType semanticType : semanticTypes) {
			if (tuis.length() > 0) {
				tuis += ";";
			}
			tuis += semanticType.getCode();
		}
		return tuis;
	}

	private String deriveNobleCoderInputFromSentenceAnnotation(
			gate.Annotation sentenceAnnotation) throws InvalidOffsetException {
		String result = null;
		long sPosition = sentenceAnnotation.getStartNode().getOffset()
				.longValue();
		long ePosition = sentenceAnnotation.getEndNode().getOffset()
				.longValue();
		String sentenceText = getDocument().getContent()
				.getContent(sPosition, ePosition).toString();
		result = sentenceText.replaceAll("\n", " ");
		return result;
	}

	private int[] deriveAnnotationRange(Annotation[] annotations) {
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

} // class NobleCoderGateProcessingResource
