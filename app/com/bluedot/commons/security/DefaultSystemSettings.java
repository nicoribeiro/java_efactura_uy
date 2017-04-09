package com.bluedot.commons.security;

import play.db.jpa.JPAApi;

public class DefaultSystemSettings implements SettingsPrototype
{

	private Settings settings;

	public DefaultSystemSettings() {
		settings = new Settings();
		for (Settings.DefaultSetting defaultSetting : Settings.DefaultSetting.values())
		{
			settings.setSetting(defaultSetting.getKey(), defaultSetting.getValue());
		} 
	}

	@Override
	public Settings getSettings(JPAApi jpaApi)
	{
		if (settings == null)
		{
			settings = new Settings();
			settings.save(jpaApi);
		}
		settings.setParent(null);
		return settings;
	}

}
