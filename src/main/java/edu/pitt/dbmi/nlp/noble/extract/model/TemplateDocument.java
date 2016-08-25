package edu.pitt.dbmi.nlp.noble.extract.model;

import java.util.*;

import edu.pitt.dbmi.nlp.noble.coder.model.Document;
import edu.pitt.dbmi.nlp.noble.terminology.Annotation;

/**
 * this class represents the original decument along with processed concept annotations
 * @author tseytlin
 *
 */
public class TemplateDocument extends Document {
	private Map<Template,List<ItemInstance>> itemInstances =  null;
	

	/**
	 * get a mapping of templates to item instance maps
	 * @return
	 */
	public Map<Template,List<ItemInstance>> getItemInstances(){
		if(itemInstances == null){
			itemInstances = new LinkedHashMap<Template, List<ItemInstance>>();
		}
		return itemInstances;
	}
		
	/**
	 * get a list of templates associated with this document
	 * @return
	 */
	public Set<Template> getTemplates(){
		return getItemInstances().keySet();
	}
	
	/**
	 * add template to a document
	 * @param t
	 */
	public void addTemplate(Template t){
		if(!getItemInstances().containsKey(t))
			getItemInstances().put(t,new ArrayList<ItemInstance>());
	}
	
	/**
	 * add template to a document
	 * @param t
	 */
	public void addTemplates(Collection<Template> tt){
		for(Template t: tt)
			addTemplate(t);
	}
	
	/**
	 * get list of item instances for a template
	 * @param t
	 * @return
	 */
	public List<ItemInstance> getItemInstances(Template t){
		return getItemInstances().get(t);
	}
	
	
	/**
	 * add a list of item instances to a template
	 * @param t
	 * @return
	 */
	public void addItemInstances(Template t,List<ItemInstance> list){
		getItemInstances().put(t,list);
	}
	
	/**
	 * get matching instances
	 * @param temp
	 * @param all
	 * @return
	 */
	public List<ItemInstance> getItemInstances(TemplateItem feature){
		List<ItemInstance> inst = new ArrayList<ItemInstance>();
		for(ItemInstance item: getItemInstances(feature.getTemplate())){
			if(item.getTemplateItem().equals(feature))
				inst.add(item);
		}
		return inst;
	}
	
	/**
	 * get matching instances
	 * @param temp
	 * @param all
	 * @return
	 */
	public List<ItemInstance> getItemInstances(TemplateItem feature, TemplateItem attribute){
		List<ItemInstance> list = new ArrayList<ItemInstance>();
		for(ItemInstance item: getItemInstances(feature)){
			for(ItemInstance a: item.getAttributes()){
				if(a.getTemplateItem().equals(attribute)){
					list.addAll(item.getAttributeValues(a));
				}
			}
		}
		return list;
	}
	
	/**
	 * get filtered document 
	 * @return
	 *
	public String getFilteredDocument(){
		String filtered = getText();
		for(DocumentFilter filter : getFilters()){
			filtered = filter.filter(filtered);
		}
		return filtered;
	}
	*/

}
