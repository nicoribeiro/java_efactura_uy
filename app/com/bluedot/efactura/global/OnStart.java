package com.bluedot.efactura.global;

import javax.inject.Singleton;

import com.google.inject.Inject;

import play.api.Application;

@Singleton
public class OnStart {

	

	/**
	 * 
	 * @param app to force guice to execute this constructor after the Application startup.
	 */
	@Inject
	public OnStart(Application app) {

		/*
		 * Se supone que el codigo que esta aqui se ejecutaria con la Application de Play 
		 * completamente arriba con todas sus dependencias pero no funciona asi y cualquier 
		 * ejecucion que necesite de base de datos o use algo de la application de play 
		 * tira un error asi:
		 * 
		 * java.lang.RuntimeException: There is no started application
		 * 
		 * @see https://stackoverflow.com/questions/32583155/how-to-run-code-on-startup-in-play-framework-2-4
		 * @see https://stackoverflow.com/questions/31457536/playframework-2-4-run-some-code-after-application-has-started 
		 */
		
	}

	

}
