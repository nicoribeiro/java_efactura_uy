package com.bluedot.commons.utils;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.db.jpa.JPA;

public class ThreadMan
{

	static Logger logger = LoggerFactory.getLogger(ThreadMan.class);

	public static void hold(long millis)
	{
		logger.info(Thread.currentThread().getName() + ": HOLDING for " + millis / 1000 + " seconds ... ");
		try
		{
			Thread.sleep(millis);
		} catch (InterruptedException e)
		{
		}
	}
	
	public static void forceTransactionFlush()
	{
		forceTransactionFlush(false);
	}
	
	public static void forceTransactionFlush(boolean clear)
	{
		if (JPA.em().getTransaction().isActive())
		{
			logger.debug("Commiting and flushing current transaction.");
			JPA.em().getTransaction().commit();
			if(clear)
				JPA.em().clear();
			JPA.em().getTransaction().begin();
		}else
			logger.info("No transaction is active, forceTransactionFlush() did nothing.");
	}
}
