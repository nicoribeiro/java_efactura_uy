package com.bluedot.commons.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.ManyToOne;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.play4jpa.jpa.models.DefaultQuery;
import com.play4jpa.jpa.models.Finder;
import com.play4jpa.jpa.models.Model;

/**
 * Codigo de 6 digitos que se manda por mail para confirmar el email de un usuario o para recuperar la contrasena. Se
 * guarda solo el hash (SHA-256 con sal); vale VIGENCIA_MINUTOS, se usa una sola vez y admite MAXIMO_INTENTOS
 * equivocados. Pedir uno nuevo anula los anteriores del mismo tipo.
 */
@Entity
public class CodigoEmail extends Model<CodigoEmail> {

	private static final long serialVersionUID = 1L;

	public enum Tipo {
		CONFIRMAR_EMAIL, RECUPERAR_PASSWORD
	}

	public static final int VIGENCIA_MINUTOS = 15;

	public static final int MAXIMO_INTENTOS = 5;

	/** Para no mandar mails seguidos: un codigo por minuto y por tipo, y como mucho MAXIMO_POR_HORA por hora */
	static final int SEGUNDOS_ENTRE_CODIGOS = 60;

	static final int MAXIMO_POR_HORA = 5;

	private static final SecureRandom RANDOM = new SecureRandom();

	@Id
	@GeneratedValue
	private long id;

	@ManyToOne(fetch = FetchType.LAZY)
	private User user;

	@Enumerated(EnumType.STRING)
	private Tipo tipo;

	/** Base64(sal) + "$" + hex(SHA-256(sal + codigo)) */
	private String hash;

	private Date creado;

	private Date vence;

	private int intentos;

	private boolean usado;

	public CodigoEmail() {
	}

	public static Finder<Long, CodigoEmail> find = new Finder<Long, CodigoEmail>(Long.class, CodigoEmail.class);

	/**
	 * Crea un codigo nuevo para el usuario (anula los anteriores del mismo tipo) y lo devuelve en claro para mandarlo por
	 * mail; no se guarda en claro.
	 *
	 * @throws APIException
	 *             DEMASIADOS_INTENTOS si se pidio otro hace menos de un minuto o ya se pidieron MAXIMO_POR_HORA en la hora
	 */
	public static String emitir(User user, Tipo tipo, Date ahora) throws APIException {
		List<CodigoEmail> anteriores = delUsuario(user, tipo);
		int ultimaHora = 0;
		for (CodigoEmail c : anteriores) {
			if (c.creado != null && ahora.getTime() - c.creado.getTime() < SEGUNDOS_ENTRE_CODIGOS * 1000L)
				throw APIException.raise(APIErrors.DEMASIADOS_INTENTOS).withParams(1);
			if (c.creado != null && ahora.getTime() - c.creado.getTime() < 3600000L)
				ultimaHora++;
		}
		if (ultimaHora >= MAXIMO_POR_HORA)
			throw APIException.raise(APIErrors.DEMASIADOS_INTENTOS).withParams(60);

		for (CodigoEmail c : anteriores)
			if (!c.usado) {
				c.usado = true;
				c.update();
			}

		String codigo = String.format("%06d", RANDOM.nextInt(1000000));
		CodigoEmail nuevo = new CodigoEmail();
		nuevo.user = user;
		nuevo.tipo = tipo;
		nuevo.hash = hash(codigo);
		nuevo.creado = ahora;
		nuevo.vence = new Date(ahora.getTime() + VIGENCIA_MINUTOS * 60000L);
		nuevo.save();
		return codigo;
	}

	/**
	 * Verifica el codigo contra el ultimo vigente del usuario; si es correcto queda usado.
	 *
	 * @throws APIException
	 *             UNAUTHORIZED con el motivo (vencido, sin intentos, incorrecto y cuantos quedan)
	 */
	public static void verificar(User user, Tipo tipo, String codigo, Date ahora) throws APIException {
		CodigoEmail vigente = null;
		for (CodigoEmail c : delUsuario(user, tipo))
			if (!c.usado) {
				vigente = c;
				break;
			}
		if (vigente == null || vigente.vence == null || !ahora.before(vigente.vence))
			throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("El código venció o no existe: pedí uno nuevo");
		if (vigente.intentos >= MAXIMO_INTENTOS)
			throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Se agotaron los intentos de este código: pedí uno nuevo");
		if (codigo == null || !coincide(vigente.hash, codigo.trim())) {
			vigente.intentos++;
			vigente.update();
			int quedan = MAXIMO_INTENTOS - vigente.intentos;
			throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage(
					quedan > 0 ? "Código incorrecto (quedan " + quedan + " intento" + (quedan == 1 ? "" : "s") + ")"
							: "Código incorrecto. Se agotaron los intentos: pedí uno nuevo");
		}
		vigente.usado = true;
		vigente.update();
	}

	/** Del usuario y tipo, del mas nuevo al mas viejo */
	private static List<CodigoEmail> delUsuario(User user, Tipo tipo) {
		DefaultQuery<CodigoEmail> q = (DefaultQuery<CodigoEmail>) find.query();
		q.getCriteria().add(Restrictions.eq("user", user));
		q.getCriteria().add(Restrictions.eq("tipo", tipo));
		q.getCriteria().addOrder(Order.desc("creado"));
		q.getCriteria().addOrder(Order.desc("id"));
		return q.findList();
	}

	static String hash(String codigo) {
		byte[] sal = new byte[16];
		RANDOM.nextBytes(sal);
		return Base64.getEncoder().encodeToString(sal) + "$" + sha256(sal, codigo);
	}

	static boolean coincide(String guardado, String codigo) {
		String[] partes = guardado != null ? guardado.split("\\$") : new String[0];
		if (partes.length != 2)
			return false;
		byte[] sal = Base64.getDecoder().decode(partes[0]);
		return MessageDigest.isEqual(partes[1].getBytes(StandardCharsets.UTF_8), sha256(sal, codigo).getBytes(StandardCharsets.UTF_8));
	}

	private static String sha256(byte[] sal, String codigo) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-256");
			md.update(sal);
			byte[] h = md.digest(codigo.getBytes(StandardCharsets.UTF_8));
			StringBuilder hex = new StringBuilder();
			for (byte b : h)
				hex.append(String.format("%02x", b));
			return hex.toString();
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
}
