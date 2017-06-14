package com.bluedot.commons.security;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.OneToMany;

import org.hibernate.criterion.Restrictions;

import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

@Entity
public class EmailMessage extends Model<EmailMessage> {

	/**
	 * 
	 */
	private static final long serialVersionUID = -8701151304733591795L;

	@Id
	@GeneratedValue
	private long id;

	private String messageId;
	private String fromEmail;
	private String subject;
	private Date sentDate;
	private String messageContent;

	@OneToMany
	private List<Attachment> attachments;

	public EmailMessage() {

	}

	public EmailMessage(com.bluedot.commons.utils.messaging.Email email) {
		this.messageId = email.getMessageId();
		this.fromEmail = email.getFrom();
		this.subject = email.getSubject();
		this.sentDate = email.getSentDate();
		this.messageContent = email.getMessageContent();
		for (com.bluedot.commons.utils.messaging.Attachment attachment : email.getAttachments()) {
			Attachment attachment2 = new Attachment(attachment);
			attachment2.save();
			getAttachments().add(attachment2);
		}

	}

	public EmailMessage(String messageId, String from, String subject, Date sentDate, String messageContent) {
		super();
		this.messageId = messageId;
		this.fromEmail = from;
		this.subject = subject;
		this.sentDate = sentDate;
		this.messageContent = messageContent;
	}

	protected static Finder<Long, EmailMessage> find = new Finder<Long, EmailMessage>(Long.class, EmailMessage.class);
	
	public static List<EmailMessage> findByMessageId(String messageId) {

		DefaultQuery<EmailMessage> q = (DefaultQuery<EmailMessage>) find.query();

		q.getCriteria().add(Restrictions.eq("messageId", messageId));

		return q.findList();

	}

	public String getFromEmail() {
		return fromEmail;
	}

	public void setFromEmail(String fromEmail) {
		this.fromEmail = fromEmail;
	}

	public String getSubject() {
		return subject;
	}

	public void setSubject(String subject) {
		this.subject = subject;
	}

	public Date getSentDate() {
		return sentDate;
	}

	public void setSentDate(Date sentDate) {
		this.sentDate = sentDate;
	}

	public String getMessageContent() {
		return messageContent;
	}

	public void setMessageContent(String messageContent) {
		this.messageContent = messageContent;
	}

	public List<Attachment> getAttachments() {
		if (attachments == null)
			attachments = new LinkedList<Attachment>();
		return attachments;
	}

	public void setAttachments(List<Attachment> attachments) {
		this.attachments = attachments;
	}

	public String getMessageId() {
		return messageId;
	}

	public void setMessageId(String messageId) {
		this.messageId = messageId;
	}
}
