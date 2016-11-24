package com.bluedot.commons.controllers;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.bluedot.commons.error.APIException;
import com.bluedot.commons.error.APIException.APIErrors;
import com.bluedot.commons.error.ErrorMessage;
import com.bluedot.commons.microControllers.factory.MicroControllerFactoryDefault;
import com.bluedot.commons.microControllers.factory.MicroControllersFactory;
import com.bluedot.commons.microControllers.interfaces.AccountMicroController.SignUpConfigurator;
import com.bluedot.commons.security.Address;
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
	public CompletionStage<Result> signIn() throws APIException
	{
		JsonNode json = request().body().asJson();
		String emailAddress = json.findPath("emailAddress").textValue();
		String password = json.findPath("password").textValue();
		
		User user = null;
		
		/*
		 * Impersonate login
		 */
		if(emailAddress.contains(":"))
		{
			String masterEmail = emailAddress.split("\\:")[0];
			String targetEmail = emailAddress.split("\\:")[1];
			
			User masterUser = User.findByEmailAddress(masterEmail);
			
			if(masterUser != null && masterUser.getRole() == Role.ADMIN)
			{
				String key = masterUser.getCredentials().getToken() + masterUser.getCredentials().getSecret();
				if(key.equals(password)){
					logger.info("Master login from " + masterEmail + " to " + targetEmail + " suceeed.");
					user = User.findByEmailAddress(targetEmail);
				}
			}else{
				logger.info("Master login from " + masterEmail + " to " + targetEmail + " failed.");
			}
			
		}
		else
		{
			user = User.findByEmailAddressAndPassword(emailAddress, password);
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
			
			
			return json(result.toString());
		} else
			throw APIException.raise(APIErrors.UNAUTHORIZED).setDetailMessage(Messages.get("bad_credentials"));

	}

	@BodyParser.Of(BodyParser.Json.class)
	@ValidateJsonPost(fields = { "firstName", "lastName", "emailAddress", "password" })
	@Tx
	public  CompletionStage<Result> signUp() throws APIException
	{
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


//	@BodyParser.Of(BodyParser.Json.class)
//	@ValidateJsonPost(fields = { "emailAddress" })
//	@Tx
//	public  Result forgotPassword() throws APIException
//	{
//		JsonNode json = request().body().asJson();
//
//		String emailAddress = json.findPath("emailAddress").textValue();
//
//		User user = User.findByEmailAddress(emailAddress);
//		if (user != null)
//		{
//			
//			MicroControllersFactory microControllersFactory =  new MicroControllerFactoryDefault();
//			
//			microControllersFactory.getAccountController().sendPasswordResetEmail(emailAddress, account, resetKey,request().host());
//			
//			return ok();
//		} else
//			throw APIException.raise(APIErrors.ACCOUNT_NOT_FOUND).setDetailMessage("The provided email address doesn't belong to any user in our system");
//
//	}

	

}
