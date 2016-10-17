package com.bluedot.commons.alerts;

public interface AlertReceiver
{

	public abstract void receiveAlert(Alert alert);
	
	public abstract boolean appliesCustomRulesToAlert(Alert alert);
	

}