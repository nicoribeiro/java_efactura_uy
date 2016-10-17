package com.bluedot.commons.notificationChannels;

import java.util.Calendar;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedList;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorColumn;
import javax.persistence.DiscriminatorType;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Inheritance;
import javax.persistence.InheritanceType;
import javax.persistence.OneToMany;

import com.bluedot.commons.alerts.Alert;
import com.bluedot.commons.messages.Message;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;


@Entity
@Inheritance(strategy = InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name = "FROM_CLASS", discriminatorType = DiscriminatorType.STRING, length = 50)
public abstract class NotificationChannel extends Model<NotificationChannel> 
{

	@Id
	@GeneratedValue
	private int id;
	private String description;
	private String validationKey;
	private Date validationDate;
	private Boolean validated;
	private Boolean enabled;

	public NotificationChannel() {

	}

	public NotificationChannel(String description) {
		super();
		this.enabled = true;
		this.validated = false;
		this.validationKey = generateValidationKey();
		this.description = description;
	}

	@ElementCollection
	private Collection<Integer> severities;

	@OneToMany(cascade = { CascadeType.PERSIST, CascadeType.REFRESH, CascadeType.MERGE }, mappedBy = "notificationChannel")
	private Collection<NotificationRecord> notificationRecords;

	private static Finder<Integer, NotificationChannel> find = new Finder<Integer, NotificationChannel>(Integer.class, NotificationChannel.class);

	public void sendAlert(Alert alert)
	{
		// TODO filter by severity
		// if (severities.contains(alert.getSeverity()) && validated && enabled)

		if (validated && enabled)
		{
			NotificationRecord notificationRecord = new NotificationRecord(this);
			getNotificationRecords().add(notificationRecord);
			alert.getNotificationRecords().add(notificationRecord);
			sendNotification(alert);
			notificationRecord.save();
			this.update();
			alert.update();
		}
	}

	public abstract void sendNotification(Alert alert);
	
	public abstract void sendMessage(Message message);

	public abstract void test();

	public abstract void sendValidationKey(String arg);

	protected abstract String generateValidationKey();

	public static NotificationChannel findById(int notificationChannelId)
	{
		return find.byId(notificationChannelId);
	}

	public boolean validate(String key)
	{
		if (key.equals(validationKey))
		{
			validated = true;
			validationDate = Calendar.getInstance().getTime();
			return true;
		} else
			return false;
	}

	public Boolean getValidated()
	{
		return validated;
	}

	public Boolean getEnabled()
	{
		return enabled;
	}
	
	public void setEnabled(Boolean enabled)
	{
		this.enabled = enabled;
	}

	public int getId()
	{
		return id;
	}

	public String getValidationKey()
	{
		return validationKey;
	}

	public Collection<NotificationRecord> getNotificationRecords()
	{
		if (notificationRecords == null)
			notificationRecords = new LinkedList<NotificationRecord>();
		return notificationRecords;
	}
	
	@Override
	public boolean equals(Object obj)
	{
		if(obj == null)return false;
		if(!NotificationChannel.class.isAssignableFrom(obj.getClass()))return false;
		return ((NotificationChannel)obj).getId() == this.getId();
	}

}
