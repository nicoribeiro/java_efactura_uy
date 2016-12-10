package com.bluedot.efactura.global;

import com.bluedot.commons.hazelcast.HazelcastFactory;
import com.bluedot.commons.hazelcast.Mutex;
import com.bluedot.commons.microControllers.implementation.AccountMicroControllerDefault;
import com.bluedot.commons.microControllers.interfaces.AccountMicroController;
import com.bluedot.commons.notificationChannels.MessagingHelper;
import com.bluedot.efactura.commons.Commons;
import com.bluedot.efactura.microControllers.implementation.CAEMicroControllerDefault;
import com.bluedot.efactura.microControllers.implementation.CFEMicroControllerDefault;
import com.bluedot.efactura.microControllers.implementation.ServiceMicroControllerDefault;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroController;
import com.bluedot.efactura.microControllers.interfaces.CAEMicroControllerFactory;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroController;
import com.bluedot.efactura.microControllers.interfaces.CFEMicroControllerFactory;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroController;
import com.bluedot.efactura.microControllers.interfaces.ServiceMicroControllerFactory;
import com.bluedot.efactura.pool.WSRecepcionPool;
import com.bluedot.efactura.pool.WSRutPool;
import com.bluedot.efactura.services.ConsultaRutService;
import com.bluedot.efactura.services.IntercambioService;
import com.bluedot.efactura.services.RecepcionService;
import com.bluedot.efactura.services.impl.ConsultaRutServiceImpl;
import com.bluedot.efactura.services.impl.IntercambioServiceImpl;
import com.bluedot.efactura.services.impl.RecepcionServiceImpl;
import com.bluedot.efactura.strategy.builder.CFEBuilder;
import com.bluedot.efactura.strategy.builder.CFEBuilderImpl;
import com.bluedot.efactura.strategy.builder.CFEBuilderImplFactory;
import com.bluedot.efactura.strategy.builder.CFEBuilderProvider;
import com.bluedot.efactura.strategy.builder.CFEBuilderResguardo;
import com.bluedot.efactura.strategy.builder.CFEBuilderResguardoFactory;
import com.bluedot.efactura.strategy.builder.CFEStrategy;
import com.bluedot.efactura.strategy.builder.EResguardoStrategy;
import com.bluedot.efactura.strategy.builder.EResguardoStrategyFactory;
import com.bluedot.efactura.strategy.builder.EfactStrategy;
import com.bluedot.efactura.strategy.builder.EfactStrategyFactory;
import com.bluedot.efactura.strategy.builder.EticketStrategy;
import com.bluedot.efactura.strategy.builder.EticketStrategyFactory;
import com.google.inject.AbstractModule;
import com.google.inject.assistedinject.FactoryModuleBuilder;
import com.play4jpa.jpa.db.TxAction;

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

		/*
		 * Hazelcast
		 */
		bind(HazelcastFactory.class);
		bind(Mutex.class);
		
		/*
		 * Object Pools 
		 */
		bind(WSRecepcionPool.class);
		bind(WSRutPool.class);
		
		/*
		 * Misc
		 */
		bind(Commons.class);
		
		/*
		 * Servicios
		 */
		bind(RecepcionService.class).to(RecepcionServiceImpl.class);
		bind(ConsultaRutService.class).to(ConsultaRutServiceImpl.class);
		bind(IntercambioService.class).to(IntercambioServiceImpl.class);
		
		/*
		 * CFE Builder
		 */
		install(new FactoryModuleBuilder()
			     .implement(CFEBuilder.class, CFEBuilderImpl.class)
			     .build(CFEBuilderImplFactory.class));
		install(new FactoryModuleBuilder()
			     .implement(CFEBuilder.class, CFEBuilderResguardo.class)
			     .build(CFEBuilderResguardoFactory.class));
		
		bind(CFEBuilder.class).toProvider(CFEBuilderProvider.class);
		
		
		/*
		 * MicroControllers Commons
		 */
		bind(AccountMicroController.class).to(AccountMicroControllerDefault.class);
		
		/*
		 * MicroControllers Efactura
		 */
		//CAEMicroControllerDefault necesita Empresa debemos hacer un assitedInject
		install(new FactoryModuleBuilder()
			     .implement(CAEMicroController.class, CAEMicroControllerDefault.class)
			     .build(CAEMicroControllerFactory.class));
		//CFEMicroControllerDefault necesita MODO y Empresa debemos hacer un assitedInject
		install(new FactoryModuleBuilder()
			     .implement(CFEMicroController.class, CFEMicroControllerDefault.class)
			     .build(CFEMicroControllerFactory.class));
		//ServiceMicroControllerDefault necesita Empresa debemos hacer un assitedInject
		install(new FactoryModuleBuilder()
			     .implement(ServiceMicroController.class, ServiceMicroControllerDefault.class)
			     .build(ServiceMicroControllerFactory.class));
		
		
		/*
		 * CFE strategy 
		 */
		install(new FactoryModuleBuilder()
			     .implement(CFEStrategy.class, EfactStrategy.class)
			     .build(EfactStrategyFactory.class));
		install(new FactoryModuleBuilder()
			     .implement(CFEStrategy.class, EResguardoStrategy.class)
			     .build(EResguardoStrategyFactory.class));
		install(new FactoryModuleBuilder()
			     .implement(CFEStrategy.class, EticketStrategy.class)
			     .build(EticketStrategyFactory.class));
		
		/*
		 * play4JPA
		 */
		bind(TxAction.class);
		
		/*
		 * Helpers
		 */
		bind(MessagingHelper.class);
		
	}

}
