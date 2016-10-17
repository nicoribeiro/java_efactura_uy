package com.bluedot.commons.messages;

import java.util.Date;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.Lob;
import javax.persistence.Transient;

import com.play4jpa.jpa.models.Model;

@Entity
public class Message extends Model<Message>
{

	/**
	 * 
	 */
	private static final long serialVersionUID = 5611551406013653227L;

	@Id
	@GeneratedValue
	private long id;

	@Lob
	private String message;

	private Date generationDate;

	@Transient
	private MessageReceiver fromObject;

	private String fromString;

	@Transient
	private List<MessageReceiver> toObejcts;

	@ElementCollection
	private List<String> toStrings;

	public Message(String message, MessageReceiver from, List<MessageReceiver> toObjects) {
		super();
		this.message = message;
		this.fromObject = from;
		this.fromString = fromObject.getURN();
		toStrings = new LinkedList<String>();
		for (Iterator<MessageReceiver> iterator = toObjects.iterator(); iterator.hasNext();)
		{
			MessageReceiver messageReceiver = iterator.next();
			toStrings.add(messageReceiver.getURN());

		}
		this.toObejcts = toObjects;
		this.generationDate = new Date();
	}

	public Message() {
		super();
	}

	public String getMessage()
	{
		return message;
	}

	public void setMessage(String message)
	{
		this.message = message;
	}

	public Date getGenerationDate()
	{
		return generationDate;
	}

	public void setGenerationDate(Date generationDate)
	{
		this.generationDate = generationDate;
	}

	public MessageReceiver getFrom()
	{
		if (fromObject == null)
			fromObject = MessageHelper.getMessageReceiver(fromString);
		return fromObject;
	}

	public void setFrom(MessageReceiver from)
	{
		this.fromObject = from;
	}

	public List<MessageReceiver> getTo()
	{
		if (toObejcts == null)
			toObejcts = MessageHelper.getMessageReceivers(toStrings);
		return toObejcts;
	}

	public void setTo(List<MessageReceiver> to)
	{
		this.toObejcts = to;
	}

	public String getFromString()
	{
		return fromString;
	}

	public void setFromString(String fromString)
	{
		this.fromString = fromString;
	}

	public List<String> getToStrings()
	{
		return toStrings;
	}

	public void setToStrings(List<String> toStrings)
	{
		this.toStrings = toStrings;
	}

}
