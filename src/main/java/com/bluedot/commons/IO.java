package com.bluedot.commons;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * A simple class to read and write files
 * @author nicolasribeiro
 *
 */
public class IO
{

	public static String readFile(String path, Charset encoding) throws IOException
	{
		byte[] encoded = Files.readAllBytes(Paths.get(path));
		return new String(encoded, encoding);
	}

	public static void writeFile(String filepath, String string) throws IOException 
	{
		File file = new File(filepath);
		file.getParentFile().mkdirs();
		
		byte dataToWrite[] = string.getBytes();
		FileOutputStream out = new FileOutputStream(filepath);
		out.write(dataToWrite);
		out.close();

	}

}
