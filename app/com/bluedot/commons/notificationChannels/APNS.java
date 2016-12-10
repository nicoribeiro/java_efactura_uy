package com.bluedot.commons.notificationChannels;

import javax.persistence.Entity;

import com.bluedot.commons.alerts.Alert;
import com.bluedot.commons.messages.Message;

@Entity
public class APNS extends NotificationChannel
{

	public APNS(String description) {
		super(description);
	}

	@Override
	public void sendNotification(MessagingHelper messagingHelper, Alert alert)
	{
		
	}

	@Override
	public void test(MessagingHelper messagingHelper)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendValidationKey(MessagingHelper messagingHelper, String host)
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	protected String generateValidationKey()
	{
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void sendMessage(MessagingHelper messagingHelper, Message message)
	{
		// TODO Auto-generated method stub
		
	}

}
