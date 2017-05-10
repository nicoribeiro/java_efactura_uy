package com.bluedot.commons.hazelcast;

import java.util.concurrent.TimeUnit;

public enum LockPolicy {
	NO_WAIT(0,TimeUnit.SECONDS), LOW_WAIT(5,TimeUnit.SECONDS), STANDARD_WAIT(10,TimeUnit.SECONDS), BIG_WAIT(60,TimeUnit.SECONDS);
	
	private TimeUnit timeUnit;
	private int unit;
	
	private LockPolicy(int unit, TimeUnit timeUnit){
		this.timeUnit = timeUnit;
		this.unit = unit;
	}

	public TimeUnit getTimeUnit()
	{
		return timeUnit;
	}

	public int getUnit()
	{
		return unit;
	}
	
}
