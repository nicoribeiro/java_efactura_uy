package com.bluedot.commons.utils;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class Email {

	private String from;
	private String subject;
	private Date sentDate;
	private String messageContent;
	private Map<String, String> attachments;
	
	public Email(){
		
	}
	
	public Email(String from, String subject, Date sentDate, String messageContent) {
		super();
		this.from = from;
		this.subject = subject;
		this.sentDate = sentDate;
		this.messageContent = messageContent;
		attachments = new HashMap<String, String>();
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
	public Map<String, String> getAttachments() {
		return attachments;
	}
	public void setAttachments(Map<String, String> attachments) {
		this.attachments = attachments;
	}
	

}
