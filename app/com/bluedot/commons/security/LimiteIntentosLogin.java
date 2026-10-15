package com.bluedot.commons.security;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import play.Play;

/**
 * Limite de intentos fallidos de login (POST /api/v1/signin), por usuario y por IP, en una ventana de tiempo. Cada login
 * cuesta ~0.5 s de CPU (PasswordHash): sin limite, probar contrasenas sirve para adivinarlas y para cargar el servidor.
 *
 * Solo cuentan los intentos fallidos; un login correcto borra los del usuario. Si en los ultimos ventanaMinutos hubo
 * intentosPorUsuario fallidos para ese usuario o intentosPorIp desde esa IP, segun seguridad.signin.limite:
 * <ul>
 * <li>off: sin limite</li>
 * <li>log (default): se loguea "LOGIN SERIA BLOQUEADO" y se sigue</li>
 * <li>enforce: se rechaza con 429 sin verificar la contrasena</li>
 * </ul>
 * Los contadores estan en memoria (una sola instancia de la API; se pierden al reiniciar).
 */
public class LimiteIntentosLogin {

	final static Logger logger = LoggerFactory.getLogger(LimiteIntentosLogin.class);

	public enum Modo {
		OFF, LOG, ENFORCE
	}

	private final Map<String, Deque<Long>> fallas = new ConcurrentHashMap<String, Deque<Long>>();

	private static final LimiteIntentosLogin INSTANCIA = new LimiteIntentosLogin();

	public static LimiteIntentosLogin instancia() {
		return INSTANCIA;
	}

	/** Si el usuario o la IP superaron el limite, cuantos minutos faltan para poder volver a intentar; si no, 0 */
	public int minutosBloqueado(String usuario, String ip, long ahora, int intentosPorUsuario, int intentosPorIp, int ventanaMinutos) {
		long ventana = ventanaMinutos * 60000L;
		long hasta = Math.max(desbloqueo(clave("u", usuario), intentosPorUsuario, ventana, ahora),
				desbloqueo(clave("ip", ip), intentosPorIp, ventana, ahora));
		return hasta > ahora ? (int) Math.ceil((hasta - ahora) / 60000.0) : 0;
	}

	public void registrarFalla(String usuario, String ip, long ahora) {
		for (String clave : new String[] { clave("u", usuario), clave("ip", ip) })
			if (clave != null) {
				Deque<Long> intentos = fallas.computeIfAbsent(clave, k -> new ArrayDeque<Long>());
				synchronized (intentos) {
					intentos.addLast(ahora);
				}
			}
	}

	/** Login correcto: se olvidan las fallas del usuario (las de la IP siguen contando) */
	public void registrarExito(String usuario) {
		String clave = clave("u", usuario);
		if (clave != null)
			fallas.remove(clave);
	}

	/** Hasta cuando esta bloqueada la clave (0 si no lo esta); de paso descarta las fallas viejas */
	private long desbloqueo(String clave, int maximo, long ventana, long ahora) {
		if (clave == null || maximo <= 0)
			return 0;
		Deque<Long> intentos = fallas.get(clave);
		if (intentos == null)
			return 0;
		synchronized (intentos) {
			for (Iterator<Long> it = intentos.iterator(); it.hasNext();)
				if (it.next() <= ahora - ventana)
					it.remove();
				else
					break;
			if (intentos.isEmpty()) {
				fallas.remove(clave, intentos);
				return 0;
			}
			// Bloqueado mientras haya maximo fallas dentro de la ventana: hasta que la mas vieja de esas salga
			if (intentos.size() < maximo)
				return 0;
			Long[] tiempos = intentos.toArray(new Long[0]);
			return tiempos[tiempos.length - maximo] + ventana;
		}
	}

	private static String clave(String tipo, String valor) {
		return valor == null || valor.trim().isEmpty() ? null : tipo + ":" + valor.trim().toLowerCase();
	}

	public static Modo modo() {
		String valor = Play.application().configuration().getString("seguridad.signin.limite", "log");
		try {
			return Modo.valueOf(valor.trim().toUpperCase());
		} catch (IllegalArgumentException e) {
			logger.error("Valor invalido para seguridad.signin.limite: '" + valor + "', se usa 'log'");
			return Modo.LOG;
		}
	}

	/**
	 * IP del cliente: si viene por el proxy del panel, la ultima de X-Forwarded-For (la que agrega el proxy; las anteriores
	 * las puede mandar cualquiera); si no, la de la conexion
	 */
	public static String ipCliente(play.mvc.Http.Request request) {
		String reenviado = request.getHeader("X-Forwarded-For");
		if (reenviado != null && !reenviado.trim().isEmpty()) {
			String[] ips = reenviado.split(",");
			return ips[ips.length - 1].trim();
		}
		return request.remoteAddress();
	}

	/** Si la IP o el usuario estan bloqueados, segun la configuracion: minutos que faltan (0 = no) */
	public int minutosBloqueado(String usuario, String ip) {
		return minutosBloqueado(usuario, ip, System.currentTimeMillis(), config("seguridad.signin.intentosPorUsuario", 5),
				config("seguridad.signin.intentosPorIp", 20), config("seguridad.signin.ventanaMinutos", 15));
	}

	public static int config(String clave, int defecto) {
		return Play.application().configuration().getInt(clave, defecto);
	}
}
