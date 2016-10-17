package com.bluedot.commons.alerts;

import java.util.Calendar;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.ManyToOne;
import javax.persistence.OneToMany;
import javax.persistence.OneToOne;

import com.bluedot.commons.notificationChannels.NotificationRecord;
import com.bluedot.commons.security.Account;
import com.play4jpa.jpa.models.Model;

@Entity
public class Alert extends Model<Alert>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = -5160232066844930417L;

	@Id
	@GeneratedValue
	private int id;

	private Date creationTimestamp;
	private boolean alertRead;
	private String description;
	private String sourceName;
	private int severity;
	private String value;
	@Lob
	private String body;
	private String subject;
	private String attachment;

	@OneToMany(cascade = CascadeType.ALL)
	private List<NotificationRecord> notificationRecords;

	@ManyToOne
	private Account account;

	@OneToOne
	private AlertMetadata alertMetadata;

	public Alert() {
	}

	public Alert(String description, String sourceName, int severity, String value, String body, String subject, String attachment, Account account, AlertMetadata alertMetadata) {
		super();
		this.creationTimestamp = Calendar.getInstance().getTime();
		this.alertRead = false;
		this.description = description;
		this.sourceName = sourceName;
		this.severity = severity;
		this.body = body;
		this.subject = subject;
		this.value = value;
		this.attachment = attachment;
		this.account = account;
		this.notificationRecords = new LinkedList<NotificationRecord>();
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

	public Date getCreationTimestamp()
	{
		return creationTimestamp;
	}

	public void setCreationTimestamp(Date creationTimestamp)
	{
		this.creationTimestamp = creationTimestamp;
	}

	public boolean isAlertRead()
	{
		return alertRead;
	}

	public void setAlertRead(boolean alertRead)
	{
		this.alertRead = alertRead;
	}

	public String getDescription()
	{
		return description;
	}

	public void setDescription(String description)
	{
		this.description = description;
	}

	public String getSourceName()
	{
		return sourceName;
	}

	public void setSourceName(String sourceName)
	{
		this.sourceName = sourceName;
	}

	public int getSeverity()
	{
		return severity;
	}

	public void setSeverity(int severity)
	{
		this.severity = severity;
	}

	public String getValue()
	{
		return value;
	}

	public void setValue(String value)
	{
		this.value = value;
	}

	public String getAttachment()
	{
		return attachment;
	}

	public void setAttachment(String attachment)
	{
		this.attachment = attachment;
	}

	public List<NotificationRecord> getNotificationRecords()
	{
		if (notificationRecords == null)
			notificationRecords = new LinkedList<NotificationRecord>();
		return notificationRecords;
	}

	public void setNotifications(List<NotificationRecord> notifications)
	{
		this.notificationRecords = notifications;
	}

	public String getBody()
	{
		return body;
	}

	public void setBody(String body)
	{
		this.body = body;
	}

	public String getSubject()
	{
		return subject;
	}

	public void setSubject(String subject)
	{
		this.subject = subject;
	}

	public Account getAccount()
	{
		return account;
	}

	public void setAccount(Account account)
	{
		this.account = account;
	}

	public void setNotificationRecords(List<NotificationRecord> notificationRecords)
	{
		this.notificationRecords = notificationRecords;
	}

	public AlertMetadata getAlertMetadata()
	{
		return alertMetadata;
	}

	public void setAlertMetadata(AlertMetadata alertMetadata)
	{
		this.alertMetadata = alertMetadata;
	}

	@Override
	public String toString()
	{
		return "Alert [id=" + id + ", creationTimestamp=" + creationTimestamp + ", alertRead=" + alertRead + ", description=" + description + ", sourceName=" + sourceName + ", severity=" + severity
				+ ", value=" + value + ", body=" + body + ", subject=" + subject + ", attachment=" + attachment + ", notificationRecords=" + notificationRecords + ", account=" + account
				+ ", alertMetadata=" + alertMetadata + "]";
	}

	@Override
	public boolean equals(Object obj)
	{
		if (obj == null)
			return false;
		if (!Alert.class.isAssignableFrom(obj.getClass()))
			return false;
		return ((Alert) obj).getId() == this.getId();
	}

}
