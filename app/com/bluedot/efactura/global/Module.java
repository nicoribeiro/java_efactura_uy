package com.bluedot.efactura.global;

import com.bluedot.commons.hazelcast.HazelcastFactory;
import com.bluedot.commons.hazelcast.Mutex;
import com.google.inject.AbstractModule;

/**
 * Esta clase configura guice. Guice es un framework de dependency injection.
 * 
 * @see https://github.com/google/guice
 * 
 * @author nicoribeiro
 *
 */
public class Module extends AbstractModule {

	@Override
	protected void configure() {
		bind(OnStart.class).asEagerSingleton();

		bind(HazelcastFactory.class);

		bind(Mutex.class);
	}

}
