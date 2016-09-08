package edu.pitt.dbmi.nlp.noble.ui;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import java.beans.PropertyChangeSupport;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import edu.pitt.dbmi.nlp.noble.ontology.DefaultRepository;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.OntologyUtils;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BOntology;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalRepository;
import edu.pitt.dbmi.nlp.noble.ontology.owl.OOntology;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.util.ConceptImporter;
import edu.pitt.dbmi.nlp.noble.util.PathHelper;
import edu.pitt.dbmi.nlp.noble.util.StringUtils;





/**
 * loads IndexFinder loaders
 * @author tseytlin
 *
 */
public class TerminologyImporter implements ItemListener, ActionListener, PropertyChangeListener {
	private final String [] FORMAT_NAMES = new String [] 
	{"UMLS/Metathesaurus RRF Directory","OWL Ontology","OBO Taxonomy","BioPortal Ontology","Terminology Text File"};
	private final String [] FORMAT_ARGS = new String [] {"-rrf","-owl","-obo","-bioportal","-txt"};
	private JFrame frame;
	private JComboBox inputFormats,metathesaurusList;
	private JTextField inputLocation,outputLocation,semanticTypeList,sourceList,languageList,memSize;
	private JTextArea console;
	private JCheckBox useStemmer,stripDigits,compact,suppressObsoleteTerms,useMetaInfo; //inMemory, truncateURI
	private JPanel buttonPanel,commonOptions;
	private JDialog bioportalDialog;
	private OntologyImporter importer;
	private JButton run,options;
	private JLabel inputLabel;
	private JProgressBar progress;
	private File file;
	private JCheckBox createAncestors;
	private JTextField hierarchySourceList;
	private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
	private JTextField maxWordsInTerm;
	private JCheckBox maxWordsInTermCheck;
	
	public void addPropertyChangeListener(PropertyChangeListener l){
		pcs.addPropertyChangeListener(l);
	}
	
	public void removePropertyChangeListener(PropertyChangeListener l){
		pcs.removePropertyChangeListener(l);
	}
	
	
	
	/**
	 * create UI
	 */
	public void showDialog() {
		Vector<String> formats = new Vector<String>();
		Collections.addAll(formats,FORMAT_NAMES);
		frame = new JFrame("Import Terminology");
		frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0);
		GridBagLayout l = new GridBagLayout();
		l.setConstraints(panel,c);
		panel.setLayout(l);
		
		
		inputFormats = new JComboBox(formats);
		inputFormats.addItemListener(this);
		inputLabel = new JLabel("Input RRF Directory");
		inputLocation = new JTextField(30);
		JButton browse = new JButton("Browse");
		browse.addActionListener(this);
		browse.setActionCommand("i_browser");
		options = new JButton("Options");	
		options.setActionCommand("options");
		options.addActionListener(this);
		
		panel.add(new JLabel("Input Format"),c);c.gridx++;
		panel.add(inputFormats,c);c.gridx++;
		panel.add(options,c);c.gridx = 0;c.gridy++;
		panel.add(inputLabel,c);c.gridx++;
		panel.add(inputLocation,c);c.gridx++;
		panel.add(browse,c);c.gridx=0;c.gridy++;
		//panel.add(new JLabel("Input Options"),c);c.gridx++;
		//panel.add(options,c);c.gridy++;c.gridx=0;
		
		
		memSize = new JTextField(30);
		
		//panel.add(new JLabel("Memory Size"),c);c.gridx++;
		//panel.add(memSize,c);c.gridx=0;c.gridy++;

		
		
		outputLocation = new JTextField(30);
		browse = new JButton("Browse");
		browse.addActionListener(this);
		browse.setActionCommand("o_browser");
	
		panel.add(new JLabel("Output Directory"),c);c.gridx++;
		panel.add(outputLocation,c);c.gridx++;
		panel.add(browse,c);c.gridx=0;c.gridy++;
		
		
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
		run = new JButton("Import Terminology");
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
		
		
		frame.setContentPane(p);
		frame.pack();
		frame.setVisible(true);
		
		Dimension d =Toolkit.getDefaultToolkit().getScreenSize();
		Dimension s = frame.getSize();
		frame.setLocation(new Point((d.width-s.width)/2,(d.height-s.height)/2));
	}
	
	public JFrame getFrame(){
		return frame;
	}
	
	
	private void doBrowse(JTextField text){
		// if input location and BioPortal selected
		if(text == inputLocation && inputFormats.getSelectedIndex() == 3){
			final JTextField textField = text;
			(new Thread(new Runnable(){
				public void run(){
					if(bioportalDialog == null){
						importer = new OntologyImporter(new BioPortalRepository());
						bioportalDialog = importer.showImportWizard(frame);
					}else{
						bioportalDialog.setVisible(true);
					}
					while(bioportalDialog.isShowing()){
						try {
							Thread.sleep(500);
						} catch (InterruptedException e) {
							e.printStackTrace();
						}
					}
					if(importer.isSelected()){
						// check format
						IOntology ont = importer.getSelectedOntology();
						if("OWL".equalsIgnoreCase(ont.getFormat())){
							textField.setText(""+ont.getURI());
							// guess output
							if(textField.equals(inputLocation)){
								String name = OntologyUtils.toResourceName(ont.getName());
								outputLocation.setText(new File(NobleCoderTerminology.getPersistenceDirectory(),name+NobleCoderTerminology.TERM_SUFFIX).getAbsolutePath());
							}
						}else{
							String message = "BioPortal import of "+ont.getName()+" failed as "+ont.getFormat()+" format is currently not supported." ;
							JOptionPane.showMessageDialog(frame, message,"Error",JOptionPane.ERROR_MESSAGE);
						}
					}
					
				}
			})).start();
		}else{
			JFileChooser fc = new JFileChooser(file);
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			fc.setMultiSelectionEnabled(true);
			int r = (outputLocation == text)?fc.showSaveDialog(frame):fc.showOpenDialog(frame);
			if(r == JFileChooser.APPROVE_OPTION){
				file = fc.getSelectedFile();
				text.setText(file.getAbsolutePath());
				// guess output
				if(text.equals(inputLocation)){
					outputLocation.setText(new File(NobleCoderTerminology.getPersistenceDirectory(),
					StringUtils.stripSuffix(file.getName())).getAbsolutePath()+NobleCoderTerminology.TERM_SUFFIX);
				}
				
			}
		}
	}
	
	private void doRun(){
		(new Thread(new Runnable(){
			public void run() {
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						buttonPanel.removeAll();
						buttonPanel.add(progress,BorderLayout.CENTER);
						buttonPanel.validate();
						buttonPanel.repaint();
						console.setText("");
					}
				});
				
				// create a new process
				String classpath = System.getProperty("java.class.path");
			    String path = System.getProperty("java.home")+File.separator+"bin"+File.separator+"java";
			    
			    // check memmory parameter
			    String mem = memSize.getText();
			    
			    // build parameters
			    List<String> args = new ArrayList<String>();
			    args.add(path);
			    if(mem.length() > 0)
			    	args.add("-Xmx"+mem);
			    args.add("-cp");
			    args.add(classpath);
			    args.add(TerminologyImporter.class.getName());
			    args.add(FORMAT_ARGS[inputFormats.getSelectedIndex()]);
			    args.add(inputLocation.getText());
			    
			    if(outputLocation.getText().length() > 0){
			    	args.add("-output");
			    	args.add(outputLocation.getText());
			    }
			    
			    if(sourceList != null && sourceList.getText().length() > 0){
			    	args.add("-sources");
			    	args.add(sourceList.getText());
			    }
			    
			    if(hierarchySourceList != null && hierarchySourceList.getText().length() > 0){
			    	args.add("-hierarchySources");
			    	args.add(hierarchySourceList.getText());
			    }
			    
			    if(semanticTypeList != null && semanticTypeList.getText().length() > 0){
			    	args.add("-semanticTypes");
			    	args.add(semanticTypeList.getText());
			    }
			    
			    if(languageList != null && languageList.getText().length() > 0){
			    	args.add("-languages");
			    	args.add(languageList.getText());
			    }
			    
			    if(useStemmer == null || useStemmer.isSelected())
			    	args.add("-stemWords");
			  
			    if(stripDigits != null && stripDigits.isSelected())
			    	args.add("-stripDigits");
			    
			    if(createAncestors != null && createAncestors.isSelected())
			    	args.add("-createAncestry");
			    
			    //if(truncateURI != null && truncateURI.isSelected())
			    //	args.add("-abbreviateURIcodes");
			    
			    /*if(inMemory != null && inMemory.isSelected())
			    	args.add("-inMem");*/
			   
			    if(useMetaInfo != null && useMetaInfo.isSelected()){
			    	args.add("-useMeta");
			    	args.add(""+metathesaurusList.getSelectedItem());
			    }
			    	
			    
			    if(compact != null && compact.isSelected())
			    	args.add("-compact");
			    
			    
			    if(maxWordsInTermCheck != null && maxWordsInTermCheck.isSelected())
			    	args.add("-maxWordsInTerm "+maxWordsInTerm.getText());
			    
			    
			    if(suppressObsoleteTerms != null && suppressObsoleteTerms.isSelected())
			    	args.add("-suppressObsoleteTerms");
			    
			    // execute import
			    execute(args);
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						buttonPanel.removeAll();
						buttonPanel.add(run,BorderLayout.CENTER);
						buttonPanel.validate();
						buttonPanel.repaint();
					}
				});
				
			}
		})).start();
	}
	
	
	private void execute(List<String> args){
		try {
				load(args.toArray(new String [0]));
			} catch (Exception e1) {
				e1.printStackTrace();
			}
		 
		/*ProcessBuilder processBuilder = new ProcessBuilder(args.toArray(new String [0]));
	    processBuilder.redirectErrorStream(true);
	    Process process;
		try {
			int r = 0;
			do{
				process = processBuilder.start();
				StreamGobbler sg = new StreamGobbler(process.getInputStream());
				sg.start();
				process.waitFor();
				r = process.exitValue();
			}while(r != 0);
		} catch (Exception e) {
			e.printStackTrace();
		}*/
	}
	
	private class StreamGobbler extends Thread {
	    private InputStream is;
	    public StreamGobbler(InputStream is){
	        this.is = is;
	    }
	    
	    public void run(){
	        try {
	            InputStreamReader isr = new InputStreamReader(is);
	            BufferedReader br = new BufferedReader(isr);
	            String line=null;
	            while ((line = br.readLine()) != null){
	            	console.append(line+"\n");
	            }
            } catch (IOException ioe){
                ioe.printStackTrace();  
            }
	    }
	}
	
	
	private JPanel getCommonOptions(){
		if(commonOptions == null){
			commonOptions = new JPanel();
			commonOptions.setLayout(new BoxLayout(commonOptions, BoxLayout.Y_AXIS));
			commonOptions.setBorder(new TitledBorder("Common Options"));
			
			useStemmer = new JCheckBox("Stem words with a Porter Stemmer when saving term information",true);
			useStemmer.setToolTipText("Stemming words will help with handling word inflections and will increase term recall at the expense of precision.");
			stripDigits = new JCheckBox("Strip digits when saving term information",false);
			stripDigits.setToolTipText("Don't store digits that are part of a term. Don't use this option, if there could be an important numeric information that is part of a term.");
			createAncestors = new JCheckBox("Create ancestry cache for terminology",false);
			createAncestors.setToolTipText("Pre-built ancestry cache for a terminology to spead up ancestry resolution access later");
			maxWordsInTerm = new JTextField("10",3);
			maxWordsInTerm.setHorizontalAlignment(JTextField.CENTER);
			maxWordsInTerm.setToolTipText("Number of irrelevant words that can occur betwen adjacent words in a term");
			
			maxWordsInTermCheck = new JCheckBox("Limit the size of a term to that number of words",true);
			maxWordsInTermCheck.setToolTipText("Exclude terms that are so large and so specific that they are no longer useful.");
			maxWordsInTerm.setEditable(maxWordsInTermCheck.isSelected());
			maxWordsInTermCheck.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					maxWordsInTerm.setEditable(maxWordsInTermCheck.isSelected());
				}
			});			
			JPanel p = new JPanel();
			p.setLayout(new FlowLayout(FlowLayout.LEFT));
			p.setBorder(new EmptyBorder(-2, -5, -2,0));
			p.setAlignmentX(JPanel.LEFT_ALIGNMENT);
			p.add(maxWordsInTermCheck);
			p.add(maxWordsInTerm);
			
			//inMemory = new JCheckBox("Create in-memory terminology instead of file cache",false);
			compact = new JCheckBox("Compact terminology after import to improve performance.",true);
			compact.setToolTipText("<html>Compacting terminology improves lookup performance, but adds extra time during loading.<br>"
					+ "Compacting a terminology is not recommended if its content is not static and will change over time.");
			
			suppressObsoleteTerms = new JCheckBox("Suprress obsolete terms",true);
			suppressObsoleteTerms.setToolTipText("Do not include terms that are marked 'Obsolete' in a dictionary");
			
			commonOptions.add(useStemmer);
			commonOptions.add(stripDigits);
			commonOptions.add(createAncestors);
			commonOptions.add(p);
			commonOptions.add(compact);
			//commonOptions.add(inMemory);
			commonOptions.add(suppressObsoleteTerms);
			
		}
		return commonOptions;
	}
	
	private JPanel getOWLDialog(){
		//if(owlOptions == null){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		/*truncateURI = new JCheckBox("Abbreviate class URIs as a concept code");
		truncateURI.setToolTipText("<html>If <b>code</b> property is not defined class URI is typically used as concept code.<br>"
				+ "Ex: http://www.ontologies.com/ontologies/MyTestOntology.owl#Class_Name -> MTO:Class_Name, if abbreviation is used.");
		panel.add(truncateURI);*/
		panel.add(getCommonOptions());
		return panel;
	}
	
	private JPanel getBioportalDialog(){
		//if(owlOptions == null){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		JTextArea description = new JTextArea(4,20);
		description.setBorder(new EmptyBorder(10, 10, 10, 10));
		description.setText(
				"Import an ontology from BioPortal repository that is available in OWL format.");
		description.setEditable(false);
		description.setWrapStyleWord(true);
		description.setLineWrap(true);
		description.setBackground(new Color(255,255,200));
		description.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		panel.add(description);
		panel.add(getCommonOptions());
		return panel;
	}
	
	private JPanel getTxtDialog(){
		//if(owlOptions == null){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		
		JTextArea description = new JTextArea(6,20);
		description.setBorder(new EmptyBorder(10, 10, 10, 10));
		description.setText(
				"Import a text file where each line is a semi-column delimited list of terms for each concept. "+
				"Each line can be indented with tabs to indicate a hierarchy of concepts. "+
				"If a term looks like a UMLS CUI or a semantic type TUI, then this information is added appropriately. "+
				"Optionally additional concept meta information can be pulled from another terminlogy s.a. UMLS if it is a direct match.");
		description.setEditable(false);
		description.setWrapStyleWord(true);
		description.setLineWrap(true);
		description.setBackground(new Color(255,255,200));
		description.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		
		useMetaInfo  = new JCheckBox("Pull additional concept information from another terminology",false);
		useMetaInfo.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		
		DefaultRepository repo = new DefaultRepository();
		Terminology [] terms = repo.getTerminologies();
		metathesaurusList = new JComboBox<Terminology>(terms);
		metathesaurusList.setEnabled(false);
		metathesaurusList.setMaximumSize(useMetaInfo.getPreferredSize());
		metathesaurusList.setAlignmentX(JComponent.LEFT_ALIGNMENT);
		for(Terminology t: terms){
			if(t.getName().contains("UMLS") || t.getName().contains("Metathesaurus")){
				metathesaurusList.setSelectedItem(t);
				break;
			}
		}
		
		useMetaInfo.setOpaque(false);
		useMetaInfo.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				metathesaurusList.setEnabled(useMetaInfo.isSelected());
			}
		});
		panel.add(description);
		panel.add(useMetaInfo);
		panel.add(metathesaurusList);
		panel.add(Box.createRigidArea(new Dimension(20,20)));
		panel.add(getCommonOptions());
		
		return panel;
	}
	
	
	
	private JPanel getRRFDialog(){
		//if(rrfOptions == null){
		JPanel panel = new JPanel();
		panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
		GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0);
		GridBagLayout l = new GridBagLayout();
		l.setConstraints(panel,c);
		panel.setLayout(l);
		
		sourceList = new JTextField(30);
		JButton browse = new JButton("Browse");
		browse.addActionListener(this);
		browse.setActionCommand("src_browser");
			
		panel.add(new JLabel("Filter by Source"),c);c.gridx++;
		panel.add(sourceList,c);c.gridx++;
		panel.add(browse,c);c.gridx=0;c.gridy++;
		
		hierarchySourceList = new JTextField(30);
		browse = new JButton("Browse");
		browse.addActionListener(this);
		browse.setActionCommand("src_browser2");
		
		panel.add(new JLabel("Filter Hierarchy by Source"),c);c.gridx++;
		panel.add(hierarchySourceList,c);c.gridx++;
		panel.add(browse,c);c.gridx=0;c.gridy++;
		
		
		semanticTypeList = new JTextField(30);
		browse = new JButton("Browse");
		browse.addActionListener(this);
		browse.setActionCommand("st_browser");
	
		panel.add(new JLabel("Filter by Semantic Type"),c);c.gridx++;
		panel.add(semanticTypeList,c);c.gridx++;
		panel.add(browse,c);c.gridx=0;c.gridy++;
		
		languageList = new JTextField("ENG",30);
		panel.add(new JLabel("Filter by Language"),c);c.gridx++;
		panel.add(languageList,c);c.gridy++;c.gridx=0;
		c.gridwidth=3;
		c.gridheight=1;
		panel.add(getCommonOptions(),c);
		return panel;
	}
	
	
	
	
	private void doOptions(){
		switch(inputFormats.getSelectedIndex()){
		case 0: JOptionPane.showMessageDialog(frame,getRRFDialog(),"RRF Options",JOptionPane.PLAIN_MESSAGE);;break;
		case 1: JOptionPane.showMessageDialog(frame,getOWLDialog(),"OWL Options",JOptionPane.PLAIN_MESSAGE); break;
		case 3: JOptionPane.showMessageDialog(frame,getBioportalDialog(),"TXT Options",JOptionPane.PLAIN_MESSAGE); break;
		case 4: JOptionPane.showMessageDialog(frame,getTxtDialog(),"TXT Options",JOptionPane.PLAIN_MESSAGE); break;
		default:  JOptionPane.showMessageDialog(frame,getCommonOptions(),"Common Options",JOptionPane.PLAIN_MESSAGE); break;
		}
	}
	
	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("run".equals(cmd)){
			doRun();
		}else if("i_browser".equals(cmd)){
			doBrowse(inputLocation);
		}else if("o_browser".equals(cmd)){
			doBrowse(outputLocation);
		}else if("src_browser".equals(cmd)){
			doBrowse(sourceList);
		}else if("src_browser2".equals(cmd)){
			doBrowse(hierarchySourceList);
		}else if("st_browser".equals(cmd)){
			doBrowse(semanticTypeList);
		}else if("options".equals(cmd)){
			doOptions();
		}	
	}

	public void itemStateChanged(ItemEvent e) {
		if(e.getStateChange() == ItemEvent.SELECTED){
			//options.setEnabled(false);
			switch(inputFormats.getSelectedIndex()){
			case 0: inputLabel.setText("Input RRF Directory");break;
			case 1: inputLabel.setText("Input OWL File"); break;
			case 2: inputLabel.setText("Input OBO File");break;
			case 3: inputLabel.setText("Input BioPortal URL");break;
			case 4: inputLabel.setText("Input Text File");break;
		
			}
		}
		
	}
	
	public void propertyChange(PropertyChangeEvent e) {
		final String prop = e.getPropertyName();
		final String val  = ""+ e.getNewValue();
		
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				if(ConceptImporter.LOADING_TOTAL.equals(prop)){
					progress.setIndeterminate(false);
					progress.setMaximum(Integer.parseInt(val));
				}else if(ConceptImporter.LOADING_PROGRESS.equals(prop)){
					progress.setIndeterminate(false);
					progress.setValue(Integer.parseInt(val));
				}else if(ConceptImporter.LOADING_MESSAGE.equals(prop)){
					progress.setIndeterminate(true);
					progress.setString(val);
					log(val);
				}else{
					log(val);
				}
			}
		});
		
	}
	
	/**
	 * get option from parameter list, null if not passed
	 * @param params
	 * @param option
	 * @return
	 */
	private String getOption(List<String> params, String option){
		int i = params.indexOf(option);
		if(i > -1 && i+1 < params.size()){
			return params.get(i+1);
		}
		return null;
	}
	
	/**
	 * actually load
	 */
	public void load(String [] args) throws Exception{
		List<String> params = Arrays.asList(args);
		String output = getOption(params,"-output");
		String rrf = getOption(params,"-rrf");
		String owl = getOption(params,"-owl");
		String obo = getOption(params,"-obo");
		String bioportal = getOption(params,"-bioportal");
		String txt = getOption(params,"-txt");
		String lng = getOption(params,"-languages");
		String sr = getOption(params,"-sources");
		String hsr = getOption(params,"-hierarchySources");
		String se = getOption(params,"-semanticTypes");
		String mwit = getOption(params,"-maxWordsInTerm");
		String meta = getOption(params,"-useMeta");
		boolean stemWords = params.contains("-stemWords");
		boolean stripDigits = params.contains("-stripDigits");
		boolean createAncestry = params.contains("-createAncestry");
		//boolean abbreviateURI = params.contains("-abbreviateURIcodes");
		boolean compact = params.contains("-compact");
		//boolean inmem = params.contains("-inMem");
		boolean hmwit = params.contains("-maxWordsInTerm");
		boolean suppressObsoleteTerms = params.contains("-suppressObsoleteTerms");
		
		// remove previous listener
		ConceptImporter.getInstance().removePropertyChangeListener(this);
		ConceptImporter.getInstance().addPropertyChangeListener(this);
		
		// start index finder terminology
		NobleCoderTerminology terminology = new NobleCoderTerminology();
		terminology.setStemWords(stemWords);
		terminology.setIgnoreDigits(stripDigits);
		if(hmwit)
			terminology.setMaximumWordsInTerm(Integer.parseInt(mwit));
		
		// setup compact option
		if(compact)
			ConceptImporter.getInstance().setCompact(compact);
		ConceptImporter.getInstance().setInMemory(false);
		
	
		// normalize meta info
		Terminology metaTerm = null;
		if(meta != null){
			metaTerm = new NobleCoderTerminology(meta);
		}
		
		
		// setup persistance directory
		String name = null;
		if(output != null){
			File f = new File(output);
			if(f.getParentFile().exists()){
				if(f.isDirectory()){
					NobleCoderTerminology.setPersistenceDirectory(f);
				}else{
					NobleCoderTerminology.setPersistenceDirectory(f.getParentFile());
					name = f.getName();
				}
			}
		}
					
		
		
		// load approprate ontology		
		if(rrf != null){
			File f = new File(rrf);
			List<String> lang = (lng != null)?readList(lng):null;
			List<String> src =  (sr  != null)?readList(sr):null;
			List<String> hsrc =  (hsr  != null)?readList(hsr):null;
			List<String> sem =  (se  != null)?readList(se):null;
			
			// load ontology
			log("Loading RRF terminology from "+f.getAbsolutePath()+"...");
			log("Languages: "+lang);
			log("Sources: "+src);
			log("Hierarchy Sources: "+hsrc);
			log("SemanticTypes: "+sem);
			
			Map<String,List<String>> pmap = new HashMap<String,List<String>>();
			pmap.put("name",Arrays.asList(name));
			pmap.put("languages",lang);
			pmap.put("sources",src);
			pmap.put("semanticTypes",sem);
			pmap.put("hierarchySources",hsrc);
			pmap.put("suppressObsoleteTerms",Arrays.asList(""+suppressObsoleteTerms));
			
			// load the terminology
			ConceptImporter.getInstance().loadRRF(terminology,f,pmap);
			name = (name == null)?f.getName():name;
		}else if(owl != null){
			log("Loading OWL terminology from "+owl+"...");
			IOntology ont = OOntology.loadOntology(owl);
			if(ont != null){
				ConceptImporter.getInstance().loadOntology(terminology,ont,name);
			}
			name = (name == null)?ont.getName():name;
		}else if(obo != null){
			log("Loading OBO terminology from "+obo+"...");
			List<File> files = new ArrayList<File>();
			for(String f: obo.split(","))
				files.add(new File(f.trim()));
			ConceptImporter.getInstance().loadOBO(terminology,files,name);	
			name = (name == null)?files.get(0).getName():name;
		}else if(bioportal != null){
			log("Loading BioPortal terminology from "+bioportal+"...");
			BioPortalRepository r = new BioPortalRepository();
			IOntology ont = r.getOntology(URI.create(bioportal));
			if(ont != null){
				ont.addPropertyChangeListener(this);
				if(ont instanceof BOntology){
					String format = ont.getFormat();
					if("OWL".equalsIgnoreCase(format)){
						ont = OOntology.loadOntology(((BOntology) ont).getDownloadURL());
					}else{
						String message = "BioPortal import of "+ont.getName()+" failed as "+ont.getFormat()+" format is currently not supported." ;
						JOptionPane.showMessageDialog(frame, message,"Error",JOptionPane.ERROR_MESSAGE);
						ont = null;
					}
				}
				if(ont != null)
					ConceptImporter.getInstance().loadOntology(terminology,ont,name);
			}
			name = (name == null)?ont.getName():name;
		}else if(txt != null){
			log("Loading Text terminology from "+txt+"...");
			File f = new File(txt);
			ConceptImporter.getInstance().loadText(terminology,f,name,metaTerm);
			name = (name == null)?f.getName():name;
		}
		
		//print info
		log("Testing Terminology "+name+" ...");
		
		// reopen it
		printInfo(terminology);
		
		// doing ancestry index
		if(createAncestry){
			log("Creating Ancestry Cache for "+name+" ..,");
			PathHelper ph = new PathHelper(terminology);
			ph.createAncestryCache();
		}
		
		
		log("\n\nAll Done!");
		terminology.dispose();
		terminology = null;
		System.gc();
		pcs.firePropertyChange("LOADING",null,"done");
		ConceptImporter.getInstance().removePropertyChangeListener(this);
	}
	
	
	private void log(Object obj){
		System.out.println(obj);
		if(console != null){
			console.append(obj+"\n");
		}
	}
	
	
	public void printInfo(Terminology terminology) throws Exception {
		log("\n[INFO]");
		log("name:\t\t"+terminology.getName());
		log("version:\t"+terminology.getVersion());
		log("description:\t"+terminology.getDescription());
		log("location:\t"+terminology.getLocation());
		log("uri:\t\t"+terminology.getURI());
		log("languages:\t"+Arrays.toString(terminology.getLanguages()));
		log("relations:\t"+Arrays.toString(terminology.getRelations()));
		log("sources:\t"+Arrays.toString(terminology.getSources()));
		System.out.print("roots:\t");
		for(Concept c: terminology.getRootConcepts()){
			System.out.print(c.getName()+", "); 
		}
		log("\n");
	}
	
	
	private List<String> readList(String text) throws Exception {
		List<String> list = null;
		File file = new File(text);
		if(file.exists()){
			list = new ArrayList<String>();
			BufferedReader reader = new BufferedReader(new FileReader(file));
			for(String line=reader.readLine();line != null;line = reader.readLine()){
				line = line.trim();
				if(line.length() > 0)
					list.add(line);
			}
			reader.close();
		}else{
			list = Arrays.asList(text.split("[,; ]"));
		}
		return list;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args){
		//args = new String [] {"-bioportal","http://bioportal.bioontology.org/ontologies/African_Traditional_Medicine"};
		//args = new String [] {"-rrf","/home/tseytlin/Data/Terminologies/NCI_Metathesaurus-201203D","-output","/home/tseytlin/Test"};
		
		TerminologyImporter loader = new TerminologyImporter();
		if(args.length == 0){
			loader.showDialog();
		}else{
			try{
				loader.load(args);
			}catch(Exception ex){
				ex.printStackTrace();
			}
		}
	}

	
}
