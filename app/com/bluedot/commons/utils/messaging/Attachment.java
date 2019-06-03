package com.bluedot.commons.utils.messaging;

public class Attachment 
{
	private String url;
	
	private AttachmentType attachmentType;
	
	private String payload;
	
	private String createdBy;
	
	private long timestamp;
	
	private String name;
	
	public String getUrl()
	{
		return url;
	}

	public void setUrl(String url)
	{
		this.url = url;
	}

	public AttachmentType getAttachmentType()
	{
		return attachmentType;
	}

	public void setAttachmentType(AttachmentType attachmentType)
	{
		this.attachmentType = attachmentType;
	}

	public long getTimestamp()
	{
		return timestamp;
	}

	public void setTimestamp(long timestamp)
	{
		this.timestamp = timestamp;
	}

	public String getCreatedBy()
	{
		return createdBy;
	}

	public void setCreatedBy(String createdBy)
	{
		this.createdBy = createdBy;
	}

	public String getPayload() {
		return payload;
	}

	public void setPayload(String payload) {
		this.payload = payload;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
