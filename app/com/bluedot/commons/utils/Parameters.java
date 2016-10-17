package com.bluedot.commons.utils;


import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class Parameters
{

	private static Parameters instance;
	private Properties properties;

	public static Parameters getInstance()
	{
		if (instance == null)
			instance = new Parameters();
		return instance;
	}

	public int getIntegerProperty(String str, int defaultVal)
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

	public String getStringProperty(String str, String defaultVal)
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

	public long getLongProperty(String key, long defaultVal)
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

	private Parameters() {
		try
		{
			properties = new Properties();
			
			InputStream is = play.Play.application().resourceAsStream("parameters.properties");
			
			properties.load(is);
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
		} catch (IOException e)
		{
			e.printStackTrace();
		}

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
