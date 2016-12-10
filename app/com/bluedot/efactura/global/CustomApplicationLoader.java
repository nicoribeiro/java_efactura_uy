package com.bluedot.efactura.global;

import com.typesafe.config.ConfigFactory;

import play.ApplicationLoader;
import play.Configuration;
import play.Environment;
import play.inject.guice.GuiceApplicationBuilder;
import play.inject.guice.GuiceApplicationLoader;

/**
 * from http://stackoverflow.com/questions/30787961/configuration-depending-on-launch-mode
 */
public class CustomApplicationLoader extends GuiceApplicationLoader {

    @Override
    public GuiceApplicationBuilder builder(ApplicationLoader.Context context) {
        final Environment environment = context.environment();
        GuiceApplicationBuilder builder = initialBuilder.in(environment);
        Configuration config = context.initialConfiguration();
        
        /*
         * Here we can select a custom Guice Binding for each environment
         */
        if (environment.isTest()) {
            config = merge("secret.conf", config);
            builder = builder.bindings(new Module());
        } else if (environment.isDev()) {
            config = merge("secret.conf", config);
            builder = builder.bindings(new Module());
        } else if (environment.isProd()) {
            config = merge("secret.conf", config);
            builder = builder.bindings(new Module());
        } else {
            throw new IllegalStateException("No such mode.");
        }
        return builder.in(environment).loadConfig(config);
    }

    private Configuration merge(String configName, Configuration currentConfig) {
        return new Configuration(currentConfig.getWrappedConfiguration().$plus$plus(new play.api.Configuration(ConfigFactory.load(configName))));
    }
}

