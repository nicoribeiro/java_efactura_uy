package com.bluedot.commons.alerts;

import java.util.List;
import java.util.Map;

import play.db.jpa.JPAApi;


public interface AlertAccept
{

	List<AlertReceiver> getAlertRecivers(JPAApi jpaApi, Alert alert);
	
	abstract Map<Integer,AlertMetadata> getPosibleAlertMetadatas();
	
	AlertMap getAlertMap();
	
}
