package com.bluedot.commons.security;

import java.util.LinkedList;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;

import com.bluedot.commons.utils.messaging.AttachmentType;
import com.play4jpa.jpa.models.Model;

@Entity
public class Attachment extends Model<Attachment> {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6770091807939153590L;

	@Id
	@GeneratedValue
	private int id;

	private String url;

	@Enumerated(EnumType.STRING)
	private AttachmentType attachmentType;

	@Lob
	private String payload;

	private String createdBy;

	private long timestamp;

	public Attachment() {

	}

	public Attachment(com.bluedot.commons.utils.messaging.Attachment attachment) {
		this.attachmentType = attachment.getAttachmentType();
		this.url = attachment.getUrl();
		this.payload = attachment.getPayload();
		this.createdBy = attachment.getCreatedBy();
		this.timestamp = attachment.getTimestamp();
	}

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}

	public AttachmentType getAttachmentType() {
		return attachmentType;
	}

	public void setAttachmentType(AttachmentType attachmentType) {
		this.attachmentType = attachmentType;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}

	public String getCreatedBy() {
		return createdBy;
	}

	public void setCreatedBy(String createdBy) {
		this.createdBy = createdBy;
	}

	public static com.bluedot.commons.utils.messaging.Attachment convert(Attachment att) {
		com.bluedot.commons.utils.messaging.Attachment attachment = new com.bluedot.commons.utils.messaging.Attachment();
		attachment.setAttachmentType(att.attachmentType);
		attachment.setCreatedBy(att.createdBy);
		attachment.setPayload(att.payload);
		attachment.setTimestamp(att.timestamp);
		attachment.setUrl(att.url);
		return attachment;
	}

	public static List<com.bluedot.commons.utils.messaging.Attachment> convert(List<Attachment> attList) {
		List<com.bluedot.commons.utils.messaging.Attachment> list = new LinkedList<com.bluedot.commons.utils.messaging.Attachment>();
		for (Attachment attachment : attList) {
			list.add(convert(attachment));
		}
		return list;
	}
}
