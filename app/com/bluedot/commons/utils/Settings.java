package com.bluedot.commons.utils;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

/**
 * Settings class gives Higher level methods to JAVA {@link Properties}.
 * Settings can be loaded only from a file, by default try to load from "resources/conf/settings.properties".
 * File path can be setted by setting "settingsPath" System Variable.
 * 
 * @author nicolasribeiro
 *
 */
public class Settings
{

	private static Settings instance;

	private Properties properties;

	private static final String PROPERTIES_PATH = "resources/conf/settings.properties";

	public static Settings getInstance() throws FileNotFoundException, IOException
	{
		if (instance == null)
			instance = new Settings();
		return instance;
	}

	public int getInteger(String str, int defaultVal)
	{

		String totalStr = properties.getProperty(str);
		int result = defaultVal;
		try
		{
			totalStr.trim();
			totalStr.replaceAll(" ", "");
			result = Integer.parseInt(totalStr);
		} catch (Exception e)
		{
		}
		return result;
	}

	public int getInteger(String str)
	{

		String totalStr = properties.getProperty(str);
		int result = -1;
		try
		{
			totalStr.trim();
			totalStr.replaceAll(" ", "");
			result = Integer.parseInt(totalStr);
		} catch (Exception e)
		{
		}
		return result;
	}

	public String getString(String str, String defaultVal)
	{

		String tmp = defaultVal;
		try
		{
			if (properties.getProperty(str) != null)
				tmp = properties.getProperty(str);
		} catch (Exception e)
		{
		}
		return tmp;
	}

	public String getString(String str)
	{

		String tmp = null;
		try
		{
			if (properties.getProperty(str) != null)
				tmp = properties.getProperty(str);
		} catch (Exception e)
		{
		}
		return tmp;
	}

	public long getLong(String key, long defaultVal)
	{
		String tmp = properties.getProperty(key);
		long result = defaultVal;
		try
		{
			result = Long.parseLong(tmp);
		} catch (Exception e)
		{
		}
		return result;
	}

	public long getLong(String key)
	{
		String tmp = properties.getProperty(key);
		long result = -1;
		try
		{
			result = Long.parseLong(tmp);
		} catch (Exception e)
		{
		}
		return result;
	}

	private Settings() throws FileNotFoundException, IOException {

		properties = new Properties();
		if (System.getProperty("settingsPath")!=null)
			properties.load(new FileInputStream(System.getProperty("settingsPath")));
		else
			properties.load(new FileInputStream(PROPERTIES_PATH));

	}

	public boolean getBooleanProperty(String key, boolean defaultVal)
	{
		String tmp = properties.getProperty(key);
		boolean result = defaultVal;
		try
		{
			result = Boolean.parseBoolean(tmp);
		} catch (Exception e)
		{
		}
		return result;
	}

}
