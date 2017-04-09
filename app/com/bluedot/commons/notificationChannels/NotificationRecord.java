package com.bluedot.commons.notificationChannels;

import java.util.Calendar;
import java.util.Date;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import com.play4jpa.jpa.models.Model;

import play.db.jpa.JPAApi;

@Entity
public class NotificationRecord extends Model<NotificationRecord>
{
	@Id
	@GeneratedValue
	private int id;
	private Date deliveryTimestamp;

	@ManyToOne(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE })
	private NotificationChannel notificationChannel;

	public NotificationRecord() {
		super();
		
	}

	public NotificationRecord(NotificationChannel notificationChannel) {
		super();
		this.notificationChannel = notificationChannel;
		deliveryTimestamp = Calendar.getInstance().getTime();
	}
	
	@Override
	protected void preDelete(JPAApi jpaApi)
	{
		super.preDelete(jpaApi);
		this.notificationChannel = null;
		this.update(jpaApi);
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null)return false;
		if(!NotificationRecord.class.isAssignableFrom(obj.getClass()))return false;
		return ((NotificationRecord)obj).getId() == this.getId();
	}

	public int getId()
	{
		return id;
	}

	public void setId(int id)
	{
		this.id = id;
	}

	public NotificationChannel getNotificationChannel()
	{
		return notificationChannel;
	}

	public void setNotificationChannel(NotificationChannel notificationChannel)
	{
		this.notificationChannel = notificationChannel;
	}
	
	
	

}
