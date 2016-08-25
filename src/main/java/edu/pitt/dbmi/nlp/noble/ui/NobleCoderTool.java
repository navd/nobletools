package edu.pitt.dbmi.nlp.noble.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Properties;
import java.util.TreeSet;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import javax.swing.AbstractButton;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;

import edu.pitt.dbmi.nlp.noble.coder.NobleCoder;
import edu.pitt.dbmi.nlp.noble.coder.model.Document;
import edu.pitt.dbmi.nlp.noble.ontology.DefaultRepository;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.SemanticType;
import edu.pitt.dbmi.nlp.noble.terminology.Source;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.CSVExporter;
import edu.pitt.dbmi.nlp.noble.util.HTMLExporter;


/**
 * wrapper class that wraps NobleCoderTerminology to do stand-alon concept coding
 * @author tseytlin
 *
 */
public class NobleCoderTool implements ActionListener{
	private final URL LOGO_ICON = getClass().getResource("/icons/NobleLogo256.png");
	private final URL IMPORT_ICON = getClass().getResource("/icons/Import16.gif");
	private final URL EXPORT_ICON = getClass().getResource("/icons/Export16.gif");
	private final URL OPTIONS_ICON = getClass().getResource("/icons/Preferences16.gif");
	private static final String DEFAULT_TERMINOLOGY_DOWNLOAD = "http://noble-tools.dbmi.pitt.edu/data/NCI_Thesaurus.term.zip";
	private final String DEFAULT_TERMINOLOGY = "NCI_Thesaurus";
	private final String ABBREV_TERMINOLOGY = "BiomedicalAbbreviations";
	private final String RESULT_CSV = "RESULT.csv";
	private final String IMPORT_MESSAGE = "You can import additional terminologies from the File menu";
	private final String ABOUT_TEXT = 
			"<html><table width=600><tr><td><p>NOBLE Tools is a suite of Natural Language Processing (NLP) tools and Application Programming Interfaces (API) written in "
			+ "Java for interfacing with ontologies, auto coding text and information extraction. The suite includes NOBLE Coder Named Entity Recognition "
			+ "(NER) engine for biomedical text, a generic ontology API for interfacing with Web Ontology Language (OWL) files, OBO and BioPortal ontologies "
			+ "as well as a number of support utilities and methods useful for NLP s.a. string normalization, ngram, stemming etc..</p><br/>" + 
			"<p>NOBLE Tools was developed by Eugene Tseytlin in the Crowley-Jacobson Lab at the <a href=\"http://dbmi.pitt.edu/\">Department of Biomedical Informatics, University of Pittsburgh</a>. "
			+ "NOBLE was originally developed for the SlideTutor Project, but is now a key component of the TIES NLP software. Other projects have used NOBLE tool as well: EDDA </p> <br/> " + 
			"<p> Work on NOBLE Coder was supported by the following grants: </p> <br/>" + 
			"<ul>" + 
			"    <li>NCI - 1U24CA180921 (PI Crowley-Jacobson)</li>" + 
			"    <li>NCI - R01 CA132672 (PI Crowley-Jacobson)</li>" + 
			"    <li>NCI - R25 CA101959 (PI Crowley-Jacobson)</li>" + 
			"    <li>NLM - R00 LM010943 (PI Bekhuis)</li>" + 
			"</ul></td></tr></table></html>";
	
	
	
	private JFrame frame;
	private JTextField input,output,semanticTypes,sources,slidingWindow,abbreviationWhitelistText,pathText,wordWindow;
	private JTextArea console,tip;
	private JComboBox terminologies,searchMethods;
	private JProgressBar progress;
	private JPanel buttonPanel,options,semanticPanel,sourcePanel;
	private JButton run,b1,b2,b3;
	private File file;
	private JCheckBox stripSmallWords,stripCommonWords,selectBestCandidates,filterBySemanticTypes,scoreConcepts; //stripDigits,
	private JCheckBox filterBySources,useSlidingWindow,openHTML,handleAbbreviations,handleAcronymExpansion,handleNegationDetection;
	private JCheckBox subsumptionMode,overlapMode, orderedMode, contiguousMode,partialMode,ignoreUsedWords;
	private DefaultRepository repository;
	private NobleCoderTerminology terminology; //,abbreviations;
	private boolean skipAbbrreviationLogic,handleAcronyms = true,handleNegation = true;
	private RepositoryManager imanager;
	private TerminologyImporter loader;
	private TerminologyExporter exporter;
	private HTMLExporter htmlExporter;
	private CSVExporter csvExporter;
	
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		try{
			NobleCoderTool nc = new NobleCoderTool();
			if(args.length == 0){
				nc.printUsage(System.out);
				nc.showDialog();
			}else{
				nc.process(Arrays.asList(args));
			}
		}catch(OutOfMemoryError ex){
			if(!GraphicsEnvironment.isHeadless()){
				JOptionPane.showMessageDialog(null,"<html>NobleCoderTool ran out of memory!<br>" +
					"Try to pass more memory to JVM using <font color=green>-Xmx1G</font> flag.",
					"Error",JOptionPane.ERROR_MESSAGE);
			}
			throw ex;
		}catch(Exception ex){
			if(!GraphicsEnvironment.isHeadless()){
				JOptionPane.showMessageDialog(null,"<html>NobleCoderTool ran into a problem!<br>" +
						ex.getClass().getSimpleName()+": "+ex.getMessage(),
						"Error",JOptionPane.ERROR_MESSAGE);
			}
			throw ex;
		}
	}
	
	/**
	 * print usage statement
	 */
	private void printUsage(PrintStream out) {
		out.println("Usage: java -jar NobleCoderTool.jar -terminology <name> -input <dir> -output <dir> [options]");
		out.println("You can invoke NobleCoderTool using command line as well as via UI");
		out.println("\t-terminology - terminology to use. All terminolgies are located in <user.home>/.terminologies directory.");
		out.println("\t-input\t- input directory containing a set of text files with (.txt) extension");
		out.println("\t-output\t- output directory where "+RESULT_CSV+" output will be stored along with output HTML files");
		out.println("\t-search\t- search strategy: <best-match|precise-match|all-match|nonoverlap-match|partial-match|custom-match>");
		out.println("\t-stripDigits\t- don't try to match stand-alone digits");
		out.println("\t-stripSmallWords\t- don't try to match one letter words");
		out.println("\t-stripCommonWords\t- don't try to match most common English words");
		out.println("\t-selectBestCandidates\t- for each match only select the best candidate");
		out.println("\t-semanticTypes\t- <list of semantic types> only include matches from a given semantic types");
		out.println("\t-sources\t- <list of sources> only invlude matches from a given list of sources");
		out.println("\t-slidingWindow\t- <N> don't consider words that are N words apart to be part of the same concept");
		out.println("\t-abbreviations\t- <whitelist text file> a custom text file that suppresses all abbreviations except the ones in a list");
		out.println("\t-ignoreUsedWords\t- speed up search by not considering words that are already part of some concept");
		out.println("\t-subsumptionMode\t- subsume more general concepts if more specific concept is found");
		out.println("\t-overlapMode\t- overlapping concepts are allowed");
		out.println("\t-contiguousMode\t- matched terms must be contiguous in text");
		out.println("\t-orderedMode\t- matchd terms must use the same word order in text");
		out.println("\t-partialMode\t- match a term if more then 50% of its words are found in text");
		out.println("\t-acronymExpansion\t- if acronym is found in its expanded form, use its meaning to tag all other mentions of it");
		out.println("\t-negationDetection\t- invoke ConText algorithm to detect negated concepts and other modifiers");
		out.println("\n\n");
	}



	/**
	 * create dialog for noble coder
	 */
	public void showDialog(){
		frame = new JFrame("Noble Coder");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		frame.setJMenuBar(getMenuBar());
		frame.setIconImage(new ImageIcon(LOGO_ICON).getImage());
		
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0);
		GridBagLayout l = new GridBagLayout();
		l.setConstraints(panel,c);
		panel.setLayout(l);
		
		terminologies = new JComboBox(getTerminologies());
		terminologies.setToolTipText("Select a terminology that will be used for coding");
		Terminology t = repository.getTerminology(DEFAULT_TERMINOLOGY);
		if(t != null)
			terminologies.setSelectedItem(t);
		
		input = new JTextField(30);
		input.setToolTipText("Select a directory with text documents (.txt) to process");
		JButton browse = new JButton("Browse");
		browse.setToolTipText("Select a directory with text documents (.txt) to process");
		browse.addActionListener(this);
		browse.setActionCommand("i_browser");
		
		/*JButton options = new JButton("Advanced Options");
		options.setToolTipText("Set advanced options for a given terminology");
		options.setActionCommand("options");
		options.addActionListener(this);*/
		
		/*JButton info = new JButton("Info");
		info.setToolTipText("Display information about selected terminology");
		info.setActionCommand("info");
		info.addActionListener(this);*/
		
		JButton query = new JButton("Explore");
		query.setToolTipText("Use selected terminology to do concept search");
		query.setActionCommand("query");
		query.addActionListener(this);
		
		panel.add(new JLabel("Input Terminology"),c);c.gridx++;
		panel.add(terminologies,c);c.gridx++;
		panel.add(query,c);c.gridy++;c.gridx = 0;
		//panel.add(new JLabel("Input Options"),c);c.gridx++;
		//panel.add(options,c);c.gridx++;
		//panel.add(query,c);
		c.gridy++;c.gridx=0;
		panel.add(new JLabel("Input Directory "),c);c.gridx++;
		panel.add(input,c);c.gridx++;
		panel.add(browse,c);c.gridx=0;c.gridy++;

		String tip = "<html>Select a directory where output will be saved<br>" +
				"Output directory will contains<ul><li><b>index.html</b> - for presenting annotated document to a human.</li>" +
				"<li><b>"+RESULT_CSV+"</b> - tabulated spreadsheet file containing all extracted concept</li></ul></html> ";
		output = new JTextField(30);
		output.setToolTipText(tip);
		browse = new JButton("Browse");
		browse.setToolTipText(tip);
		browse.addActionListener(this);
		browse.setActionCommand("o_browser");
	
		panel.add(new JLabel("Output Directory"),c);c.gridx++;
		panel.add(output,c);c.gridx++;
		panel.add(browse,c);c.gridx=0;c.gridy++;
		panel.add(Box.createRigidArea(new Dimension(10,10)),c);
		
		JPanel conp = new JPanel();
		conp.setLayout(new BorderLayout());
		conp.setBorder(new TitledBorder("Console"));
		console = new JTextArea(10,40);
		//console.setLineWrap(true);
		console.setEditable(false);
		conp.add(new JScrollPane(console),BorderLayout.CENTER);
		//c.gridwidth=3;		
		//panel.add(conp,c);c.gridy++;c.gridx=0;
		
		buttonPanel = new JPanel();
		buttonPanel.setLayout(new BorderLayout());
		buttonPanel.setBorder(new EmptyBorder(10,30,10,30));
		run = new JButton("Process Documents");
		run.addActionListener(this);
		run.setActionCommand("run");
		buttonPanel.add(run,BorderLayout.CENTER);
		//panel.add(buttonPanel,c);
		
		progress = new JProgressBar();
		progress.setIndeterminate(true);
		progress.setString("Please Wait. It will take a while ...");
		progress.setStringPainted(true);
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.add(panel,BorderLayout.NORTH);
		p.add(conp,BorderLayout.CENTER);
		p.add(buttonPanel,BorderLayout.SOUTH);
		
		getOptions();
		loadDefaults();
		
		// add defaults listener
		terminologies.addItemListener(new ItemListener() {
			public void itemStateChanged(ItemEvent e) {
				loadDefaults();
			}
		});
	
		
		// wrap up, and display
		frame.setContentPane(p);
		frame.pack();
	
		//center on screen
		Dimension d = frame.getSize();
		Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
		frame.setLocation(new Point((s.width-d.width)/2,(s.height-d.height)/2));
		frame.setVisible(true);
		
		if(getTerminologies().length == 0){
			int r = JOptionPane.showConfirmDialog(frame, "<html>It appears you do not have any terminologies imported.<br>" +
					"Would you like to download NCI Thesaurus terminology for use with NobleCoder?","Download?",JOptionPane.YES_NO_OPTION);
			if(JOptionPane.NO_OPTION == r){
				JOptionPane.showMessageDialog(frame,IMPORT_MESSAGE,"",JOptionPane.INFORMATION_MESSAGE);
			}else if(JOptionPane.YES_OPTION == r){
				downloadTerminology();
			}
		}
	}
	

	private JMenuBar getMenuBar() {
		JMenuBar menu = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenu opt = new JMenu("Options");
		JMenu helpm = new JMenu("Help");
		
		
		JMenuItem importer = new JMenuItem("Import Terminologies ..",new ImageIcon(IMPORT_ICON));
		JMenuItem exporter = new JMenuItem("Export Terminologies..",new ImageIcon(EXPORT_ICON));
		JMenuItem path = new JMenuItem("Repository Location ..");
		JMenuItem help = new JMenuItem("Help");
		JMenuItem about = new JMenuItem("About");
		JMenuItem options = new JMenuItem("Runtime Options ..",new ImageIcon(OPTIONS_ICON));
		
		importer.setToolTipText("Import terminlogies from RRF/OWL/OBO into NobleCoder repository");
		exporter.setToolTipText("Export parts of terminologies from NobleCoder repository into an OWL file");
		path.setToolTipText("Change Terminology Repository Path");	
		options.setToolTipText("Change runtime options of the selected terminology");
		JMenuItem exit = new JMenuItem("Exit");
		importer.addActionListener(this);
		exit.addActionListener(this);
		importer.setActionCommand("importer");
		exit.setActionCommand("exit");
		path.addActionListener(this);
		path.setActionCommand("path");
		exporter.setActionCommand("exporter");
		exporter.addActionListener(this);
		help.addActionListener(this);
		about.addActionListener(this);
		help.setActionCommand("help");
		about.setActionCommand("about");
		options.addActionListener(this);
		options.setActionCommand("options");
		file.add(importer);
		file.add(exporter);
		file.addSeparator();
		opt.add(options);
		opt.addSeparator();
		opt.add(path);
		file.add(exit);
		helpm.add(help);
		helpm.add(about);
		menu.add(file);
		menu.add(opt);
		menu.add(helpm);
		
		return menu;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("run".equals(cmd)){
			doRun();
		}else if("i_browser".equals(cmd)){
			doBrowse(input);
		}else if("p_browser".equals(cmd)){
			doBrowse(pathText);
		}else if("o_browser".equals(cmd)){
			doBrowse(output);
		}else if("info".equals(cmd)){
			doInfo();
		}else if("add_source".equals(cmd)){
			doBrowse(sources);
		}else if("add_semantic_type".equals(cmd)){
			doBrowse(semanticTypes);
		}else if("add_whitelist".equals(cmd)){
			doBrowse(abbreviationWhitelistText);
		}else if("options".equals(cmd)){
			JOptionPane.showMessageDialog(frame,getOptions(),"Runtime Options",JOptionPane.PLAIN_MESSAGE);
		}else if(e.getSource() instanceof JCheckBox){
			syncOptions();
		}else if("importer".equals(cmd)){
			doImport();
		}else if("exporter".equals(cmd)){
			doExport();
		}else if("manager".equals(cmd)){
			doManage();
		}else if("path".equals(cmd)){
			doPath();
		}else if("exit".equals(cmd)){
			System.exit(0);
		}else if("query".equals(cmd)){
			doQuery();
		}else if("about".equals(cmd)){
			JOptionPane.showMessageDialog(frame,ABOUT_TEXT,"About",JOptionPane.INFORMATION_MESSAGE);
		}
		
	}
	
	private void doQuery() {
		if(terminology != null){
			registerOptions();
			TerminologyBrowser b = new TerminologyBrowser();
			b.setTerminologies(new Terminology [] {terminology});
			b.showDialog(frame, terminology.getName());
			/*QueryTool tools = new QueryTool();
			tools.setTerminology(terminology);
			
			OntologyExplorer ontologyExplorer = new OntologyExplorer();
			try {
				ontologyExplorer.setRoot(terminology.getRootConcepts());
			} catch (TerminologyException e) {
				e.printStackTrace();
			}
			
			JTabbedPane tabs = new JTabbedPane();
			tabs.addTab("Search", tools);
			tabs.addTab("Browse", ontologyExplorer);
			
			
			JOptionPane.showMessageDialog(frame,tabs,"Query "+terminology.getName(),JOptionPane.PLAIN_MESSAGE);*/
		}else{
			JOptionPane.showMessageDialog(frame,"No Terminology Selected","Error",JOptionPane.ERROR_MESSAGE);
		}
	}

	private void doPath() {
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		JTextField text=  new JTextField(20);
		text.setText(NobleCoderTerminology.getPersistenceDirectory().getAbsolutePath());
		p.add(new JLabel("Repository Path  "),BorderLayout.WEST);
		p.add(text,BorderLayout.CENTER);
		JButton browse = new JButton("Browse");
		browse.addActionListener(this);
		browse.setActionCommand("p_browser");
		p.add(browse,BorderLayout.EAST);
		pathText = text;
		int r = JOptionPane.showConfirmDialog(frame,p,"Change Repository Path",JOptionPane.OK_CANCEL_OPTION,JOptionPane.PLAIN_MESSAGE);
		if(r == JOptionPane.OK_OPTION){
			NobleCoderTerminology.setPersistenceDirectory(new File(text.getText()));
			refreshTerminologies();
		}
	}
	
	private void refreshTerminologies(){
		repository = null;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				terminologies.setModel(new DefaultComboBoxModel(getTerminologies()));
			}
		});
	}
	

	private void doManage() {
		if(imanager == null){
			imanager = new RepositoryManager(RepositoryManager.TERMINOLOGIES_ONLY);
			imanager.start(repository);
			imanager.getFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			imanager.getFrame().setLocation(frame.getLocation());
		}else{
			imanager.getFrame().setVisible(true);
		}
	}

	private void doImport() {
		if(loader == null){
			loader = new TerminologyImporter();
			loader.showDialog();
			loader.getFrame().setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
			loader.getFrame().setLocation(frame.getLocation());
			loader.addPropertyChangeListener(new PropertyChangeListener() {
				public void propertyChange(PropertyChangeEvent evt) {
					refreshTerminologies();
				}
			});
		}else{
			loader.getFrame().setVisible(true);
		}
	}

	private void doExport() {
		if(exporter == null)
			exporter = new TerminologyExporter(repository);
		registerOptions();
		exporter.showExportWizard(frame);
	}
	
	/**
     * Display a file in the system browser. If you want to display a file, you
     * must include the absolute path name.
     *
     * @param url
     *            the file's url (the url must start with either "http://" or
     *            "file://").
     */
     private void browseURLInSystemBrowser(String url) {
    	 Desktop desktop = Desktop.getDesktop();
    	 if( !desktop.isSupported( java.awt.Desktop.Action.BROWSE ) ) {
    		 progress("Could not open "+url+"\n");
    	 }
    	 try {
    		 java.net.URI uri = new java.net.URI( url );
    		 desktop.browse( uri );
    	 }catch ( Exception e ) {
           System.err.println( e.getMessage() );
    	 }
     }
	
	private void loadDefaults(){
		// set up defaults
		terminology = null;
		NobleCoderTerminology t = getTerminology();
		if(t != null){
			// set semantic types
			String s = "";
			for(SemanticType st: t.getSemanticTypeFilter()){
				s+=st.getName()+";";
			}
			if(s.length() > 0){
				filterBySemanticTypes.setSelected(true);
				semanticTypes.setEditable(true);
				semanticTypes.setText(s.substring(0,s.length()-1));
				b1.setEnabled(true);
			}else{
				filterBySemanticTypes.setSelected(false);
				semanticTypes.setText("");
			}
			// set sources
			s = "";
			for(Source st: t.getSourceFilter()){
				s+=st.getCode()+";";
			}
			if(s.length() > 0){
				filterBySources.setSelected(true);
				sources.setEditable(true);
				sources.setText(s.substring(0,s.length()-1));
				b2.setEnabled(true);
			}else{
				filterBySources.setSelected(false);
				sources.setText("");
			}
			
			searchMethods.setSelectedItem(t.getDefaultSearchMethod());
			stripSmallWords.setSelected(t.isIgnoreSmallWords());
			stripCommonWords.setSelected(t.isIgnoreCommonWords());
			//stripDigits.setSelected(t.isIgnoreDigits());
			selectBestCandidates.setSelected(t.isSelectBestCandidate());
			//useSlidingWindow.setSelected(t.getWindowSize() > 0);
			//slidingWindow.setText(""+t.getWindowSize());
			handleAbbreviations.setSelected(t.isIgnoreAcronyms());
			ignoreUsedWords.setSelected(t.isIgnoreUsedWords());
			subsumptionMode.setSelected(t.isSubsumptionMode());
			overlapMode.setSelected(t.isOverlapMode());
			contiguousMode.setSelected(t.isContiguousMode());
			orderedMode.setSelected(t.isOrderedMode());
			partialMode.setSelected(t.isPartialMode());
			handleAcronymExpansion.setSelected(isAcronymExpansion());
			handleNegationDetection.setSelected(isHandleNegation());
			semanticPanel = null;
			sourcePanel = null;
			
			/*try {
				setupAcronyms(new File(t.getLocation()));
			} catch (IOException e) {
				e.printStackTrace();
			}*/
			
			syncOptions();
		}
	}
	
	private void syncOptions(){
		String strategy =searchMethods.getSelectedItem().toString();
		if(NobleCoderTerminology.BEST_MATCH.equals(strategy)){
			tip.setText("Provides the narrowest meaningful match, with fewest candidates. Best for concept coding and information extraction.");
			subsumptionMode.setSelected(true);
			overlapMode.setSelected(true);
			orderedMode.setSelected(false);
			contiguousMode.setSelected(true);
			wordWindow.setText("1");
			partialMode.setSelected(false);
			
			stripCommonWords.setSelected(false);
			stripCommonWords.doClick();
			//stripDigits.setSelected(true);
			stripSmallWords.setSelected(true);
			scoreConcepts.setSelected(true);
			selectBestCandidates.setSelected(true);
			ignoreUsedWords.setSelected(true);
		
		}else if(NobleCoderTerminology.ALL_MATCH.equals(strategy)){
			tip.setText("Provides as many matched candidates as possible. Best for information retrieval and text mining.");
			subsumptionMode.setSelected(false);
			overlapMode.setSelected(true);
			orderedMode.setSelected(false);
			contiguousMode.setSelected(false);
			wordWindow.setText("3");
			partialMode.setSelected(false);
			
			stripCommonWords.setSelected(true);
			stripCommonWords.doClick();
			//stripDigits.setSelected(false);
			stripSmallWords.setSelected(false);
			scoreConcepts.setSelected(false);
			selectBestCandidates.setSelected(false);
			ignoreUsedWords.setSelected(false);
		}else if(NobleCoderTerminology.PRECISE_MATCH.equals(strategy)){
			tip.setText("Attempts to minimize the number of false positives by filtering out candidates that do not " +
					"appear in exactly the same form as in controlled terminology.  " +
					"Similar to best match, but boosts precision at the expense of recall. ");
			subsumptionMode.setSelected(true);
			overlapMode.setSelected(true);
			orderedMode.setSelected(true);
			contiguousMode.setSelected(true);
			wordWindow.setText("0");
			partialMode.setSelected(false);
			
			stripCommonWords.setSelected(false);
			stripCommonWords.doClick();
			//stripDigits.setSelected(true);
			stripSmallWords.setSelected(true);
			scoreConcepts.setSelected(true);
			selectBestCandidates.setSelected(true);
			
		}else if(NobleCoderTerminology.PARTIAL_MATCH.equals(strategy)){
			tip.setText("Allows matching of concepts even if the entire term representing it is not mentioned in input text. " +
					"Best for concept coding with small controlled terminologies with poorly developed synonymy.");
			subsumptionMode.setSelected(false);
			overlapMode.setSelected(true);
			orderedMode.setSelected(false);
			contiguousMode.setSelected(false);
			wordWindow.setText("3");
			partialMode.setSelected(true);
			ignoreUsedWords.setSelected(true);
			stripCommonWords.setSelected(true);
			stripCommonWords.doClick();
			//stripDigits.setSelected(false);
			stripSmallWords.setSelected(false);
			scoreConcepts.setSelected(false);
			selectBestCandidates.setSelected(false);
		}else if(NobleCoderTerminology.CUSTOM_MATCH.equals(strategy)){
			tip.setText("Manually adjust search strategy by tweaking individual parameters.");
			subsumptionMode.setSelected(true);
			overlapMode.setSelected(true);
			orderedMode.setSelected(false);
			contiguousMode.setSelected(false);
			wordWindow.setText("3");
			partialMode.setSelected(false);
			
			stripCommonWords.setSelected(true);
			stripCommonWords.doClick();
			//stripDigits.setSelected(false);
			stripSmallWords.setSelected(false);
			scoreConcepts.setSelected(false);
			selectBestCandidates.setSelected(false);
			ignoreUsedWords.setSelected(true);
		}
		
		
		
		//slidingWindow.setEditable(useSlidingWindow.isSelected());
		sources.setEditable(filterBySources.isSelected());
		semanticTypes.setEditable(filterBySemanticTypes.isSelected());
		b2.setEnabled(filterBySources.isSelected());
		b1.setEnabled(filterBySemanticTypes.isSelected());
		abbreviationWhitelistText.setEditable(handleAbbreviations.isSelected());
		b3.setEnabled(handleAbbreviations.isSelected());
		
		subsumptionMode.setEnabled(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethods.getSelectedItem()));
		overlapMode.setEnabled(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethods.getSelectedItem()));
		orderedMode.setEnabled(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethods.getSelectedItem()));
		contiguousMode.setEnabled(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethods.getSelectedItem()));
		partialMode.setEnabled(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethods.getSelectedItem()));
	}
	
	
	/**
	 * register options with selected terminology
	 */
	private void registerOptions(){
		// extrac handlers
		if(terminology != null){
			setAcronymExpansion(handleAcronymExpansion.isSelected());
			setHandleNegation(handleNegationDetection.isSelected());
		
			// initialize terminology with vaiours option
			String searchMethod = ""+searchMethods.getSelectedItem();
			terminology.setIgnoreCommonWords(stripCommonWords.isSelected());
			//terminology.setIgnoreDigits(stripDigits.isSelected());
			terminology.setIgnoreSmallWords(stripSmallWords.isSelected());
			terminology.setSelectBestCandidate(selectBestCandidates.isSelected());
			terminology.setDefaultSearchMethod(searchMethod);
			terminology.setIgnoreUsedWords(ignoreUsedWords.isSelected());
			if(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethod)){
				terminology.setSubsumptionMode(subsumptionMode.isSelected());
				terminology.setOverlapMode(overlapMode.isSelected());
				terminology.setContiguousMode(contiguousMode.isSelected());
				terminology.setPartialMode(partialMode.isSelected());
				terminology.setOrderedMode(orderedMode.isSelected());
			}
			/*if(useSlidingWindow.isSelected()){
				int n = Integer.parseInt(slidingWindow.getText());
				terminology.setWindowSize(n);
			}*/
			if(filterBySemanticTypes.isSelected()){
				terminology.setSemanticTypeFilter(getSemanticTypes(semanticTypes.getText()));
			}else{
				terminology.setSemanticTypeFilter("");
			}
			if(filterBySources.isSelected()){
				terminology.setSourceFilter(getSource(sources.getText()));
			}else{
				terminology.setSourceFilter("");
			}
				
			skipAbbrreviationLogic = true;
			terminology.setIgnoreAcronyms(false);
			if(handleAbbreviations.isSelected()){
				String path = abbreviationWhitelistText.getText();
				//abbreviationWhitelist = TextTools.loadResource(path);
				skipAbbrreviationLogic = false;
				terminology.setIgnoreAcronyms(true);
			}
			terminology.clearCache();
		}
	}
	
	
	private JPanel getSemanticTypePanel(){
		if(semanticPanel == null){
			semanticPanel = new JPanel();
			semanticPanel.setBackground(Color.white);
			semanticPanel.setLayout(new BoxLayout(semanticPanel,BoxLayout.Y_AXIS));
			JCheckBox all = new JCheckBox("All Semantic Types");
			all.setOpaque(false);
			all.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JCheckBox all = (JCheckBox) e.getSource();
					for(int i =0; i<semanticPanel.getComponentCount();i++){
						Component c = semanticPanel.getComponent(i);
						if(c instanceof JCheckBox && c != e.getSource()){
							((JCheckBox)c).setSelected(all.isSelected());
						}
					}
				}
			});
			semanticPanel.add(all);
			semanticPanel.add(new JSeparator(SwingConstants.HORIZONTAL));
			for(String name: getAllSemanticTypes()){
				JCheckBox b = new JCheckBox(name);
				b.setOpaque(false);
				semanticPanel.add(b);
			}
		}
		return semanticPanel;
	}
	
	private JPanel getSourcePanel(){
		if(sourcePanel == null){
			sourcePanel = new JPanel();
			sourcePanel.setBackground(Color.white);
			sourcePanel.setLayout(new BoxLayout(sourcePanel,BoxLayout.Y_AXIS));
			JCheckBox all = new JCheckBox("All Sources");
			all.setOpaque(false);
			all.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					JCheckBox all = (JCheckBox) e.getSource();
					for(int i =0; i<sourcePanel.getComponentCount();i++){
						Component c = sourcePanel.getComponent(i);
						if(c instanceof JCheckBox && c != e.getSource()){
							((JCheckBox)c).setSelected(all.isSelected());
						}
					}
				}
			});
			sourcePanel.add(all);
			sourcePanel.add(new JSeparator(SwingConstants.HORIZONTAL));
			Terminology t = getTerminology();
			if(t != null){
				for(Source name: t.getSources()){
					JCheckBox b = new JCheckBox(name.getCode());
					b.setOpaque(false);
					b.setToolTipText("<html><body><table><tr><td width=500>"+name.getDescription()+"</td></tr></table></body></html>");
					sourcePanel.add(b);
				}
			}
			sourcePanel.add(Box.createVerticalGlue());
		}
		return sourcePanel;
	}
	
	
	private JPanel getOptions() {
		if(options == null){
			options = new JPanel();
			options.setLayout(new BoxLayout(options,BoxLayout.Y_AXIS));
			
			searchMethods = new JComboBox(getSearchMethods());
			searchMethods.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent e) {
					syncOptions();
				}
			});
			tip = new JTextArea(4,50);
			tip.setEditable(false);
			tip.setBackground(new Color(255,255,204));
			tip.setLineWrap(true);
			tip.setWrapStyleWord(true);
			tip.setBorder(new LineBorder(Color.darkGray));
			
			
			subsumptionMode = new JCheckBox("SUBSUMPTION: Subsume more general concepts if more specific concept is found");
			subsumptionMode.setToolTipText("Ex: Terms 'big' and 'dog' are not matched if concept 'big dog' is matched");
			overlapMode = new JCheckBox("OVERLAP: Overlapping concepts are allowed");
			overlapMode.setToolTipText("Ex: 'big cats and dogs' will match 'big cat' and 'big dog'");
			contiguousMode = new JCheckBox("CONTIGUITY: Words must be adjacent to each other to be considered");
			contiguousMode.setToolTipText("Ex: 'big dog' matches to 'big dog', while 'big friendly dog' does not");
			orderedMode = new JCheckBox("ORDER: Words must be in the same order to be considered a term");
			orderedMode.setToolTipText("Ex: 'dog, big' will not be matched as 'big dog'");
			partialMode = new JCheckBox("PARTIAL: Match a term if more then 50% of its words are found in text");
			partialMode.setToolTipText("Ex: Concept 'red dog' will be selected if word 'red' or 'dog' are found in text");
			wordWindow = new JTextField("3",3);
			wordWindow.setHorizontalAlignment(JTextField.CENTER);
			wordWindow.setToolTipText("Number of irrelevant words that can occur betwen adjacent words in a term");
			contiguousMode.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					wordWindow.setEditable(contiguousMode.isSelected());
				}
			});
			wordWindow.setEditable(contiguousMode.isSelected());
			stripSmallWords = new JCheckBox("Skip single letter words",true);
			stripSmallWords.setToolTipText("Do not consider single letter words as candidates for matching");
			stripCommonWords = new JCheckBox("Skip the most common English words",true);
			stripCommonWords.setToolTipText("Do not consider 100 most common English words as candidates for matching");
			final JButton b = new JButton("VIEW");
			b.setEnabled(false);
			b.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					String str = new TreeSet<String>(TextTools.getCommonWords()).toString();
					JTextArea text = new JTextArea(10,50);
					text.setLineWrap(true);
					text.setEditable(false);
					text.setText(str.substring(1,str.length()-1));
					JOptionPane.showMessageDialog(getOptions(),new JScrollPane(text),"Common Word List",JOptionPane.PLAIN_MESSAGE);
				}
			});
			stripCommonWords.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					b.setEnabled(stripCommonWords.isSelected());
				}
			});
			//stripDigits = new JCheckBox("Skip stand alone digits",true);
			//stripDigits.setToolTipText("Ignore digits in text and don't try to match them to dictionary");
			
			
			scoreConcepts = new JCheckBox("Score and filter matched concepts");
			scoreConcepts.setToolTipText("<html>Score each matched concepts based on several heuristics." +
											"This option can filter out some false positive matches. <br>" +
											"It is also necessary for selecting the best candidate in cases where there are multiple matches to the same term");
			selectBestCandidates = new JCheckBox("Select highest scored candidate for each matching term");
			selectBestCandidates.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					if(selectBestCandidates.isSelected())
						scoreConcepts.setSelected(true);
				}
			});
			selectBestCandidates.setToolTipText("When there are multiple candidate concepts matching the same term, select the highest scoring one.");
			//useSlidingWindow = new JCheckBox("Set maximum lookup window size ");
			//useSlidingWindow.setToolTipText("This option is useful if ");
			//useSlidingWindow.addActionListener(this);
			filterBySemanticTypes = new JCheckBox("Filter by SemanticTypes");
			filterBySemanticTypes.addActionListener(this);
			filterBySemanticTypes.setToolTipText("Only include concepts that have their semantic type selected in a filter list");
			filterBySources = new JCheckBox("Filter by Vocabulary Source");
			filterBySources.setToolTipText("Only include concepts that have their source selected in a filter list");
			filterBySources.addActionListener(this);
			//slidingWindow = new JTextField("10",3);
			//slidingWindow.setHorizontalAlignment(JTextField.CENTER);
			semanticTypes = new JTextField(30);
			semanticTypes.setToolTipText("Only include concepts that have their semantic type selected in a filter list");
			
			sources = new JTextField(30);
			sources.setToolTipText("Only include concepts that have their source selected in a filter list");
			openHTML = new JCheckBox("Open coded HTML output in the web browser",true);
			openHTML.setToolTipText("NobleCoder can open default system web browser to show the result of coding");
			handleAbbreviations = new JCheckBox("Exclude abbreviations except the ones below",false);
			abbreviationWhitelistText = new JTextField("",30);
			handleAbbreviations.addActionListener(this);
			handleAbbreviations.setToolTipText("If there are multiple abbreviation that match input text, use the one in the whitelist or suppress if not in a list");
			abbreviationWhitelistText.setToolTipText("If there are multiple abbreviation that match input text, use the one in the whitelist or suppress if not in a list");
			ignoreUsedWords = new JCheckBox("Skip already matched words in text to speed up the search");
			ignoreUsedWords.setToolTipText("<html>Use gready strategy to speed up search. In some cases this strategy may skip some matches.<br>" +
					"Ex: with text 'big easy, blue cat' it might not find a concept of 'easy cat' if such exists in a dictionary");
			handleAcronymExpansion = new JCheckBox("Detect acronyms found in its expanded form");
			handleAcronymExpansion.setToolTipText("If a document contains an acronym in its expanded form that has a matching concept, use that concept for subsequent matches.");
			handleNegationDetection = new JCheckBox("Detect negation and other linguistic modifiers");
			handleNegationDetection.setToolTipText("Uses ConText alogirthm to detect negation, certainty, experiencer and other linguistic modifiers for identified concepts.");
			
			b1 = new JButton("+");
			b1.setActionCommand("add_semantic_type");
			b1.addActionListener(this);
		
			
			b2 = new JButton("+");
			b2.setActionCommand("add_source");
			b2.addActionListener(this);
			
			b3 = new JButton("+");
			b3.setActionCommand("add_whitelist");
			b3.addActionListener(this);
			
			/*JPanel p = new JPanel();
			p.setLayout(new FlowLayout(FlowLayout.LEFT));
			p.setBorder(new EmptyBorder(-5,-5,-5,0));
			p.add(useSlidingWindow);
			p.add(slidingWindow);
			 */
			JPanel p1 = new JPanel();
			p1.setLayout(new FlowLayout(FlowLayout.LEFT));
			p1.setBorder(new EmptyBorder(-5,-5,-5,0));
			p1.add(semanticTypes);
			p1.add(b1);
		
			JPanel p2 = new JPanel();
			p2.setLayout(new FlowLayout(FlowLayout.LEFT));
			p2.setBorder(new EmptyBorder(-5,-5,-5,0));
			p2.add(sources);
			p2.add(b2);
			
			JPanel p3 = new JPanel();
			p3.setLayout(new FlowLayout(FlowLayout.LEFT));
			p3.setBorder(new EmptyBorder(-5,-5,-5,0));
			p3.add(abbreviationWhitelistText);
			p3.add(b3);
			
			JPanel p4 = new JPanel();
			p4.setLayout(new FlowLayout(FlowLayout.LEFT));
			p4.setBorder(new EmptyBorder(-5,-5,-5,0));
			p4.add(contiguousMode);
			p4.add(wordWindow);
			
			JPanel p5 = new JPanel();
			p5.setLayout(new FlowLayout(FlowLayout.LEFT));
			p5.setBorder(new EmptyBorder(-5,-5,-5,0));
			p5.add(stripCommonWords);
			p5.add(b);
			
			
			options.add(new JLabel("Search Strategy"));
			options.add(searchMethods);
			options.add(tip);
			options.add(new JLabel(" "));
			options.add(subsumptionMode);
			options.add(overlapMode);
			options.add(p4);
			options.add(orderedMode);
			options.add(partialMode);
			options.add(new JLabel(" "));
			options.add(stripSmallWords);
			options.add(p5);
			//options.add(stripDigits);
			options.add(scoreConcepts);
			options.add(selectBestCandidates);
			//options.add(p);
			options.add(ignoreUsedWords);
			options.add(new JLabel(" "));
			options.add(handleAcronymExpansion);
			options.add(handleNegationDetection);
			options.add(filterBySemanticTypes);
			options.add(p1);
			options.add(filterBySources);
			options.add(p2);
			options.add(handleAbbreviations);
			options.add(p3);
			options.add(openHTML);
			
			for(Component c: options.getComponents()){
				((JComponent)c).setAlignmentX(JComponent.LEFT_ALIGNMENT);
			}
			
			syncOptions();
			
		}
		return options;
	}


	private String [] getSearchMethods() {
		Terminology t = getTerminology();
		return (t != null)?t.getSearchMethods():new String [] {""};
	}

	private void doInfo() {
		Terminology d = getTerminology();
		JEditorPane text = new JEditorPane();
		text.setContentType("text/html; charset=UTF-8");
		text.setEditable(false);
		text.setPreferredSize(new Dimension(400,400));
		text.setBorder(new LineBorder(Color.gray));
		
		String desc = "<b>"+d.getName()+"</b> "+d.getVersion()+"<br>"+d.getURI()+"<hr>"+d.getDescription();
		if(d instanceof Terminology){
			desc +="<hr>";
			Terminology t = (Terminology) d;
			desc += "Languages: "+Arrays.toString(t.getLanguages())+"<br>";
			desc += "Sources: "+Arrays.toString(t.getSources())+"<br>";

		}
		text.setText(desc);
		JOptionPane.showMessageDialog(frame,text,"",JOptionPane.PLAIN_MESSAGE);
		
	}
	
	private void setBusy(final boolean busy){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(buttonPanel != null){
					buttonPanel.removeAll();
					buttonPanel.add((busy)?progress:run,BorderLayout.CENTER);
					buttonPanel.validate();
					buttonPanel.repaint();
					if(busy)
						console.setText("");
				}
			}
		});
	}


	private void doRun() {
		if(checkInputs())
			return;
		
		(new Thread(new Runnable(){
			public void run() {
				setBusy(true);
				
				// create a new process
				//String classpath = System.getProperty("java.class.path");
			    //String path = System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";
			    
			    // build parameters
			    List<String> args = new ArrayList<String>();
			    
			    //args.add(path);
			    //args.add("-cp");
			    //args.add(classpath);
			    //args.add(NobleCoderTool.class.getName());
			 
			    args.add("-terminology");
			    args.add(getTerminology().getName());
			    
			    args.add("-input");
			    args.add(input.getText());
			    
			    args.add("-output");
			    args.add(output.getText());
			    
			    args.add("-search");
			    args.add(""+searchMethods.getSelectedItem());
			    
			    //if(stripDigits.isSelected())
			    //	args.add("-stripDigits");
			    if(stripSmallWords.isSelected())
			    	args.add("-stripSmallWords");
			    if(stripCommonWords.isSelected())
			    	args.add("-stripCommonWords");
			    if(selectBestCandidates.isSelected())
			    	args.add("-selectBestCandidates");
			    if(filterBySemanticTypes.isSelected()){
			    	args.add("-semanticTypes");
			    	args.add(semanticTypes.getText());
			    }
			    if(filterBySources.isSelected()){
			    	args.add("-sources");
			    	args.add(sources.getText());
			    }
			   /* if(useSlidingWindow.isSelected()){
			    	args.add("-slidingWindow");
			    	args.add(slidingWindow.getText());
			    }*/
			    
			    if(handleAbbreviations.isSelected()){
			    	args.add("-abbreviations");
			    	args.add(abbreviationWhitelistText.getText());
			    }
			    if(ignoreUsedWords.isSelected())
			    	args.add("-ignoreUsedWords");
			    if(scoreConcepts.isSelected())
			    	args.add("-scoreConcepts");
			    
			    // customize search
			    if(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethods.getSelectedItem())){
			    	if(subsumptionMode.isSelected())
			    		args.add("-subsumptionMode");
			     	if(overlapMode.isSelected())
			    		args.add("-overlapMode");
			     	if(contiguousMode.isSelected()){
			    		args.add("-contiguousMode");
			    		args.add(wordWindow.getText());
			     	}
			     	if(orderedMode.isSelected())
			    		args.add("-orderedMode");
			     	if(partialMode.isSelected())
			    		args.add("-partialMode");
			    }
			    // extra handlers
			    if(handleAcronymExpansion.isSelected())
			    	args.add("-acronymExpansion");
			    if(handleNegationDetection.isSelected())
			    	args.add("-negationDetection");
			    
			    // execute import
			    try{
			    	process(args);
			    }catch(Exception ex){
			    	JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
			    	ex.printStackTrace();			    	
			    }
				
				setBusy(false);
				
				// open in browser
				if(openHTML != null && openHTML.isSelected()){
					browseURLInSystemBrowser(new File(output.getText()+File.separator+"index.html").toURI().toString());
				}
				
			}
		})).start();
		
	}

	protected boolean checkInputs() {
		if(getTerminology() == null){
			JOptionPane.showMessageDialog(frame,"You don't have any terminologies imported","Error",JOptionPane.ERROR_MESSAGE);
			return true;
		}
		
		if(input.getText().length() == 0 || output.getText().length() == 0){
			JOptionPane.showMessageDialog(frame,"Input / Output can not be blank","Error",JOptionPane.ERROR_MESSAGE);
			return true;
		}
		
		File f = new File(input.getText());
		if(!f.exists()){
			JOptionPane.showMessageDialog(frame,"Input "+f.getAbsolutePath()+" does not exist!","Error",JOptionPane.ERROR_MESSAGE);
			return true;
		}
		return false;
	}

	/**
	 * get string from check list
	 * @param p
	 * @return
	 */
	private String getCheckList(JPanel p){
		StringBuffer b = new StringBuffer();
		for(Component c: p.getComponents()){
			if(c instanceof AbstractButton && ((AbstractButton)c).isSelected()){
				b.append(((AbstractButton)c).getText()+";");
			}
		}
		return (b.length() > 0)?b.substring(0,b.length()-1):"";
	}
	
	/**
	 * get string from check list
	 * @param p
	 * @return
	 */
	private void setCheckList(JPanel p,String s){
		for(Component c: p.getComponents()){
			if(c instanceof AbstractButton){
				AbstractButton a = (AbstractButton) c;
				if(s.contains(";"+a.getText()+";") || s.startsWith(a.getText()) || s.endsWith(a.getText())){;
					a.setSelected(true);
				}else{
					a.setSelected(false);
				}
			}
		}
	}
	
	private void doBrowse(JTextField text){
		if(text == sources){
			setCheckList(getSourcePanel(),sources.getText());
			JScrollPane p = new JScrollPane(getSourcePanel());
			p.setPreferredSize(new Dimension(200,400));
			p.getVerticalScrollBar().setUnitIncrement(20);
			JOptionPane.showMessageDialog(frame,p,"Source",JOptionPane.PLAIN_MESSAGE);
			sources.setText(getCheckList(getSourcePanel()));
		}else if(text == semanticTypes) {
			setCheckList(getSemanticTypePanel(),semanticTypes.getText());
			JScrollPane p = new JScrollPane(getSemanticTypePanel());
			p.setPreferredSize(new Dimension(500,500));
			p.getVerticalScrollBar().setUnitIncrement(20);
			JOptionPane.showMessageDialog(frame,p,"Semantic Types",JOptionPane.PLAIN_MESSAGE);
			semanticTypes.setText(getCheckList(getSemanticTypePanel()));
		}else{
			JFileChooser fc = new JFileChooser(file);
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			/*fc.setFileFilter(new FileFilter() {
				public String getDescription() {
					return "Text Documents (.txt)";
				}
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().toLowerCase().endsWith(".txt");
				}
			});*/
			int r = (output == text)?fc.showSaveDialog(frame):fc.showOpenDialog(frame);
			if(r == JFileChooser.APPROVE_OPTION){
				file = fc.getSelectedFile();
				text.setText(file.getAbsolutePath());
				
				// if input, change output to default
				if(text == input){
					output.setText(new File(file.getParent()+File.separator+"Output"+File.separator+file.getName()).getAbsolutePath());
				}
			}
		}
	}
	
	private Terminology [] getTerminologies() {
		if(repository == null)
			repository =  new DefaultRepository();
		Terminology [] terms =  repository.getTerminologies();
		Arrays.sort(terms,new Comparator<Terminology>() {
			public int compare(Terminology o1, Terminology o2) {
				return o1.getName().compareTo(o2.getName());
			}
		});
		
		return terms;
	}
	
	private void downloadTerminology(){
		(new Thread(){
			public void run(){
				try{
					setBusy(true);
					URL url = new URL(DEFAULT_TERMINOLOGY_DOWNLOAD);
					InputStream is = url.openStream();
					unzip(is,NobleCoderTerminology.getPersistenceDirectory());
					repository =  new DefaultRepository();
					SwingUtilities.invokeLater(new Runnable() {
						public void run() {
							DefaultComboBoxModel<Terminology> model = new DefaultComboBoxModel<Terminology>();
							for(Terminology t: repository.getTerminologies()){
								model.addElement(t);
							}
							terminologies.setModel(model);
							terminologies.validate();
							terminologies.repaint();
							terminologies.setSelectedIndex(0);
							options = null;
							getOptions();
						}
					});
					setBusy(false);
					JOptionPane.showMessageDialog(frame,"<html>NCI Thesaurus succesfully imported.<br>"+IMPORT_MESSAGE,"",JOptionPane.INFORMATION_MESSAGE);
					
				}catch(Exception ex){
					JOptionPane.showMessageDialog(frame,"<html>Failed to download Terminology!<br>"+ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			}
		}).start();
	}
	
	/**
	 * unzip file to directory
	 * copy/pasted from http://javadevtips.blogspot.com/2011/10/unzip-files.html
	 * http://www.thecoderscorner.com/team-blog/java-and-jvm/12-reading-a-zip-file-from-java-using-zipinputstream
	 * and modified
	 * @param is
	 * @param destDirectory
	 * @return
	 */
	 public void unzip(InputStream is, File destDirectory) throws Exception {
		 // create the destination directory structure (if needed)
		 if(!destDirectory.exists())
			 destDirectory.mkdirs();
			 
		 // create a buffer to improve copy performance later.
	     byte[] buffer = new byte[2048];

	     // open the zip file stream
	     ZipInputStream stream = new ZipInputStream(new BufferedInputStream(is));
	     try {

	    	 // now iterate through each item in the stream. The get next
	         // entry call will return a ZipEntry for each file in the
	         // stream
	         ZipEntry entry;
	         while((entry = stream.getNextEntry())!=null) {
	        	 // Once we get the entry from the stream, the stream is
	             // positioned read to read the raw data, and we keep
	             // reading until read returns 0 or less.
	        	 File outpath =  new File(destDirectory,entry.getName());
		         if(entry.isDirectory()){
		        	 outpath.mkdirs();
	        	 }else{
		        	 if(!outpath.getParentFile().exists())
		            	 outpath.getParentFile().mkdirs();
		             FileOutputStream output = null;
		             try{
		            	 output = new FileOutputStream(outpath);
		                 int len = 0;
		                 while ((len = stream.read(buffer)) > 0){
		                	 output.write(buffer, 0, len);
		                 }
		             }finally {
		                 // we must always close the output file
		                 if(output !=null) 
		                	 output.close();
		            }
	        	}
	        }
	     }finally{
	         // we must always close the zip file.
	         stream.close();
	     }
	 }
	
	
	public NobleCoderTerminology getTerminology(){
		if(terminology == null && terminologies != null)
			terminology = (NobleCoderTerminology) terminologies.getSelectedItem();
		return terminology;
	}

	private SemanticType [] getSemanticTypes(String str){
		String [] p = str.split(";");
		SemanticType [] src = new SemanticType [p.length];
		for(int i=0;i<p.length;i++)
			src[i] = SemanticType.getSemanticType(p[i].trim());
		return src;
	}
	
	private Source [] getSource(String str ){
		String [] p = str.split(";");
		Source [] src = new Source [p.length];
		for(int i=0;i<p.length;i++)
			src[i] = Source.getSource(p[i].trim());
		return src;
	}
	
	/**
	 * process  documents
	 * @param args
	 */
	public void process(List<String> args) throws Exception{
		getTerminologies();
		String term = args.get(args.indexOf("-terminology")+1);
		String in = args.get(args.indexOf("-input")+1);
		String out = args.get(args.indexOf("-output")+1);
		
		// set properties object
		Properties p = new Properties();
		String searchMethod = args.get(args.indexOf("-search")+1);
		p.setProperty("default.search.method",searchMethod);
		p.setProperty("ignore.small.words",""+args.contains("-stripCommonWords"));
		p.setProperty("ignore.common.words",""+args.contains("-stripCommonWords"));
		p.setProperty("ignore.digits",""+args.contains("-stripDigits"));
		p.setProperty("select.best.candidate",""+args.contains("-selectBestCandidates"));
		p.setProperty("score.concepts",""+args.contains("-scoreConcepts"));
		
		p.setProperty("sliding.window","0");
		p.setProperty("source.filter","");
		p.setProperty("semantic.type.filter","");
		
		p.setProperty("ignore.used.words",""+args.contains("-ignoreUsedWords"));
		p.setProperty("subsumption.mode",""+args.contains("-subsumptionMode"));
		p.setProperty("overlap.mode",""+args.contains("-overlapMode"));
		p.setProperty("contiguous.mode",""+args.contains("-contiguousMode"));
		p.setProperty("ordered.mode",""+args.contains("-orderedMode"));
		p.setProperty("partial.mode",""+args.contains("-partialMode"));
		
		// extrac handlers
		setAcronymExpansion(args.contains("-acronymExpansion"));
		setHandleNegation(args.contains("-negationDetection"));
		
		// initialize terminology with vaiours option
		terminology =  (NobleCoderTerminology) repository.getTerminology(term);
		terminology.setIgnoreCommonWords(args.contains("-stripCommonWords"));
		terminology.setIgnoreDigits(args.contains("-stripDigits"));
		terminology.setIgnoreSmallWords(args.contains("-stripSmallWords"));
		terminology.setSelectBestCandidate(args.contains("-selectBestCandidates"));
		terminology.setDefaultSearchMethod(searchMethod);
		terminology.setIgnoreUsedWords(args.contains("-ignoreUsedWords"));
		if(NobleCoderTerminology.CUSTOM_MATCH.equals(searchMethod)){
			terminology.setSubsumptionMode(Boolean.parseBoolean(p.getProperty("subsumption.mode")));
			terminology.setOverlapMode(Boolean.parseBoolean(p.getProperty("overlap.mode")));
			terminology.setContiguousMode(Boolean.parseBoolean(p.getProperty("contiguous.mode")));
			terminology.setPartialMode(Boolean.parseBoolean(p.getProperty("partial.mode")));
			terminology.setOrderedMode(Boolean.parseBoolean(p.getProperty("ordered.mode")));
		}
		
		terminology.setIgnoreAcronyms(false);
		p.setProperty("terminology.name",terminology.getName());
		
		// clear flash
		terminology.clearCache();
		
		
		int x = args.indexOf("-slidingWindow");
		if(x > -1){
			int n = Integer.parseInt(args.get(x+1));
			terminology.setWindowSize(n);
			p.setProperty("sliding.window",""+n);
		}
		x = args.indexOf("-contiguousMode");
		if(x > -1){
			int n = Integer.parseInt(args.get(x+1));
			terminology.setMaximumWordGap(n);
			p.setProperty("word.window.size",""+n);
		}
		x = args.indexOf("-semanticTypes");
		if(x > -1){
			terminology.setSemanticTypeFilter(getSemanticTypes(args.get(x+1)));
			p.setProperty("semantic.type.filter",args.get(x+1));
		}else{
			terminology.setSemanticTypeFilter("");
		}
		x = args.indexOf("-sources");
		if(x > -1){
			terminology.setSourceFilter(getSource(args.get(x+1)));
			p.setProperty("source.filter",args.get(x+1));
		}else{
			terminology.setSourceFilter("");
		}
		skipAbbrreviationLogic = true;
		x = args.indexOf("-abbreviations");
		if(x > -1){
			String path = args.get(x+1);
			//abbreviationWhitelist = TextTools.loadResource(path);
			skipAbbrreviationLogic = false;
			terminology.setIgnoreAcronyms(true);
			p.setProperty("ignore.acronyms","true");
			p.setProperty("abbreviation.whitelist",path);
			p.setProperty("abbreviation.terminology",ABBREV_TERMINOLOGY);
			
		}
		
		// process file
		List<File> files = getFiles(new File(in),new ArrayList<File>());
		if(files.isEmpty()){
			JOptionPane.showMessageDialog(frame,"No input files found","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		
		
		if(progress != null){
			final int n = files.size();
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					progress.setIndeterminate(false);
					progress.setMaximum(n);
				}
			});
		}
		Collections.sort(files);
		
		// process report
		File outputDir = new File(out);
		if(!outputDir.exists())
			outputDir.mkdirs();
		
		// reset stat counters
		int processCount = 0;
		long processTime = 0;
		
		// now create NobleCoder instance and run it
		NobleCoder coder = new NobleCoder(terminology);
		coder.setAcronymExpansion(handleAcronyms);
		coder.setContextDetection(handleNegation);
		//coder.setProcessFilter(NobleCoder.FILTER_DEID|NobleCoder.FILTER_HEADER);
		
		// initialize writers
		htmlExporter = new HTMLExporter(outputDir);
		csvExporter = new CSVExporter(outputDir);
		
		for(int i=0;i<files.size();i++){
			progress("processing report ("+(processCount+1)+") "+files.get(i).getName()+" ... ");
			Document doc = coder.process(files.get(i));
			processTime += coder.getProcessTime();
			progress(coder.getProcessTime()+" ms\n");
			
			// now output HTML for this report
			htmlExporter.export(doc);
			csvExporter.export(doc);
			
			//processReport(files.get(i), outputDir);
			if(progress != null){
				final int n = i+1;
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						progress.setValue(n);
					}
				});
			}
			processCount++;
		}
		
		// save search properties
		FileOutputStream os = new FileOutputStream(new File(outputDir,"search.properties"));
		p.store(os,"NobleCoderTool parameters for a given run");
		os.close();
		
		// wrap up
		try {
			htmlExporter.flush();
			csvExporter.flush();
		} catch (Exception e) {
			progress("Error: "+e.getMessage());
			e.printStackTrace();
		}
		
		// summary
		double ave = (processCount > 0)?processTime/processCount:0;
		progress("\nTotal process time for all reports:\t"+processTime+" ms\n");
		progress("Average process time per report:\t"+ave+" ms\n");
	}

	private List<File> getFiles(File in,List<File> list) {
		if(in.isDirectory()){
			for(File f: in.listFiles()){
				getFiles(f,list);
			}
		}else if(in.isFile()){ // && in.getName().endsWith(".txt")
			list.add(in);
			
		/*	try{
				// add text files
				String type = Files.probeContentType(in.toPath());
				if (type.equals("text/plain")) {
					list.add(in); 
				}
			}catch(NoClassDefFoundError ex){
				// if Files not there then we have Java < 1.7, then add anyway
				list.add(in);
			} catch (IOException e) {
				e.printStackTrace();
			}*/
		}
		return list;
	}



	
	/**
	 * get a list of all emantic types
	 * @return
	 */
	private List<String> getAllSemanticTypes(){
		List list = new ArrayList();
		for(SemanticType s: SemanticType.getDefinedSemanticTypes()){
			list.add(s.getName());
		}
		Collections.sort(list);
		return list;
	}
	
		
	public boolean isAbbrreviationFiltering() {
		return !skipAbbrreviationLogic;
	}
	
	/**
	 * set abbreviation filtering against the abbreviation terminology and white list if available.
	 * @param filter
	 */
	public void setAbbrreviationFiltering(boolean filter) {
		this.skipAbbrreviationLogic = !filter;
	}

	
	/**
	 * is acronym expansion enabled
	 * @return
	 */
	public boolean isAcronymExpansion() {
		return handleAcronyms;
	}

	/**
	 * handle acronym expansion
	 * @param handleAcronyms
	 */
	public void setAcronymExpansion(boolean handleAcronyms) {
		this.handleAcronyms = handleAcronyms;
	}

	
	
	public boolean isHandleNegation() {
		return handleNegation;
	}

	public void setHandleNegation(boolean handleNegation) {
		this.handleNegation = handleNegation;
	}
	
	
	private void progress(String str){
		System.out.print(str);
		if(console != null){
			final String s = str;
			SwingUtilities.invokeLater(new Runnable(){
				public void run(){
					console.append(s);
				}
			});
			
		}
	}
	
		
}
