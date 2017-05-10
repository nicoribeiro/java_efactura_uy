package com.bluedot.commons.security;

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
	public Settings getSettings()
	{
		if (settings == null)
		{
			settings = new Settings();
			settings.save();
		}
		settings.setParent(null);
		return settings;
	}

}
