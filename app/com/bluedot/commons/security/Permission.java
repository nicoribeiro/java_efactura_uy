package com.bluedot.commons.security;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;

import com.play4jpa.jpa.models.Model;

@Entity
public class Permission extends Model<Permission>
{
	/**
	 * 
	 */
	private static final long serialVersionUID = -1286388420579459667L;

	@Id
	@GeneratedValue
	private int id;
	
	private String permissionId;
	
	private String description;
	
	public Permission(){
		
	}
	
	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public String getPermissionId()
	{
		return permissionId;
	}

	public void setPermissionId(String permissionId)
	{
		this.permissionId = permissionId;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (!Permission.class.isAssignableFrom(obj.getClass()))
			return false;
		return ((Permission) obj).getPermissionId().equals(this.getPermissionId());
	}
}
