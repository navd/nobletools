package edu.pitt.dbmi.nlp.noble.ui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;

import javax.swing.DefaultComboBoxModel;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.BevelBorder;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import edu.pitt.dbmi.nlp.noble.ontology.DefaultRepository;
import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IOntologyException;
import edu.pitt.dbmi.nlp.noble.ontology.IRepository;
import edu.pitt.dbmi.nlp.noble.ontology.bioportal.BioPortalRepository;
import edu.pitt.dbmi.nlp.noble.terminology.Describable;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.terminology.impl.NobleCoderTerminology;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.ui.widgets.ResourceCellRenderer;
import edu.pitt.dbmi.nlp.noble.util.ConceptImporter;
import edu.pitt.dbmi.nlp.noble.util.FileTools;

public class RepositoryManager implements ActionListener, ListSelectionListener, PropertyChangeListener{
	
	private JFrame frame;
	private IRepository repository;
	private JList ontologies, terminologies;
	private JLabel status;
	private JProgressBar progress;
	private JComponent toolbar;
	private JTextField repositoryPath;
	private boolean selectionSwitch;
	private TerminologyImporter loader;
	private TerminologyExporter exporter; 
	// bioportal import
	//private BioPortalOntologyImporter bioportal;
	private static boolean STAND_ALONE;
	public static final int ONTOLOGIES_ONLY = 1;
	public static final int TERMINOLOGIES_ONLY = 2;
	
	
	/**
	 * create GUI manager
	 */
	public RepositoryManager(){
		frame = createGUI(TERMINOLOGIES_ONLY);
	}
	
	public RepositoryManager(int mode){
		frame = createGUI(mode);
	}
	
	/**
	 * create GUI component
	 * @return
	 */
	private JFrame createGUI(int mode){
		JFrame f = new JFrame("Terminology Manager");
		if(STAND_ALONE)
			f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		f.getContentPane().setLayout(new BorderLayout());
		
		
		
		
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		p.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.LOWERED),new EmptyBorder(10, 5, 10, 5)));
		repositoryPath=  new JTextField(20);
		repositoryPath.setText(NobleCoderTerminology.getPersistenceDirectory().getAbsolutePath());
		p.add(new JLabel("Repository "),BorderLayout.WEST);
		p.add(repositoryPath,BorderLayout.CENTER);
		JButton browse = new JButton("Browse");
		browse.addActionListener(this);
		browse.setActionCommand("PATH");
		p.add(browse,BorderLayout.EAST);
		
		
		ontologies = new JList();
		ontologies.setCellRenderer(new ResourceCellRenderer());
		terminologies = new JList();
		terminologies.setCellRenderer(new ResourceCellRenderer());
		ontologies.addListSelectionListener(this);
		terminologies.addListSelectionListener(this);
		JScrollPane s1 = new JScrollPane(ontologies);
		JScrollPane s2 = new JScrollPane(terminologies);
		s1.setPreferredSize(new Dimension(300,200));
		s2.setPreferredSize(new Dimension(300,200));
		s1.setBorder(new TitledBorder("Ontologies"));
		s2.setBorder(new TitledBorder("Terminologies"));
		JSplitPane split = new JSplitPane(JSplitPane.VERTICAL_SPLIT);
		split.setTopComponent(s1);
		split.setBottomComponent(s2);
		split.setResizeWeight(.5);
		status = new JLabel(" ");
		progress = new JProgressBar();
		progress.setString("Please Wait ...");
		progress.setStringPainted(true);
		progress.setIndeterminate(true);
		toolbar = createToolBar();
		JComponent c = split;
		if(ONTOLOGIES_ONLY == mode)
			c = s1;
		else if(TERMINOLOGIES_ONLY == mode)
			c = s2;
		
		f.getContentPane().add(p,BorderLayout.NORTH);
		f.getContentPane().add(c,BorderLayout.CENTER);
		f.getContentPane().add(toolbar,BorderLayout.EAST);
		f.getContentPane().add(status,BorderLayout.SOUTH);
		f.pack();
		return f;
	}
	
	/**
	 * display busy
	 * @param b
	 */
	public void setBusy(boolean busy){
		if(busy){
			frame.getContentPane().remove(status);
			frame.getContentPane().add(progress,BorderLayout.SOUTH);
		}else{
			frame.getContentPane().remove(progress);
			frame.getContentPane().add(status,BorderLayout.SOUTH);
		}
		frame.getContentPane().validate();
		frame.getContentPane().repaint();
	}
	
	/**
	 * start application
	 * @param config
	 *
	public void start(String config){
		try{
			start(new ProtegeRepository(config));
		}catch(IOntologyException ex){
			ex.printStackTrace();
		}
	}
	*/
	
	/**
	 * start application
	 * @param config
	 */
	public void start(IRepository r){
		frame.setVisible(true);
		repository = r;
		repository.addPropertyChangeListener(this);
		setBusy(true);
		
		(new Thread(new Runnable(){
			public void run() {
				final IOntology [] ont = repository.getOntologies();
				final Terminology [] term = repository.getTerminologies();
				Arrays.sort(ont,new Comparator<IOntology>(){
					public int compare(IOntology o1, IOntology o2) {
						if(o1 instanceof IOntology && o2 instanceof IOntology)
							return ((IOntology)o1).getName().compareTo(((IOntology)o2).getName());
						return o1.toString().compareTo(o2.toString());
					}
					
				});
				Arrays.sort(term,new Comparator<Terminology>(){
					public int compare(Terminology o1, Terminology o2) {
						return o1.getName().compareTo(o2.getName());
					}
				});
				
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						ontologies.setListData(ont);
						terminologies.setListData(term);
						setBusy(false);
					}
				});
				
			}
		})).start();
		
	}
	
	
	/**
	 * handle reloads
	 */
	public void propertyChange(PropertyChangeEvent evt){
		if(OntologyImporter.PROPERTY_PROGRESS_MSG.equals(evt.getPropertyName())){
			progress.setString(""+evt.getNewValue());
		}else{
			ontologies.setListData(repository.getOntologies());
			terminologies.setListData(repository.getTerminologies());
			ontologies.revalidate();
			terminologies.revalidate();
		}
	}
	
	/**
	 * create button
	 * @param text
	 * @return
	 */
	private JButton createButton(String text, String icon, String tip){
		JButton bt = new JButton(text);
		bt.setToolTipText(tip);
		bt.addActionListener(this);
		bt.setActionCommand(text);
		bt.setHorizontalAlignment(SwingConstants.LEFT);
		bt.setMaximumSize(new Dimension(175,30));
		if(icon != null)
			bt.setIcon(new ImageIcon(getClass().getResource(icon)));
		return bt;
	}
	
	/**
	 * create toolbar with buttons
	 * @return
	 */
	private JComponent createToolBar(){
		JToolBar toolbar = new JToolBar(JToolBar.VERTICAL);
		//toolbar.add(createButton("PORTAL","/icons/WebComponentAdd24.gif"));
		toolbar.add(createButton("IMPORT","/icons/Import24.gif","Import terminlogies from RRF/OWL/OBO into NobleCoder repository"));
		toolbar.add(createButton("EXPORT","/icons/Export24.gif","Export parts of terminologies from NobleCoder repository into an OWL file"));
		toolbar.addSeparator();
		toolbar.add(createButton("ADD","/icons/New24.gif","Add new terminology file (.term.zip) to repository."));
		toolbar.add(createButton("DELETE","/icons/Delete24.gif","Delete terminology from repository and disk. "));
		toolbar.addSeparator();
		toolbar.add(createButton("COMPACT","/icons/Compact24.png","Compact existing terminology to imporove terminology lookup speed."));
		toolbar.addSeparator();
		toolbar.add(createButton("EXPLORE","/icons/Search24.gif","Explore terminology content."));
		//toolbar.add(createButton("OPTIONS","/icons/Preferences24.gif","Set terminology lookup options."));


		return toolbar;
	}
	
	
	/**
	 * get selected value
	 * @return
	 */
	private Describable getSelectedValue(){
		Describable d = (Describable) ontologies.getSelectedValue();
		if(d == null)
			d = (Describable) terminologies.getSelectedValue();
		return d;
	}
	
	
	/**
	 * actions on buttons
	 * @param e
	 */
	public void actionPerformed(ActionEvent e){
		String cmd = e.getActionCommand();
		if(cmd.equalsIgnoreCase("ADD")){
			doAdd();
		}else if(cmd.equalsIgnoreCase("PORTAL")){
			doPortal();
		}else if(cmd.equalsIgnoreCase("IMPORT")){
			doImport();
		}else if(cmd.equalsIgnoreCase("EXPORT")){
			doExport();
		}else if(cmd.equalsIgnoreCase("INFO")){
			doInfo();
		}else if(cmd.equalsIgnoreCase("EXPLORE")){
			doExplore();
		}else if(cmd.equalsIgnoreCase("QUERY")){
			doQuery();
		}else if(cmd.equalsIgnoreCase("DELETE")){
			doRemove();
		}else if(cmd.equalsIgnoreCase("COMPACT")){
			doCompact();
		}else if(cmd.equalsIgnoreCase("PATH")){
			doPath();
		}
	}

	private void doPath(){
		JFileChooser fc = new JFileChooser(repositoryPath.getText());
		fc.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
		int r = fc.showOpenDialog(frame);
		if(r == JFileChooser.APPROVE_OPTION){
			File file = fc.getSelectedFile();
			repositoryPath.setText(file.getAbsolutePath());
			NobleCoderTerminology.setPersistenceDirectory(file);
			refreshTerminologies();
		}
	}
	
	
	private void doAdd(){
		JFileChooser fc = new JFileChooser();
		fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
		fc.setFileFilter(new FileFilter() {
			public String getDescription() {
				return "Terminology file or directory (.term or .term.zip)";
			}
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".term.zip");
			}
		});
		int r = fc.showOpenDialog(frame);
		if(r == JFileChooser.APPROVE_OPTION){
			final File file = fc.getSelectedFile();
			new Thread(new Runnable(){
				public void run(){
					setBusy(true);
					File location = NobleCoderTerminology.getPersistenceDirectory();
					if(file.isDirectory() && file.getName().endsWith(".term")){
						file.renameTo(new File(location+File.separator+file.getName()));
					}else if(file.isFile() && file.getName().endsWith(".zip")){
						String name = file.getName();
						name = name.substring(0,name.length()-".zip".length());
						FileInputStream is;
						try {
							is = new FileInputStream(file);
							FileTools.unzip(is, new File(location+File.separator+name));
							is.close();
						} catch (Exception e) {
							JOptionPane.showMessageDialog(frame,"Error unzipping the terminology: "+e.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
							e.printStackTrace();
						}
						
					}
					setBusy(false);
					refreshTerminologies();
				}
			}).start();
		}
	}
	
	
	/**
	 * remove ontology
	 */
	private void doRemove(){
		if(terminologies.getSelectedValues().length == 0){
			JOptionPane.showMessageDialog(frame,"No terminology selected","Nothing selected",JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		
		
		int r = JOptionPane.showConfirmDialog(frame,"Are you sure you want to delete selected terminology from disk?","Confirm",JOptionPane.YES_NO_OPTION);
		// if not canceled, remove entry
		if(r == JOptionPane.YES_OPTION){
			setBusy(true);
			for(Object o : terminologies.getSelectedValues()){
				
				Terminology term = (Terminology) o;
				// remove entry
				repository.removeTerminology(term);
				// remove data
				if(term instanceof NobleCoderTerminology){
					((NobleCoderTerminology)term).dispose();
					FileTools.deleteDirectory(new File(term.getLocation()));
				}
			}
		}
		setBusy(false);
		refreshTerminologies();
	}
	
	
	private void refreshTerminologies(){
		repository = new DefaultRepository();
		((DefaultRepository)repository).setTerminologyLocation(NobleCoderTerminology.getPersistenceDirectory());
		start(repository);
	}
	
	/**
	 * remove ontology
	 */
	private void doCompact(){
		final Terminology term = (Terminology) terminologies.getSelectedValue();
		
		// check if nothing selected
		if(term == null){
			JOptionPane.showMessageDialog(frame,"No terminology selected","Nothing selected",JOptionPane.INFORMATION_MESSAGE);
			return;
		}
		
		if(!(term instanceof NobleCoderTerminology)){
			JOptionPane.showMessageDialog(frame,"Selected terminology cannot be compacted","Error",JOptionPane.ERROR_MESSAGE);
			return;
		}
	
		if(((NobleCoderTerminology)term).isCompacted()){
			JOptionPane.showMessageDialog(frame,"Selected terminology appeard to be compacted already.","Nothing to do",JOptionPane.INFORMATION_MESSAGE);
			return;
		}
	
		
		int r = JOptionPane.showConfirmDialog(frame,"Are you sure you want to compact selected terminology? This may take a while.","Confirm",JOptionPane.YES_NO_OPTION);
		// if not canceled, remove entry
		if(r == JOptionPane.YES_OPTION){
			new Thread(new Runnable(){
				public void run(){
					setBusy(true);
					final JTextArea text = new JTextArea(10,40);
					
					// keep track of progress
					ConceptImporter.getInstance().addPropertyChangeListener(new PropertyChangeListener() {
						public void propertyChange(PropertyChangeEvent evt) {
							final String msg =  ""+evt.getNewValue();
							SwingUtilities.invokeLater(new Runnable(){
								public void run(){
									text.append(msg+"\n");
								}
							});
							
						}
					});
				
					
					JDialog dialog = new JDialog(frame, "Terminology Compact Progress",false);
					JPanel panel = new JPanel();
					panel.setBorder(new CompoundBorder(new BevelBorder(BevelBorder.RAISED),new EmptyBorder(10,10,10,10)));
					panel.add(new JScrollPane(text));
					dialog.setContentPane(panel);
					dialog.pack();
					dialog.setLocationRelativeTo(frame);
					dialog.setVisible(true);
					
						
					text.append("Compacting "+term.getName() + " ...");
					if(term instanceof NobleCoderTerminology){
						NobleCoderTerminology ncterm = (NobleCoderTerminology) term;
						if(!ncterm.isCompacted()){
							try {
								ConceptImporter.getInstance().compact(ncterm);
							} catch (IOException e) {
								// TODO Auto-generated catch block
								e.printStackTrace();
							}
						}else{
							JOptionPane.showMessageDialog(frame,"Terminology appears to be compacted already.","Nothing to do",JOptionPane.INFORMATION_MESSAGE);
						}
					}
					text.append("\ndone\n");
					
					
					setBusy(false);
				}
			}).start();
		}
	}

	/**
	 * import ontology
	 */
	private void doPortal(){
		
		(new Thread(new Runnable(){
			public void run(){
				try{
					OntologyImporter importer = new OntologyImporter(new BioPortalRepository());
					importer.addPropertyChangeListener(RepositoryManager.this);
					JDialog d = importer.showImportWizard(frame);
					
					
					// wait for selection
					while(d.isShowing()){
						try{
							Thread.sleep(500);
						}catch(Exception ex){}
					}
					//selected
					if(importer.isSelected()){
						setBusy(true);
					
						IOntology source = importer.getSelectedOntology();
						IClass [] scls = importer.getSelectedClasses();
						
						// create new ontology
						IOntology ont = repository.createOntology(source.getURI());
						
						//copy content
						importer.copy(scls,ont.getRoot());
						
						// import ontology
						repository.importOntology(ont);
					}
					importer.removePropertyChangeListener(RepositoryManager.this);
				}catch(Exception ex){
					ex.printStackTrace();
					JOptionPane.showMessageDialog(frame,ex.getMessage(),
								"Error",JOptionPane.ERROR_MESSAGE);
				}
				setBusy(false);
			}
		})).start();

		
	}
	
	/**
	 * import ontology from file
	 *
	private void doImport(){
		JFileChooser chooser = new JFileChooser();
		if(chooser.showOpenDialog(frame) == JFileChooser.APPROVE_OPTION){
			final File f = chooser.getSelectedFile();
			if(f != null && f.canRead()){
				setBusy(true);
				(new Thread(new Runnable(){
					public void run(){
						try{
							repository.importOntology(f.toURI());
						}catch(Exception ex){
							ex.printStackTrace();
							JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
						}
						setBusy(false);
					}
				})).start();
			}
		}
	}*/
	
	
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
		//registerOptions();
		exporter.showExportWizard(frame);
	}
	
	
	/**
	 * export ontology
	 *
	private void doExport(){
		final Object value = getSelectedValue();
		if(value instanceof IOntology){
			JFileChooser chooser = new JFileChooser();
			if(chooser.showSaveDialog(frame) == JFileChooser.APPROVE_OPTION){
				final File f = chooser.getSelectedFile();
				setBusy(true);
				(new Thread(new Runnable(){
					public void run(){
						IOntology ont = (IOntology) value;
						try{
							ont.load();
							ont.write(new FileOutputStream(f),IOntology.OWL_FORMAT);
							JOptionPane.showMessageDialog(frame,ont.getName()+" saved as "+f.getAbsolutePath());
						}catch(Exception ex){
							ex.printStackTrace();
							JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);							
						}
						setBusy(false);
					}
				})).start();
			}
			
		}else
			JOptionPane.showMessageDialog(frame,"Not Implemented!");
	}
	*/
	
	/**
	 * open ontology expolorer
	 *
	 */
	private void doInfo(){
		Describable d = getSelectedValue();
		if(d == null)
			return;
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
	
	private void doExplore() {
		Object value = getSelectedValue();
		if(value != null && value instanceof Terminology){
			final Terminology terminology = (Terminology) value;
			TerminologyBrowser b = new TerminologyBrowser();
			b.setTerminologies(new Terminology [] {terminology});
			b.showDialog(frame, terminology.getName());
		}else{
			JOptionPane.showMessageDialog(frame,"No Terminology Selected","Nothing selected",JOptionPane.INFORMATION_MESSAGE);
		}
	}
	
	/**
	 * open ontology expolorer
	 *
	 */
	private void doBrowser(){
		final OntologyExplorer explorer = new OntologyExplorer();
		JFrame f = new JFrame("Ontology Explorer");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(explorer);
		f.pack();
		f.setVisible(true);
		// set root
		Object value = getSelectedValue();
		if(value instanceof IOntology){
			final IOntology ont = (IOntology) value;
			explorer.setBusy(true);
			(new Thread(new Runnable(){
				public void run(){
					/*
					try{
						ont.load();
					}catch(IOntologyException ex){
						ex.printStackTrace();
					}*/
					explorer.setRoot(ont.getRootClasses());
					explorer.setBusy(false);
				}
			})).start();
		}else if(value instanceof Terminology){
			final Terminology term = (Terminology) value;
			explorer.setBusy(true);
			(new Thread(new Runnable(){
				public void run(){
					try{
						explorer.setRoot(term.getRootConcepts());
					}catch(TerminologyException ex){
						ex.printStackTrace();
					}
					explorer.setBusy(false);
				}
			})).start();
		}
	}
	
	/**
	 * open ontology expolorer
	 *
	 */
	private void doQuery(){
		final QueryTool explorer = new QueryTool();
		JFrame f = new JFrame("Terminology Query");
		f.setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		f.getContentPane().add(explorer);
		f.pack();
		f.setVisible(true);
		// set root
		Object value = getSelectedValue();
		if(value instanceof IOntology){
			final IOntology ont = (IOntology) value;
			explorer.setBusy(true);
			(new Thread(new Runnable(){
				public void run(){
					try{
						ont.load();
					}catch(IOntologyException ex){
						ex.printStackTrace();
					}
					explorer.setOntology(ont);
					explorer.setBusy(false);
				}
			})).start();
		}else if(value instanceof Terminology){
			explorer.setTerminology((Terminology)value);
		}
	}
	
	
	/**
	 * follow lists
	 */
	public void valueChanged(ListSelectionEvent e){
		if(!e.getValueIsAdjusting() && !selectionSwitch){
			selectionSwitch = true;
			if(e.getSource() == ontologies){
				terminologies.clearSelection();
				//setOntologyButtonsEnabled(true);
			}else if(e.getSource() == terminologies){
				ontologies.clearSelection();
				//setOntologyButtonsEnabled(false);
			}
			selectionSwitch = false;
		}
	}
	
	/**
	 * enable/disable ontology buttons
	 * @param b
	 */
	private void setOntologyButtonsEnabled(boolean b){
		if(toolbar != null){
			for(int i=0;i<4;i++){
				toolbar.getComponent(i).setEnabled(b);
			}
		}
	}
	
	/**
	 * @return the frame
	 */
	public JFrame getFrame() {
		return frame;
	}
	

	/**
	 * @return the repository
	 */
	public IRepository getRepository() {
		return repository;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		RepositoryManager manager = new RepositoryManager(TERMINOLOGIES_ONLY);
		manager.getFrame().setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		manager.getFrame().setLocationRelativeTo(null);
		manager.start(new DefaultRepository());
	}

}
