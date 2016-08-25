package edu.pitt.dbmi.nlp.noble.ontology.owl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.semanticweb.owlapi.model.OWLClass;
import org.semanticweb.owlapi.model.OWLDataFactory;
import org.semanticweb.owlapi.model.OWLDataProperty;
import org.semanticweb.owlapi.model.OWLDataPropertyExpression;
import org.semanticweb.owlapi.model.OWLIndividual;
import org.semanticweb.owlapi.model.OWLLiteral;
import org.semanticweb.owlapi.model.OWLNamedIndividual;
import org.semanticweb.owlapi.model.OWLObject;
import org.semanticweb.owlapi.model.OWLObjectProperty;
import org.semanticweb.owlapi.model.OWLObjectPropertyExpression;
import org.semanticweb.owlapi.model.OWLOntology;
import org.semanticweb.owlapi.reasoner.NodeSet;

import edu.pitt.dbmi.nlp.noble.ontology.IClass;
import edu.pitt.dbmi.nlp.noble.ontology.IInstance;
import edu.pitt.dbmi.nlp.noble.ontology.IProperty;
import edu.pitt.dbmi.nlp.noble.ontology.LogicExpression;

public class OInstance extends OResource implements IInstance {
	private OWLIndividual individual;
	
	protected OInstance(OWLObject obj, OOntology ont) {
		super(obj,ont);
		individual = (OWLIndividual) obj;
	}


	public IProperty[] getProperties() {
		List<IProperty> props = new ArrayList<IProperty>();
		Collections.addAll(props,super.getProperties());
		for(OWLOntology ont: getOWLOntologyManager().getOntologies()){
			for(OWLDataPropertyExpression e: getOWLIndividual().getDataPropertyValues(ont).keySet()){
				props.add((IProperty)convertOWLObject(e));
			}
			for(OWLObjectPropertyExpression e: getOWLIndividual().getObjectPropertyValues(ont).keySet()){
				props.add((IProperty)convertOWLObject(e));
			}
		}
		return props.toArray(new IProperty [0]);
	}

	public Object getPropertyValue(IProperty prop) {
		Object [] o = getPropertyValues(prop);
		return o.length > 0?o[0]:null;
	}

	public Object[] getPropertyValues(IProperty prop) {
		if(prop.isAnnotationProperty())
			return super.getPropertyValues(prop);
		else if(prop.isDatatypeProperty()){
			LogicExpression list = new LogicExpression(LogicExpression.AND);
			for(OWLLiteral l: getOWLReasoner().getDataPropertyValues((OWLNamedIndividual)getOWLIndividual(),(OWLDataProperty)convertOntologyObject(prop))){
				list.add(convertOWLObject(l));
			}
			return list.toArray();
		}else if(prop.isObjectProperty()){
			LogicExpression list = new LogicExpression(LogicExpression.AND);
			for(OWLIndividual l: getOWLReasoner().getObjectPropertyValues((OWLNamedIndividual)getOWLIndividual(),
					(OWLObjectPropertyExpression)convertOntologyObject(prop)).getFlattened()){
				list.add(convertOWLObject(l));
			}
			return list.toArray();
		}
		return new Object [0];
	}


	
	public void addPropertyValue(IProperty prop, Object value) {
		OWLDataFactory df = getOWLDataFactory();
		OWLIndividual subj = getOWLIndividual();
		if(prop.isAnnotationProperty()){
			super.addPropertyValue(prop, value);
		}else if(prop.isDatatypeProperty()){
			OWLDataProperty dp = (OWLDataProperty)convertOntologyObject(prop);
			OWLLiteral dl = (OWLLiteral)convertOntologyObject(value);
			addAxiom(df.getOWLDataPropertyAssertionAxiom(dp,subj,dl));
		}else if(prop.isObjectProperty()){
			OWLObjectProperty op = (OWLObjectProperty)convertOntologyObject(prop);
			OWLIndividual oo = (OWLIndividual)convertOntologyObject(value);
			addAxiom(df.getOWLObjectPropertyAssertionAxiom(op,subj,oo));
		}
	}
	public void setPropertyValue(IProperty prop, Object value) {
		removePropertyValues(prop);
		addPropertyValue(prop, value);
	}

	public void setPropertyValues(IProperty prop, Object[] values) {
		removePropertyValues(prop);
		for(Object o: values)
			addPropertyValue(prop, o);
	}

	public void removePropertyValues(IProperty prop) {
		for(Object o: getPropertyValues(prop)){
			removePropertyValue(prop,o);
		}
	}

	public void removePropertyValue(IProperty prop, Object value) {
		OWLDataFactory df = getOWLDataFactory();
		OWLIndividual subj = getOWLIndividual();
		if(prop.isAnnotationProperty()){
			super.removePropertyValue(prop, value);
		}else if(prop.isDatatypeProperty()){
			OWLDataProperty dp = (OWLDataProperty)convertOntologyObject(prop);
			OWLLiteral dl = (OWLLiteral)convertOntologyObject(value);
			removeAxiom(df.getOWLDataPropertyAssertionAxiom(dp,subj,dl));
		}else if(prop.isObjectProperty()){
			OWLObjectProperty op = (OWLObjectProperty)convertOntologyObject(prop);
			OWLIndividual oo = (OWLIndividual)convertOntologyObject(value);
			removeAxiom(df.getOWLObjectPropertyAssertionAxiom(op,subj,oo));
		}
	}

	public void removePropertyValues() {
		for(IProperty p: getProperties()){
			removePropertyValues(p);
		}
	}
	
	public boolean hasPropetyValue(IProperty p, Object value) {
		if(p.isAnnotationProperty())
			return super.hasPropetyValue(p, value);
		if(p.isDatatypeProperty()){
			for(OWLOntology ont: getOWLOntologyManager().getOntologies()){
				if(getOWLIndividual().hasDataPropertyValue((OWLDataPropertyExpression)convertOntologyObject(p),
					(OWLLiteral)convertOntologyObject(value), ont))
				return true;
			}
			return false;
		}
		if(p.isObjectProperty()){
			for(OWLOntology ont: getOWLOntologyManager().getOntologies()){
				if(getOWLIndividual().hasObjectPropertyValue((OWLObjectPropertyExpression)convertOntologyObject(p),
					(OWLIndividual)convertOntologyObject(value),ont)){
					return true;
				}
			}
			return false;
		}
		return false;
	}
	
	public void addType(IClass cls) {
		OWLClass pr = (OWLClass) convertOntologyObject(cls);
		addAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(pr,individual));
	}
	
	public void removeType(IClass cls) {
		OWLClass pr = (OWLClass) convertOntologyObject(cls);
		removeAxiom(getOWLDataFactory().getOWLClassAssertionAxiom(pr,individual));
	}

	public IClass[] getTypes() {
		NodeSet<OWLClass> sub = getOWLReasoner().getTypes((OWLNamedIndividual)individual,false);
		return getClasses(sub.getFlattened());
	}

	public IClass[] getDirectTypes() {
		NodeSet<OWLClass> sub = getOWLReasoner().getTypes((OWLNamedIndividual)individual,true);
		return getClasses(sub.getFlattened());
	}

	public boolean hasType(IClass cls) {
		NodeSet<OWLClass> sub = getOWLReasoner().getTypes((OWLNamedIndividual)individual,false);
		return sub.containsEntity((OWLClass)convertOntologyObject(cls));
	}

	public OWLIndividual getOWLIndividual() {
		return individual;
	}

}
