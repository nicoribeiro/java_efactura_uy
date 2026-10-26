package com.bluedot.efactura.controllers;

import java.io.InputStream;
import java.util.Properties;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.controllers.AbstractController;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.security.Secured;
import com.play4jpa.jpa.db.Tx;

import play.libs.F.Promise;
import play.mvc.Result;
import play.mvc.Security;

/**
 * GET /api/v1/version: version de la API que esta corriendo (version de build.sbt, commit y fecha de compilacion), para
 * mostrarla en el panel. Sale de version.properties, que genera build.sbt al compilar.
 *
 * Lleva @Tx aunque no use la base: Secured busca la sesion con JPA y sin transaccion responde 401.
 */
@ErrorMessage
@Tx
@Security.Authenticated(Secured.class)
public class VersionController extends AbstractController {

	final static Logger logger = LoggerFactory.getLogger(VersionController.class);

	private static final String DESCONOCIDO = "desconocido";

	private static JSONObject version;

	public Promise<Result> getVersion() {
		return json(leer().toString());
	}

	private static synchronized JSONObject leer() {
		if (version == null) {
			Properties props = new Properties();
			try (InputStream in = VersionController.class.getClassLoader().getResourceAsStream("version.properties")) {
				if (in != null)
					props.load(in);
				else
					logger.warn("No esta version.properties en el classpath");
			} catch (Exception e) {
				logger.warn("No se pudo leer version.properties", e);
			}
			JSONObject json = new JSONObject();
			json.put("version", props.getProperty("version", DESCONOCIDO));
			json.put("commit", props.getProperty("commit", DESCONOCIDO));
			json.put("compilado", props.containsKey("compilado") ? props.getProperty("compilado") : JSONObject.NULL);
			version = json;
		}
		return version;
	}
}
