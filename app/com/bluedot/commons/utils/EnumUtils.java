package com.bluedot.commons.utils;

public class EnumUtils
{
	public static <E extends Enum<E>> E valueOf(String s, Class<E> e){
		try{
			return Enum.valueOf(e, s);
		}catch(IllegalArgumentException ex){
			ex.printStackTrace();
		}
		return null;
	}
}
