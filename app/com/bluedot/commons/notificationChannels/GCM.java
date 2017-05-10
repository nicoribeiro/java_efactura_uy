package com.bluedot.commons.notificationChannels;

import javax.persistence.Entity;

import com.bluedot.commons.alerts.Alert;
import com.bluedot.commons.messages.Message;

@Entity
public class GCM extends NotificationChannel
{

	public GCM(String description) {
		super(description);
	}

	@Override
	public void sendNotification(Alert alert)
	{
		
	}

	@Override
	public void test()
	{
		// TODO Auto-generated method stub
		
	}

	@Override
	public void sendValidationKey(String host)
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
	public void sendMessage(Message message)
	{
		// TODO Auto-generated method stub
		
	}

}
