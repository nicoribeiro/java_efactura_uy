package com.bluedot.efactura.pollers;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.efactura.microControllers.interfaces.ServiceMicroControllerFactory;
import com.google.inject.Injector;

import play.Play;
import play.db.jpa.JPAApi;
import play.inject.ApplicationLifecycle;
import play.libs.F;

public class PollerManager {

	static Logger logger = LoggerFactory.getLogger(PollerManager.class);

	private static ExecutorService executor = Executors.newFixedThreadPool(5);
	
	public static boolean shutdownInProgress = false;

	private JPAApi jpaApi;
	
	private ServiceMicroControllerFactory serviceMicroControllerFactory;
	
	@Inject
	public PollerManager(JPAApi jpaApi, ApplicationLifecycle lifecycle, ServiceMicroControllerFactory serviceMicroControllerFactory) {

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
		
		this.jpaApi = jpaApi;
		this.serviceMicroControllerFactory = serviceMicroControllerFactory;

	}

	public void queue() {
		Runnable runner; 
		
			logger.info("Encolador de Documentos: running");
			runner = new EmailEntrantesRunner(jpaApi, serviceMicroControllerFactory);
			executor.execute(runner);
		}

}