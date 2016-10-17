package com.bluedot.commons.utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.security.Secured;

public class TimeGauge
{
	final static Logger logger = LoggerFactory.getLogger(Secured.class);
	
	private static long currentStartTime;
	private static long lastTickTime;
	
	public static void reset()
	{
		currentStartTime = System.currentTimeMillis();
	}
	
	public static void tick()
	{
		tick(null);
	}
	
	public static void tick(String tag)
	{
		long currentTime = System.currentTimeMillis();
		logger.info((tag!=null?tag:"Tick")+" time: "+(currentTime-lastTickTime)+"ms. Time since last reset: "+(currentTime-currentStartTime)+"ms.");
		lastTickTime = currentTime;
	}
	
}
