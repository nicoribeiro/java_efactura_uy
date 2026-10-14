package com.bluedot.commons.security;

import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

/**
 * Hash de contrasenas con sal: PBKDF2-HMAC-SHA256, guardado como texto "pbkdf2-sha256$iteraciones$sal$hash" (sal y
 * hash en Base64). Las iteraciones van en el propio hash, asi se pueden subir sin invalidar los que ya estan guardados.
 */
public class PasswordHash {

	static final String ALGORITMO = "pbkdf2-sha256";

	/** Recomendacion de OWASP para PBKDF2-HMAC-SHA256 */
	static final int ITERACIONES = 600000;

	private static final int BYTES_SAL = 16;

	private static final int BYTES_HASH = 32;

	private static final SecureRandom RANDOM = new SecureRandom();

	/** Hash de una contrasena para comparar cuando el usuario no existe, asi la respuesta tarda lo mismo */
	private static final String HASH_FICTICIO = hash("ficticio");


	public static String hash(String password) {
		return hash(password, ITERACIONES);
	}

	static String hash(String password, int iteraciones) {
		byte[] sal = new byte[BYTES_SAL];
		RANDOM.nextBytes(sal);
		return ALGORITMO + "$" + iteraciones + "$" + Base64.getEncoder().encodeToString(sal) + "$"
				+ Base64.getEncoder().encodeToString(pbkdf2(password, sal, iteraciones, BYTES_HASH));
	}

	/** Si la contrasena corresponde al hash guardado (false si el hash no tiene el formato esperado) */
	public static boolean verificar(String password, String guardado) {
		if (password == null || guardado == null)
			return false;
		String[] partes = guardado.split("\\$");
		if (partes.length != 4 || !ALGORITMO.equals(partes[0]))
			return false;
		try {
			int iteraciones = Integer.parseInt(partes[1]);
			byte[] sal = Base64.getDecoder().decode(partes[2]);
			byte[] esperado = Base64.getDecoder().decode(partes[3]);
			return MessageDigest.isEqual(esperado, pbkdf2(password, sal, iteraciones, esperado.length));
		} catch (IllegalArgumentException e) {
			return false;
		}
	}

	/** Gasta el mismo tiempo que una verificacion real, para cuando el usuario no existe */
	public static void verificarFicticio(String password) {
		verificar(password != null ? password : "", HASH_FICTICIO);
	}

	/** Bytes al azar: valor para la columna vieja sha_password (no nula) que no corresponde a ninguna contrasena */
	public static byte[] bytesAlAzar(int cantidad) {
		byte[] bytes = new byte[cantidad];
		RANDOM.nextBytes(bytes);
		return bytes;
	}

	private static byte[] pbkdf2(String password, byte[] sal, int iteraciones, int bytes) {
		PBEKeySpec spec = new PBEKeySpec(password.toCharArray(), sal, iteraciones, bytes * 8);
		try {
			return SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256").generateSecret(spec).getEncoded();
		} catch (Exception e) {
			throw new IllegalStateException("No se pudo calcular el hash de la contrasena", e);
		} finally {
			spec.clearPassword();
		}
	}

	/** Para comparar el hash viejo (SHA-512 sin sal) sin cortar en el primer byte distinto */
	static boolean igualesSha(byte[] a, byte[] b) {
		return a != null && b != null && MessageDigest.isEqual(a, b);
	}
}
