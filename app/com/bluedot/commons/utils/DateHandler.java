package com.bluedot.commons.utils;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
/**
 * Simple Date Class 
 * @author nicolasribeiro
 *
 */
public class DateHandler
{

	/**
	 * Get System Date
	 * @return a <b>STRING</b> with this format: "dd-MM-yyyy"
	 */
	public static String nowDate()
	{
		Date ahora = new Date();
		SimpleDateFormat formateador = new SimpleDateFormat("dd-MM-yyyy");
		return formateador.format(ahora);
	}
	
	/**
	 * Get System Date
	 * @return a <b>STRING</b> with the provided format
	 */
	public static String nowDate(SimpleDateFormat sdf)
	{
		Date ahora = new Date();
		return sdf.format(ahora);
	}
	
	
	

	/**
	 * Get System Time 
	 * @return a <b>STRING</b> with this format: "hh:mm:ss"
	 */
	public static String nowTime()
	{
		Date ahora = new Date();
		SimpleDateFormat formateador = new SimpleDateFormat("hh:mm:ss");
		return formateador.format(ahora);
	}

	/**
	 * Add units to a date
	 * @param fch base date
	 * @param value 
	 * @param unit unit of the given value
	 * @return base date plus value units
	 */
	public static java.sql.Timestamp add(java.util.Date fch, int value, int unit)
	{
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(fch.getTime());
		cal.add(unit, value);
		
		return new java.sql.Timestamp(cal.getTimeInMillis());
	}

	/**
	 * Subtracts units to a date
	 * @param fch base date
	 * @param value 
	 * @param unit unit of the given value
	 * @return base date minus value units
	 */
	public static synchronized java.sql.Date minus(java.util.Date fch, int value, int unit)
	{
		Calendar cal = new GregorianCalendar();
		cal.setTimeInMillis(fch.getTime());
		cal.add(unit, -value);
		return new java.sql.Date(cal.getTimeInMillis());
	}

	/**
	 * Difference between dates
	 * @param fechaInicial init date
	 * @param fechaFinal end date
	 * @return difference between "init" and "end" in days
	 */
	public static int diff(Date fechaInicial, Date fechaFinal)
	{

		DateFormat df = DateFormat.getDateInstance(DateFormat.MEDIUM);
		String fechaInicioString = df.format(fechaInicial);
		try
		{
			fechaInicial = df.parse(fechaInicioString);
		} catch (ParseException ex)
		{
		}

		String fechaFinalString = df.format(fechaFinal);
		try
		{
			fechaFinal = df.parse(fechaFinalString);
		} catch (ParseException ex)
		{
		}

		long fechaInicialMs = fechaInicial.getTime();
		long fechaFinalMs = fechaFinal.getTime();
		long diferencia = fechaFinalMs - fechaInicialMs;
		double dias = Math.floor(diferencia / (1000 * 60 * 60 * 24));
		return ((int) dias);
	}

	/**
	 * Converts from String to Date 
	 * @param fecha date to convert
	 * @param formatoDelTexto textFormat, if null assumes "dd-MM-yyyy"
	 * @return
	 */
	public static java.util.Date fromStringToDate(String fecha, SimpleDateFormat formatoDelTexto)
	{
		if (formatoDelTexto == null)
			formatoDelTexto = new SimpleDateFormat("dd-MM-yyyy");
		Date fechaEnviar = null;
		try
		{
			fechaEnviar = formatoDelTexto.parse(fecha);
			return fechaEnviar;
		} catch (ParseException ex)
		{
			ex.printStackTrace();
			return null;
		}
	}

	/**
	 * Converts from Date to String 
	 * @param fecha date to convert
	 * @param formatoDelTexto textFormat, if null assumes "dd-MM-yyyy"
	 * @return
	 */
	public static String fromDateToString(Date fecha, SimpleDateFormat formatoDelTexto)
	{
		if (formatoDelTexto == null)
			formatoDelTexto = new SimpleDateFormat("dd-MM-yyyy");
		String fechaEnviar = null;
		fechaEnviar = formatoDelTexto.format(fecha);
		return fechaEnviar;

	}

	/**
	 * Returns epoch date format from java.util.Date
	 * @param date
	 * @return
	 */
	public static Long fromDateToUnix(Date date)
	{
		return date.getTime() / 1000;
	}

	/**
	 * Returns java.util.Date from epoch format
	 * @param epoch
	 * @return
	 */
	public static synchronized java.util.Date fromUnixToDate(Long epoch)
	{
		Date date = new Date();
		date.setTime((long) epoch * 1000);
		return date;
	}

}
