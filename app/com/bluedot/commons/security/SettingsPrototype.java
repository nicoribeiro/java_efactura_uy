package com.bluedot.commons.security;

import play.db.jpa.JPAApi;

public interface SettingsPrototype
{

	Settings getSettings(JPAApi jpaApi);
	
}
