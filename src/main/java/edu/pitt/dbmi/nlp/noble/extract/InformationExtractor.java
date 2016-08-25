package edu.pitt.dbmi.nlp.noble.extract;

import java.awt.BorderLayout;
import java.awt.Desktop;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JEditorPane;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.text.html.HTMLDocument;

import edu.pitt.dbmi.nlp.noble.coder.NobleCoder;
import edu.pitt.dbmi.nlp.noble.coder.model.Processor;
import edu.pitt.dbmi.nlp.noble.coder.processor.DocumentProcessor;
import edu.pitt.dbmi.nlp.noble.extract.model.Template;
import edu.pitt.dbmi.nlp.noble.extract.model.TemplateDocument;
import edu.pitt.dbmi.nlp.noble.extract.model.TemplateFactory;
import edu.pitt.dbmi.nlp.noble.extract.model.TemplateItem;
import edu.pitt.dbmi.nlp.noble.terminology.CompositTerminology;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyError;
import edu.pitt.dbmi.nlp.noble.terminology.TerminologyException;
import edu.pitt.dbmi.nlp.noble.tools.TextTools;
import edu.pitt.dbmi.nlp.noble.util.CSVExporter;
import edu.pitt.dbmi.nlp.noble.util.HTMLExporter;


/**
 * process a set of reports and generate an HTML to get
 * @author tseytlin
 *
 */
public class InformationExtractor implements ActionListener, Processor<TemplateDocument> {
	private JFrame frame;
	private JTextField input,output;
	private JList<Template> templateList;
	private JTextArea console;
	private JProgressBar progress;
	private JPanel buttonPanel;
	private JButton run;
	private File file;
	private long processTime,totalTime;
	private long processCount;
	private Map<Set<Template>,NobleCoder> coders;
	private TemplateFactory templateFactory;
	private HTMLExporter htmlExporter;
	private CSVExporter csvExporter;
	private static boolean statandlone = false;
	
	
	/**
	 * What 
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		statandlone = true;
		InformationExtractor nc = new InformationExtractor();
		if(args.length == 0){
			nc.showDialog();
		}else if(args.length == 3){
			nc.process(Arrays.asList(nc.importTemplates(args[0])),args[1],args[2]);
		}else{
			System.err.println("Usage: java InformationExtractor [template] [input directory] [output directory]");
			System.err.println("Note:  If you don't specify parameters, GUI will pop-up");
		}
	}

	
	/**
	 * int report processor for a given ontology
	 * @param ont
	 */
	public InformationExtractor(){
		templateFactory = TemplateFactory.getInstance();
	}
	

		
	
	/**
	 * create dialog for noble coder
	 */
	public void showDialog(){
		if(frame == null){
			frame = new JFrame("Information Extractor");
			frame.setDefaultCloseOperation(statandlone?JFrame.EXIT_ON_CLOSE:JFrame.DISPOSE_ON_CLOSE);
			frame.setJMenuBar(getMenuBar());
			
			JPanel panel = new JPanel();
			panel.setLayout(new BoxLayout(panel,BoxLayout.Y_AXIS));
			GridBagConstraints c = new GridBagConstraints(0,0,1,1,1,1,GridBagConstraints.CENTER,GridBagConstraints.HORIZONTAL,new Insets(5,5,5,5),0,0);
			GridBagLayout l = new GridBagLayout();
			l.setConstraints(panel,c);
			panel.setLayout(l);
			
			input = new JTextField(30);
			templateList = new JList(new DefaultListModel<Template>());
			JButton browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("i_browser");
		
			
			JButton export = new JButton("Export");
			export.setActionCommand("export");
			export.addActionListener(this);
			JButton add = new JButton("Import");
			add.setActionCommand("import");
			add.addActionListener(this);
			JButton info = new JButton("Preview");
			info.setActionCommand("preview");
			info.addActionListener(this);
			JScrollPane scroll = new JScrollPane(templateList);
			scroll.setPreferredSize(new Dimension(100,100));
			
			panel.add(new JLabel("Input Template(s)"),c);c.gridx++;c.gridheight=3;
			panel.add(scroll,c);c.gridx++;c.gridheight=1;
			panel.add(add,c);c.gridy++;
			panel.add(export,c);c.gridy++;
			panel.add(info,c);c.gridy++;
			c.gridx = 0;
			panel.add(new JLabel("Input Directory "),c);c.gridx++;
			panel.add(input,c);c.gridx++;
			panel.add(browse,c);c.gridx=0;c.gridy++;
	
			output = new JTextField(30);
			browse = new JButton("Browse");
			browse.addActionListener(this);
			browse.setActionCommand("o_browser");
		
			panel.add(new JLabel("Output Directory"),c);c.gridx++;
			panel.add(output,c);c.gridx++;
			panel.add(browse,c);c.gridx=0;c.gridy++;
			panel.add(Box.createRigidArea(new Dimension(10,10)),c);
			
			JPanel conp = new JPanel();
			conp.setLayout(new BorderLayout());
			conp.setBorder(new TitledBorder("Output Console"));
			console = new JTextArea(10,40);
			//console.setLineWrap(true);
			console.setEditable(false);
			conp.add(new JScrollPane(console),BorderLayout.CENTER);
			//c.gridwidth=3;		
			//panel.add(conp,c);c.gridy++;c.gridx=0;
			
			buttonPanel = new JPanel();
			buttonPanel.setLayout(new BorderLayout());
			buttonPanel.setBorder(new EmptyBorder(10,30,10,30));
			run = new JButton("Run Information Extractor");
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
			
				
			// wrap up, and display
			frame.setContentPane(p);
			frame.pack();
		
			//center on screen
			Dimension d = frame.getSize();
			Dimension s = Toolkit.getDefaultToolkit().getScreenSize();
			frame.setLocation(new Point((s.width-d.width)/2,(s.height-d.height)/2));
			
			// load defaults
			loadDeafaults();
		}
		frame.setVisible(true);
	}	
	
	/**
	 * 
	 * @return
	 */
	private void loadDeafaults(){
		(new Thread(new Runnable(){
			public void run(){
				setBusy(true);
				try {
					List<URL> urls = new ArrayList<URL>();
					//urls.add(new URL("http://slidetutor.upmc.edu/curriculum/templates/InvasiveMelanoma.template"));
					//urls.add(new URL("http://slidetutor.upmc.edu/curriculum/templates/GeneMutations.template"));
					//urls.add(new File("/home/tseytlin/PICO.template").toURI().toURL());
					
					for(URL u: urls){
						templateFactory.importTemplate(u.openStream());
					}
					//templateFactory.importTemplates("http://slidetutor.upmc.edu/curriculum/owl/skin/PITT/Melanocytic.owl");
					/*
					File f = new File(NobleCoderTerminology.getPersistenceDirectory(),"GeneMutations.term");
					if(f.exists())
						templateFactory.importTemplates("GeneMutations");
					*/
					
				} catch (Exception e) {
					e.printStackTrace();
				}
				refreshTemplateList();
				//input.setText("/home/tseytlin/Data/Reports/ReportProcessorInput/");
				//output.setText("/home/tseytlin/Data/Reports/Output/ReportProcessorInput/");
				setBusy(false);
			}
		})).start();
	}
	
	private void refreshTemplateList(){
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				((DefaultListModel<Template>)templateList.getModel()).removeAllElements();
				for(Template t: templateFactory.getTemplates()){
					((DefaultListModel<Template>)templateList.getModel()).addElement(t);
				}
				templateList.validate();
			}
		});
	}
	
	
	/**
	 * set busy 
	 * @param b
	 */
	private void setBusy(boolean b){
		final boolean busy = b;
		SwingUtilities.invokeLater(new Runnable(){
			public void run(){
				buttonPanel.removeAll();
				if(busy){
					progress.setIndeterminate(true);
					progress.setString("Please Wait. It may take a while ...");
					progress.setStringPainted(true);
					buttonPanel.add(progress,BorderLayout.CENTER);
					console.setText("");
				}else{
					buttonPanel.add(run,BorderLayout.CENTER);
				}
				buttonPanel.validate();
				buttonPanel.repaint();
				
			}
		});
	}
	

	private JMenuBar getMenuBar() {
		JMenuBar menu = new JMenuBar();
		JMenu file = new JMenu("File");
		JMenuItem exit = new JMenuItem("Exit");
		exit.addActionListener(this);
		file.add(exit);
		menu.add(file);
		return menu;
	}

	public void actionPerformed(ActionEvent e) {
		String cmd = e.getActionCommand();
		if("run".equals(cmd)){
			doRun();
		}else if("i_browser".equals(cmd)){
			doBrowse(input);
		}else if("d_browser".equals(cmd)){
			
		}else if("o_browser".equals(cmd)){
			doBrowse(output);
		}else if("exit".equals(cmd)){
			System.exit(0);
		}else if("export".equals(cmd)){
			doExport();
		}else if("import".equals(cmd)){
			doImport();
		}else if("preview".equals(cmd)){
			doPreview();
		}
	}
	
	/**
	 * do preview
	 */
	private void doPreview() {
		Template t = templateList.getSelectedValue();
		if(t == null)
			return;
		
		StringBuffer b = new StringBuffer();
		b.append("<h2>"+t.getName()+"</h2>");
		b.append(t.getDescription()+"<hr>");
		b.append("<ul>");
		for(TemplateItem i: t.getTemplateItems()){
			b.append("<li><b>"+i.getName()+"</b>"); 
		}
		b.append("</ul>");
		
		JEditorPane text = new JEditorPane();
		text.setContentType("text/html; charset=UTF-8");
		HTMLDocument doc = (HTMLDocument) text.getDocument();
		doc.getStyleSheet().addRule("body { font-family: sans-serif;");
		text.setText(b.toString());
		JPanel msg = new JPanel();
		msg.setLayout(new BorderLayout());
		msg.setPreferredSize(new Dimension(500,500));
		msg.add(new JScrollPane(text),BorderLayout.CENTER);
		JOptionPane.showMessageDialog(frame,msg,"Preview",JOptionPane.PLAIN_MESSAGE);
	}


	/**
	 * do export of highlighted template
	 */
	private void doExport() {
		Template template = templateList.getSelectedValue();
		if(template != null){
			JFileChooser chooser = new JFileChooser();
			chooser.setFileFilter(new FileFilter(){
				public boolean accept(File f) {
					return f.isDirectory() || f.getName().endsWith(".template");
				}
				public String getDescription() {
					return "Template XML File";
				}
				
			});
			chooser.setSelectedFile(new File(template.getName()+".template"));
			int r = chooser.showSaveDialog(frame);
			if(r == JFileChooser.APPROVE_OPTION){
				try{
					File f = chooser.getSelectedFile();
					FileOutputStream out = new FileOutputStream(f);
					templateFactory.exportTemplate(template, out);
					out.close();
				}catch(Exception ex){
					JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
					ex.printStackTrace();
				}
			}
		}
	}
	
	/**
	 * do export of highlighted template
	 */
	private void doImport() {
		JFileChooser chooser = new JFileChooser(file);
		chooser.setFileFilter(new FileFilter(){
			public boolean accept(File f) {
				return f.isDirectory() || f.getName().endsWith(".template");
			}
			public String getDescription() {
				return "Template XML File";
			}
			
		});
		int r = chooser.showOpenDialog(frame);
		if(r == JFileChooser.APPROVE_OPTION){
			try{
				file = chooser.getSelectedFile();
				importTemplates(file.getAbsolutePath());
				refreshTemplateList();
			}catch(Exception ex){
				JOptionPane.showMessageDialog(frame,ex.getMessage(),"Error",JOptionPane.ERROR_MESSAGE);
				ex.printStackTrace();
			}
		}
		
	}

	public Template importTemplates(String url) throws Exception{
		InputStream is = null;
		if(url.startsWith("http://"))
			is = new URL(url).openStream();
		else 
			is = new FileInputStream(new File(url));
		if(is != null){
			Template t =  templateFactory.importTemplate(is);
			is.close();
			return t;
		}
		return null;
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
	
	
     /**
      * check UI inputs
      * @return
      */
    private boolean checkInputs(){
 		if(templateList.getSelectedValuesList().isEmpty()){
			JOptionPane.showMessageDialog(frame,"Please Select Templates");
			return false;
		}
		if(!new File(input.getText()).exists()){
			JOptionPane.showMessageDialog(frame,"Please Select Input Report Directory");
			return false;
		}
		return true;
    }
     
    /**
     * run the damn thing
     */
	private void doRun() {
		(new Thread(new Runnable(){
			public void run() {
				if(!checkInputs()){
					return;
				}
				setBusy(true);
				
				
				try {
					process(getSelectedValuesList(),input.getText(),output.getText());
				} catch (Exception e) {
					e.printStackTrace();
				}
				
				setBusy(false);
				
				// open in browser
				browseURLInSystemBrowser(new File(output.getText()+File.separator+"index.html").toURI().toString());
				
			}
		})).start();
	}

	private List<Template> getSelectedValuesList(){
		List<Template> list = new ArrayList<Template>();
		for(Object o: templateList.getSelectedValues()){
			list.add((Template) o);
		}
		return list;
	}
	
	
	private void doBrowse(JTextField text){
		//if(text == domain){
		//	
		//}else{
			JFileChooser fc = new JFileChooser(file);
			fc.setFileSelectionMode(JFileChooser.FILES_AND_DIRECTORIES);
			int r = (output == text)?fc.showSaveDialog(frame):fc.showOpenDialog(frame);
			if(r == JFileChooser.APPROVE_OPTION){
				file = fc.getSelectedFile();
				text.setText(file.getAbsolutePath());
				
				// if input, change output to default
				if(text == input){
					output.setText(new File(file.getParent()+File.separator+"Output"+File.separator+file.getName()).getAbsolutePath());
				}
			}
		//}
	}

	/**
	 * process  documents
	 * @param args
	 */
	public void process(List<Template> templates,String in, String out){	
		// process file
		List<File> files = getFiles(new File(in),new ArrayList<File>());
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
		
		// initialize writers
		htmlExporter = new HTMLExporter(outputDir);
		csvExporter = new CSVExporter(outputDir);
		
		// reset stat counters
		processCount = 0;
		totalTime = 0;
		
		for(int i=0;i<files.size();i++){
			try {
				process(templates,files.get(i));
			} catch (Exception e) {
				progress("Error: "+e.getMessage());
				e.printStackTrace();
			}
			if(progress != null){
				final int n = i+1;
				SwingUtilities.invokeLater(new Runnable(){
					public void run(){
						progress.setValue(n);
					}
				});
			}
		}
		
		
		// wrap up
		try {
			htmlExporter.flush();
			csvExporter.flush();
		} catch (Exception e) {
			progress("Error: "+e.getMessage());
			e.printStackTrace();
		}
		
		
		// summary
		progress("\nTotal process time for all reports:\t"+totalTime+" ms\n");
		progress("Average process time per report:\t"+(totalTime/processCount)+" ms\n");
	}

	private List<File> getFiles(File in,List<File> list) {
		if(in.isDirectory()){
			for(File f: in.listFiles()){
				getFiles(f,list);
			}
		}else if(in.isFile() && in.getName().endsWith(".txt")){
			list.add(in);
		}
		return list;
	}

	/**
	 * get NobleCoder for a given set of templates
	 * @param templates
	 * @return
	 */
	private NobleCoder getCoder(Set<Template> templates){
		if(coders == null)
			coders = new HashMap<Set<Template>, NobleCoder>();
		if(!coders.containsKey(templates)){
			CompositTerminology terminology = new CompositTerminology();
			for(Template t: templates){
				//doc.getFilters().addAll(t.getFilters());
				terminology.addTerminology(t.getTerminology());
			}
			// do first level processing with NobleCoder
			NobleCoder coder = new NobleCoder(terminology);
			coders.put(templates,coder);
		}
		return coders.get(templates);
	}
	
	
	/**
	 * processed coded document
	 * 
	 */
	public TemplateDocument process(TemplateDocument doc) throws TerminologyException {
		processTime = System.currentTimeMillis();
		// combine terminologies into a single instance and add filters
		/*	CompositTerminology terminology = new CompositTerminology();
		for(Template t: doc.getTemplates()){
			//doc.getFilters().addAll(t.getFilters());
			terminology.addTerminology(t.getTerminology());
		}
		
		// do first level processing with NobleCoder
		NobleCoder coder = new NobleCoder(terminology);*/
		getCoder(doc.getTemplates()).process(doc);
		
		// now lets do information extraction
		for(Template template: doc.getTemplates()){
			if(template.isAppropriate(doc)){
				try {
					doc.addItemInstances(template,template.process(doc));
				} catch (TerminologyException e) {
					throw new TerminologyError("Unforseen issue",e);
				}
			}
		}
		processTime = System.currentTimeMillis() -processTime;
		totalTime += processTime;
		doc.getProcessTime().put(getClass().getSimpleName(),processTime);
		return doc;
	}
	
	

	/**
	 * process report
	 * @param text
	 * @param out
	 */
	private void process(List<Template> templates,File reportFile) throws Exception {
		progress("processing report ("+(processCount+1)+") "+reportFile.getName()+" ... ");
		
		// read in the report, do first level proce
		String text = TextTools.getText(new FileInputStream(reportFile));
		TemplateDocument doc = new TemplateDocument();
		doc.setTitle(reportFile.getName());
		doc.setLocation(reportFile.getAbsolutePath());
		doc.setText(text);
		doc.addTemplates(templates);
		doc.setDocumentType(DocumentProcessor.suggestDocumentType(text));
		//doc.getFilters().addAll(DocumentFilter.getDeIDFilters());
		
		// do information extraction
		process(doc);
		
		processCount ++;
			
		// now output HTML for this report
		htmlExporter.export(doc);
		csvExporter.export(doc);
		
		// do progress
		progress(getProcessTime()+" ms\n");
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

	public long getProcessTime() {
		return processTime;
	}

}
