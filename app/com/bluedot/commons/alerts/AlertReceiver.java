package com.bluedot.commons.alerts;

import com.bluedot.commons.notificationChannels.MessagingHelper;

import play.db.jpa.JPAApi;

public interface AlertReceiver
{

	void receiveAlert(JPAApi jpaApi, MessagingHelper messagingHelper, Alert alert);
	
	boolean appliesCustomRulesToAlert(JPAApi jpaApi, Alert alert);
	

}