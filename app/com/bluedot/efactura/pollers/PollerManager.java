package com.bluedot.efactura.pollers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.inject.Provider;

import play.Application;
import play.inject.ApplicationLifecycle;
import play.libs.F;

public class PollerManager {

	static Logger logger = LoggerFactory.getLogger(PollerManager.class);

	private static ExecutorService executor = Executors.newFixedThreadPool(5);
	
	public static boolean shutdownInProgress = false;
	
	private Provider<Application> applicationProvider;

	@Inject
	public PollerManager(ApplicationLifecycle lifecycle, Provider<Application> applicationProvider) {

		this.applicationProvider = applicationProvider;
		
		lifecycle.addStopHook(() -> {
			
			shutdownInProgress = true;
			
			executor.shutdown();

			try {
				if (!executor.awaitTermination(5, TimeUnit.MINUTES)) {
					logger.info("Executor is taking too much to finalize, forcing exit now...");
					executor.shutdownNow();
				} else
					logger.info("Executor finished succesfully");
				logger.info("Bye");
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			return F.Promise.pure(null);
		});

	}

	public void queue() {
		Runnable runner; 
		
		if (applicationProvider.get().configuration().getBoolean("EmailEntrantesPoller.run", false)){
			logger.info("EmailEntrantesPoller: running");
			runner = new EmailEntrantesPoller();
			executor.execute(runner);
		}else
			logger.info("EmailEntrantesPoller: not running");
		
		if (applicationProvider.get().configuration().getBoolean("NotificationPoller.run", false)){
			logger.info("NotificationPoller: running");
			runner = new NotificationPoller();
			executor.execute(runner);
		}else
			logger.info("NotificationPoller: not running");
		
	}

}