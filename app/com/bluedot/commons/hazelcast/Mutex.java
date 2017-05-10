package com.bluedot.commons.hazelcast;

import java.util.LinkedList;
import java.util.List;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.core.ILock;


public class Mutex
{

	final static Logger logger = LoggerFactory.getLogger(Mutex.class);

	private HazelcastFactory hazelcastfactory;
	
	private HazelcastInstance hazelcastClient;
	
	@Inject
	public Mutex(HazelcastFactory hazelcastfactory){
		this.hazelcastfactory = hazelcastfactory;
	}
	

	/**
	 * Simplification of {@link com.bluedot.commons.hazelcast.Mutex#mutex(List,LockPolicy,MutexBlock) mutex} method.
	 */
	public boolean mutex(String key, LockPolicy lockPolicy, MutexBlock mutex) throws APIException
	{
		List<String> keys = new LinkedList<String>();
		keys.add(key);
		return mutex(keys, lockPolicy, mutex);
	}

	/**
	 * Mutual-exclusion method. This method will attempt to acquire a lock on
	 * all keys. If successful mutexblock of code will be executed
	 * 
	 * @param keys the keys to lock-on
	 * @param lockPolicy the policy used to lock on each key
	 * @param mutex the code block
	 * @return true if locks were acquired and code block executed
	 * @throws APIException if the execution of code block fails
	 */
	public boolean mutex(List<String> keys, LockPolicy lockPolicy, MutexBlock mutex) throws APIException
	{
		if (hazelcastClient == null)
			hazelcastClient = hazelcastfactory.getNewHazelcastClient(true, 5);

		String key = keys.get(0);

		logger.info("Trying to obtain Lock on {} ", key);

		ILock lock = hazelcastClient.getLock(key);

		try
		{
			boolean obtainedLock = lock.tryLock(lockPolicy.getUnit(), lockPolicy.getTimeUnit());

			if (obtainedLock)
			{
				try
				{
					logger.info("Obtained lock on {}", key);

					keys.remove(0);

					if (keys.isEmpty())
						mutex.run();
					else
						return mutex(keys, lockPolicy, mutex);

				} finally
				{
					if (obtainedLock)
					{
						lock.unlock();
						logger.info("Released Lock on {} ", key);
					}
				}
			} else
				logger.info("Could not obtain Lock on {} ", key);

			return obtainedLock;

		} catch (InterruptedException e)
		{
			String message = "Thread was interrupted when handling key:" + key; 
			logger.error(message);
			throw APIException.raise(APIErrors.INTERNAL_SERVER_ERROR).setDetailMessage(message);
		}

	}

}
