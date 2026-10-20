package com.bluedot.commons.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Files;

import play.Play;

/**
 * A simple class to read and write files
 * @author nicolasribeiro
 *
 */
public class IO
{

	/**
	 * Resuelve un path relativo contra el directorio raiz de la aplicacion
	 * (Play application path) en lugar del working directory del proceso, que
	 * depende de como se haya levantado (systemd, script, IDE). Los paths
	 * absolutos se devuelven sin cambios.
	 */
	public static File resolve(String path)
	{
		File file = new File(path);
		if (file.isAbsolute())
			return file;
		try {
			return Play.application().getFile(path);
		} catch (RuntimeException e) {
			// No hay aplicacion Play levantada (ej. utilitarios standalone)
			return file;
		}
	}

	/**
	 * Un dato que viene de afuera (RUT, serie, nombre de carpeta) usado como nombre de archivo o carpeta: solo letras,
	 * digitos, '-' y '_'; cualquier otro caracter pasa a '_', asi no puede subir de carpeta ("../") ni ser absoluto.
	 * Vacio o null: "_".
	 */
	public static String segmento(String valor)
	{
		if (valor == null || valor.isEmpty())
			return "_";
		return valor.replaceAll("[^A-Za-z0-9_-]", "_");
	}

	/**
	 * base/partes[0]/partes[1]/... con cada parte pasada por {@link #segmento(String)}, y verificando que el resultado
	 * quede dentro de base (tambien con links simbolicos).
	 */
	public static File dentroDe(File base, String... partes) throws IOException
	{
		File archivo = base;
		for (String parte : partes)
			archivo = new File(archivo, segmento(parte));
		String raiz = base.getCanonicalPath();
		String destino = archivo.getCanonicalPath();
		if (!destino.equals(raiz) && !destino.startsWith(raiz + File.separator))
			throw new IOException("Ruta fuera de " + raiz + ": " + destino);
		return archivo;
	}

	public static String readFile(String path, Charset encoding) throws IOException
	{
		byte[] encoded = Files.readAllBytes(resolve(path).toPath());
		return new String(encoded, encoding);
	}

	public static void writeFile(String filepath, String string) throws IOException 
	{
		File file = resolve(filepath);
		file.getParentFile().mkdirs();
		
		byte dataToWrite[] = string.getBytes();
		FileOutputStream out = new FileOutputStream(file);
		out.write(dataToWrite);
		out.close();

	}

}
