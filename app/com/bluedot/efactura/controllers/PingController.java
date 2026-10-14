package com.bluedot.efactura.controllers;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.persistence.EntityManager;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.efactura.global.RequestUtils;

import play.Play;
import play.db.jpa.JPA;
import play.mvc.Controller;
import play.mvc.Result;

/**
 * GET /api/v1/ping, para el monitoreo (ej. Zabbix): sin sesion ni credenciales, responde {"ok":true} si la API y la base
 * responden (503 si la base no). No muestra ningun dato.
 *
 * Solo se puede llamar desde las redes de seguridad.ping.redes (por defecto loopback y redes privadas): tienen que estar
 * en ellas la direccion de quien llama y cada IP de X-Forwarded-For, asi lo que llega de internet por el proxy del panel
 * (que agrega la IP real) se rechaza aunque mande un X-Forwarded-For armado.
 */
public class PingController extends Controller {

	final static Logger logger = LoggerFactory.getLogger(PingController.class);

	static final List<String> REDES_DEFECTO = Arrays.asList("127.0.0.0/8", "::1/128", "10.0.0.0/8", "172.16.0.0/12", "192.168.0.0/16");

	public Result ping() {
		List<String> ips = new ArrayList<String>();
		ips.add(request().remoteAddress());
		String reenviado = request().getHeader(RequestUtils.FORWARD_HEADER);
		if (reenviado != null)
			for (String ip : reenviado.split(","))
				if (!ip.trim().isEmpty())
					ips.add(ip.trim());

		List<String> redes = redes();
		for (String ip : ips)
			if (!permitida(ip, redes)) {
				logger.warn("PING rechazado desde " + String.join(", ", ips));
				return forbidden();
			}

		EntityManager em = null;
		try {
			em = JPA.em("default");
			em.createNativeQuery("select 1").getSingleResult();
			return ok("{\"ok\":true}").as("application/json");
		} catch (Throwable t) {
			logger.error("PING: la base no responde", t);
			return status(SERVICE_UNAVAILABLE, "{\"ok\":false}").as("application/json");
		} finally {
			if (em != null)
				em.close();
		}
	}

	private static List<String> redes() {
		try {
			List<String> redes = Play.application().configuration().getStringList("seguridad.ping.redes");
			return redes != null && !redes.isEmpty() ? redes : REDES_DEFECTO;
		} catch (RuntimeException e) {
			return REDES_DEFECTO;
		}
	}

	/** Si la IP esta en alguna de las redes (CIDR, ej. 192.168.0.0/16; una IP sola vale como /32 o /128) */
	static boolean permitida(String ip, List<String> redes) {
		String literal = ip.replaceAll("^\\[|\\]$", "");
		// Solo IPs literales: InetAddress.getByName resolveria un nombre por DNS (y un nombre falso podria apuntar a una IP
		// privada)
		boolean ipv4 = literal.matches("\\d{1,3}(\\.\\d{1,3}){3}");
		boolean ipv6 = literal.contains(":") && literal.matches("[0-9a-fA-F:.]+");
		if (!ipv4 && !ipv6)
			return false;
		try {
			byte[] direccion = InetAddress.getByName(literal).getAddress();
			for (String red : redes) {
				String[] partes = red.trim().split("/");
				byte[] base = InetAddress.getByName(partes[0]).getAddress();
				if (base.length != direccion.length)
					continue;
				int bits = partes.length > 1 ? Integer.parseInt(partes[1]) : base.length * 8;
				boolean coincide = true;
				for (int i = 0; i < bits && coincide; i++) {
					int mascara = 0x80 >> (i % 8);
					coincide = (base[i / 8] & mascara) == (direccion[i / 8] & mascara);
				}
				if (coincide)
					return true;
			}
		} catch (Exception e) {
			// IP mal formada: no se permite
		}
		return false;
	}
}
