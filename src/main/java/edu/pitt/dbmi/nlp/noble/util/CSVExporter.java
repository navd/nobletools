package edu.pitt.dbmi.nlp.noble.util;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import edu.pitt.dbmi.nlp.noble.coder.model.Document;
import edu.pitt.dbmi.nlp.noble.coder.model.Mention;
import edu.pitt.dbmi.nlp.noble.extract.model.ItemInstance;
import edu.pitt.dbmi.nlp.noble.extract.model.Template;
import edu.pitt.dbmi.nlp.noble.extract.model.TemplateDocument;
import edu.pitt.dbmi.nlp.noble.extract.model.TemplateItem;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;

public class CSVExporter {
	public static final String DEFAULT_RESULT_FILE = "RESULTS.tsv";
	private File outputFile;
	private BufferedWriter csvWriter;
	private String S = "\t";
	
	public CSVExporter(File file){
		if(file.isFile())
			outputFile = file;
		else if(file.isDirectory())
			outputFile = new File(file,DEFAULT_RESULT_FILE);
	}
	
	public File getOutputFile() {
		return outputFile;
	}

	public void setOutputFile(File outputFile) {
		this.outputFile = outputFile;
	}



	public String getDelimeter() {
		return S;
	}

	public void setDelimeter(String s) {
		S = s;
	}


	/**
	 * create codes csv report
	 * @param file
	 * @param name
	 * @param resultMap
	 * @throws Exception
	 */
	public void export(TemplateDocument doc )  throws Exception{
		String name = doc.getTitle();
		Map<Template,List<ItemInstance>> resultMap = doc.getItemInstances();
		BufferedWriter writer = getCSVWriter(outputFile,resultMap.keySet());
		writer.write(name);
		for(Template template: resultMap.keySet()){
			for(TemplateItem temp: template.getTemplateItems()){
				for(String question: temp.getQuestions()){
					TemplateItem attribute = temp.getAttribute(question);
					List<ItemInstance> instances = attribute == null?doc.getItemInstances(temp):doc.getItemInstances(temp, attribute);
					StringBuffer b = new StringBuffer();
					for(ItemInstance inst :instances){
						b.append((inst.getAnswer(false))+" ;"); 
					}
					writer.write(S+b.toString().trim());
				}
			}
		}
		writer.write("\n");
		writer.flush();
	}
	
	
	/**
	 * create codes csv report
	 * @param file
	 * @param name
	 * @param resultMap
	 * @throws Exception
	 */
	public void export(Document doc)  throws Exception{
		NumberFormat nf = NumberFormat.getInstance();
		nf.setMaximumFractionDigits(3);
		BufferedWriter writer = getCSVWriter(outputFile);
		
		// go over all of the mentions
		for(Mention m: doc.getMentions()){
			Concept c = m.getConcept();
			String s = Arrays.toString(c.getSemanticTypes());
			StringBuffer a = new StringBuffer();
			for(Annotation an : m.getAnnotations()){
				a.append(an.getText()+"/"+(+an.getOffset())+", ");
			}
			if(a.length()> 0){
				a = new StringBuffer(a.substring(0,a.length()-2));
			}
			writer.write(doc.getTitle()+S+m.getText()+S+c.getCode()+S+c.getName()+S+s.substring(1,s.length()-1)+S+a+getModifierValues(m)+"\n");
		}
		writer.flush();
	}	
	
	
	/**
	 * flush all writers
	 */
	public void flush() throws Exception {
		if(csvWriter != null){
			csvWriter.close();
		}
		csvWriter = null;
	}
	
	private String getModifierTypes(){
		StringBuffer st = new StringBuffer();
		for(String s: Mention.getModifierTypes()){
			st.append(S+s);
		}
		return st.toString();
	}
	
	private String getModifierValues(Mention m){
		StringBuffer st = new StringBuffer();
		for(String s: Mention.getModifierTypes()){
			String v = m.getModifierValue(s);
			st.append(S+(v == null?"":v));
		}
		return st.toString();
	}
	
	private BufferedWriter getCSVWriter(File out) throws Exception {
		if(csvWriter == null){
			csvWriter = new BufferedWriter(new FileWriter(out));
			csvWriter.write("Document"+S+"Matched Term"+S+"Code"+S+"Concept Name"+S+"Semantic Type"+S+"Annotations"+getModifierTypes()+"\n");
		}
		return csvWriter;
	}
	
	
	private BufferedWriter getCSVWriter(File out,Set<Template> templates) throws Exception {
		if(csvWriter == null){
			csvWriter = new BufferedWriter(new FileWriter(out));
			csvWriter.write("Report");
			for(Template template: templates){
				for(TemplateItem temp: template.getTemplateItems()){
					for(String question: temp.getQuestions()){
						csvWriter.write(S+question);
					}		
					//if(!temp.getUnits().isEmpty())
					//	csvWriter.write(S+getQuestion(temp)+" (units)");
				}
			}
			csvWriter.write("\n");
		}
		return csvWriter;
	}
}
