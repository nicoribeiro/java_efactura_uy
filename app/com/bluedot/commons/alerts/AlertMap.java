package com.bluedot.commons.alerts;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import com.bluedot.commons.security.User;
import com.play4jpa.jpa.models.Model;


@Entity
public class AlertMap extends Model<AlertMap> implements AlertAccept
{

	@Id
	@GeneratedValue
	private int id;

	@OneToMany(cascade = CascadeType.ALL)
	private Collection<AlertGroup> alertGroups;

	public Collection<AlertGroup> getAlertGroups()
	{
		if (alertGroups==null)
			alertGroups= new LinkedList<AlertGroup>();
		return alertGroups;
	}
	
	public AlertMap() {

	}

	@Override
	public List<AlertReceiver> getAlertRecivers(Alert alert)
	{
		List<AlertReceiver> list = new LinkedList<AlertReceiver>();
		
		for (AlertGroup alertGroup : getAlertGroups())	
		{
			if (alertGroup.getAlertMetadata().getId()==alert.getAlertMetadata().getId())
				list.add(alertGroup.getUser());
		}
		
		return list;
	}

	@Override
	public Map<Integer,AlertMetadata> getPosibleAlertMetadatas()
	{
		return null;
	}

	public void addMapping(User user, AlertMetadata alertMetadata)
	{
		boolean finded = false;
		for (AlertGroup alertGroup : getAlertGroups())	
		{
			if (alertGroup.getAlertMetadata().getId()==alertMetadata.getId() && alertGroup.getUser().getId()==user.getId()){
				finded = true;
				break;
			}
			
		}
		
		if (!finded){
			AlertGroup alertGroup = new AlertGroup(user, alertMetadata);
			alertGroup.save();
			getAlertGroups().add(alertGroup);
			this.update();
		}
	}

	public void deleteAllMappings()
	{
//		Collection<AlertGroup> tempAlertGroups = new LinkedList<AlertGroup>();
		
//		tempAlertGroups.addAll(alertGroups);
		
		alertGroups.clear();
		this.update();
		
//		this.refresh();

		
//		for(AlertGroup alertGroup : tempAlertGroups){
//			alertGroup.delete();
//		}
	}
	
	public void deleteMapping(User user, AlertMetadata alertMetadata){
		deleteMapping(user, alertMetadata.getId());
	}
	
	public void deleteMapping(User user, int alertMetadataId){
		for (AlertGroup alertGroup : getAlertGroups())	
		{
			if (alertGroup.getAlertMetadata().getId()==alertMetadataId && alertGroup.getUser().getId()==user.getId()){
				//alertGroup.delete();
				alertGroups.remove(alertGroup);
				this.update();
				break;
			}
		}
	}
	
	@Override
	public AlertMap getAlertMap()
	{
		return null;
	}
	
}
