package com.bluedot.commons.alerts;

import java.util.List;
import java.util.Map;


public interface AlertAccept
{

	List<AlertReceiver> getAlertRecivers(Alert alert);
	
	abstract Map<Integer,AlertMetadata> getPosibleAlertMetadatas();
	
	AlertMap getAlertMap();
	
}
