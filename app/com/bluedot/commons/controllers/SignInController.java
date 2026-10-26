package com.bluedot.commons.controllers;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.microControllers.factory.MicroControllerFactoryDefault;
import com.bluedot.commons.microControllers.factory.MicroControllersFactory;
import com.bluedot.commons.microControllers.interfaces.AccountMicroController.SignUpConfigurator;
import com.bluedot.commons.security.Address;
import com.bluedot.commons.security.LimiteIntentosLogin;
import com.bluedot.commons.security.Session;
import com.bluedot.commons.security.User;
import com.bluedot.commons.security.User.Role;
import com.bluedot.commons.security.ValidateJsonPost;
import com.bluedot.commons.serializers.JSONSerializerProvider;
import com.bluedot.efactura.global.RequestUtils;
import com.fasterxml.jackson.databind.JsonNode;
import com.play4jpa.jpa.db.Tx;

import play.i18n.Messages;
import play.libs.F.Promise;
import play.mvc.BodyParser;
import play.mvc.Result;

@ErrorMessage
public class SignInController extends AbstractController
{
	
	final static Logger logger = LoggerFactory.getLogger(SignInController.class);

	@BodyParser.Of(BodyParser.Json.class)
	@ValidateJsonPost(fields = { "emailAddress", "password" })
	@Tx
	public Promise<Result> signIn() throws APIException
	{
		JsonNode json = request().body().asJson();
		String emailAddress = json.findPath("emailAddress").textValue();
		String password = json.findPath("password").textValue();

		// Limite de intentos fallidos por usuario y por IP (ver LimiteIntentosLogin): antes de verificar la contrasena
		LimiteIntentosLogin limite = LimiteIntentosLogin.instancia();
		LimiteIntentosLogin.Modo modoLimite = LimiteIntentosLogin.modo();
		String ip = LimiteIntentosLogin.ipCliente(request());
		long ahora = System.currentTimeMillis();
		if (modoLimite != LimiteIntentosLogin.Modo.OFF) {
			int minutos = limite.minutosBloqueado(emailAddress, ip, ahora,
					LimiteIntentosLogin.config("seguridad.signin.intentosPorUsuario", 5),
					LimiteIntentosLogin.config("seguridad.signin.intentosPorIp", 20),
					LimiteIntentosLogin.config("seguridad.signin.ventanaMinutos", 15));
			if (minutos > 0) {
				logger.warn((modoLimite == LimiteIntentosLogin.Modo.ENFORCE ? "LOGIN BLOQUEADO" : "LOGIN SERIA BLOQUEADO") + ": usuario "
						+ emailAddress + " desde " + ip + " (demasiados intentos fallidos, " + minutos + " min)");
				if (modoLimite == LimiteIntentosLogin.Modo.ENFORCE)
					throw APIException.raise(APIErrors.DEMASIADOS_INTENTOS).withParams(minutos);
			}
		}

		// Antes un email "admin:otro" con el token+secreto HMAC del admin entraba como cualquier usuario (sin verificar que el
		// admin estuviera activo). No se usaba: se saco
		User user = User.findByEmailAddressAndPassword(emailAddress, password);

		if (user == null)
			limite.registrarFalla(emailAddress, ip, ahora);
		else
			limite.registrarExito(emailAddress);

		if (user != null && !user.isActivo())
		{
			logger.info("Login rechazado: usuario desactivado " + user.getEmailAddress());
			throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Usuario desactivado");
		}

		if (user != null)
		{
			Session session = new Session(user, RequestUtils.requestAddress(request()), request().getHeader("User-Agent"));
			session.save();

			//TODO hacer db flush antes de responder con la session
			
//			Account masterAccount = user.getMasterAccount();
//			if (masterAccount == null)
//				throw APIException.raise(APIErrors.ACCOUNT_NOT_FOUND);

			//TODO mover esta validacion a accountAction
//			if (masterAccount.getValidated() == null || !masterAccount.getValidated())
//				throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage("Account needs validation");

			//TODO mover esto a un serializer como la gente
//			JSONSerializer serializer = new JSONSerializer()
//					.include("token", "creationTimestamp", "valid", "user.firstName", "user.lastName", "user.emailAddress", "user.role")
//					.include("user.masterAccount", "user.masterAccount.users", "user.masterAccount.tags", "user.masterAccount.invites", "user.masterAccount.gateways.name",
//							"user.masterAccount.gateways.gatewayId", "user.permissions", "user.addresses", "user.accounts.id", "user.accounts.owner.firstName", "user.accounts.owner.lastName",
//							"user.accounts.companyName").exclude("user.masterAccount.gateways.*", "user.masterAccount.users.masterAccount", "user.accounts.*", "*.class").prettyPrint(true);
			JSONObject result = JSONSerializerProvider.getSessionSerializer().objectToJson(session);
			// El panel lo usa para pedir el cambio de una contrasena temporal antes de seguir
			result.put("debeCambiarPassword", user.isDebeCambiarPassword());
			// Y para pedir que confirme el email (usuarios nuevos) antes que nada
			result.put("debeConfirmarEmail", user.isDebeConfirmarEmail() && !user.isValidated());
			result.put("emailConfirmado", user.isValidated());
			
			
			return json(result.toString());
		} else
			throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage(Messages.get("bad_credentials"));

	}

	@BodyParser.Of(BodyParser.Json.class)
	@ValidateJsonPost(fields = { "firstName", "lastName", "emailAddress", "password" })
	@Tx
	public  Promise<Result> signUp() throws APIException
	{
		/*
		 * Registro cerrado: los usuarios los crea un ADMIN (POST /api/v1/usuarios). Solo se permite sin ningun ADMIN, para
		 * crear el primero en una instalacion nueva.
		 */
		if (User.find(Role.ADMIN).size() > 0)
			throw APIException.raise(APIErrors.SOLO_ADMIN).setDetailMessage("El registro está cerrado: los usuarios los crea un administrador");

		JsonNode userJson = request().body().asJson();

		String emailAddress = userJson.findPath("emailAddress").textValue();
		String password = userJson.findPath("password").textValue();
		String firstName = userJson.findPath("firstName").textValue();
		String lastName = userJson.findPath("lastName").asText();
		String companyName = userJson.has("companyName") ? userJson.findPath("companyName").asText() : null;
		String phone=userJson.findPath("phone").textValue();
		
		List<Address> addresses = new ArrayList<Address>();
		
		if (userJson.has("address"))
		{
			JsonNode addressNode = userJson.get("address");
			Address address = Address.fromJson(addressNode);
			addresses.add(address);
		}

		MicroControllersFactory microControllersFactory =  new MicroControllerFactoryDefault();
		
		SignUpConfigurator signUpConfigurator;
		
		/*
		 * If there is not any ADMIN on the system assume next user is an ADMIN   
		 */
		if (User.find(Role.ADMIN).size()==0)
			signUpConfigurator = new SignUpConfigurator(false, true, true, false, request().host(), Role.ADMIN);
		else
			signUpConfigurator = new SignUpConfigurator(false, false, false, false, request().host(), Role.USER);
		
		microControllersFactory.getAccountController().signUp(emailAddress, password, firstName, lastName, companyName, null, addresses, phone, signUpConfigurator);

		return Promise.<Result> pure(created());
	}

}
