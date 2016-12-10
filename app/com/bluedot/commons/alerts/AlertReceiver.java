package com.bluedot.commons.alerts;

import com.bluedot.commons.notificationChannels.MessagingHelper;

public interface AlertReceiver
{

	public abstract void receiveAlert(MessagingHelper messagingHelper, Alert alert);
	
	public abstract boolean appliesCustomRulesToAlert(Alert alert);
	

}