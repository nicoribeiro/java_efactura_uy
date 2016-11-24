package com.bluedot.commons.notificationChannels;

import java.util.Random;

import javax.persistence.Entity;

import com.bluedot.commons.alerts.Alert;
import com.bluedot.commons.messages.Message;

import play.i18n.Messages;

@Entity
public class SMS extends NotificationChannel
{


	public SMS() {

	}

	public SMS(String description, String phone) {
		super(description);
		this.phone = phone;
	}

	private String phone;

	@Override
	public void sendNotification(Alert alert)
	{
		if (getValidated() && getEnabled())
		{
			new MessagingHelper().withPlayConfig().sendSMS(phone, alert.getBody(), alert.getSubject(), alert.getAttachment());
		}
	}

	@Override
	public void test()
	{
		if (getValidated() && getEnabled())
		{
			new MessagingHelper().withPlayConfig().sendSMS(phone, "Test SMS", "Test SMS", null);
		}
	}

	@Override
	public void sendValidationKey(String arg)
	{
		new MessagingHelper().withPlayConfig().sendSMS(phone, "Confirmation CODE: " + getValidationKey(), "Confirmation CODE", null);

	}

	public String getPhone()
	{
		return phone;
	}

	public void setPhone(String phone)
	{
		this.phone = phone;
	}

	@Override
	protected String generateValidationKey()
	{
		Random r = new Random(System.currentTimeMillis());
		return String.valueOf((1 + r.nextInt(9)) * 10000 + r.nextInt(10000));
	}

	@Override
	public void sendMessage(Message message)
	{
		if (getValidated() && getEnabled())
		{
			new MessagingHelper().withPlayConfig().sendSMS(phone, message.getMessage(), Messages.get("messages_new_message_title"), message.getFromString());
		}
	}
}
