package edu.pitt.dbmi.nlp.noble.ui.widgets;

import java.awt.Component;
import java.net.URL;

import javax.swing.DefaultListCellRenderer;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JList;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IOntology;
import edu.pitt.dbmi.nlp.noble.ontology.IResource;
import edu.pitt.dbmi.nlp.noble.terminology.Concept;
import edu.pitt.dbmi.nlp.noble.terminology.Terminology;

public class ResourceCellRenderer extends DefaultListCellRenderer {
	private final URL ONTOLOGY_ICON = getClass().getResource("/icons/Ontology.gif");
	private final URL TERMINOLOGY_ICON = getClass().getResource("/icons/Terminology.gif");
	private final URL CLASS_ICON = getClass().getResource("/icons/Class.gif");
	private final URL INSTANCE_ICON = getClass().getResource("/icons/Instance.gif");
	private Icon ontologyIcon = new ImageIcon(ONTOLOGY_ICON);
	private Icon terminologyIcon = new ImageIcon(TERMINOLOGY_ICON);
	private Icon classIcon = new ImageIcon(CLASS_ICON);
	private Icon instanceIcon = new ImageIcon(INSTANCE_ICON);
	
	/**
	 * add icon
	 */
	public Component getListCellRendererComponent(JList a, Object b,int c, boolean d, boolean e){
		JLabel lbl = (JLabel) super.getListCellRendererComponent(a, b, c, d, e);
		if(b instanceof IOntology)
			lbl.setIcon(ontologyIcon);
		else if(b instanceof Terminology)
			lbl.setIcon(terminologyIcon);
		else if(b instanceof IClass || b instanceof Concept)
			lbl.setIcon(classIcon);
		else if(b instanceof IInstance)
			lbl.setIcon(instanceIcon);
		else
			lbl.setIcon(null);
		// set text
		if(b instanceof IResource){
			IResource r = (IResource) b;
			//String [] l = r.getLabels();
			//lbl.setText((l.length > 0)?l[0]:r.getName());
			lbl.setText(r.getName());
		}else if(b instanceof Concept){
			lbl.setText(((Concept)b).getName());
		}		
		return lbl;
	}
}
