package com.bluedot.efactura.commons;

import java.util.regex.Pattern;

/**
 * Formatos de DGI para datos que llegan en los CFE recibidos (los elige el emisor). Se controlan antes de crear la
 * empresa emisora: el RUT termina en rutas de archivos (PDF) y en busquedas.
 */
public final class FormatosDgi {

	/** RUC uruguayo: 12 digitos (RUCEmisor en el formato de CFE) */
	private static final Pattern RUC = Pattern.compile("\\d{12}");

	private FormatosDgi() {
	}

	public static boolean esRuc(String valor) {
		return valor != null && RUC.matcher(valor).matches();
	}
}
