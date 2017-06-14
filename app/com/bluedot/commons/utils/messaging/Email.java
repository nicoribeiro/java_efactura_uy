package com.bluedot.commons.utils.messaging;

import java.util.Date;
import java.util.LinkedList;
import java.util.List;

public class Email {

	private String messageId;
	private String from;
	private String subject;
	private Date sentDate;
	private String messageContent;
	private List<Attachment> attachments;

	public Email() {

	}

	public Email(String messageId, String from, String subject, Date sentDate, String messageContent) {
		super();
		this.messageId = messageId;
		this.from = from;
		this.subject = subject;
		this.sentDate = sentDate;
		this.messageContent = messageContent;
	}

	public String getFrom() {
		return from;
	}

	public void setFrom(String from) {
		this.from = from;
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
		if (attachments==null)
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
