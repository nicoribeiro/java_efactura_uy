package com.bluedot.commons.security;

import java.util.Date;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;

/**
 * Expiracion de los tokens de sesion (AUTH-TOKEN / api_key). Las credenciales HMAC no se ven afectadas.
 *
 * Una sesion vence por inactividad (seguridad.sesion.inactividadMinutos desde el ultimo uso, default 480) o por
 * duracion maxima (seguridad.sesion.duracionMaximaHoras desde el login, default 168). Segun
 * seguridad.sesion.expiracion:
 * <ul>
 * <li>off: los tokens no vencen (comportamiento anterior)</li>
 * <li>log: se loguea "SESION SERIA RECHAZADA" sin rechazar</li>
 * <li>enforce (default, tambien si falta o es invalido): se rechaza con 401</li>
 * </ul>
 */
public class ExpiracionSesion {

	static final Logger logger = LoggerFactory.getLogger(ExpiracionSesion.class);

	public enum Modo {
		OFF, LOG, ENFORCE
	}

	/** El ultimo acceso se guarda como mucho una vez cada este intervalo, para no escribir en cada request */
	private static final long INTERVALO_REGISTRO_ACCESO_MS = 5 * 60 * 1000;

	public static Modo modo() {
		String valor = Play.application().configuration().getString("seguridad.sesion.expiracion", "enforce");
		try {
			return Modo.valueOf(valor.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			logger.error("Valor invalido para seguridad.sesion.expiracion: '" + valor + "', se usa 'enforce'");
			return Modo.ENFORCE;
		}
	}

	private static long inactividadMs() {
		return Play.application().configuration().getLong("seguridad.sesion.inactividadMinutos", 480l) * 60 * 1000;
	}

	private static long duracionMaximaMs() {
		return Play.application().configuration().getLong("seguridad.sesion.duracionMaximaHoras", 168l) * 60 * 60 * 1000;
	}

	public static long inactividadMinutos() {
		return inactividadMs() / (60 * 1000);
	}

	public static long duracionMaximaHoras() {
		return duracionMaximaMs() / (60 * 60 * 1000);
	}

	/**
	 * @return el motivo por el que la sesion esta vencida, o null si esta vigente
	 */
	public static String motivoVencimiento(Session session, Date ahora) {
		Date creacion = session.getCreationTimestamp();
		if (creacion != null && ahora.getTime() - creacion.getTime() > duracionMaximaMs())
			return "duracion maxima superada (creada " + creacion + ")";

		Date ultimoUso = session.getLastAccess() != null ? session.getLastAccess() : creacion;
		if (ultimoUso != null && ahora.getTime() - ultimoUso.getTime() > inactividadMs())
			return "inactiva desde " + ultimoUso;

		return null;
	}

	/**
	 * Verifica la sesion y registra el acceso.
	 *
	 * @return false si la sesion esta vencida y el modo es enforce (hay que rechazar el request)
	 */
	public static boolean verificar(Session session, String operacion) {
		Modo modo = modo();
		if (modo == Modo.OFF)
			return true;

		Date ahora = new Date();
		String motivo = motivoVencimiento(session, ahora);

		if (motivo != null) {
			String usuario = session.getUser() != null ? session.getUser().getEmailAddress() : "desconocido";
			logger.warn((modo == Modo.ENFORCE ? "SESION RECHAZADA" : "SESION SERIA RECHAZADA") + ": usuario " + usuario
					+ ", token " + prefijo(session.getToken()) + "..., " + motivo + " (" + operacion + ")");
			if (modo == Modo.ENFORCE)
				return false;
		}

		registrarAcceso(session, ahora);
		return true;
	}

	private static void registrarAcceso(Session session, Date ahora) {
		Date ultimoUso = session.getLastAccess();
		if (ultimoUso != null && ahora.getTime() - ultimoUso.getTime() < INTERVALO_REGISTRO_ACCESO_MS)
			return;
		try {
			session.setLastAccess(ahora);
			session.update();
		} catch (RuntimeException e) {
			// Fuera de una transaccion (controladores sin @Tx) no se puede guardar; no debe impedir el request
			logger.debug("No se pudo registrar el ultimo acceso de la sesion", e);
		}
	}

	/** Solo el comienzo del token en los logs: el token completo es una credencial */
	private static String prefijo(String token) {
		return token == null ? "" : token.substring(0, Math.min(8, token.length()));
	}
}
