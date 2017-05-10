package com.bluedot.commons.alerts;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToOne;

import com.bluedot.commons.security.User;
import com.play4jpa.jpa.models.Model;


@Entity
public class AlertGroup extends Model<AlertGroup>
{

	@Id
	@GeneratedValue
	private int id;

	@OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
	private User user;

	@OneToOne(cascade = {CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE})
	private AlertMetadata alertMetadata;
	
	public AlertGroup(){
		
	}

	public AlertGroup(User user, AlertMetadata alertMetadata) {
		super();
		this.user = user;
		this.alertMetadata = alertMetadata;
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public User getUser()
	{
		return user;
	}

	public void setUser(User user)
	{
		this.user = user;
	}

	public AlertMetadata getAlertMetadata()
	{
		return alertMetadata;
	}

	public void setAlertMetadata(AlertMetadata alertMetadata)
	{
		this.alertMetadata = alertMetadata;
	}
}
