package com.bluedot.commons.hazelcast;

public enum MutexKey {

	GATEWAY_ID("gatewayId"), PROPERTY_ID("propertyId"), RESERVATION_ID("reservationId");

	public String key;

	private MutexKey(String key) {
		this.key = key;
	}

	public String getKey(String value)
	{
		return key + "-" + value;
	}
	
	public String getKey(int value)
	{
		return key + "-" + value;
	}
	
	public String getKey(long value)
	{
		return key + "-" + value;
	}

}
